/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;

/**
 *  This class implements the SourceControlElement methods for a PVCS
 *  repository.
 *
 *  @author <a href="mailto:Richard.Wagner@alltel.com">Richard Wagner</a>
 */
public class PVCS implements SourceControl {

    private static final Logger LOG = Logger.getLogger(PVCS.class);
    private static final String LINE_SEPARATOR =
        System.getProperty("line.separator");
    private static final String DOUBLE_QUOTE = "\"";

    private Hashtable properties = new Hashtable();
    private Date lastBuild;

    private String pvcsbin;
    private String pvcsProject;
    // i.e. "esa";
    // i.e. "esa/uihub2";
    private String pvcsSubProject;

    /**
     * Date format required by commands passed to PVCS
     */
    private SimpleDateFormat inDateFormat =
        new SimpleDateFormat("MM/dd/yyyy/HH:mm");

    /**
     * Date format returned in the output of PVCS commands.
     */
    private SimpleDateFormat outDateFormat =
        new SimpleDateFormat("MMM dd yyyy HH:mm:ss");

    private static final String PVCS_INSTRUCTIONS_FILE =
        "CruiseControlPVCS.pcli";
    private static final String PVCS_TEMP_WORK_FILE = "files.tmp";
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

    public void setInDateFormat(String inDateFormat) {
        this.inDateFormat = new SimpleDateFormat(inDateFormat);
    }

    public void setOutDateFormat(String outDateFormat) {
        this.outDateFormat = new SimpleDateFormat(outDateFormat);
    }

    /**
     * Unsupported by PVCS.
     */
    public void setProperty(String property) {
    }

    /**
     * Unsupported by PVCS.
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        if (pvcsProject == null) {
            throw new CruiseControlException("'pvcsproject' is a required attribute on PVCS");
        }
        if (pvcsSubProject == null) {
            throw new CruiseControlException("'pvcssubproject' is a required attribute on PVCS");
        }
    }

    /**
     *  Returns an {@link java.util.List List} of {@link Modification}
     *  detailing all the changes between now and the last build.
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
        final String command =
            getExecutable("pcli") + " run -s" + PVCS_INSTRUCTIONS_FILE;
        try {
            buildExecFilePre();
            exec(command);
            fixFilesTmp();
            buildExecFilePost(lastBuildDate, nowDate);
            exec(command);
        } catch (Exception e) {
            LOG.error("Error in executing the PVCS command : ", e);
            return new ArrayList();
        }
        List modifications = makeModificationsList();

        return modifications;
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

    private void exec(String command)
        throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(command);
        StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
        new Thread(errorPumper).start();
        InputStream input = p.getInputStream();
        p.waitFor();
        p.getOutputStream();
        p.getInputStream();
        p.getErrorStream();
    }

    /**
     * Read the file produced by PCLI listing all changes to the source repository
     * Once we've read the file, produce a list of changes.
     */
    private List makeModificationsList() {
        List theList;
        File inputFile = new File(PVCS_RESULTS_FILE);
        BufferedReader brIn;
        ModificationBuilder modificationBuilder = new ModificationBuilder();
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
     * Builds a file of PVCS instructions to execute.  The format should be roughly:
     *
     *  set -vProject "-prv:\esa"
     *  set -vSubProject "/esa/uihub2"
     *  Echo Getting list of files
     *  run ->files.tmp listversionedfiles -z -aw $Project $SubProject
     *
     * vlog command will be executed later, after tmp file cleanup
     *
     */
    private void buildExecFilePre() {
        String line1 =
            "set -vProject "
                + DOUBLE_QUOTE
                + "-pr"
                + pvcsProject
                + DOUBLE_QUOTE;
        String line2 =
            "set -vSubProject " + DOUBLE_QUOTE + pvcsSubProject + DOUBLE_QUOTE;
        String line3 =
            "run ->files.tmp listversionedfiles -z -aw $Project $SubProject";

        LOG.debug("#### PVCSElement about to write this line:\n " + line3);

        String[] instructions = new String[] { line1, line2, line3 };
        writeInstructionFile(instructions);
    }

    /**
     * Builds a file of PVCS instructions to execute.  The format should be roughly:
     *
     *  set -vProject "-prv:\esa"
     *  set -vSubProject "/esa/uihub2"
     *  Echo Getting History
     *  run -e vlog  "-xo+evlog.txt" "-d07/20/2001/10:49*07/30/2001" "@files.tmp"
     *
     */
    private void buildExecFilePost(String lastBuild, String now) {
        String line1 =
            "set -vProject "
                + DOUBLE_QUOTE
                + "-pr"
                + pvcsProject
                + DOUBLE_QUOTE;
        String line2 =
            "set -vSubProject " + DOUBLE_QUOTE + pvcsSubProject + DOUBLE_QUOTE;

        String line3Subline1 =
            "run -e vlog "
                + DOUBLE_QUOTE
                + "-xo+e"
                + PVCS_RESULTS_FILE
                + DOUBLE_QUOTE
                + " ";
        String line3Subline2 =
            DOUBLE_QUOTE + "-d" + lastBuild + "*" + now + DOUBLE_QUOTE + " ";
        String line3Subline3 =
            DOUBLE_QUOTE + "@" + PVCS_TEMP_WORK_FILE + DOUBLE_QUOTE;
        String line3 = line3Subline1 + line3Subline2 + line3Subline3;

        LOG.debug(
            "#### PVCSElement about to write this line:\n " + line3Subline2);

        String[] instructions = new String[] { line1, line2, line3 };

        writeInstructionFile(instructions);
    }

    void writeInstructionFile(String[] instructions) {
        File outputFile = new File(PVCS_INSTRUCTIONS_FILE);
        BufferedWriter bwOut = null;
        try {
            bwOut = new BufferedWriter(new FileWriter(outputFile));
            for (int i = 0; i < instructions.length; i++) {
                String instruction = instructions[i];
                bwOut.write(instruction);
                bwOut.write(LINE_SEPARATOR);
            }
        } catch (IOException e) {
            LOG.error("Error in building PVCS pcli file : ", e);
        } finally {
            if (bwOut != null) {
                try {
                    bwOut.close();
                } catch (Exception e) {
                }
            }
        }
    }

    // ugly but it's a temporary fix that works...
    private void fixFilesTmp() throws Exception {
        final File file = new File(PVCS_TEMP_WORK_FILE);

        List lines = readFile(file);
        writeFileAndFixLines(lines, file);
    }

    List readFile(File file) throws IOException {
        List lines = new ArrayList();
        // read files.tmp into List...
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            LOG.debug(
                "Error in reading "
                    + PVCS_TEMP_WORK_FILE
                    + " file of PVCS file listing: "
                    + e);
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        }
        return lines;
    }

