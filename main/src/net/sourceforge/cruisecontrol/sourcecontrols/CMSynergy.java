/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;

import org.apache.log4j.Logger;

/**
 * Checks for modifications made to a Telelogic CM Synergy repository.
 * It does this by examining a provided reference project, getting
 * the tasks from all folders in that project, and checking the 
 * completion time of those tasks against the last build.
 * 
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
 */
public class CMSynergy implements SourceControl {

    private static final Logger LOG = Logger.getLogger(CMSynergy.class);

    /**
     * A collection of properties which will be passed to and set
     * within the builder.
     */
    private Hashtable properties = new Hashtable();

    /**
     * The name of the property which will be set and passed to the
     * builder if any object has changed since the last build.
     */
    private String property = "cc.ccm.haschanged";

    /**
     * The name of the property which will be set and passed to the
     * builder if any objects have been deleted since the last build.
     */
    private String propertyOnDelete = null;
    
    /**
     * The version number delimeter used by the database with which
     * this CM Synergy session is connected.
     */
    private String ccmDelimiter = "-";
    
    /**
     * The URL for your installation of Change Synergy
     */
    private String changeSynergyURL = null;
    
    /**
     * The CCM database with which we wish to connect
     */
    private String ccmDb = null;
    
    /**
     * The CM Synergy executable used for executing commands. If not set,
     * we will use the default value "ccm".
     */
    private String ccmExe = "ccm";
    
    /**
     * The CM Synergy project spec (2 part name) of the project we will
     * use as a template to determine if any new tasks have been completed.
     */
    private String projectSpec = null;
    
    /**
     * If set to true, the contents of the folders contained within the
     * project's reconfigure properties will be updated before we query
     * to find new tasks.
     */
    private boolean updateFolders = true;
    
    /**
     * The ID of the CM Synergy ccmSession we will use to execute commands. If this
     * value is not set, we will defer the decision to the ccm client.
     */
    private String ccmSession = null;
    
    /**
     * The date format as returned by your installation of CM Synergy
     */
    private String ccmDateFormat = "EEE MMM dd HH:mm:ss yyyy"; // Fri Dec  3 17:51:56 2004
    
    /**
     * The number of modified tasks found
     */
    private int numTasks = 0;
    
    /**
     * The number of modified objects found
     */
    private int numObjects = 0;
    
    /**
     * Date format used to create CM Synergy queries 
     */
    public static final SimpleDateFormat TO_CCM_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
    
    /**
     * The environment variable used by CM Synergy to determine
     * which backend ccmSession to use when issuing commands.
     */
    public static final String CCM_SESSION_VAR = "CCM_ADDR";
    
    /**
     * A delimiter used for data values returned from a CM Synergy query
     */
    private static final String CCM_ATTR_DELIMITER = "@#@#@#@";

    /**
     * A delimiter used to mark the end of a multi-lined result from a query
     */
    private static final String CCM_END_OBJECT = "<<<#@#@#>>>";
    
    /**
     * Sets the name of the CM Synergy executable to use when issuing
     * commands.
     * 
     * @param ccmExe the name of the CM Synergy executable
     */
    public void setCcmExe(String ccmExe) {
        this.ccmExe = ccmExe;
    }
    
    /**
     * Sets the CM Synergy project spec to be used as a template for calculating
     * changes. The value set here can be accessed from within the build as the
     * property "cc.ccm.project".
     * 
     * @param projectSpec
     *            The project spec (in 2 part name format).
     */
    public void setProject(String projectSpec) {
        this.projectSpec = projectSpec;
    }
    
    /**
     * Sets the URL for your installation of Change Synergy. This is used to
     * create active links from the modification report to the Change Requests
     * associated with the modified tasks. If not set, the links will not be
     * created. If you wish to use this feature, you must also set the ccmdb
     * attribute to the remote location of the Synergy database.
     * 
     * @param url
     *            The URL of your ChangeSynergy installation
     */
    public void setChangeSynergyURL(String url) {
        this.changeSynergyURL = url;
    }

    /**
     * Sets the remote Synergy database with which to connect. This is only
     * needed if you wish to create active links from the build results page to
     * your installation of Change Synergy. If you set this attribute, you must
     * also set the changesynergyurl attribute.
     * 
     * @param db
     *            The remote Synergy database with which to connect (e.g.
     *            /ccmdb/mydb).
     */
    public void setCcmDb(String db) {
        this.ccmDb = db;
    }
    
