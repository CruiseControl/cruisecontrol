package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * The class implements the SourceControl interface to allow communication with
 * Microsoft Visual Studio Team Foundation Server
 * 
 * @author <a href="http://www.woodwardweb.com">Martin Woodward</a>
 */
public class TeamFoundationServer implements SourceControl {

    private static final Logger LOG = Logger.getLogger(TeamFoundationServer.class);

    /** UTC Date format - best one to pass dates across the wire. */
    private static final String TFS_UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** Configuration parameters */

    private String server;
    private String projectPath;
    private String username;
    private String password;
    private String tfPath = "tf";
    private String options;

    private final SourceControlProperties properties = new SourceControlProperties();

    /**
     * The main getModification method called by the build loop. Responsible for
     * querying history from TFS, parsing the results and then transforming that
     * into a list of CruiseControl Modification objects.
     * 
     * @see net.sourceforge.cruisecontrol.SourceControl
     *      getModifications(java.util.Date, java.util.Date)
     */
    public List getModifications(Date lastBuild, Date now) {

        List modifications = new ArrayList();
        final Commandline command = buildHistoryCommand(lastBuild, now);

        try {
            modifications = execHistoryCommand(command, lastBuild);
        } catch (Exception e) {
            LOG.error("Error executing tf history command " + command, e);
        }

        fillPropertiesIfNeeded(modifications);
        return modifications;
    }

