/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.Modification.ModifiedFile;
import net.sourceforge.cruisecontrol.util.CommandExecutor;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * This class implements the SourceControlElement methods for a PVCS repository.
 * 
 * @author <a href="mailto:Richard.Wagner@alltel.com">Richard Wagner </a>
 * @version $Id$
 */
public class PVCS implements SourceControl {

    public static class PvcsStreamConsumer implements StreamConsumer {

        private static final Logger LOGGER = Logger.getLogger(PVCS.class);
        private final String archiveFileSuffix;
        private boolean firstModifiedTime = true;
        private boolean firstRev = true;
        private boolean firstUserName = true;
        private final Date lastBuild;
        private final String ls = System.getProperty("line.separator");
        private Modification modification;
        private final List modificationList = new ArrayList();
        private boolean nextLineIsComment = false;
        private final DateFormat outDateFormat;
        private final DateFormat outDateFormatSub = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        private final String proj;
        private final StringBuffer string = new StringBuffer();
        private boolean waitingForNextValidStart = false;

        public PvcsStreamConsumer(final Date lastBuild, DateFormat format, final String proj,
                String suffix) {
            super();
            this.proj = proj;
            this.lastBuild = lastBuild;
            this.archiveFileSuffix = suffix;
            this.outDateFormat = format;
        }

        /**
         * @see net.sourceforge.cruisecontrol.util.StreamConsumer#consumeLine(java.lang.String)
         */
        public void consumeLine(String line) {
            string.append(line + ls);

            if (line.startsWith("Archive:")) {
                initializeModification();
                String fileName;

                int startIndex = (line.indexOf(proj) + proj.length());
                int endIndex = line.indexOf(archiveFileSuffix);
                if (endIndex == -1) {
                    endIndex = line.length();
                }
                fileName = line.substring(startIndex, endIndex);
                if (fileName.startsWith("/") || fileName.startsWith("\\")) {
                    fileName = fileName.substring(1);
                }
                if (fileName.startsWith("archives")) {
                    fileName = fileName.substring("archives".length());
                }
                modification.createModifiedFile(fileName, null);

            } else if (waitingForNextValidStart) {
                // we're in this state after we've got the last useful line
                // from the previous item, but haven't yet started a new one
                // -- we should just skip these lines till we start a new one
                //return
                //            } else if (line.startsWith("Workfile:")) {
                //                modification.createModifiedFile(line.substring(18), null);
            } else if (line.startsWith("Archive created:")) {
                try {
                    String createdDate = line.substring(18);
                    Date createTime;
                    try {
                        createTime = outDateFormat.parse(createdDate);
                    } catch (ParseException e) {
                        createTime = outDateFormatSub.parse(createdDate);
                    }
                    ModifiedFile file = (ModifiedFile) modification.files.get(0);
                    if (createTime.after(lastBuild)) {
                        file.action = "added";
                    } else {
                        file.action = "modified";
                    }
                } catch (ParseException e) {
                    LOGGER.error("Error parsing create date: " + e.getMessage(), e);
                }
            } else if (line.startsWith("Rev") && !line.startsWith("Rev count")) {
                if (firstRev) {
                    firstRev = false;
                    String revision = line.substring(4);
                    modification.revision = revision;
                    ModifiedFile file = (ModifiedFile) modification.files.get(0);
                    file.revision = revision;
                }
            } else if (line.startsWith("Last modified:")) {
                // if this is the newest revision...
                if (firstModifiedTime) {
                    firstModifiedTime = false;
                    String lastMod = null;
                    try {
                        lastMod = line.substring(16);
                        modification.modifiedTime = outDateFormat.parse(lastMod);
                    } catch (ParseException e) {
                        try {
                            modification.modifiedTime = outDateFormatSub.parse(lastMod);
                        } catch (ParseException pe) {
                            modification.modifiedTime = null;
                            LOGGER.error("Error parsing modification time : ", e);
                        }
                    }
                }
            } else if (nextLineIsComment) {
                // used boolean because don't know what comment will
                // startWith....
                boolean isDashesLine = line.startsWith("----------");
                boolean isEqualsLine = line.startsWith("==========");
                boolean isEndOfCommentsLine = isDashesLine || isEqualsLine;
                if (modification.comment == null || modification.comment.length() == 0) {
                    modification.comment = line;
                } else if (!isEndOfCommentsLine) {
                    modification.comment = modification.comment
                            + System.getProperty("line.separator") + line;
                } else {
                    //  then set indicator to ignore future lines till next new
                    // item
                    modificationList.add(modification);
                    waitingForNextValidStart = true;
                }
            } else if (line.startsWith("Author id:")) {
                // if this is the newest revision...
                if (firstUserName) {
                    String sub = line.substring(11);
                    StringTokenizer st = new StringTokenizer(sub, " ");
                    modification.userName = st.nextToken().trim();
                    firstUserName = false;
                    nextLineIsComment = true;
                }
            } // end of Author id

        }

        public List getModificationList() {
            return this.modificationList;
        }

        public String getOutput() {
            return string.toString();
        }

        private void initializeModification() {
            modification = new Modification("pvcs");
            firstModifiedTime = true;
            firstUserName = true;
            nextLineIsComment = false;
            waitingForNextValidStart = false;
        }

    }

    private static final Logger LOG = Logger.getLogger(PVCS.class);

    private static final String PCLI = "pcli";

    private String archiveFileSuffix = "-arc";

    /**
     * Date format required by commands passed to PVCS
     */
    private SimpleDateFormat inDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    private String loginId;

    /**
     * Date format returned in the output of PVCS commands.
     */
    private SimpleDateFormat outDateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");