    /**
     * Sets the value of the updateFolders attribute. If set to true, the
     * contents of the folders contained within the project's reconfigure
     * properties will be updated before we query to find new tasks.
     * 
     * @param updateFolders
     */
    public void setUpdateFolders(boolean updateFolders) {
        this.updateFolders = updateFolders;
    }
    
    /**
     * Sets the CM Synergy ccmSession ID to use while executing ccm commands. If
     * this value is not set, we will defer the decision to the client. The
     * value set here can be accessed from within the build as the property
     * "cc.ccm.session".
     * 
     * @param ccmSession
     *            The ccmSession ID
     */
    public void setCcmSession(String session) {
        this.ccmSession = session;
    }
    
    /**
     * Sets the date format used by your installation of CM Synergy. The format
     * string should use the syntax described in <code>SimpleDateFormat</code>.
     * The default is "EEE MMM dd HH:mm:ss yyyy" The value set here can be
     * accessed from within the build as the property "cc.ccm.dateformat".
     * 
     * @param format
     *            the date format
     */
    public void setCcmDateFormat(String format) {
        this.ccmDateFormat = format;
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.SourceControl#getProperties()
     */
    public Hashtable getProperties() {
        return properties;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.SourceControl#setProperty(java.lang.String)
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.SourceControl#setPropertyOnDelete(java.lang.String)
     */
    public void setPropertyOnDelete(String property) {
        this.propertyOnDelete = property;
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.SourceControl#validate()
     */
    public void validate() throws CruiseControlException {
        if (projectSpec == null) {
            throw new CruiseControlException("The 'project' attribute is required for CMSynergy.");
        }
        
        // Attempt to get the database delimiter
        ManagedCommandline getDelimiter = createCcmCommand();
        getDelimiter.createArgument().setValue("delimiter");
        try {
            getDelimiter.execute();
            getDelimiter.assertExitCode(0);
            this.ccmDelimiter = getDelimiter.getStdoutAsString().trim();
        } catch (Exception e) {
            StringBuffer buff = new StringBuffer(
                    "Could not connect to provided CM Synergy ccmSession");
            if (ccmSession != null) {
                buff.append(" \"" + ccmSession + "\".");
            }
            throw new CruiseControlException(buff.toString(), e);
        }
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.SourceControl#getModifications(java.util.Date, java.util.Date)
     */
    public List getModifications(Date lastBuild, Date now) {
        LOG.info("Checking for modifications between " + lastBuild.toString()
                + " and " + now.toString());
                
        // If we were asked to update the folders, do so
        if (updateFolders) {
            refreshReconfigureProperties();
        }

        // Create a list of modifications based upon tasks completed
        // since the last build.
        List modifications = getModifiedTasks(lastBuild);
        
        LOG.info("Found " + numObjects + " modified object(s) in " + numTasks
                + " new task(s).");
               
        // Pass to the build any relevent properties 
        properties.put("cc.ccm.project", projectSpec);
        properties.put("cc.ccm.dateformat", ccmDateFormat);
        if (ccmSession != null) {
            properties.put("cc.ccm.session", ccmSession);
        }
        if (numObjects > 0) {
            properties.put(property, "true");
        }
        
        return modifications; 
    }

    /**
     * Update the folders within the given project's reconfigure
     * properties.
     */
    private void refreshReconfigureProperties() {
        ManagedCommandline updateFoldersCommand = createCcmCommand();
        updateFoldersCommand.createArgument().setValue("reconfigure_properties");
        updateFoldersCommand.createArgument().setValue("-refresh");
        updateFoldersCommand.createArgument().setValue(projectSpec);
        try {
            updateFoldersCommand.execute();
            updateFoldersCommand.assertExitCode(0);
        } catch (Exception e) {
            LOG.warn("Could not refresh reconfigure properties for project \""
                    + projectSpec + "\".", e);
        }
    }

    /**
     * Get a list of all tasks which are contained in all folders in the
     * reconfigure properties of the specified project and were completed after
     * the last build.
     * 
     * @return A list of <code>CMSynergyModifications</code> which represent
     *         the new tasks
     */
    private List getModifiedTasks(Date lastBuild) {
        ManagedCommandline cmd = createCcmCommand();
        cmd.createArgument().setValue("query");
        cmd.createArgument().setValue("-u");
        
        // Set up the output format
        cmd.createArgument().setValue("-f");
        cmd.createArgument().setValue(
                "%displayname" + CCM_ATTR_DELIMITER +      // 0
                "%release" + CCM_ATTR_DELIMITER +          // 1
                "%owner" + CCM_ATTR_DELIMITER +            // 2
                "%completion_date" + CCM_ATTR_DELIMITER +  // 3
                "%task_synopsis" + CCM_END_OBJECT);        // 4
        
        // Construct the query string
        cmd.createArgument().setValue(
                "is_task_in_folder_of(is_folder_in_rp_of('" 
                + projectSpec 
                + ":project:1')) and completion_date>time('"
                + TO_CCM_DATE.format(lastBuild)
                + "')");
        
        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            LOG.error("Could not query for new tasks. The modification list "
                    + "will be empty!", e);
        }

        //create a modification list with discovered tasks
        List modificationList = new ArrayList();
        Iterator tasks = format(cmd.getStdoutAsList()).iterator();
        while (tasks.hasNext()) {
            numTasks++;
            String[] attributes = tokeniseEntry((String) tasks.next(), 5);
            if (attributes == null) {
                LOG.warn("Could not determine attributes for at least one "
                        + "discovered task! The modification set is suspect.");
                continue;
            }
            CMSynergyModification mod = new CMSynergyModification();
            mod.taskNumber = attributes[0];
            mod.revision = attributes[1];
            mod.userName = attributes[2];
            mod.modifiedTime = getDateFromSynergy(attributes[3]);
            mod.comment = attributes[4];
            
            // Populate the included files by quering for objects in the task
            getModifiedObjects(mod);
            
            // Find any Change Requests with which the task is associated
            getAssociatedCRs(mod);

            // Add the modification to the list
            modificationList.add(mod);
        }
        
        return modificationList;
    }

    private String[] tokeniseEntry(String line, int maxTokens) {
        int minTokens = maxTokens - 1; // comment may be absent.
        String[] tokens  = new String[maxTokens];
        Arrays.fill(tokens, "");
        int tokenIndex = 0;
        for (int oldIndex = 0, index = line.indexOf(CCM_ATTR_DELIMITER, 0); 
             true; 
             oldIndex = index + CCM_ATTR_DELIMITER.length(), 
                 index = line.indexOf(CCM_ATTR_DELIMITER, oldIndex), tokenIndex++) {
            if (tokenIndex > maxTokens) {
                LOG.debug("Too many tokens; skipping entry");
                return null;
            }
            if (index == -1) {
                tokens[tokenIndex] = line.substring(oldIndex);
                break;
            } else {
                tokens[tokenIndex] = line.substring(oldIndex, index);
            }
        }
        if (tokenIndex < minTokens) {
            LOG.debug("Not enough tokens; skipping entry");
            return null;
        }
        return tokens;
    }
    
    
    /**
     * Populate the object list of a Modification by quering for objects
     * associated with the task.
     */
    private void getModifiedObjects(CMSynergyModification mod) {    
        
        ManagedCommandline cmd = createCcmCommand();
        cmd.createArgument().setValue("query");
        cmd.createArgument().setValue("-u");
            
        // Set up the output format
        cmd.createArgument().setValue("-f");
        cmd.createArgument().setValue(
                "%name" + CCM_ATTR_DELIMITER +      // 0
                "%version" + CCM_ATTR_DELIMITER +   // 1
                "%type" + CCM_ATTR_DELIMITER +      // 2
                "%instance" + CCM_ATTR_DELIMITER +  // 3
                "%project" + CCM_ATTR_DELIMITER +   // 4
                "%comment" + CCM_END_OBJECT);       // 5
            
        // Construct the query string
        cmd.createArgument().setValue(
                "is_associated_object_of('" 
                + "task" 
                + mod.taskNumber 
                + ccmDelimiter
                + "1:task:probtrac')"); 
            
        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            LOG.warn("Could not query for objects in task \"" + mod.taskNumber 
                    + "\". The modification list will be incomplete!", e);
        }
        
        // Populate the modification with the object data from the task
        Iterator objects = format(cmd.getStdoutAsList()).iterator();
        while (objects.hasNext()) {
            numObjects++;
            String object = (String) objects.next();
            String[] attributes = tokeniseEntry(object, 6);
            if (attributes == null) {
                LOG.warn("Could not determine attributes for object associated "
                        + "with task \"" + mod.revision + "\".");
                continue;
            }
            // Add each object to the CMSynergyModification
            mod.createModifiedObject(attributes[0], attributes[1],
                    attributes[2], attributes[3], attributes[4], attributes[5]);
        }   
    }

    /**
     * Queries the CM Synergy repository to find any Change Requests with which
     * a task is associated. If the Change Synergy URL and database were provided,
     * we will add HTML based links to those CRs.
     * 
     * @param mod The modification object
     */
    private void getAssociatedCRs(CMSynergyModification mod) {
        ManagedCommandline cmd = createCcmCommand();
        cmd.createArgument().setValue("query");
        cmd.createArgument().setValue("-u");
        
        // Set up the output format
        cmd.createArgument().setValue("-f");
        cmd.createArgument().setValue("%displayname");
        
        // Construct the query string
        cmd.createArgument().setValue(
                "cvtype='problem' and has_associated_task('task"
                + mod.taskNumber
                + ccmDelimiter
                + "1:task:probtrac')");
        
        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            LOG.warn("Could not query for associated CRs. The modification list "
                    + "may be incomplete!", e);
        }

        // Add the Change Request(s) to the modification
        List crList = cmd.getStdoutAsList();
        if (crList != null) {
            Iterator crs = crList.iterator();
            while (crs.hasNext()) {
                String crNum = ((String) crs.next()).trim();
                CMSynergyModification.ChangeRequest cr = mod.createChangeRequest(crNum);
                if (changeSynergyURL != null && ccmDb != null) {
                    StringBuffer href = new StringBuffer(changeSynergyURL);
                    href.append("/servlet/com.continuus.webpt.servlet.PTweb?");
                    href.append("ACTION_FLAG=frameset_form&#38;TEMPLATE_FLAG=ProblemReportView&#38;database=");
                    href.append(ccmDb);
                    href.append("&#38;role=User&#38;problem_number=");
                    href.append(crNum);
                    cr.href = href.toString();
                }
            }
        }               
    }
    
    /**
     * Format the output of a CM Synergy query by removing
     * newlines introduced by comments.
     * 
     * @param in The <code>List</code> to be formated
     * @return The formated <code>List</code>
     */
    private List format(List in) {
        // Concatenate output lines until we hit the end of
        // object delimiter.
        List out = new ArrayList();
        Iterator it = in.iterator();
        StringBuffer buff = new StringBuffer();
        while (it.hasNext()) {
            buff.append((String) it.next());
            int index = buff.toString().lastIndexOf(CCM_END_OBJECT);
            if (index > -1) {
                buff.delete(index, buff.length());
                out.add(buff.toString());
                buff = new StringBuffer();
            }
        }   
        return out;
    }

    /**
     * Parse a CM Synergy date string into a Java <code>Date</code>. If the
     * string cannot be parsed, a warning is written to the log, and the current
     * date is returned.
     * 
     * @param dateString
     *            the date string to parse
     * @return The date
     * 
     * @see #setCcmDateFormat(String)
     */
    private Date getDateFromSynergy(String dateString) {
        SimpleDateFormat fromCcmDate = new SimpleDateFormat(ccmDateFormat);
        Date date = null;
        try {
            date = fromCcmDate.parse(dateString);
        } catch (ParseException e) {
            LOG.warn("Could not parse CM Synergy date \"" + dateString
                    + "\" into Java Date using format \"" + ccmDateFormat
                    + "\".", e);
            date = new Date();
        }
        return date;
    }
    
    /**
     * Creates a <code>ManagedCommandline</code> configured to run
     * CM Synergy commands.
     * 
     * @return A ManagedCommandline configured for CM Synergy
     */
    private ManagedCommandline createCcmCommand() {
        // Create a managed command line
        ManagedCommandline cmd = new ManagedCommandline(ccmExe);
        
        // If we were given a ccmSession ID, use it
        if (ccmSession != null) {
            cmd.setVariable(CCM_SESSION_VAR, ccmSession);
        }
        
        return cmd;
    }
}