    /**
     * Populate the source control properties.  As well as detecting if modifications found
     * and if any deletion is found, also put the maximum changeset found in the modification list.
     * The changeset ID represents the state of the repository at the time the modifications were
     * detected and therefore can be used in a subsquent get, label etc to ensure consistency.
     * @param modifications the list of modifications reported by TFS
     */
    void fillPropertiesIfNeeded(List modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();
            int maxChangset = 0;
            for (int i = 0; i < modifications.size(); i++) {
                Modification modification = (Modification) modifications.get(i);
                maxChangset = Math.max(maxChangset, Integer.parseInt(modification.revision));
                Modification.ModifiedFile file = (Modification.ModifiedFile) modification.files.get(0);
                if (file.action.equals("delete")) {
                    properties.deletionFound();
                    break;
                }
            }
            properties.put("tfschangeset", "" + maxChangset);
        }
    }
    
    
    /**
     * Build a history command like the following:-
     * 
     * tf history -noprompt -server:http://tfsserver:8080 $/TeamProjectName/path
     * -version:D2006-12-01T01:01:01Z~D2006-12-13T20:00:00Z -recursive
     * -format:detailed -login:DOMAIN\name,password
     * 
     * For more details on history command syntax see
     * 
     * <a href="http://msdn2.microsoft.com/en-us/library/yxtbh4yh(VS.80).aspx">
     * http://msdn2.microsoft.com/en-us/library/yxtbh4yh(VS.80).aspx </a>
     */
    Commandline buildHistoryCommand(Date lastBuild, Date now)  {

        Commandline command = new Commandline();
        command.setExecutable(tfPath);
        command.createArgument().setValue("history");
        command.createArgument().setValue("-noprompt");
        command.createArgument().setValue("-server:" + server);
        command.createArgument().setValue(projectPath);

        command.createArgument().setValue("-version:D" + formatUTCDate(lastBuild) + "~D" + formatUTCDate(now));

        command.createArgument().setValue("-recursive");
        command.createArgument().setValue("-format:detailed");

        if (username != null && password != null) {
            command.createArgument().setValue("-login:" + username + "," + password + "");
        }

        if (options != null) {
            command.createArgument().setValue(options);
        }

        LOG.debug("Executing command: " + command);

        return command;
    }

    private List execHistoryCommand(Commandline command, Date lastBuild)
            throws InterruptedException, IOException, ParseException {

        Process p = command.execute();

        logErrorStream(p);
        InputStream svnStream = p.getInputStream();
        List modifications = parseStream(svnStream, lastBuild);

        p.waitFor();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();

        return modifications;
    }

    /**
     * Helper method to send stderr from the tf command to CruiseControl stderr
     */
    private void logErrorStream(Process p) {
        Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
        stderr.start();
    }

    /**
     * Parse the result stream. Delegates to the TFSHistoryParser.parse method.
     */
    private List parseStream(InputStream tfStream, Date lastBuild)
            throws IOException, ParseException {
        
        InputStreamReader reader = new InputStreamReader(tfStream, "UTF-8");
        return TFHistoryParser.parse(reader, lastBuild);
    }

    /**
     * Convert the passed date into the UTC Date format best used when talking
     * to Team Foundation Server command line.
     */
    static String formatUTCDate(Date date) {
        DateFormat f = new SimpleDateFormat(TFS_UTC_DATE_FORMAT);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(date);
    }

    /**
     * @see net.sourceforge.cruisecontrol.SourceControl#getProperties()
     */
    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    /**
     * Validates that the plug-in has its mandatory inputs satisfied. The only
     * mandatory requirements are a server and project path.
     * 
     * @see net.sourceforge.cruisecontrol.SourceControl#validate()
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(server, "server", this.getClass());
        ValidationHelper.assertIsSet(projectPath, "projectPath", this.getClass());
        ValidationHelper.assertTrue(projectPath.startsWith("$/"), "A TFS server path must begin with $/");
    }

    /**
     * Internal class to handle parsing of TF command line output.
     */
    static final class TFHistoryParser {

        private TFHistoryParser() {
        }

        private static final String CHANGESET_SEPERATOR = "---------------------------------";

        /**
         * The magic regex to identify the key data elements within the
         * changeset *
         */
        private static final Pattern PATTERN_CHANGESET = Pattern.compile("^[^:]*:[ \t]([0-9]*)\n"
                + "[^:]*:[ \t](.*)\n[^:]*:[ \t](.*)\n" 
                + "[^:]*:((?:\n.*)*)\n\n[^\n :]*:(?=\n  )((?:\n[ \t]+.*)*)");

        /**
         * An additional regex to split the items into their parts (change type
         * and filename) *
         */
        private static final Pattern PATTERN_ITEM = Pattern.compile("\n  ([^$]+) (\\$/.*)");

        /**
         * Parse the passed stream of data from the command line.
         */
        static List parse(Reader reader, Date lastBuild) throws IOException, ParseException {
            ArrayList modifications = new ArrayList();
            StringBuffer buffer = new StringBuffer();

            BufferedReader br = new BufferedReader(reader);
            String line;
            int linecount = 0;

            while ((line = br.readLine()) != null) {
                linecount++;
                if (line.startsWith(CHANGESET_SEPERATOR)) {
                    if (linecount > 1) {
                        // We are starting a new changeset.
                        modifications.addAll(parseChangeset(buffer.toString(), lastBuild));
                        buffer.setLength(0);
                    }
                } else {
                    buffer.append(line).append('\n');
                }
            }

            // Add the last changeset
            modifications.addAll(parseChangeset(buffer.toString(), lastBuild));

            return modifications;
        }

        /**
         * Parse the changeset data and convert into a list of CruiseControl
         * modifications.
         */
        static ArrayList parseChangeset(String data, Date lastBuild) throws ParseException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parsing Changeset Data:\n" + data);
            }

            ArrayList modifications = new ArrayList();

            Matcher m = PATTERN_CHANGESET.matcher(data);
            if (m.find()) {
                String revision = m.group(1);
                String userName = m.group(2);

                Date modifiedTime = parseDate(m.group(3));
                
                // CC-735.  Ignore changesets that occured before the specified lastBuild.
                if (modifiedTime.compareTo(lastBuild) < 0) {
                    return new ArrayList();
                }

                // Remove the indentation from the comment
                String comment = m.group(4).replaceAll("\n  ", "\n");
                if (comment.length() > 0) {
                    // remove leading "\n"
                    comment = comment.trim();
                }

                // Parse the items.
                Matcher itemMatcher = PATTERN_ITEM.matcher(m.group(5));
                int items = 0;
                while (itemMatcher.find()) {
                    items++;
                    // Create the modification. Note that although the
                    // Modification class model supports more than one Modified
                    // file per modification most of the things downstream (such
                    // as the report JSP, email noticiation etc) do not take
                    // this into account. Therefore we flatten 1 changeset
                    // containing three files into three modifications
                    // with the same revision.

                    Modification modification = new Modification("tfs");
                    modification.revision = revision;
                    modification.userName = userName;
                    modification.modifiedTime = modifiedTime;
                    modification.comment = comment;

                    // In a similar way to Subversion, TFS will record additions
                    // of folders etc
                    // Therefore we have to report all modifictaion by the file
                    // and not split
                    // into file and folder as there is no easy way to
                    // distinguish
                    // $/path/filename
                    // from
                    // $/path/foldername
                    //
                    Modification.ModifiedFile modfile = modification.createModifiedFile(itemMatcher.group(2), null);
                    if (!modfile.fileName.startsWith("$/")) {
                        // If this happens then we have a bug, output some data
                        // to make it easy to figure out what the problem was so
                        // that we can fix it.
                        throw new ParseException("Parse error. Mistakenly identified \"" + modfile.fileName
                                + "\" as an item, but it does not appear to "
                                + "be a valid TFS path.  Please report this as a bug.  Changeset" + "data = \"\n"
                                + data + "\n\".", itemMatcher.start());
                    }
                    modfile.action = itemMatcher.group(1).trim();
                    modfile.revision = modification.revision;

                    modifications.add(modification);
                }
                if (items < 1) {
                    // We should always find at least one item. If we don't
                    // then this will be because we have not parsed correctly.
                    throw new ParseException("Parse error. Unable to find an item within "
                            + "a changeset.  Please report this as a bug.  Changeset" 
                            + "data = \"\n" + data + "\n\".",
                            0);
                }
            }

            return modifications;
        }

        protected static Date parseDate(String dateString) throws ParseException {
            Date date = null;
            try {
                // Use the deprecated Date.parse method as this is very good at detecting
                // dates commonly output by the US and UK standard locales of dotnet that
                // are output by the Microsoft command line client.
                date = new Date(Date.parse(dateString));
            } catch (IllegalArgumentException e) {
                // ignore - parse failed.
            }
            if (date == null) {
                // The old fashioned way did not work. Let's try it using a more
                // complex alternative.
                DateFormat[] formats = createDateFormatsForLocaleAndTimeZone(null, null);
                return parseWithFormats(dateString, formats);
            }
            return date;
        }

        private static Date parseWithFormats(String input, DateFormat[] formats) throws ParseException {
            ParseException parseException = null;
            for (int i = 0; i < formats.length; i++) {
                try {
                    return formats[i].parse(input);
                } catch (ParseException ex) {
                    parseException = ex;
                }
            }

            throw parseException;
        }

        /**
         * Build an array of DateFormats that are commonly used for this locale
         * and timezone.
         */
        private static DateFormat[] createDateFormatsForLocaleAndTimeZone(Locale locale, TimeZone timeZone) {
            if (locale == null) {
                locale = Locale.getDefault();
            }

            if (timeZone == null) {
                timeZone = TimeZone.getDefault();
            }

            List formats = new ArrayList();

            for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
                for (int timeStyle = DateFormat.FULL; timeStyle <= DateFormat.SHORT; timeStyle++) {
                    DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
                    if (timeZone != null) {
                        df.setTimeZone(timeZone);
                    }
                    formats.add(df);
                }
            }

            for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
                DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
                df.setTimeZone(timeZone);
                formats.add(df);
            }

            return (DateFormat[]) formats.toArray(new DateFormat[formats.size()]);
        }

    }

    // --- Property setters

    /**
     * If the username or password is not supplied, then none will be passed to
     * the command. On windows system using the Microsoft tf.exe command line
     * client, the credential of that the CruiseControl process is running as
     * will be used for the connection to the server.
     * 
     * @param password
     *            the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Mandatory. The path from which you want to check for modifications.
     * Usually something like &quot;$/TeamProjectName/path/to/project&quot;
     * 
     * Any changes in and folder in that path or below will register as
     * modifications.
     * 
     * @param projectPath
     *            the projectPath to set
     */
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * The server to talk to. The easiest way to define this is in the URL
     * format http://servername:8080 where the URL is that to the TFS
     * Application Tier. On windows systems running in an environment where the
     * server has already been registered (using the Microsoft graphical client
     * for example) and the tf command being used is the Microsoft one, then the
     * servername only could be used as it will resolve this in the registry -
     * however the URL syntax is preferred as it is more accurate and easier to
     * change.
     * 
     * @param server
     *            the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * The username to use when talking to TFS. This should be in the format
     * DOMAIN\name or name@DOMAIN if the domain portion is required. Note that
     * name@DOMAIN is the easiest format to use from Unix based systems. If the
     * username contains characters likely to cause problems when passed to the
     * command line then they can be escaped in quotes by passing the following
     * into the config.xml:- <code>&amp;quot;name&amp;quot;</code>
     * 
     * If the username or password is not supplied, then none will be passed to
     * the command. On windows system using the Microsoft tf.exe command line
     * client, the credential of that the CruiseControl process is running as
     * will be used for the connection to the server.
     * 
     * @param username
     *            the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * The path to the tf command. Either the &quot;tf.exe&quot; command
     * provided by Microsoft in the <a
     * href="http://download.microsoft.com/download/2/a/d/2ad44873-8ccb-4a1b-9c0d-23224b3ba34c/VSTFClient.img">
     * Team Explorer Client</a> can be used or the &quot;tf&quot; command line
     * client provided by <a href="http://www.teamprise.com">Teamprise</a> can
     * be used. The Teamprise client works cross-platform. Both clients are free
     * to use provided the developers using CruiseControl have a TFS Client
     * Access License (and in the case of Teamprise a license to the Teamprise
     * command line client).
     * 
     * If not supplied then the command "tf" will be called and CruiseControl
     * will rely on that command being able to be found in the path.
     * 
     * @param tfPath
     *            the path where the tf command resides
     */
    public void setTfPath(String tfPath) {
        this.tfPath = tfPath;
    }

    /**
     * An optional argument to add to the end of the history command that is
     * generated
     * 
     * @param options
     *            the options to set
     */
    public void setOptions(String options) {
        this.options = options;
    }
}
