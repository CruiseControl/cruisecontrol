/***********************************************************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 *
 *      Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *      following disclaimer.
 *
 *      Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 *      following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *      Neither the name of ThoughtWorks, Inc., CruiseControl, nor the names of its contributors may be used to endorse 
 *      or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **********************************************************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class handles all VSS-related aspects of determining the modifications since the last good build.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author Eli Tucker
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Arun Aggarwal
 */
public class Vss implements SourceControl {

    private static final Logger LOG = Logger.getLogger(Vss.class);

    private SimpleDateFormat vssDateTimeFormat;
    private String ssDir;
    private String vssPath;
    private String serverPath;
    private String login;
    private String dateFormat;
    private String timeFormat;
    private Hashtable properties = new Hashtable();
    private String property;
    private String propertyOnDelete;

    public Vss() {
        dateFormat = "MM/dd/yy";
        timeFormat = "hh:mma";
        constructVssDateTimeFormat();
    }

    /**
     * Set the project to get history from
     * 
     * @param vsspath
     */
    public void setVsspath(String vsspath) {
        this.vssPath = "$" + vsspath;
    }

    /**
     * Set the path to the ss executable
     * 
     * @param ssdir
     */
    public void setSsDir(String ssdir) {
        this.ssDir = ssdir;
    }

    /**
     * Set the path to the directory containing the srcsafe.ini file.
     * 
     * @param dirWithSrcsafeIni
     */
    public void setServerPath(String dirWithSrcsafeIni) {
        serverPath = dirWithSrcsafeIni;
    }

    /**
     * Login for vss
     * 
     * @param usernameCommaPassword
     */
    public void setLogin(String usernameCommaPassword) {
        login = usernameCommaPassword;
    }

    /**
     * Choose a property to be set if the project has modifications if we have a change that only requires repackaging,
     * i.e. jsp, we don't need to recompile everything, just rejar.
     * 
     * @param propertyName
     */
    public void setProperty(String propertyName) {
        property = propertyName;
    }

    /**
     * Choose a property to be set if the project has deletions
     * 
     * @param propertyName
     */
    public void setPropertyOnDelete(String propertyName) {
        propertyOnDelete = propertyName;
    }

    /**
     * Sets the date format to use for querying VSS and processing reports. The default date format is
     * <code>MM/dd/yy</code> . If your computer is set to a different region, you may wish to use a format such as
     * <code>dd/MM/yy</code> .
     * 
     * @see java.text.SimpleDateFormat
     */
    public void setDateFormat(String format) {
        dateFormat = format;
        constructVssDateTimeFormat();
    }

    /**
     * Sets the time format to use for querying VSS and processing reports. The default time format is
     * <code>hh:mma</code> . If your computer is set to a different region, you may wish to use a format such as
     * <code>HH:mm</code> .
     * 
     * @see java.text.SimpleDateFormat
     */
    public void setTimeFormat(String format) {
        timeFormat = format;
        constructVssDateTimeFormat();
    }

