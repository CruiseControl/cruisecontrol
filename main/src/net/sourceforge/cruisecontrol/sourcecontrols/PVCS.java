/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 *  This class implements the SourceControlElement methods for a PVCS repository.
 *
 *  @author <a href="mailto:Richard.Wagner@alltel.com">Richard Wagner</a>
 *  @version $Id$
 */
public class PVCS implements SourceControl {

    private static final Logger LOG = Logger.getLogger(PVCS.class);
    private static final String DOUBLE_QUOTE = "\"";

    private Date lastBuild;

    private String archiveFileSuffix = "-arc";
    private String pvcsbin;
    private String pvcsProject;
    // i.e. "esa";
    // i.e. "esa/uihub2";
    private String pvcsSubProject;
    private String pvcsVersionLabel;
    private String loginId;

    /**
     * Date format required by commands passed to PVCS
     */
    private SimpleDateFormat inDateFormat = new SimpleDateFormat("MM/dd/yyyy h:mma");
    private SimpleDateFormat outDateFormatSub = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

    /**
     * Date format returned in the output of PVCS commands.
     */
    private SimpleDateFormat outDateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");

    private static final String PVCS_RESULTS_FILE = "vlog.txt";

    /**
     * Get name of the PVCS bin directory
     * @return String
     */
    public String getPvcsbin() {
        return pvcsbin;
    }


    /**
     * Specifies the location of the PVCS bin directory
     * @param bin Specifies the location of the PVCS bin directory
     */
    public void setPvcsbin(String bin) {
        this.pvcsbin = bin;
    }

    public void setPvcsproject(String project) {
        pvcsProject = project;
    }

    public void setPvcssubproject(String subproject) {
        pvcsSubProject = subproject;
    }

    public void setPvcsversionlabel(String versionlabel) {
        pvcsVersionLabel = versionlabel;
    }

    public void setInDateFormat(String inDateFormat) {
        this.inDateFormat = new SimpleDateFormat(inDateFormat);
    }

    public void setOutDateFormat(String outDateFormat) {
        this.outDateFormat = new SimpleDateFormat(outDateFormat);
    }

    public Map getProperties() {
        return Collections.EMPTY_MAP;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(pvcsProject, "pvcsproject", this.getClass());
        ValidationHelper.assertIsSet(pvcsSubProject, "pvcssubproject", this.getClass());
    }

    /**
     *  Returns an {@link java.util.List List} of {@link Modification}s detailing all the changes between now
     *  and the last build.
     *
     *@param  lastBuild the last build time
     *@param  now time now, or time to check
     *@return  the list of modifications, an empty (not null) list if no
     *      modifications or if developer had checked in files since quietPeriod seconds ago.
     *
     *  Note:  Internally uses external filesystem for files CruiseControlPVCS.pcli, files.tmp, vlog.txt
     */
    public List getModifications(Date lastBuild, Date now) {
        this.lastBuild = lastBuild;
        // build file of PVCS command line instructions
        String lastBuildDate = inDateFormat.format(lastBuild);
        String nowDate = inDateFormat.format(now);

        try {
            Commandline command = buildExecCommand(lastBuildDate, nowDate);
            command.executeAndWait(LOG);
        } catch (Exception e) {
            LOG.error("Error in executing the PVCS command : ", e);
            return new ArrayList();
        }

        return makeModificationsList(new File(PVCS_RESULTS_FILE));
    }

    String getExecutable(String exe) {
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
     * Read the file produced by PCLI listing all changes to the source repository
     * Once we've read the file, produce a list of changes.
     * @param inputFile TODO
     */
    List makeModificationsList(File inputFile) {
        List theList;
        BufferedReader brIn;
        ModificationBuilder modificationBuilder = new ModificationBuilder(pvcsProject);
        try {
            brIn = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = brIn.readLine()) != null) {
                modificationBuilder.addLine(line);
            }
            brIn.close();
        } catch (IOException e) {
            LOG.error("Error in reading vlog file of PVCS modifications : ", e);
        }
        theList = modificationBuilder.getList();

        if (theList == null) {
            theList = new ArrayList();
        }

