/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.ExamplesFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Checks for modifications made to a Telelogic CM Synergy repository. It does
 * this by examining a provided reference project, getting the tasks from all
 * folders in that project, and checking the completion time of those tasks
 * against the last build.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
@DescriptionFile
@ExamplesFile
public class CMSynergy implements SourceControl {

    private static final long serialVersionUID = -65675636345070572L;

    /**
     * A delimiter used for data values returned from a CM Synergy query
     */
    public static final String CCM_ATTR_DELIMITER = "@#@#@#@";

    /**
     * A delimiter used to mark the end of a multi-lined result from a query
     */
    private static final String CCM_END_OBJECT = "<<<#@#@#>>>";

    /**
     * The default CM Synergy command line client executable
     */
    public static final String CCM_EXE = "ccm";

    /**
     * The environment variable used by CM Synergy to determine which backend
     * ccmSession to use when issuing commands.
     */
    private static final String CCM_SESSION_VAR = "CCM_ADDR";

    /**
     * The default CM Synergy session map file
     */
    public static final String CCM_SESSION_FILE = System.getProperty("user.home") + File.separator + ".ccmsessionmap";

    /**
     * An instance of the logging class
     */
    private static final Logger LOG = Logger.getLogger(CMSynergy.class);

    /**
     * A collection of properties which will be passed to and set within the
     * builder.
     */
    private final SourceControlProperties properties = new SourceControlProperties();

    /**
     * The name of the property which will be set and passed to the builder if
     * any object has changed since the last build.
     */
    private String property = "cc.ccm.haschanged";

    /**
     * The version number delimeter used by the database with which this CM
     * Synergy session is connected.
     */
    private String ccmDelimiter = "-";

    /**
     * The URL for your installation of Change Synergy
     */
    private String changeSynergyURL;

    /**
     * The CCM database with which we wish to connect
     */
    private String ccmDb;

    /**
     * The CM Synergy executable used for executing commands. If not set, we
     * will use the default value "ccm".
     */
    private String ccmExe;

    /**
     * The CM Synergy project spec (2 part name).
     */
    private String projectSpec;

    /**
     * The instance number of the project. This is almost always "1", but might
     * need to be overridden if you are using DCM?
     */
    private String projectInstance = "1";

    /**
     * The CM Synergy project four part name we will use as a template to
     * determine if any new tasks have been completed.
     */
    private String projectFourPartName;

    /**
     * If set to true, the contents of the folders contained within the
     * project's reconfigure properties will be updated before we query to find
     * new tasks.
     */
    private boolean updateFolders = true;

    /**
     * The file which contains the mapping between CM Synergy session names and
     * IDs.
     */
    private File sessionFile;

    /**
     * The name of the CM Synergy session to use.
     */
    private String sessionName;

    /**
     * The date format as returned by your installation of CM Synergy.
     */
    private String ccmDateFormat = "EEE MMM dd HH:mm:ss yyyy"; // Fri Dec 3

    // 17:51:56 2004

    /**
     * If set to true, the project will be reconfigured when changes are
     * detected.
     */
    private boolean reconfigure = false;

    /**
     * Used in conjunction with reconfigure. If set to true, all subprojects
     * will be reconfigured when changes are detected.
     */
    private boolean recurse = true;

    /**
     * If set to true, the time a task came into a reconfigure folder is used
     * to determine modified tasks instead of the tasks completion time. Works
     * for Synergy 6.3SP1 and newer only.
     */
    private boolean useBindTime = false;

    /**
     * If set to true, the work area location will not be queried and passed to
     * the builder.
     */
    private boolean ignoreWorkarea = false;

    /**
     * The locale used for parsing dates.
     */
    private Locale locale;

    /**
     * The language used to set the locale for parsing CM Synergy dates.
     */
    private String language = "en";

    /**
     * A reusable commandline for issuing CM Synergy commands
     */
    private ManagedCommandline cmd;