    public Map getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(vssPath, "vsspath", this.getClass());
        ValidationHelper.assertIsSet(login, "login", this.getClass());
    }

    /**
     * Calls "ss history [dir] -R -Vd[now]~[lastBuild] -Y[login] -I-N -O[tempFileName]" Results written to a file since
     * VSS will start wrapping lines if read directly from the stream.
     * 
     * @param lastBuild
     * @param now
     * @return List of modifications
     */
    public List getModifications(Date lastBuild, Date now) {
        ArrayList modifications = new ArrayList();

        String[] env = VSSHelper.loadVSSEnvironment(serverPath);
        LOG.info("Vss: getting modifications for " + vssPath);

        try {
            Process p = Runtime.getRuntime().exec(getCommandLine(lastBuild, now), env);
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();

            parseTempFile(modifications);
        } catch (IOException e) {
            LOG.equals(e);
            throw new RuntimeException(e.getMessage());
        } catch (CruiseControlException e) {
            LOG.equals(e);
            throw new RuntimeException(e.getMessage());
        } catch (InterruptedException e) {
            LOG.equals(e);
            throw new RuntimeException(e.getMessage());
        }

        if (property != null && modifications.size() > 0) {
            properties.put(property, "true");
        }

        return modifications;
    }

    private void parseTempFile(ArrayList modifications) throws IOException {
        Level loggingLevel = LOG.getEffectiveLevel();
        if (Level.DEBUG.equals(loggingLevel)) {
            logVSSTempFile();
        }

        File tempFile = getTempFile();
        BufferedReader reader = new BufferedReader(new FileReader(tempFile));

        parseHistoryEntries(modifications, reader);

        reader.close();
        tempFile.delete();
    }

    private void logVSSTempFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(getTempFile()));
        String currLine = reader.readLine();
        LOG.debug(" ");
        while (currLine != null) {
            LOG.debug(getTempFile().getName() + ": " + currLine);
            currLine = reader.readLine();
        }
        LOG.debug(" ");
        reader.close();
    }

    private File getTempFile() {
        return new File(createFileNameFromVssPath());
    }

    String createFileNameFromVssPath() {
        // don't include the leading $
        String filename = vssPath.substring(1).replace('/', '_') + ".tmp";
        while (filename.charAt(0) == '_') {
            filename = filename.substring(1);
        }
        return filename;
    }

    void parseHistoryEntries(List modifications, BufferedReader reader) throws IOException {
        String currLine = reader.readLine();

        while (currLine != null) {
            if (isVssEntryHeader(currLine)) {
                ArrayList vssEntry = new ArrayList();
                vssEntry.add(currLine);
                currLine = reader.readLine();
                while (currLine != null && !isVssEntryHeader(currLine)) {
                    vssEntry.add(currLine);
                    currLine = reader.readLine();
                }
                Modification mod = handleEntry(vssEntry);
                if (mod != null) {
                    modifications.add(mod);
                }
            } else {
                currLine = reader.readLine();
            }
        }
    }

    /**
     * @param line
     * @return true if line matches 5 asterisks, 2 spaces, any text, 2 spaces, 5 asterisks
     */
    private boolean isVssEntryHeader(String line) {
        // This can still fail if the entry has a comment containing something of the form '***** some text *****' but
        // is probably not worth handling at this point. If it does come up, we'll need to look at implementing some
        // form of state machine.

        return line.matches("\\*{5} {2}.+ {2}\\*{5}");
    }

    protected String[] getCommandLine(Date lastBuild, Date now) throws CruiseControlException {
        Commandline commandline = new Commandline();
        String execCommand;
        try {
            execCommand = (ssDir != null) ? new File(ssDir, "ss.exe").getCanonicalPath() : "ss.exe";
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }

        commandline.setExecutable(execCommand);
        commandline.createArgument().setValue("history");
        commandline.createArgument().setValue(vssPath);
        commandline.createArgument().setValue("-R");
        commandline.createArgument().setValue("-Vd" + formatDateForVSS(now) + "~" + formatDateForVSS(lastBuild));
        commandline.createArgument().setValue("-Y" + login);
        commandline.createArgument().setValue("-I-N");
        commandline.createArgument().setValue("-O" + getTempFile().getName());

        String[] line = commandline.getCommandline();

        LOG.debug(" ");
        for (int i = 0; i < line.length; i++) {
            LOG.debug("Vss command line arguments: " + line[i]);
        }
        LOG.debug(" ");

        return line;
    }

    /**
     * Format a date for vss in the format specified by the dateFormat. By default, this is in the form
     * <code>12/21/2000;8:14A</code> (vss doesn't like the m in am or pm). This format can be changed with
     * <code>setDateFormat()</code>
     * 
     * @param d
     *            Date to format.
     * @return String of date in format that VSS requires.
     * @see #setDateFormat
     */
    private String formatDateForVSS(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat + ";" + timeFormat, Locale.US);
        String vssFormattedDate = sdf.format(d);
        if (timeFormat.endsWith("a")) {
            return vssFormattedDate.substring(0, vssFormattedDate.length() - 1);
        }
        return vssFormattedDate;
    }

    // ***** the rest of this is just parsing the vss output *****

    /**
     * Parse individual VSS history entry
     * 
     * @param entry
     */
    protected Modification handleEntry(List entry) {
        LOG.debug("VSS history entry BEGIN");
        for (Iterator i = entry.iterator(); i.hasNext();) {
            LOG.debug("entry: " + i.next());
        }
        LOG.debug("VSS history entry END");

        try {
            final String labelDelimiter = "**********************";
            boolean isLabelEntry = labelDelimiter.equals(entry.get(0));

            if (isLabelEntry) {
                LOG.debug("this is a label; ignoring this entry");
                return null;
            }

            // but need to adjust for cases where Label: line exists
            //
            // ***** DateChooser.java *****
            // Version 8
            // Label: "Completely new version!"
            // User: Arass Date: 10/21/02 Time: 12:48p
            // Checked in $/code/development/src/org/ets/cbtidg/common/gui
            // Comment: This is where I add a completely new, but alot nicer
            // version of the date chooser.
            // Label comment:

            int nameAndDateIndex = 2;
            if (((String) entry.get(0)).startsWith("***************** ")) {
                nameAndDateIndex = 1;
            }
            String nameAndDateLine = (String) entry.get(nameAndDateIndex);
            if (nameAndDateLine.startsWith("Label:")) {
                nameAndDateIndex++;
                nameAndDateLine = (String) entry.get(nameAndDateIndex);
                LOG.debug("adjusting for the line that starts with Label");
            }

            Modification modification = new Modification("vss");
            modification.userName = parseUser(nameAndDateLine);
            modification.modifiedTime = parseDate(nameAndDateLine);

            String folderLine = (String) entry.get(0);
            int fileIndex = nameAndDateIndex + 1;
            String fileLine = (String) entry.get(fileIndex);

            if (fileLine.startsWith("Checked in")) {

                LOG.debug("this is a checkin");
                int commentIndex = fileIndex + 1;
                modification.comment = parseComment(entry, commentIndex);
                String fileName = folderLine.substring(7, folderLine.indexOf("  *"));
                String folderName = fileLine.substring(12);

                Modification.ModifiedFile modfile = modification.createModifiedFile(fileName, folderName);
                modfile.action = "checkin";

            } else if (fileLine.endsWith("Created")) {
                modification.type = "create";
                LOG.debug("this folder was created");
            } else {
                String fileName;
                String folderName;

                if (nameAndDateIndex == 1) {
                    folderName = vssPath;
                } else {
                    folderName = vssPath + "\\" + folderLine.substring(7, folderLine.indexOf("  *"));
                }
                int lastSpace = fileLine.lastIndexOf(" ");
                if (lastSpace != -1) {
                    fileName = fileLine.substring(0, lastSpace);
                } else {
                    fileName = fileLine;
                    if (fileName.equals("Branched")) {
                        LOG.debug("Branched file, ignoring as branch directory is handled separately");
                        return null;
                    }
                }

                Modification.ModifiedFile modfile = modification.createModifiedFile(fileName, folderName);

                if (fileLine.endsWith("added")) {
                    modfile.action = "add";
                    LOG.debug("this file was added");
                } else if (fileLine.endsWith("deleted")) {
                    modfile.action = "delete";
                    LOG.debug("this file was deleted");
                    addPropertyOnDelete();
                } else if (fileLine.endsWith("destroyed")) {
                    modfile.action = "destroy";
                    LOG.debug("this file was destroyed");
                    addPropertyOnDelete();
                } else if (fileLine.endsWith("recovered")) {
                    modfile.action = "recover";
                    LOG.debug("this file was recovered");
                } else if (fileLine.endsWith("shared")) {
                    modfile.action = "share";
                    LOG.debug("this file was shared");
                } else if (fileLine.endsWith("branched")) {
                    modfile.action = "branch";
                    LOG.debug("this file was branched");
                } else if (fileLine.indexOf(" renamed to ") != -1) {
                    modfile.fileName = fileLine;
                    modfile.action = "rename";
                    LOG.debug("this file was renamed");
                    addPropertyOnDelete();
                } else if (fileLine.startsWith("Labeled")) {
                    LOG.debug("this is a label; ignoring this entry");
                    return null;
                } else {
                    LOG.debug("action for this vss entry (" + fileLine + ") is unknown");
                }
            }

            if (property != null) {
                properties.put(property, "true");
                LOG.debug("setting property " + property + " to be true");
            }

            LOG.debug(" ");

            return modification;

        } catch (RuntimeException e) {
            LOG.fatal("RuntimeException handling VSS entry:");
            for (int i = 0; i < entry.size(); i++) {
                LOG.fatal(entry.get(i));
            }
            throw e;
        }
    }

    private void addPropertyOnDelete() {
        if (propertyOnDelete != null) {
            properties.put(propertyOnDelete, "true");
            LOG.debug("setting property " + propertyOnDelete + " to be true");
        }
    }

    /**
     * Parse comment from VSS history (could be multi-line)
     * 
     * @param commentList
     * @return the comment
     */
    private String parseComment(List commentList, int commentIndex) {
        StringBuffer comment = new StringBuffer();
        comment.append(commentList.get(commentIndex)).append(" ");
        for (int i = commentIndex + 1; i < commentList.size(); i++) {
            comment.append(commentList.get(i)).append(" ");
        }

        return comment.toString().trim();
    }

    /**
     * Parse date/time from VSS file history The nameAndDateLine will look like <br>
     * <code>User: Etucker      Date:  6/26/01   Time: 11:53a</code><br>
     * Sometimes also this<br>
     * <code>User: Aaggarwa     Date:  6/29/:1   Time:  3:40p</code><br>
     * Note the ":" instead of a "0"
     * 
     * @param nameAndDateLine
     * @return Date in form "'Date: 'MM/dd/yy 'Time: 'hh:mma", or a different form based on dateFormat
     * @see #setDateFormat
     */
    public Date parseDate(String nameAndDateLine) {
        String dateAndTime = nameAndDateLine.substring(nameAndDateLine.indexOf("Date: "));

        int indexOfColon = dateAndTime.indexOf("/:");
        if (indexOfColon != -1) {
            dateAndTime = dateAndTime.substring(0, indexOfColon)
                    + dateAndTime.substring(indexOfColon, indexOfColon + 2).replace(':', '0')
                    + dateAndTime.substring(indexOfColon + 2);
        }

        try {
            Date lastModifiedDate;
            if (timeFormat.endsWith("a")) {
                lastModifiedDate = vssDateTimeFormat.parse(dateAndTime.trim() + "m");
            } else {
                lastModifiedDate = vssDateTimeFormat.parse(dateAndTime.trim());
            }

            return lastModifiedDate;
        } catch (ParseException pe) {
            LOG.warn(pe);
            return null;
        }
    }

    /**
     * Parse username from VSS file history
     * 
     * @param userLine
     * @return the user name who made the modification
     */
    public String parseUser(String userLine) {
        final int userIndex = "User: ".length();

        return userLine.substring(userIndex, userLine.indexOf("Date: ") - 1).trim();
    }

    /**
     * Constructs the vssDateTimeFormat based on the dateFormat for this element.
     * 
     * @see #setDateFormat
     */
    private void constructVssDateTimeFormat() {
        vssDateTimeFormat = new SimpleDateFormat("'Date: '" + dateFormat + "   'Time: '" + timeFormat, Locale.US);
    }

    protected SimpleDateFormat getVssDateTimeFormat() {
        return vssDateTimeFormat;
    }

}
