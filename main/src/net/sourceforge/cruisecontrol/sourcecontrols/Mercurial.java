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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

/**
 * This class implements the SourceControl methods for a Mercurial repository.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @see <a href="http://www.selenic.com/mercurial">Mercurial web site</a>
 */
public class Mercurial implements SourceControl {
    
    static final DateFormat HG_DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private static final Logger LOG = Logger.getLogger(Mercurial.class);

    private final SourceControlProperties properties = new SourceControlProperties();

    /**
     * Configuration parameters
     */
    private String localWorkingCopy = ".";
    private String hgCommand = INCOMING_CMD;
    
    private static final String INCOMING_CMD = "incoming"; 
    private static final String LOG_CMD = "log"; 

    static final String MODIFICATION_XML_TEMPLATE = "<hgChange>\n\t<author>{author}</author>\n\t<rev>{rev}</rev>\n\t"
            + "<node>{node}</node>\n\t<description>{desc|escape}</description>\n\t<date>{date|hgdate}</date>\n\t"
            + "<addedFiles>{file_adds}</addedFiles>\n\t<removedFiles>{file_dels}</removedFiles>\n\t"
            + "<changedFiles>{files}</changedFiles>\n</hgChange>\n";

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setProperty(final String property) {
        properties.assignPropertyName(property);
    }

