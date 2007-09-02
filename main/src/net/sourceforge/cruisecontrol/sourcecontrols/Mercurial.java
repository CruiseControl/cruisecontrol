/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the SourceControl methods for a Mercurial repository.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @see <a href="http://www.selenic.com/mercurial">Mercurial web site</a>
 */
public class Mercurial implements SourceControl {

    private static final Logger LOG = Logger.getLogger(Mercurial.class);

    private final SourceControlProperties properties = new SourceControlProperties();

    /**
     * Configuration parameters
     */
    private String localWorkingCopy;

    static final String INCOMING_XML_TEMPLATE = "<hgChange>\n\t<author>{author}</author>\n\t<rev>{rev}</rev>\n\t"
            + "<node>{node}</node>\n\t<description>{desc|escape}</description>\n\t<date>{date|hgdate}</date>\n\t"
            + "<addedFiles>{file_adds}</addedFiles>\n\t<removedFiles>{file_dels}</removedFiles>\n\t"
            + "<changedFiles>{files}</changedFiles>\n</hgChange>\n";

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    /**
     * Sets the local working copy to use when making calls to mercurial.
     *
     * @param localWorkingCopy String indicating the relative or absolute path
     *                         to the local working copy of the mercurial
     *                         repository of which to find the log history.
     */
    public void setLocalWorkingCopy(String localWorkingCopy) {
        this.localWorkingCopy = localWorkingCopy;
    }

    /**
     * This method validates that at least the repository location or the local
     * working copy location has been specified.
     *
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *          Thrown when the repository location and
     *          the local working copy location are both
     *          null
     */
    public void validate() throws CruiseControlException {
        if (localWorkingCopy != null) {
            File workingDir = new File(localWorkingCopy);
            ValidationHelper.assertTrue(workingDir.exists() && workingDir.isDirectory(),
                    "'localWorkingCopy' must be an existing directory. Was "
                            + workingDir.getAbsolutePath());
        }
    }

    /**
     * Returns a list of modifications detailing all the changes between
     * the last build and the latest revision in the repository.
     *
     * @return the list of modifications, or an empty list if we failed
     *         to retrieve the changes.
     */
    public List getModifications(Date lastBuildDate, Date now) {
        String version = getMercurialVersion();
        LOG.info("Using Mercurial: '" + version + "'");
        Commandline command = null;
        List modifications = Collections.EMPTY_LIST;
        try {
            command = buildHistoryCommand();

            modifications = execHistoryCommand(command);

            // TODO: should we filter out the results ?
            // modifications = filterModifications(modifications, lastBuildDate, now);

        } catch (Exception e) {
            LOG.error("Error executing mercurial history command " + command, e);
        }
        fillPropertiesIfNeeded(modifications);
        return modifications;
    }

    private String getMercurialVersion() {
        Commandline command = null;
        try {
            command = buildVersionCommand();

            return execVersionCommand(command);
        } catch (Exception e) {
            LOG.error("Error executing mercurial version command " + command, e);
            return "version unresolved...";
        }
    }

    /**
     * Generates the command line for the hg incoming command.
     * <p/>
     * For example:
     * <p/>
     * 'hg incoming --template "........."'
     *
     * @return history command
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *          exception
     */
    Commandline buildHistoryCommand()
            throws CruiseControlException {

        Commandline command = new Commandline();
        command.setExecutable("hg");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("incoming");
        // --debug required to get file_adds and file_dels to work in this version of mercurial (0.9.4+20070830)
        command.createArgument("--debug");
        command.createArgument("--template");
        command.createArgument(INCOMING_XML_TEMPLATE);

        return command;
    }

    private static List execHistoryCommand(Commandline command)
            throws InterruptedException, IOException, ParseException, JDOMException {

        LOG.debug("Executing command: " + command);

        Process p = command.execute();

        Thread stderr = logErrorStream(p);
        InputStream commandOutputStream = p.getInputStream();
        List modifications = parseStream(commandOutputStream);

        p.waitFor();
        stderr.join();
        IO.close(p);

        return modifications;
    }