    /**
     * The country used to set the locale for parsing CM Synergy dates.
     */
    private String country = "US";

    /**
     * The number of modified tasks found
     */
    private int numTasks;

    /**
     * The number of modified objects found
     */
    private int numObjects;

    /**
     * Sets the name of the CM Synergy executable to use when issuing commands.
     *
     * @param ccmExe
     *            the name of the CM Synergy executable
     */
    @Description("The name of the CM Synergy command line client. If not provided, the "
            + "plugin will search the system path for an executable called \"ccm\" (or "
            + "\"ccm.exe\" for Windows).")
    @Optional
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
    @Description("The project spec (two part name) of the CM Synergy project in which "
            + "you wish to search for changes.")
    @Required
    public void setProject(String projectSpec) {
        this.projectSpec = projectSpec;
    }

    /**
     * Sets the project's instance value. This value will be used in any query
     * which involves the project. Defaults to "1". This default should work for
     * most people. You might, however, need to override this value when using
     * DCM?
     *
     * @param projectInstance
     *            The instance number of the project.
     */
    @Description("Used to set the project's instance value. As CM Synergy only allows a "
            + "single instance of a project object in any given database, this attribute "
            + "defaults to \"1\", and should not need to be changed by most users. "
            + "<strong>You might, however, need to set this value when using the DCM "
            + "(Distributed Change Management) feature of the tool - which appends the DB "
            + "name to the instance value.</strong>")
    @Optional
    public void setInstance(String projectInstance) {
        this.projectInstance = projectInstance;
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
    @Description("If provided, an active link will be created from the build results web "
            + "page to any change requests associated with any new tasks. The format "
            + "should be \"http://server:port\". If you wish to use this option, you must "
            + "also set the \"ccmdb\" attribute.")
    @Optional
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
    @Description("Used in conjunction with changesynergyurl. This should be set to the "
            + "location of the database on the CM Synergy server. (e.g. \"/ccmdb/mydb\")")
    @Optional
    public void setCcmDb(String db) {
        this.ccmDb = db;
    }

    /**
     * Sets the value of the updateFolders attribute.
     * @param updateFolders If set to true, the
     * contents of the folders contained within the project's reconfigure
     * properties will be updated before we query to find new tasks.
     */
    @Description("By default, the plugin will always refresh the reconfigure properties of "
            + "the given project before searching for changes. This allows any query based "
            + "folders to update themselves with new tasks. If you wish to disable this "
            + "feature (not recommended), you may set this attribute to false. <strong>This "
            + "feature will only work with CM Synergy version 6.3 and above. If you are "
            + "using an older version, you <em>must</em> set this option to false. In this "
            + "case, you'll want to use the <a href=\"#cmsynergybootstrapper\">&lt;"
            + "cmsynergybootstrapper&gt;</a> as a workaround. Please see example 2 below "
            + "as well as the Wiki site for more information.</strong>")
    @Optional
    public void setUpdateFolders(boolean updateFolders) {
        this.updateFolders = updateFolders;
    }

    /**
     * Sets the file which contains the mapping between CM Synergy session names
     * and IDs. This file should be in the standard properties file format. Each
     * line should map one name to a CM Synergy session ID (as returned by the
     * "ccm status" command).
     * <p>
     * example: <br>
     * <br>
     * session1=localhost:65024:192.168.1.17
     *
     * @param sessionFile
     *            The session file
     */
    @Description("The session file used by the <a href=\"#cmsynergysessionmonitor\">&lt;"
            + "cmsynergysessionmonitor&gt;</a> to persist your CM Synergy session "
            + "information. If this attribute is not set, it defaults to the file "
            + "\".ccmsessionmap\" in your home directory.")
    @Optional
    public void setSessionFile(String sessionFile) {
        this.sessionFile = new File(sessionFile);
    }

    /**
     * Sets the name of the CM Synergy session to use with this plugin. This
     * name should appear in the specified session file.
     *
     * @param sessionName
     *            The session name
     *
     * @see #setSessionFile(String)
     */
    @Description("The session name of the CM Synergy session you wish to use. This name "
            + "must appear in the session file. If not set, the plugin will attempt to "
            + "use the default (current) session as returned by the \"ccm status\" command.")
    @Optional
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
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
    @Description("The default (output) date format used by CM Synergy is \"EEE MMM dd "
            + "HH:mm:ss yyyy\" If you have customized this for your installation, you "
            + "must provide the correct format here. The format should follow the standard "
            + "defined in Java's SimpleDateFormat class.")
    @Optional
    public void setCcmDateFormat(String format) {
        this.ccmDateFormat = format;
    }

    /**
     * Sets the value of the reconfigure attribute.
     * @param reconfigure If set to true, the project
     * will be reconfigured when changes are detected. Default value is false.
     */
    @Description("Disabled by default. If you set this option to true, the project (and "
            + "optionally any subprojects) will be automatically reconfigured when changes "
            + "are detected. This eliminates the need to handle this from within your build "
            + "scripts.")
    @Optional
    @Default("false")
    public void setReconfigure(boolean reconfigure) {
        this.reconfigure = reconfigure;
    }

    /**
     * Sets the value of the useBindtime attribute.
     * @param useBindTime If set to true, the time the
     * task came into the reconfigure folders is used to query the modifications
     * instead of the time the task was completed. Works
     * for Synergy 6.3SP1 and newer only.
     * Default value is false.
     */
    @Description("If set to true, the time a task came into the reconfigure folder is used "
            + "to determine modified tasks instead of the tasks completion date. This is a "
            + "more precise query to find modifications since the last build when the "
            + "reconfiguration rules are based on other criteria e.g. the status of a "
            + "Change Synergy change request. <strong>This feature will only work with CM "
            + "Synergy version 6.3SP1 and above. If you are using an older version, you "
            + "<em>must</em> set this option to false (default).</strong>")
    @Optional
    public void setUseBindTime(boolean useBindTime) {
        this.useBindTime = useBindTime;
    }

    /**
     * Sets the value of the recurse attribute. Used in conjuction with the
     * reconfigure attribute.
     * @param recurse If set to true, all subprojects will also be
     * reconfigured when changes are detected. Default is true.
     */
    @Description("Used in conjunction with the reconfigure option. If set to true (which is "
            + "the default) all subprojects will also be reconfigured.")
    @Optional
    @Default("true")
    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }

    /**
     * Sets the value of the ignoreWorkarea attribute.
     * @param ignoreWorkarea If set to true, we will
     * not attempt to determine the location of the project's workarea, nor will
     * we pass the cc.ccm.workarea attribute to the builders. Default is false.
     */
    @Description("By default, the plugin will query CM Synergy to determine the work area "
            + "location of the project. This location is then passed to the builders in "
            + "the property \"cc.ccm.workarea\". If you wish to disable this feature (not "
            + "recommended), you can set this attribute to <em>true</em>.")
    @Optional
    public void setIgnoreWorkarea(boolean ignoreWorkarea) {
        this.ignoreWorkarea = ignoreWorkarea;
    }

    /**
     * Sets the language used to create the locale for parsing CM Synergy dates.
     * The format should follow the ISO standard as specified by
     * <code>java.util.Locale</code>. The default is "en" (English).
     *
     * @param language
     *            The language to use when creating the <code>Locale</code>
     */
    @Description("If you have a non U.S. English installation of CM Synergy, you may "
            + "specify the ISO language code here. (e.g. fr, de, etc.)")
    @Optional
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Sets the country used to create the locale for parsing CM Synergy dates.
     * The format should follow the ISO standard as specified by
     * <code>java.util.Locale</code>. The default is "US" (United States).
     *
     * @param country
     *            The ISO country code to use
     */
    @Description("If you have a non U.S. English installation of CM Synergy, you may "
            + "specify the ISO country code here. (e.g. FR, DE, etc.)")
    @Optional
    public void setCountry(String country) {
        this.country = country;
    }

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    @Description("Added for compliance with the CruiseControl API. A property of this "
            + "name will be provided to the builders if any CM Synergy object has changed "
            + "since the last build. The default is cc.ccm.haschanged, and probably "
            + "shouldn't be altered.")
    @Optional
    @Default("cc.ccm.haschanged")
    public void setProperty(String property) {
        this.property = property;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(projectSpec, "project", this.getClass());
    }

    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        // Create a Locale appropriate for this installation
        locale = new Locale(language, country);
        if (!locale.equals(Locale.US)) {
            LOG.info("Locale has been set to " + locale.toString());
        }