    public void setPropertyOnDelete(final String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    /**
     * Sets the local working copy to use when making calls to mercurial.
     *
     * @param localWorkingCopy String indicating the relative or absolute path
     *                         to the local working copy of the mercurial
     *                         repository of which to find the log history.
     */
    public void setLocalWorkingCopy(final String localWorkingCopy) {
        this.localWorkingCopy = localWorkingCopy;
    }
    
    /**
     * Sets the hg command to use when checking modifications.
     *
     * @param hgCommand String either "incoming" or "log".
     */
    public void setHgCommand(final String hgCommand) {
        this.hgCommand = hgCommand;
    }

    /**
     * This method validates that the local working copy location has been specified.
     *
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *          Thrown when the repository location and
     *          the local working copy location are both
     *          null
     */
    public void validate() throws CruiseControlException {
        final File workingDir = new File(localWorkingCopy);
        ValidationHelper.assertTrue(workingDir.exists() && workingDir.isDirectory(),
                "'localWorkingCopy' must be an existing directory. Was " + workingDir.getAbsolutePath());
        
        ValidationHelper.assertTrue(INCOMING_CMD.equals(hgCommand) || LOG_CMD.equals(hgCommand),
                "'hgCommand' must be either " + INCOMING_CMD + " or " + LOG_CMD);
    }

    /**
     * Returns a list of modifications detailing all the changes between
     * the last build and the latest revision in the repository.
     *
     * @param  lastBuildDate date of last build
     * @param  now current date
     * @return the list of modifications, or an empty list if we failed
     *         to retrieve the changes.
     */
    public List<Modification> getModifications(final Date lastBuildDate, final Date now) {
        final String version = getMercurialVersion();
        LOG.info("Using Mercurial: '" + version + "'");

        Commandline command = null;
        List<Modification> modifications = Collections.emptyList();
        try {
            command = buildHistoryCommand(lastBuildDate, now);

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
     * Generates the command line for the hg incoming or log command.
     * <p/>
     * For example:
     * <p/>
     * 'hg incoming --template "........."'
     *
     * @param  from date of last build
     * @param  to current date
     * @return history command
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *          exception
     */
    Commandline buildHistoryCommand(final Date from, final Date to)
            throws CruiseControlException {

        final Commandline command = new Commandline();
        command.setWorkingDirectory(localWorkingCopy);
        command.setExecutable("hg");
        
        if (INCOMING_CMD.equals(hgCommand)) {
            usingIncomingToGetModifications(command);
        } else {
            usingLogToGetModifications(from, to, command);
        }

        return command;
    }

    private void usingIncomingToGetModifications(final Commandline command) {
        command.createArgument(INCOMING_CMD);
        command.createArgument("--debug");
        command.createArgument("--template");
        command.createArgument(MODIFICATION_XML_TEMPLATE);
    }

    private void usingLogToGetModifications(final Date from, final Date to, final Commandline command) {
        command.createArgument(LOG_CMD);
        command.createArgument("--debug");
        command.createArguments("--date", HG_DATE_PARSER.format(from) + " to " + HG_DATE_PARSER.format(to));
        command.createArguments("--template", MODIFICATION_XML_TEMPLATE);
        command.createArgument(new File(localWorkingCopy).getAbsolutePath());
    }

    private static List<Modification> execHistoryCommand(final Commandline command)
            throws InterruptedException, IOException, ParseException, JDOMException {

        LOG.debug("Executing command: " + command);

        final Process p = command.execute();

        final Thread stderr = logErrorStream(p);
        final InputStream commandOutputStream = p.getInputStream();
        final List<Modification> modifications = parseStream(commandOutputStream);

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

        final Commandline command = new Commandline();
        command.setWorkingDirectory(localWorkingCopy);
        command.setExecutable("hg");
        command.createArgument("version");
        return command;
    }

    private String execVersionCommand(final Commandline command) throws CruiseControlException {

        LOG.debug("Executing command: " + command);

        try {
            final Process p = command.execute();

            final Thread stderr = logErrorStream(p);
            final InputStream svnStream = p.getInputStream();
            final String revision = parseVersionStream(svnStream);

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

    static String parseVersionStream(final InputStream svnStream) throws ParseException, IOException {
        final InputStreamReader reader = new InputStreamReader(svnStream, "UTF-8");
        return HgVersionParser.parse(reader);
    }

    private static Thread logErrorStream(final Process p) {
        final Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
        stderr.start();
        return stderr;
    }

    static List<Modification> parseStream(final InputStream inputStream)
            throws JDOMException, IOException, ParseException {

        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String line;
        final StringBuffer buffer = new StringBuffer();
        boolean startFound = false;
        while ((line = br.readLine()) != null) {
            startFound |= line.startsWith("<");
            if (startFound) {
                buffer.append(line).append("\n");
            }
        }

        final Reader reader = new StringReader("<hgChanges>" + buffer.toString() + "</hgChanges>");
        try {
            return HgLogParser.parse(reader);
        } finally {
            reader.close();
        }
    }

    void fillPropertiesIfNeeded(final List modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();

            String maxRevision = "";
            for (int i = 0; i < modifications.size(); i++) {
                final Modification modification = (Modification) modifications.get(i);
                final Modification.ModifiedFile file = modification.files.get(0);
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

        static List<Modification> parse(final Reader reader)
                throws ParseException, JDOMException, IOException {

            final SAXBuilder builder = new SAXBuilder(false);
            final Document document = builder.build(reader);
            return parseDOMTree(document);
        }


        static List<Modification> parseDOMTree(final Document document)
                throws ParseException {
            final List<Modification> modifications = new ArrayList<Modification>();

            final Element rootElement = document.getRootElement();
            final List logEntries = rootElement.getChildren("hgChange");
            for (Iterator iterator = logEntries.iterator(); iterator.hasNext();) {
                final Element logEntry = (Element) iterator.next();

                final List<Modification> modificationsOfRevision = parseLogEntry(logEntry);
                modifications.addAll(modificationsOfRevision);
            }

            return modifications;
        }

        static List<Modification> parseLogEntry(final Element logEntry)
                throws ParseException {
            final List<Modification> modifications = new ArrayList<Modification>();

            final String userName = logEntry.getChildText("author");
            final String revision = logEntry.getChildText("rev") + ":" + logEntry.getChildText("node");
            final String comment = logEntry.getChildText("description");
            // final Date modifiedTime = convertIso8601Date(logEntry.getChildText("date"));
            final Date modifiedTime = convertHgDate(logEntry.getChildText("date"));
            final String[] addedFiles = getFiles(logEntry.getChildText("addedFiles"));
            final String[] removedFiles = getFiles(logEntry.getChildText("removedFiles"));
            final String[] changedFiles = getFiles(logEntry.getChildText("changedFiles"));

            addModifications(modifications, userName, revision, comment, modifiedTime, addedFiles, "added");
            addModifications(modifications, userName, revision, comment, modifiedTime, changedFiles, "modified");
            addModifications(modifications, userName, revision, comment, modifiedTime, removedFiles, "removed");

            return modifications;
        }

        private static void addModifications(final List<Modification> modifications,
                                             final String userName, final String revision,
                                             final String comment, final Date modifiedTime,
                                             final String[] files, final String action) {
            for (int i = 0; i < files.length; i++) {
                final String filePath = files[i];
                addModifications(modifications, userName, revision, comment, modifiedTime, filePath, action);
            }
        }

        private static void addModifications(final List<Modification> modifications,
                                             final String userName, final String revision,
                                             final String comment, final Date modifiedTime,
                                             final String filePath, final String action) {
            final Modification modification = new Modification("mercurial");

            modification.modifiedTime = modifiedTime;
            modification.userName = userName;
            modification.comment = comment;
            modification.revision = revision;

            final Modification.ModifiedFile modfile = modification.createModifiedFile(filePath, null);
            modfile.action = action;
            modfile.revision = modification.revision;

            modifications.add(modification);
        }

        private static String[] getFiles(final String childText) {
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
        static Date convertHgDate(final String date) throws ParseException {
            try {
                return HgDateParser.parse(date);
            } catch (IllegalArgumentException e) {
                final ParseException parseException = new ParseException(e.getMessage(), 0);
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

        private static Date parse(final String date) throws ParseException {
            final Pattern p = Pattern.compile("([0-9]*) (.*)");
            final Matcher m = p.matcher(date);
            if (!m.matches()) {
                throw new ParseException("HgDateParser: no match of " + date, 0);
            }
            final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.setTimeInMillis(Long.parseLong(m.group(1)) * 1000);
            return c.getTime();
        }
    }

    static final class HgVersionParser {
        private HgVersionParser() {
        }

        public static String parse(final Reader reader) throws ParseException, IOException {
            final BufferedReader myReader = new BufferedReader(reader);
            final String versionLine = myReader.readLine();
            if (versionLine == null) {
                throw new IllegalStateException("hg version returned nothing");
            }

            final Pattern p = Pattern.compile("Mercurial Distributed SCM \\((.*)\\)");
            final Matcher m = p.matcher(versionLine);
            if (!m.matches()) {
                throw new ParseException("HgVersionParser: no match of " + versionLine, 0);
            }
            return m.group(1);
        }
    }
}