    /**
     * Generates the command line for the hg version command.
     * <p/>
     * 'hg version'
     *
     * @return version command
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *          exception
     */
    Commandline buildVersionCommand()
            throws CruiseControlException {

        Commandline command = new Commandline();
        command.setExecutable("hg");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("version");

        return command;
    }

    private String execVersionCommand(Commandline command) throws CruiseControlException {

        LOG.debug("Executing command: " + command);

        try {
            Process p = command.execute();

            Thread stderr = logErrorStream(p);
            InputStream svnStream = p.getInputStream();
            String revision = parseVersionStream(svnStream);

            p.waitFor();
            stderr.join();
            IO.close(p);

            return revision;
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } catch (ParseException e) {
            throw new CruiseControlException(e);
        } catch (InterruptedException e) {
            throw new CruiseControlException(e);
        }
    }

    static String parseVersionStream(InputStream svnStream) throws ParseException, IOException {
        InputStreamReader reader = new InputStreamReader(svnStream, "UTF-8");
        return HgVersionParser.parse(reader);
    }

    private static Thread logErrorStream(Process p) {
        Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
        stderr.start();
        return stderr;
    }

    static List/*Modification*/ parseStream(InputStream inputStream)
            throws JDOMException, IOException, ParseException {

        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuffer buffer = new StringBuffer();
        boolean startFound = false;
        while ((line = br.readLine()) != null) {
            startFound |= line.startsWith("<");
            if (startFound) {
                buffer.append(line).append("\n");
            }
        }
        reader = new StringReader("<hgChanges>" + buffer.toString() + "</hgChanges>");
        try {
            return HgLogParser.parse(reader);
        } finally {
            reader.close();
        }
    }

