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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class implements the SourceControl methods for a Subversion repository.
 * The call to Subversion is assumed to work without any setup. This implies
 * that either authentication data must be available or the login parameters are
 * specified in the cc configuration file.
 *
 * Note: You can also observe for changes a Subversion repository that you have
 *       not checked out locally.
 *
 * @see    <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
 * @author <a href="etienne.studer@canoo.com">Etienne Studer</a>
 */
public class SVN implements SourceControl {

    /** serialVersionUID */
    private static final long serialVersionUID = -144583234813298598L;

    private static final Logger LOG = Logger.getLogger(SVN.class);

    /** Date format expected by Subversion */
    private static final String SVN_DATE_FORMAT_IN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** Date format returned by Subversion in XML output */
    private static final String SVN_DATE_FORMAT_OUT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private final SourceControlProperties properties = new SourceControlProperties();

    /** Configuration parameters */
    private String repositoryLocation;
    private String localWorkingCopy;
    private String userName;
    private String password;
    private String configDir;
    private boolean checkExternals = false;

    private boolean useLocalRevision = false;

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    /**
     * @param configDir the configuration directory for the subversion client.
     */
    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

    /**
     * Sets whether externals used by the project should also be checked
     * for modifications.
     *
     * @param value true/false
     */
    public void setCheckExternals(boolean value) {
        checkExternals = value;
    }

    /**
     * Sets the repository location to use when making calls to Subversion.
     *
     * @param repositoryLocation  String indicating the url to the Subversion
     *                            repository on which to find the log history.
     */
    public void setRepositoryLocation(String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    /**
     * Sets the local working copy to use when making calls to Subversion.
     *
     * @param localWorkingCopy  String indicating the relative or absolute path
     *                          to the local working copy of the Subversion
     *                          repository of which to find the log history.
     */
    public void setLocalWorkingCopy(String localWorkingCopy) {
        this.localWorkingCopy = localWorkingCopy;
    }

    /**
     * Sets the username for authentication.
     * @param userName svn user
     */
    public void setUsername(String userName) {
        this.userName = userName;
    }

    /**
     * Sets the password for authentication.
     * @param password svn password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This method validates that at least the repository location or the local
     * working copy location has been specified.
     *
     * @throws CruiseControlException  Thrown when the repository location and
     *                                 the local working copy location are both
     *                                 null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(repositoryLocation != null || localWorkingCopy != null,
                "At least 'repositoryLocation'or 'localWorkingCopy' is a required attribute on the Subversion task ");

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
     * @return the list of modifications, or an empty list if we failed
     * to retrieve the changes.
     */
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        HashMap<String, List<String[]>> directories = new HashMap<String, List<String[]>>();
        Commandline propCommand = new Commandline();
        // the propget command can be pretty expensive on large projects
        // so only execute if the checkExternals flag is set in the config
        if (checkExternals) {
            try {
                propCommand = buildPropgetCommand();
            } catch (CruiseControlException e) {
                LOG.error("Error building history command", e);
            }
            try {
                directories = execPropgetCommand(propCommand);
            } catch (Exception e) {
                LOG.error("Error executing svn propget command " + propCommand, e);
            }
        }

        final List<Modification> modifications = new ArrayList<Modification>();
        Commandline command;

        final HashMap<Commandline, String> commandsAndPaths = new HashMap<Commandline, String>();
        try {
            // always check the root
            final String startRevision = formatSVNDate(lastBuild);
            String endRevision;
            if (useLocalRevision) {
                endRevision = execInfoCommand(buildInfoCommand(null));
            } else {
                endRevision = formatSVNDate(now);
            }
            command = buildHistoryCommand(startRevision, endRevision);
            commandsAndPaths.put(command, null);
            for (final String directory : directories.keySet()) {
                if (useLocalRevision) {
                    endRevision = execInfoCommand(buildInfoCommand(directory));
                } else {
                    endRevision = formatSVNDate(now);
                }
                for (final String[] external : directories.get(directory)) {
                    final String path = directory + "/" + external[0];
                    final String svnURL = external[1];
                    if (repositoryLocation != null) {
                        command = buildHistoryCommand(startRevision, endRevision, svnURL);
                        commandsAndPaths.put(command, null);
                    } else {
                        command = buildHistoryCommand(startRevision, endRevision, svnURL);
                        commandsAndPaths.put(command, path);
                    }
                }
            }
        } catch (CruiseControlException e) {
            LOG.error("Error building history command", e);
            return modifications;
        }
        try {
            for (final Commandline commandline : commandsAndPaths.keySet()) {
                command = commandline;
                final String path = commandsAndPaths.get(command);
                modifications.addAll(execHistoryCommand(
                        command, lastBuild, path));
            }
        } catch (Exception e) {
            LOG.error("Error executing svn log command " + command, e);
        }
        fillPropertiesIfNeeded(modifications);
        return modifications;
    }