        return theList;
    }

    /**
     *  Returns the command to be ran to check for repository changes
     *  run -ns -q -xo"vlog.txt" -xe"vlog.txt" vlog -id"SomeUser" 
     *  -ds"11/23/2004 8:00AM"-de"11/23/2004 1:00PM" -pr"C:/PVCS-Repos/TestProject"
     *  -v"Test Version Label" -z /TestProject
     *
     *  @return the command to be executed to check for repository changes
     */
    Commandline buildExecCommand(String lastBuild, String now) {
        Commandline command = new Commandline();
        command.setExecutable(getExecutable("pcli"));
        command.createArgument("run");
        command.createArgument("-ns");
        command.createArgument("-q");
        command.createArgument("-xo" + DOUBLE_QUOTE + PVCS_RESULTS_FILE + DOUBLE_QUOTE);
        command.createArgument("-xe" + DOUBLE_QUOTE + PVCS_RESULTS_FILE + DOUBLE_QUOTE);
        command.createArgument("vlog");
        
        if (loginId != null && !loginId.trim().equals("")) {
            command.createArgument("-id" + DOUBLE_QUOTE + loginId + DOUBLE_QUOTE);
        }

        command.createArgument("-ds" + DOUBLE_QUOTE + lastBuild + DOUBLE_QUOTE);
        command.createArgument("-de" + DOUBLE_QUOTE + now + DOUBLE_QUOTE);
        command.createArgument("-pr" + DOUBLE_QUOTE + pvcsProject + DOUBLE_QUOTE);

        if (pvcsVersionLabel != null && !pvcsVersionLabel.equals("")) {
            command.createArgument("-v" + DOUBLE_QUOTE + pvcsVersionLabel + DOUBLE_QUOTE);
         }

        command.createArgument("-z");
        command.createArgument(pvcsSubProject);
        return command;
    }

    /**
     * Inner class to build Modifications and verify the order of the lines
     * used to build them.
     */
    class ModificationBuilder {
        private String proj;
        private Modification modification;
        private ArrayList modificationList;
        private boolean firstModifiedTime = true;
        private boolean firstUserName = true;
        private boolean nextLineIsComment = false;
        private boolean waitingForNextValidStart = false;

        public ModificationBuilder(String proj) {
            this.proj = proj;
        }

        public ArrayList getList() {
            return modificationList;
        }

        private void initializeModification() {
            if (modificationList == null) {
                modificationList = new ArrayList();
            }
            modification = new Modification("pvcs");
            firstModifiedTime = true;
            firstUserName = true;
            nextLineIsComment = false;
            waitingForNextValidStart = false;
        }

        public void addLine(String line) {
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
            } else if (line.startsWith("Workfile:")) {
                modification.createModifiedFile(line.substring(18), null);
            } else if (line.startsWith("Archive created:")) {
                try {
                    String createdDate = line.substring(18);
                    Date createTime;
                    try {
                        createTime = outDateFormat.parse(createdDate);
                    } catch (ParseException e) {
                        createTime = outDateFormatSub.parse(createdDate);
                    }
                    if (createTime.after(lastBuild)) {
                        modification.type = "added";
                    } else {
                        modification.type = "modified";
                    }
                } catch (ParseException e) {
                    LOG.error("Error parsing create date: " + e.getMessage(), e);
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
                            LOG.error("Error parsing modification time : ", e);
                          }
                    }
                }
            } else if (nextLineIsComment) {
                // used boolean because don't know what comment will startWith....
                boolean isDashesLine = line.startsWith("----------");
                boolean isEqualsLine = line.startsWith("==========");
                boolean isEndOfCommentsLine = isDashesLine || isEqualsLine;
                if (modification.comment == null || modification.comment.length() == 0) {
                    modification.comment = line;
                } else if (!isEndOfCommentsLine)  {
                    modification.comment = modification.comment + System.getProperty("line.separator") + line;
                } else {
                    //  then set indicator to ignore future lines till next new item
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

        } // end of addLine

    } // end of class ModificationBuilder

   /**
    * @return loginId
    */
   public String getLoginid() {
      return loginId;
   }

   /**
    * @param loginId
    */
   public void setLoginid(String loginId) {
      this.loginId = loginId;
   }

    void setLastBuild(Date date) {
        this.lastBuild = date;
    }

    public void setArchiveFileSuffix(String archiveSuffix) {
        this.archiveFileSuffix = archiveSuffix;
    }

} // end class PVCSElement