    void fillPropertiesIfNeeded(List modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();

            String maxRevision = "";
            for (int i = 0; i < modifications.size(); i++) {
                Modification modification = (Modification) modifications.get(i);
                Modification.ModifiedFile file = (Modification.ModifiedFile) modification.files.get(0);
                if (i == modifications.size() - 1) {
                    maxRevision = modification.revision;
                }
                if (file.action.equals("deleted")) {
                    properties.deletionFound();
                }
            }
            properties.put("hgrevision", maxRevision);
        }
    }
    /*
    public static DateFormat getOutDateFormatter() {
        return Iso8601DateParser.ISO8601_DATE_PARSER;
    }
    */

    static final class HgLogParser {

        private HgLogParser() {
        }

        static List/*Modification*/ parse(Reader reader)
                throws ParseException, JDOMException, IOException {

            SAXBuilder builder = new SAXBuilder(false);
            Document document = builder.build(reader);
            return parseDOMTree(document);
        }


        static List/*Modification*/ parseDOMTree(Document document)
                throws ParseException {
            List modifications = new ArrayList();

            Element rootElement = document.getRootElement();
            List logEntries = rootElement.getChildren("hgChange");
            for (Iterator iterator = logEntries.iterator(); iterator.hasNext();) {
                Element logEntry = (Element) iterator.next();

                List/*Modification*/ modificationsOfRevision = parseLogEntry(logEntry);
                modifications.addAll(modificationsOfRevision);
            }

            return modifications;
        }

        static List/*Modification*/ parseLogEntry(Element logEntry)
                throws ParseException {
            List modifications = new ArrayList();

            String userName = logEntry.getChildText("author");
            String revision = logEntry.getChildText("rev") + ":" + logEntry.getChildText("node");
            String comment = logEntry.getChildText("description");
            // Date modifiedTime = convertIso8601Date(logEntry.getChildText("date"));
            Date modifiedTime = convertHgDate(logEntry.getChildText("date"));
            String[] addedFiles = getFiles(logEntry.getChildText("addedFiles"));
            String[] removedFiles = getFiles(logEntry.getChildText("removedFiles"));
            String[] changedFiles = getFiles(logEntry.getChildText("changedFiles"));

            addModifications(modifications, userName, revision, comment, modifiedTime, addedFiles, "added");
            addModifications(modifications, userName, revision, comment, modifiedTime, changedFiles, "modified");
            addModifications(modifications, userName, revision, comment, modifiedTime, removedFiles, "removed");

            return modifications;
        }

        private static void addModifications(List modifications, String userName, String revision,
                                             String comment, Date modifiedTime, String[] files, String action) {
            for (int i = 0; i < files.length; i++) {
                String filePath = files[i];
                addModifications(modifications, userName, revision, comment, modifiedTime, filePath, action);
            }
        }

        private static void addModifications(List modifications, String userName, String revision,
                                             String comment, Date modifiedTime, String filePath, String action) {
            Modification modification = new Modification("mercurial");

            modification.modifiedTime = modifiedTime;
            modification.userName = userName;
            modification.comment = comment;
            modification.revision = revision;

            Modification.ModifiedFile modfile = modification.createModifiedFile(filePath, null);
            modfile.action = action;
            modfile.revision = modification.revision;

            modifications.add(modification);
        }

        private static String[] getFiles(String childText) {
            if (childText.length() == 0) {
                return new String[0];
            }
            return childText.split(" ");
        }

        /**
         * Converts the specified SVN date string into a Date.
         *
         * @param date with format "2007-08-29 21:44 +0200"
         * @return converted date
         * @throws java.text.ParseException if specified date doesn't match the expected format
         */
        /*
        private static Date convertIso8601Date(String date) throws ParseException {
            try {
                return Iso8601DateParser.parse(date);
            } catch (IllegalArgumentException e) {
                throw new ParseException(e.getMessage(), 0);
            }
        }
        */

        /**
         * Converts the specified SVN date string into a Date.
         *
         * @param date with format "2007-08-29 21:44 +0200"
         * @return converted date
         * @throws java.text.ParseException if specified date doesn't match the expected format
         */
        static Date convertHgDate(String date) throws ParseException {
            try {
                return HgDateParser.parse(date);
            } catch (IllegalArgumentException e) {
                ParseException parseException = new ParseException(e.getMessage(), 0);
                parseException.initCause(e);
                throw parseException;
            }
        }
    }


    // not used as Mercurial don't display the seconds. Too bad. Will this be fixed in a further revision ?
    // would be nice as is a more readable thatn the hgdate format... 
    // 2007-08-29 21:44 +0200
    /*
    private static final class Iso8601DateParser {
        private Iso8601DateParser() {
        }

        private static final SimpleDateFormat ISO8601_DATE_PARSER = new SimpleDateFormat("yyyy-MM-d HH:mm Z");

        private static Date parse(String date) throws ParseException {
            return ISO8601_DATE_PARSER.parse(date);
        }
    }
    */

    // 1188223879 -7200
    private static final class HgDateParser {
        private HgDateParser() {
        }

        private static Date parse(String date) throws ParseException {
            Pattern p = Pattern.compile("([0-9]*) (.*)");
            Matcher m = p.matcher(date);
            if (!m.matches()) {
                throw new ParseException("HgDateParser: no match of " + date, 0);
            }
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.setTimeInMillis(Long.parseLong(m.group(1)) * 1000);
            return c.getTime();
        }
    }

    static final class HgVersionParser {
        private HgVersionParser() {
        }

        public static String parse(Reader reader) throws ParseException, IOException {
            BufferedReader myReader = new BufferedReader(reader);
            String versionLine = myReader.readLine();
            if (versionLine == null) {
                throw new IllegalStateException("hg version returned nothing");
            }

            Pattern p = Pattern.compile("Mercurial Distributed SCM \\((.*)\\)");
            Matcher m = p.matcher(versionLine);
            if (!m.matches()) {
                throw new ParseException("HgVersionParser: no match of " + versionLine, 0);
            }
            return m.group(1);
        }
    }
}