    /**
     * Generates the command line for the svn propget command.
     *
     * For example:
     *
     * 'svn propget -R svn:externals repositoryLocation'
     * @return new command line object
     * @throws net.sourceforge.cruisecontrol.CruiseControlException if working directory is invalid
     */
    Commandline buildPropgetCommand() throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable("svn");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("propget");
        command.createArgument("-R");
        command.createArgument("--non-interactive");
        command.createArgument("svn:externals");

        if (repositoryLocation != null) {
            command.createArgument(repositoryLocation);
        }

        LOG.debug("Executing command: " + command);

        return command;
    }

    Commandline buildInfoCommand(String path) throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable("svn");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }
        command.createArgument("info");
        command.createArgument("--xml");
        if (path != null) {
            command.createArgument(path);
        }
        LOG.debug("Executing command: " + command);

        return command;
    }
    /**
     * Generates the command line for the svn log command.
     *
     * For example:
     *
     * 'svn log --non-interactive --xml -v -r "{lastbuildTime}":"{checkTime}" repositoryLocation'
     * @return history command
     * @param lastBuild date
     * @param checkTime checkTime
     * @throws net.sourceforge.cruisecontrol.CruiseControlException exception
     */
    Commandline buildHistoryCommand(String lastBuild, String checkTime)
        throws CruiseControlException {
        return buildHistoryCommand(lastBuild, checkTime, null);
    }

    Commandline buildHistoryCommand(String lastBuild, String checkTime, String path)
        throws CruiseControlException {

        Commandline command = new Commandline();
        command.setExecutable("svn");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("log");
        command.createArgument("--non-interactive");
        command.createArgument("--xml");
        command.createArgument("-v");
        command.createArgument("-r");
        command.createArgument(lastBuild + ":" + checkTime);

        if (configDir != null) {
            command.createArguments("--config-dir", configDir);
        }
        if (userName != null || password != null) {
            command.createArgument("--no-auth-cache");
            if (userName != null) {
                command.createArguments("--username", userName);
            }
            if (password != null) {
                command.createArguments("--password", password);
            }
        }
        if (path != null) {
            command.createArgument(path);
        } else if (repositoryLocation != null) {
            command.createArgument(repositoryLocation);
        }

        LOG.debug("Executing command: " + command);

        return command;
    }

    static String formatSVNDate(Date date) {
        return formatSVNDate(date, Util.isWindows());
    }

    static String formatSVNDate(Date lastBuild, boolean isWindows) {
        DateFormat f = new SimpleDateFormat(SVN_DATE_FORMAT_IN);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateStr = f.format(lastBuild);
        if (isWindows) {
            return "\"{" + dateStr + "}\"";
        } else {
            return "{" + dateStr + "}";
        }
    }

    private static HashMap<String, List<String[]>> execPropgetCommand(Commandline command)
        throws InterruptedException, IOException {

        final Process p = command.execute();

        final Thread stderr = logErrorStream(p);
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), "UTF8"));

        final HashMap<String, List<String[]>> directories = new HashMap<String, List<String[]>>();
        try {
            parsePropgetReader(reader, directories);

            p.waitFor();
            stderr.join();
        } finally {
            reader.close();
            IO.close(p);
        }

        return directories;
    }

    /**
     * Parse results from exec of propget command for svn externals.
     * @param reader exec reader (UTF-8)
     * @param directories will be populated with external directories
     * @throws IOException if an error occurs
     */
    static void parsePropgetReader(final BufferedReader reader, final Map<String, List<String[]>> directories)
            throws IOException {

        String line;
        String currentDir = null;

        while ((line = reader.readLine()) != null) {
            String[] split = line.split(" - ");
            // the directory containing the externals
            if (split.length > 1) {
                currentDir = split[0];
                directories.put(currentDir, new ArrayList<String[]>());
                line = split[1];
            }
            split = line.split("\\s+"); // CC-949: "\\s" fails if multiple spaces exist as separator in external path
            if (!split[0].equals("")) {
                final List<String[]> externals = directories.get(currentDir);
                // split contains: [externalPath, externalSvnURL]
                externals.add(split);
            }
        }
    }

    private static List<Modification> execHistoryCommand(final Commandline command, final Date lastBuild,
                                    final String externalPath)
        throws InterruptedException, IOException, ParseException, JDOMException {

        final Process p = command.execute();

        final Thread stderr = logErrorStream(p);
        final InputStreamReader reader = new InputStreamReader(p.getInputStream(), "UTF-8");

        final List<Modification> modifications;
        try {
            modifications = SVNLogXMLParser.parseAndFilter(reader, lastBuild, externalPath);

            p.waitFor();
            stderr.join();
        } finally {
            reader.close();
            IO.close(p);
        }

        return modifications;
    }

    private String execInfoCommand(final Commandline command) throws CruiseControlException {
        try {
            final Process p = command.execute();

            final Thread stderr = logErrorStream(p);
            final InputStream svnStream = p.getInputStream();
            final InputStreamReader reader = new InputStreamReader(svnStream, "UTF-8");
            final String revision;
            try {
                revision = SVNInfoXMLParser.parse(reader);

                p.waitFor();
                stderr.join();
            } finally {
                reader.close();
                IO.close(p);
            }

            return revision;
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } catch (JDOMException e) {
            throw new CruiseControlException(e);
        } catch (InterruptedException e) {
            throw new CruiseControlException(e);
        }
    }

    private static Thread logErrorStream(Process p) {
        final Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
        stderr.start();
        return stderr;
    }

    void fillPropertiesIfNeeded(final List<Modification> modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();
            int maxRevision = 0;
            for (final Modification modification : modifications) {
                maxRevision = Math.max(maxRevision, Integer.parseInt(modification.revision));
                final Modification.ModifiedFile file = modification.files.get(0);
                if (file.action.equals("deleted")) {
                    properties.deletionFound();
                }
            }
            properties.put("svnrevision", "" + maxRevision);
        } else {
            String endRevision;
            Commandline infoCommand;
            try {
                infoCommand = buildInfoCommand(null);
            } catch (CruiseControlException e) {
                LOG.error("Error building svn info command", e);
                return;
            }
            try {
                endRevision = execInfoCommand(infoCommand);
                properties.put("svnrevision", "" + endRevision);
            } catch (CruiseControlException e) {
                LOG.error("Error executing svn info command " + infoCommand, e);
            }
        }
    }

    public static DateFormat getOutDateFormatter() {
        DateFormat f = new SimpleDateFormat(SVN_DATE_FORMAT_OUT);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f;
    }

    static final class SVNLogXMLParser {

        private SVNLogXMLParser() {
        }

        static List parseAndFilter(Reader reader, Date lastBuild)
                throws ParseException, JDOMException, IOException {
            return parseAndFilter(reader, lastBuild, null);
        }

        static List<Modification> parseAndFilter(final Reader reader, final Date lastBuild, final String externalPath)
                throws ParseException, JDOMException, IOException {
            final Modification[] modifications = parse(reader, externalPath);
            return filterModifications(modifications, lastBuild);
        }

        static Modification[] parse(Reader reader)
                throws ParseException, JDOMException, IOException {
            return parse(reader, null);
        }

        static Modification[] parse(Reader reader, String externalPath)
                throws ParseException, JDOMException, IOException {

            SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
            Document document = builder.build(reader);
            return parseDOMTree(document, externalPath);
        }

        static Modification[] parseDOMTree(final Document document, final String externalPath)
                throws ParseException {

            final List<Modification> modifications = new ArrayList<Modification>();

            final Element rootElement = document.getRootElement();
            final List logEntries = rootElement.getChildren("logentry");
            for (final Object logEntry1 : logEntries) {
                final Element logEntry = (Element) logEntry1;

                final Modification[] modificationsOfRevision =
                        parseLogEntry(logEntry, externalPath);
                modifications.addAll(Arrays.asList(modificationsOfRevision));
            }

            return modifications.toArray(new Modification[modifications.size()]);
        }

        static Modification[] parseLogEntry(final Element logEntry, final String externalPath)
                throws ParseException {

            final List<Modification> modifications = new ArrayList<Modification>();

            final Element logEntryPaths = logEntry.getChild("paths");
            if (logEntryPaths != null) {
                final List paths = logEntryPaths.getChildren("path");
                for (final Object path1 : paths) {
                    Element path = (Element) path1;

                    Modification modification = new Modification("svn");

                    modification.modifiedTime = convertDate(logEntry.getChildText("date"));
                    modification.userName = logEntry.getChildText("author");
                    modification.comment = logEntry.getChildText("msg");
                    modification.revision = logEntry.getAttributeValue("revision");

                    Modification.ModifiedFile modfile = modification.createModifiedFile(path.getText(), null);
                    // modfile.folderName seems to add too many /'s
                    if (externalPath != null) {
                        modfile.fileName = "/" + externalPath + ":" + modfile.fileName;
                    }
                    modfile.action = convertAction(path.getAttributeValue("action"));
                    modfile.revision = modification.revision;

                    modifications.add(modification);
                }
            }

            return modifications.toArray(new Modification[modifications.size()]);
        }

        /**
         * Converts the specified SVN date string into a Date.
         * @param date with format "yyyy-MM-dd'T'HH:mm:ss.SSS" + "...Z"
         * @return converted date
         * @throws ParseException if specified date doesn't match the expected format
         */
        static Date convertDate(String date) throws ParseException {
            final int zIndex = date.indexOf('Z');
            if (zIndex - 3 < 0) {
                throw new ParseException(date
                        + " doesn't match the expected subversion date format", date.length());
            }
            String withoutMicroSeconds = date.substring(0, zIndex - 3);

            return getOutDateFormatter().parse(withoutMicroSeconds);
        }

        static String convertAction(String action) {
            if (action.equals("A")) {
                return "added";
            }
            if (action.equals("M")) {
                return "modified";
            }
            if (action.equals("D")) {
                return "deleted";
            }
            return "unknown";
        }

        /**
         * Unlike CVS, Subversion maps dates to revisions which leads to a
         * different behavior when using the svn log command in conjunction with
         * dates, e.g., a date maps to a revision but the revision may have been
         * created earlier than the specified date. Therefore, if we are only
         * interested in changes that took place after the last build date, we
         * have to filter the modifications returned from the log command and
         * omit modifications that are older than the last build date.
         *
         * @see <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
         * @return subset of modifications
         * @param modifications source
         * @param lastBuild last build date
         */
        static List<Modification> filterModifications(final Modification[] modifications, final Date lastBuild) {
            final List<Modification> filtered = new ArrayList<Modification>();
            for (final Modification modification : modifications) {
                if (modification.modifiedTime.getTime() > lastBuild.getTime()) {
                    filtered.add(modification);
                }
            }
            return filtered;
        }
    }

    static final class SVNInfoXMLParser {
        private SVNInfoXMLParser() { }
        public static String parse(final Reader reader) throws JDOMException, IOException {
            final SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
            final Document document = builder.build(reader);
            return document.getRootElement().getChild("entry").getAttribute("revision").getValue();
        }

    }

    public void setUseLocalRevision(boolean useLocalRevision) {
        this.useLocalRevision = useLocalRevision;
    }
}