    void writeFileAndFixLines(List lines, File file) throws IOException {
        // ...now write out lines to to files.tmp
        FileWriter out = null;
        try {
            out = new FileWriter(file);
            final Iterator iter = lines.iterator();
            while (iter.hasNext()) {
                String line = (String) iter.next();
                line = getFixedLine(line);
                out.write(line);
                out.write(LINE_SEPARATOR);
            }
        } catch (IOException e) {
            LOG.debug(
                "Error in writing "
                    + PVCS_TEMP_WORK_FILE
                    + " file of PVCS file listing: "
                    + e);
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }

    String getFixedLine(String line) {
        String fixedLine = line;
        if (line.startsWith("\"\\\\") && !line.startsWith("\"\\\\\\")) {
            fixedLine = "\"\\\\\\" + line.substring(3);
        }
        return fixedLine;
    }

    /**
     * Inner class to build Modifications and verify the order of the lines
     * used to build them.
     */
    class ModificationBuilder {

        private Modification modification;
        private ArrayList modificationList;
        private String lastLine;
        private boolean firstModifiedTime = true;
        private boolean firstUserName = true;
        private boolean nextLineIsComment = false;
        private boolean waitingForNextValidStart = false;

        public ArrayList getList() {
            return modificationList;
        }

        private void initializeModification() {
            if (modificationList == null) {
                modificationList = new ArrayList();
            }
            modification = new Modification();
            firstModifiedTime = true;
            firstUserName = true;
            nextLineIsComment = false;
            waitingForNextValidStart = false;
        }

        public void addLine(String line) {
            if (line.startsWith("Archive:")) {
                initializeModification();
            } else if (waitingForNextValidStart) {
                // we're in this state after we've got the last useful line
                // from the previous item, but haven't yet started a new one
                // -- we should just skip these lines till we start a new one
                return;
            } else if (line.startsWith("Workfile:")) {
                modification.fileName = line.substring(18);
            } else if (line.startsWith("Archive created:")) {
                try {
                    String createdDate = line.substring(18);
                    Date createTime = outDateFormat.parse(createdDate);
                    if (createTime.after(lastBuild)) {
                        modification.type = "added";
                    } else {
                        modification.type = "modified";
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    LOG.error("Error parsing create date : ", e);
                }
            } else if (line.startsWith("Last modified:")) {
                // if this is the newest revision...
                if (firstModifiedTime) {
                    firstModifiedTime = false;
                    try {
                        String lastMod = line.substring(16);
                        modification.modifiedTime =
                            outDateFormat.parse(lastMod);
                    } catch (ParseException e) {
                        modification.modifiedTime = null;
                        LOG.error("Error parsing modification time : ", e);
                    }
                }
            } else if (nextLineIsComment) {
                // used boolean because don't know what comment will startWith....
                modification.comment = line;
                // comment is last line we need, so add this mod to list,
                //  then set indicator to ignore future lines till next new item
                modificationList.add(modification);
                waitingForNextValidStart = true;
            } else if (line.startsWith("Author id:")) {
                // if this is the newest revision...
                if (firstUserName) {
                    String sub = line.substring(11);
                    StringTokenizer st = new StringTokenizer(sub, " ");
                    String username = st.nextToken().trim();
                    modification.userName = username;
                    firstUserName = false;
                    nextLineIsComment = true;
                }
            } // end of Author id

        } // end of addLine

    } // end of class ModificationBuilder

} // end class PVCSElement