    private final SourceControlProperties properties = new SourceControlProperties();
    private String pvcsbin;
    private String pvcsProject;
    // i.e. "esa";
    // i.e. "esa/uihub2";
    private String pvcsPromotionGroup;
    private String pvcsSubProject;
    private String pvcsVersionLabel;

    /**
     * Returns the command to be ran to check for repository changes run -ns -q
     * vlog -idSomeUser "-ds11/23/2004 08:00:00 AM" "-de11/23/2004 01:00:00 PM"
     * "-prC:/PVCS-Repos/TestProject" "-vTest Version Label" -z /TestProject
     * 
     * @return the command to be executed to check for repository changes
     */
    Commandline buildExecCommand(String lastBuild, String now) {
        Commandline command = new Commandline();
        //command.useSafeQuoting(false);
        command.setExecutable(getExecutable(PCLI));
        command.createArgument("run");
        command.createArgument("-ns");
        command.createArgument("-q");
        command.createArgument("vlog");

        if (loginId != null && !loginId.trim().equals("")) {
            command.createArgument("-id" + loginId);
        }

        command.createArgument("-ds" + lastBuild);
        command.createArgument("-de" + now);
        command.createArgument("-pr" + pvcsProject);

        if (pvcsVersionLabel != null && !pvcsVersionLabel.equals("")) {
            command.createArgument("-v" + pvcsVersionLabel);
        }

        if (pvcsPromotionGroup != null && !pvcsPromotionGroup.equals("")) {
            command.createArgument("-g" + pvcsPromotionGroup);
        }

        command.createArgument("-z");
        command.createArgument(pvcsSubProject);
        return command;
    }

    protected void executeCommandline(Commandline command, PvcsStreamConsumer consumer)
            throws IOException, InterruptedException {
        LOG.info("Running command: " + command);
        CommandExecutor executor = new CommandExecutor(command, LOG);
        executor.setOutputConsumer(consumer);
        executor.executeAndWait();
        LOG.debug("Output: \n" + consumer.getOutput());
    }

    protected String getExecutable(String exe) {
        StringBuffer correctedExe = new StringBuffer();
        if (getPvcsbin() != null) {
            if (getPvcsbin().endsWith(File.separator)) {
                correctedExe.append(getPvcsbin());
            } else {
                correctedExe.append(getPvcsbin()).append(File.separator);
            }
        }
        return correctedExe.append(exe).toString();
    }

    /**
     * @return loginId
     */
    public String getLoginid() {
        return loginId;
    }

    /**
     * Returns an {@link java.util.List List}of {@link Modification}s
     * detailing all the changes between now and the last build.
     * 
     * @param lastBuild the last build time
     * @param now time now, or time to check
     * @return the list of modifications, an empty (not null) list if no
     *         modifications or if developer had checked in files since
     *         quietPeriod seconds ago.
     * 
     * Note: Internally uses external filesystem for files
     * CruiseControlPVCS.pcli, files.tmp, vlog.txt
     */
    public List getModifications(Date lastBuild, Date now) {

        PvcsStreamConsumer consumer = new PvcsStreamConsumer(lastBuild, this.outDateFormat,
                pvcsProject, this.archiveFileSuffix);
        List modificationList = getModifications(lastBuild, now, consumer);

        return modificationList;
    }

    protected List getModifications(Date lastBuild, Date now, PvcsStreamConsumer consumer) {
        try {
            // build file of PVCS command line instructions
            String lastBuildDate = inDateFormat.format(lastBuild);
            String nowDate = inDateFormat.format(now);
            Commandline command = buildExecCommand(lastBuildDate, nowDate);
            executeCommandline(command, consumer);
        } catch (Exception e) {
            LOG.error("Error in executing the PVCS command : ", e);
        }

        List modificationList = consumer.getModificationList();

        if (!modificationList.isEmpty()) {
            properties.modificationFound();
        }
        StringBuffer msg = new StringBuffer("" + modificationList.size());
        if (1 == modificationList.size()) {
            msg.append(" modification has been detected");
        } else {
            msg.append(" modifications have been detected");
        }
        msg.append(" for ").append(pvcsSubProject).append(".");
        LOG.info(msg.toString());
        return modificationList;
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    /**
     * Get name of the PVCS bin directory
     * 
     * @return String
     */
    public String getPvcsbin() {
        return pvcsbin;
    }

    public void setArchiveFileSuffix(String archiveSuffix) {
        this.archiveFileSuffix = archiveSuffix;
    }

    public void setInDateFormat(String inDateFormat) {
        this.inDateFormat = new SimpleDateFormat(inDateFormat);
    }

    /**
     * @param loginId
     */
    public void setLoginid(String loginId) {
        this.loginId = loginId;
    }

    public void setOutDateFormat(String outDateFormat) {
        this.outDateFormat = new SimpleDateFormat(outDateFormat);
    }

    public void setProperty(String propertyName) {
        properties.assignPropertyName(propertyName);
    }

    /**
     * Specifies the location of the PVCS bin directory
     * 
     * @param bin Specifies the location of the PVCS bin directory
     */
    public void setPvcsbin(String bin) {
        this.pvcsbin = bin;
    }

    public void setPvcsproject(String project) {
        pvcsProject = project;
    }

    public void setPvcspromotiongroup(String promotiongroup) {
        pvcsPromotionGroup = promotiongroup;
    }

    public void setPvcssubproject(String subproject) {
        pvcsSubProject = subproject;
    }

    public void setPvcsversionlabel(String versionlabel) {
        pvcsVersionLabel = versionlabel;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(pvcsProject, "pvcsproject", this.getClass());
        ValidationHelper.assertIsSet(pvcsSubProject, "pvcssubproject", this.getClass());
    }

} // end class PVCSElement
