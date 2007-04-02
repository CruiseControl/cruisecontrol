/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2007, ThoughtWorks, Inc.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Processes;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * This class implements the SourceControlElement methods for a MKS repository.
 * The call to MKS is assumed to work with any setup:
 * The call to MKS login should be done prior to calling this class.
 *  *
 * attributes:
 * localWorkingDir - local directory for the sandbox
 * project - the name and path to the MKS project
 * doNothing - if this attribute is set to true, no mks command is executed. This is for
 * testing purposes, if a potentially slow mks server connection should avoid
 *
 * @author Suresh K Bathala Skila, Inc.
 * @author Dominik Hirt, Wincor-Nixdorf International GmbH, Leipzig
 */
public class MKS implements SourceControl {
    private static final Logger LOG = Logger.getLogger(MKS.class);

    private SourceControlProperties properties = new SourceControlProperties();
    private String project;
    private File localWorkingDir;

    /**
     * if this attribute is set to true, no mks command is executed. This is for
     * testing purposes, if a potentially slow mks server connection should
     * avoid
     */
    private boolean doNothing;

    /**
     * This is the workaround for the missing feature of MKS to return differences
     * for a given time period. If a modification is detected during the quietperiod,
     * CruiseControl calls <code>getModifications</code> of this sourcecontrol object
     * again, with the new values for <code>Date now</code>. In that case, and if all
     * modification are already found in the first cycle, the list of modifications
     * becomes empty. Therefor, we have to return the summarized list of modifications:
     * the values from the last run,  and -maybe- results return by MKS for this run.
     */
    private List listOfModifications = new ArrayList();
    
    /**
     * The listOfModifications is cleared when new value for lastBuild is used.
     */
    private Date lastBuild = new Date();

    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Sets the local working copy to use when making calls to MKS.
     *
     * @param local
     *            String indicating the relative or absolute path to the local
     *            working copy of the module of which to find the log history.
     */
    public void setLocalWorkingDir(String local) {
        localWorkingDir = new File(local);
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setDoNothing(String doNothing) {
        this.doNothing = Boolean.valueOf(doNothing).booleanValue();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(localWorkingDir, "localWorkingDir", this.getClass());
        ValidationHelper.assertIsSet(project, "project", this.getClass());
    }

    /**
     * Returns an ArrayList of Modifications.
     * MKS ignores dates for such a range so therefor ALL
     * modifications since the last resynch step are returned.
     *
     * @param lastBuild
     *            Last build time.
     * @param now
     *            Time now, or time to check.
     * @return maybe empty, never null.
     * @throws CruiseControlException 
     */
    public List getModifications(Date lastBuild, Date now) {

        if (doNothing) {
            properties.modificationFound();
            return listOfModifications;
        }
        
        if (this.lastBuild.compareTo(lastBuild) != 0) {
            listOfModifications.clear();
            this.lastBuild = lastBuild;
        }

        String projectFilePath = getProjectFilePath();
        
        Commandline cmdLine = createResyncCommandLine(projectFilePath);

        executeResyncAndParseModifications(cmdLine, listOfModifications);

        return listOfModifications;
    }

    void executeResyncAndParseModifications(Commandline cmdLine, List modifications) {
        try {
            StreamConsumer stderr = new ModificationsConsumer(modifications);
            StreamConsumer stdout = StreamLogger.getWarnLogger(LOG);
            Processes.waitFor(cmdLine.execute(), stdout, stderr);
        } catch (Exception ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        LOG.info("resync finished");
    }

    Commandline createResyncCommandLine(String projectFilePath) {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("si");
        cmdLine.createArgument("resync");
        cmdLine.createArgument("-f");
        cmdLine.createArgument("-R");
        cmdLine.createArgument("-S");
        cmdLine.createArgument(projectFilePath);
        try {
            cmdLine.setWorkingDir(localWorkingDir);
        } catch (CruiseControlException e) {
            throw new RuntimeException(e);
        }
        return cmdLine;
    }

    String getProjectFilePath() {
        String projectFilePath = localWorkingDir.getAbsolutePath() + File.separator + project;
        if (!new File(projectFilePath).exists()) {
            throw new RuntimeException("project file not found at " + projectFilePath);
        }
        return projectFilePath;
    }

    /**
     * Sample output:
     * dominik.hirt;add forceDeploy peter.neumcke;path to properties file fixed,
     * copy generated properties Member added to project
     * d:/MKS/PCE_Usedom/Products/Info/Info.pj
     *
     * @param fileName
     */
    private void setUserNameAndComment(Modification modification,
            String folderName, String fileName) {

        try {
            Commandline commandLine = new Commandline();
            commandLine.setExecutable("si");

            if (localWorkingDir != null) {
                commandLine.setWorkingDirectory(localWorkingDir.getAbsolutePath());
            }

            commandLine.createArgument("rlog");
            commandLine.createArgument("--format={author};{description}");
            commandLine.createArgument("--noHeaderFormat");
            commandLine.createArgument("--noTrailerFormat");
            commandLine.createArguments("-r", modification.revision);
            commandLine.createArgument(folderName + File.separator + fileName);

            Process proc = commandLine.execute();
            UserAndCommentConsumer userData = new UserAndCommentConsumer();
            Processes.waitFor(proc, userData, StreamLogger.getWarnLogger(LOG));
            if (userData.wasFound()) {
                modification.userName = userData.getUserName();
                modification.comment = userData.getComment();
            } else {
                LOG.warn("could not find username or comment for " + fileName + " r" + modification.revision);
                modification.userName = "";
                modification.comment = "";
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            modification.userName = "";
            modification.comment = "";
        }
    }

    /* Sample output on stderr:
     * output: Connecting to baswmks1:7001 ... Connecting to baswmks1:7001
     * as dominik.hirt ... Resynchronizing files...
     * c:\temp\test\Admin\ComponentBuild\antfile.xml
     * c:\temp\test\Admin\PCEAdminCommand\projectbuild.properties: checked
     * out revision 1.1
     */

    private final class ModificationsConsumer implements StreamConsumer {
        public List modifications;
        
        private ModificationsConsumer(List modifications) {
            this.modifications = modifications;
        }
        
        public void consumeLine(String line) {
            int idxCheckedOutRevision = line
                    .indexOf(": checked out revision");

            if (idxCheckedOutRevision == -1) {
                return;
            }
            LOG.info(line);

            int idxSeparator = line.lastIndexOf(File.separator);
            String folderName = line.substring(0, idxSeparator);
            String fileName = line.substring(idxSeparator + 1,
                    idxCheckedOutRevision);
            Modification modification = new Modification();
            Modification.ModifiedFile modFile = modification
                    .createModifiedFile(fileName, folderName);
            modification.modifiedTime = new Date(new File(folderName,
                    fileName).lastModified());
            modFile.revision = line.substring(idxCheckedOutRevision + 23);
            modification.revision = modFile.revision;
            setUserNameAndComment(modification, folderName, fileName);

            modifications.add(modification);

            properties.modificationFound();
        }
    }

    private static class UserAndCommentConsumer implements StreamConsumer {
        private boolean found;
        private String userName;
        private String comment;

        public boolean wasFound() {
            return found;
        }
        public String getUserName() {
            return userName;
        }
        public String getComment() {
            return comment;
        }

        public void consumeLine(String line) {
            if (found) {
                return;
            }
            int idx = line.indexOf(";");
            if (idx == -1) {
                LOG.debug(line);
                return;
            }

            found = true;
            userName = line.substring(0, idx);
            if (idx < line.length()) {
                comment = line.substring(idx + 1);
            } else {
                comment = "";
            }            
        }
    }
}