        // Attempt to get the database delimiter
        cmd = createCcmCommand(ccmExe, sessionName, sessionFile);
        cmd.createArgument("delimiter");
        try {
            cmd.execute();
            cmd.assertExitCode(0);
            this.ccmDelimiter = cmd.getStdoutAsString().trim();
        } catch (Exception e) {
            final StringBuilder message = new StringBuilder("Could not connect to provided CM Synergy session: ");
            message.append(sessionName).append(" with session file:").append(sessionFile.getAbsolutePath());
            LOG.error(message.toString(), e);
            throw new OperationFailedException(message.toString(), e);
        }

        // Create the projectFourPartName needed for projects with instance
        // other than 1 to reconfigure properly
        projectFourPartName = projectSpec + ":project:" + projectInstance;

        LOG.info("Checking for modifications between " + lastBuild.toString() + " and " + now.toString());

        // If we were asked to update the folders, do so
        if (updateFolders) {
            refreshReconfigureProperties();
        }

        // Create a list of modifications based upon tasks completed
        // since the last build.
        numObjects = 0;
        numTasks = 0;
        final List<Modification> modifications = getModifiedTasks(lastBuild);

        LOG.info("Found " + numObjects + " modified object(s) in " + numTasks + " new task(s).");

        // If we were asked to reconfigure the project, do so
        if (reconfigure && (numObjects > 0)) {
            reconfigureProject();
        }

        // Pass to the build any relevent properties
        properties.put("cc.ccm.project", projectFourPartName);
        properties.put("cc.ccm.dateformat", ccmDateFormat);
        String sessionID = cmd.getVariable(CCM_SESSION_VAR);
        if (sessionID != null) {
            properties.put("cc.ccm.session", sessionID);
        }
        if (numObjects > 0) {
            properties.put(property, "true");
        }
        if (!ignoreWorkarea) {
            properties.put("cc.ccm.workarea", getWorkarea());
        }

        return modifications;
    }

    /**
     * Update the folders within the given project's reconfigure properties.
     */
    private void refreshReconfigureProperties() {
        LOG.debug("Refreshing reconfigure properties for project " + projectFourPartName + ".");

        // Construct the CM Synergy command
        cmd.clearArgs();
        cmd.createArgument("reconfigure_properties");
        if (recurse) {
            cmd.createArgument("-recurse");
        }
        cmd.createArguments("-refresh", projectFourPartName);
        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            String message = "Could not refresh reconfigure properties for project \"" + projectFourPartName + "\".";
            LOG.error(message, e);
            throw new OperationFailedException(message, e);
        }
    }

    /**
     * Get a list of all tasks which are contained in all folders in the
     * reconfigure properties of the specified project and were completed after
     * the last build. If useBindTime is <code>true</code> not the completion time of
     * the task is considered but the time the task came into the folder.
     *
     * @param lastBuild date of last build
     * @return A list of <code>CMSynergyModifications</code> which represent
     *         the new tasks
     */
    private List<Modification> getModifiedTasks(final Date lastBuild) {

        // The format used for converting Java dates into CM Synergy dates
        // Note that the format used to submit commands differs from the
        // format used in the results of that command!?!
        final SimpleDateFormat toCcmDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", locale);
        boolean isBaselineBasedProject = false;
        
        // Determine if the project has a System Testing purpose
        cmd.clearArgs();
        cmd.createArgument("prop");
        cmd.createArgument("-p");
        cmd.createArgument(projectFourPartName);
        cmd.createArgument("-f");
        cmd.createArgument("%purpose");
        try {
            cmd.execute();
        } catch (Exception e) {
            String message = "Could not query for new tasks.";
            LOG.error(message, e);
            throw new OperationFailedException(message, e);
        }

        // Determine if the project is either of a System Testing or
        // Insulated Development purpose. If it is, set the appropriate flag
        if (cmd.getStdoutAsString().startsWith("System Testing")) {
            isBaselineBasedProject = true;
        } else if (cmd.getStdoutAsString().startsWith("Insulated Development")) {
            isBaselineBasedProject = true;
        }
        
        // get the release of the project
        final String release = getProjectRelease(projectFourPartName);
        final String latestBaseline = getLatestBaseline(release);
        
        // Construct the CM Synergy command
        cmd.clearArgs();
        cmd.createArgument("query");
        cmd.createArgument("-u");

        // Set up the output format
        cmd.createArgument("-f");
        cmd.createArgument("%displayname" + CCM_ATTR_DELIMITER + // 0
                "%release" + CCM_ATTR_DELIMITER + // 1
                "%owner" + CCM_ATTR_DELIMITER + // 2
                "%completion_date" + CCM_ATTR_DELIMITER + // 3
                "%task_synopsis" + CCM_END_OBJECT); // 4

        // Construct the query string
        if ((useBindTime & (!isBaselineBasedProject))) {
                cmd.createArgument(
                        "is_task_in_folder_of(is_folder_in_rp_of('" + projectFourPartName
                                + "'), '>', time('"
                                + toCcmDate.format(lastBuild) + "'))");
        } else if ((!isBaselineBasedProject)) {
                cmd.createArgument(
                        "is_task_in_folder_of(is_folder_in_rp_of('" + projectFourPartName
                                + "')) and completion_date>time('"
                                + toCcmDate.format(lastBuild) + "')");
        } else if ((useBindTime & isBaselineBasedProject)) {
                cmd.createArgument(
                        "is_task_in_baseline_of('" + latestBaseline
                        + "'), '>', time('"
                        + toCcmDate.format(lastBuild) 
                        + "') or is_task_in_folder_of(is_folder_in_rp_of('" 
                        + projectFourPartName + "'), '>', time('"
                        + toCcmDate.format(lastBuild) + "'))");
        } else {
                cmd.createArgument(
                        "is_task_in_baseline_of('" + latestBaseline 
                        + "') and completion_date>time('"
                        + toCcmDate.format(lastBuild) 
                        + "') or is_task_in_folder_of(is_folder_in_rp_of('" 
                        + projectFourPartName + "')) and completion_date>time('"
                        + toCcmDate.format(lastBuild) + "')");
        }

        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            String message = "Could not query for new tasks.";
            LOG.error(message, e);
            throw new OperationFailedException(message, e);
        }

        // create a modification list with discovered tasks
        final List<Modification> modificationList = new ArrayList<Modification>();
        for (final String entry : format(cmd.getStdoutAsList())) {
            numTasks++;
            final String[] attributes = tokeniseEntry(entry, 5);
            if (attributes == null) {
                LOG.warn("Could not determine attributes for at least one "
                        + "discovered task! The modification set is suspect.");
                continue;
            }
            final CMSynergyModification mod = new CMSynergyModification();
            mod.taskNumber = attributes[0];
            mod.revision = attributes[1];
            mod.userName = attributes[2];
            mod.modifiedTime = getDateFromSynergy(attributes[3]);
            mod.comment = attributes[4];

            // Populate the included files by quering for objects in the entry
            getModifiedObjects(mod);

            // Find any Change Requests with which the entry is associated
            getAssociatedCRs(mod);

            // Add the modification to the list
            modificationList.add(mod);
        }

        return modificationList;
    }
    
    /**
     * Get the latest baseline name for a given release value
     * @param release
     *     The release value for which to find the latest baseline
     * @return The name of the latest Synergy baseline for the supplied release value
     */
    
    private String getLatestBaseline(String release) {
        // Construct the CM Synergy command
        cmd.clearArgs();
        cmd.createArgument("query");
        cmd.createArgument("-u");
        cmd.createArgument("-ns");
        
        // Set up the output format
        cmd.createArgument("-f");
        cmd.createArgument("%objectname");

        // Construct the query string
        cmd.createArgument("status='published_baseline' and release='" + release + "'");

        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            String message = "Could not query for latest baseline";
            LOG.error(message, e);
            throw new OperationFailedException(message, e);
        }
        String baselineString = cmd.getStdoutAsString();
        String[] baselineList = baselineString.split("\r\n|\r|\n");
        return baselineList[baselineList.length - 1].trim();
    }
    
    /**
     * Get the Synergy Release value for a given project
     * @param projectFourPartName
     *      The project whose release you wish to obtain
     * @return The release value for the given project
     */
    
    private String getProjectRelease(String projectFourPartName) {
        
        cmd.clearArgs();
        cmd.createArgument("prop");
        cmd.createArgument("-p");
        cmd.createArgument(projectFourPartName);
        cmd.createArgument("-f");
        cmd.createArgument("%release");
        try {
            cmd.execute();
        } catch (Exception e) {
            String message = "Could not get project release";
            LOG.error(message, e);
            throw new OperationFailedException(message, e);
        }
        String[] release = cmd.getStdoutAsString().split("\r\n|\r|\n");
        return release[0].trim();
    }

    /**
     * Split the results of a CM Synergy query into individual tokens. This
     * method was added for compatibility with the 1.3 JRE.
     *
     * @param line
     *            The line to be tokenised.
     * @param maxTokens
     *            The maximum number of tokens in the line
     *
     * @return The tokens found
     */
    private String[] tokeniseEntry(String line, int maxTokens) {
        int minTokens = maxTokens - 1; // comment may be absent.
        String[] tokens = new String[maxTokens];
        Arrays.fill(tokens, "");
        int tokenIndex = 0;
        for (int oldIndex = 0, index = line.indexOf(CCM_ATTR_DELIMITER, 0); true; oldIndex = index
                + CCM_ATTR_DELIMITER.length(), index = line.indexOf(CCM_ATTR_DELIMITER, oldIndex), tokenIndex++) {
            if (tokenIndex > maxTokens) {
                LOG.debug("Too many tokens; skipping entry");
                return null;
            }
            if (index == -1) {
                tokens[tokenIndex] = line.substring(oldIndex);
                break;
            }

            tokens[tokenIndex] = line.substring(oldIndex, index);
        }
        if (tokenIndex < minTokens) {
            LOG.debug("Not enough tokens; skipping entry");
            return null;
        }
        return tokens;
    }

    /**
     * Populate the object list of a Modification by querying for objects
     * associated with the task.
     * @param mod the modification object to be populated
     */
    private void getModifiedObjects(CMSynergyModification mod) {
        // Construct the CM Synergy command
        cmd.clearArgs();
        cmd.createArgument("task");
        cmd.createArguments("-show", "objects");

        // Set up the output format
        cmd.createArgument("-f");
        cmd.createArgument("%name" + CCM_ATTR_DELIMITER + // 0
                "%version" + CCM_ATTR_DELIMITER + // 1
                "%type" + CCM_ATTR_DELIMITER + // 2
                "%instance" + CCM_ATTR_DELIMITER + // 3
                "%project" + CCM_ATTR_DELIMITER + // 4
                "%comment" + CCM_END_OBJECT); // 5

        // Construct the query string
        cmd.createArgument(mod.taskNumber);

        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            LOG.warn("Could not query for objects in task \"" + mod.taskNumber
                    + "\". The modification list will be incomplete!", e);
        }

        // Populate the modification with the object data from the task
        for (final String object : format(cmd.getStdoutAsList())) {
            numObjects++;
            final String[] attributes = tokeniseEntry(object, 6);
            if (attributes == null) {
                LOG.warn("Could not determine attributes for object associated " + "with task \"" + mod.revision
                        + "\".");
                continue;
            }
            // Add each object to the CMSynergyModification
            mod.createModifiedObject(attributes[0], attributes[1], attributes[2], attributes[3], attributes[4],
                    attributes[5]);
        }
    }

    /**
     * Queries the CM Synergy repository to find any Change Requests with which
     * a task is associated. If the Change Synergy URL and database were
     * provided, we will add HTML based links to those CRs.
     *
     * @param mod
     *            The modification object
     */
    private void getAssociatedCRs(CMSynergyModification mod) {
        // Construct the CM Synergy command
        cmd.clearArgs();
        cmd.createArgument("query");
        cmd.createArgument("-u");

        // Set up the output format
        cmd.createArguments("-f", "%displayname" + CCM_ATTR_DELIMITER + "%change_request_synopsis");

        // Construct the query string
        cmd.createArgument(
                "cvtype='problem' and has_associated_task('task" + mod.taskNumber + ccmDelimiter + "1:task:probtrac')");

        // Execute the command
        try {
            cmd.execute();
        } catch (Exception e) {
            LOG.warn("Could not query for associated CRs. The modification list " + "may be incomplete!", e);
        }

        // Add the Change Request(s) to the modification
        final List<String> crList = cmd.getStdoutAsList();
        if (crList != null) {
            for (final String aCrList : crList) {
                final String[] crDetails = tokeniseEntry(aCrList, 2);
                final String crNum = crDetails[0];
                final String crSynopsis = crDetails[1].trim();
                final CMSynergyModification.ChangeRequest cr = mod.createChangeRequest(crNum, crSynopsis);
                if (changeSynergyURL != null && ccmDb != null) {
                    final StringBuilder href = new StringBuilder(changeSynergyURL);
                    href.append("/servlet/com.continuus.webpt.servlet.PTweb?");
                    href.append("ACTION_FLAG=frameset_form&TEMPLATE_FLAG=ProblemReportView&database=");
                    href.append(ccmDb);
                    href.append("&role=User&problem_number=");
                    href.append(crNum);
                    cr.href = href.toString();
                }
            }
        }
    }

    /**
     * Determine the work area location for the specified project.
     *
     * @return The work area location
     */
    private String getWorkarea() {
        String defaultWorkarea = ".";

        // Get the literal workarea from Synergy
        cmd.clearArgs();
        cmd.createArgument("attribute");
        cmd.createArguments("-show", "wa_path");
        cmd.createArguments("-project", projectFourPartName);

        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            LOG.warn("Could not determine the workarea location for project \"" + projectFourPartName + "\".", e);
            return defaultWorkarea;
        }

        // The command will return the literal work area, but what we are
        // really interested in is the top level directory within that work
        // area.
        File workareaPath = new File(cmd.getStdoutAsString().trim());
        if (!workareaPath.isDirectory()) {
            LOG.warn("The workarea reported by Synergy does not exist or is not accessible by this session - \""
                    + workareaPath.toString() + "\".");
            return defaultWorkarea;
        }
        String[] dirs = workareaPath.list();
        if (dirs.length != 1) {
            LOG.warn("The workarea reported by Synergy is invalid - \"" + workareaPath.toString() + "\".");
            return defaultWorkarea;
        }

        // Found it!
        return workareaPath.getAbsolutePath() + File.separator + dirs[0];
    }

    /**
     * Reconfigure the project
     */
    private void reconfigureProject() {
        LOG.debug("Reconfiguring project " + projectFourPartName + ".");

        // Construct the CM Synergy command
        cmd.clearArgs();
        cmd.createArgument("reconfigure");
        if (recurse) {
            cmd.createArgument("-recurse");
        }
        cmd.createArguments("-project", projectFourPartName);

        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            String message = "Could not reconfigure project \"" + projectFourPartName + "\".";
            LOG.error(message, e);
            throw new OperationFailedException(message, e);
        }
    }

    /**
     * Format the output of a CM Synergy query by removing newlines introduced
     * by comments.
     *
     * @param in
     *            The <code>List</code> to be formated
     * @return The formated <code>List</code>
     */
    private List<String> format(final List<String> in) {
        // Concatenate output lines until we hit the end of object delimiter.
        List<String> out = new ArrayList<String>();
        Iterator it = in.iterator();
        StringBuilder buff = new StringBuilder();
        while (it.hasNext()) {
            buff.append((String) it.next());
            int index = buff.toString().lastIndexOf(CCM_END_OBJECT);
            if (index > -1) {
                buff.delete(index, buff.length());
                out.add(buff.toString());
                buff = new StringBuilder();
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
        SimpleDateFormat fromCcmDate = new SimpleDateFormat(ccmDateFormat, locale);
        Date date;
        try {
            date = fromCcmDate.parse(dateString);
        } catch (ParseException e) {
            LOG.warn("Could not parse CM Synergy date \"" + dateString + "\" into Java Date using format \""
                    + ccmDateFormat + "\".", e);
            date = new Date();
        }
        return date;
    }

    /**
     * Given a CM Synergy session name, looks up the corresponding session ID.
     *
     * @param sessionName
     *            The CM Synergy session name
     * @param sessionFile
     *            The session map file
     * @return The session ID.
     * @throws CruiseControlException if something bad happens.
     */
    private static String getSessionID(String sessionName, File sessionFile) throws CruiseControlException {

        // If no session file was provided, try to use the default
        if (sessionFile == null) {
            sessionFile = new File(CCM_SESSION_FILE);
        }

        // Load the persisted session information from file
        Properties sessionProperties;
        try {
            sessionProperties = Util.loadPropertiesFromFile(sessionFile);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }

        // Look up and return the full session ID
        return sessionProperties.getProperty(sessionName);
    }

    /**
     * Creates a <code>ManagedCommandline</code> configured to run CM Synergy
     * commands.
     *
     * @param ccmExe
     *            Full path of the CM Synergy command line client (or
     *            <code>null</code> to use the default).
     * @param sessionName
     *            The name of the session as stored in the map file (or
     *            <code>null</code> to use the default session).
     * @param sessionFile
     *            The CM Synergy session map file (or <code>null</code> to use
     *            the default).
     * @return A configured <code>ManagedCommandline</code>
     */
    @SkipDoc
    public static ManagedCommandline createCcmCommand(String ccmExe, String sessionName, File sessionFile) {

        // If no executable name was provided, use the default
        if (ccmExe == null) {
            ccmExe = CCM_EXE;
        }

        // Attempt to get the appropriate CM Synergy session
        String sessionID = null;
        if (sessionName != null) {
            try {
                sessionID = getSessionID(sessionName, sessionFile);
                if (sessionID == null) {
                    LOG.error("Could not find a session ID for CM Synergy session named \"" + sessionName
                            + "\". Attempting to use the default (current) session.");
                }
            } catch (CruiseControlException e) {
                LOG.error("Failed to look up CM Synergy session named \"" + sessionName
                        + "\". Attempting to use the default (current) session.", e);
            }
        }

        // Create a managed command line
        ManagedCommandline command = new ManagedCommandline(ccmExe);

        // If we were able to find a CM Synergy session ID, use it
        if (sessionID != null) {
            command.setVariable(CCM_SESSION_VAR, sessionID);
        }

        return command;
    }

    class OperationFailedException extends RuntimeException {
        public OperationFailedException(String message, Exception e) {
            super(message, e);
        }
    }

}

