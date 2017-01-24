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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.CommandExecutor;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.DiscardConsumer;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * This class implements the SourceControlElement methods for a P4 depot. The call to CVS is assumed to work without any
 * setup. This implies that if the authentication type is pserver the call to cvs login should be done prior to calling
 * this class. <p> P4Element depends on the optional P4 package delivered with Ant v1.3. But since it probably doesn't
 * make much sense using the P4Element without other P4 support it shouldn't be a problem. <p> P4Element sets the
 * property ${p4element.change} with the latest changelist number or the changelist with the latest date. This should
 * then be passed into p4sync or other p4 commands.
 *
 * @author <a href="mailto:niclas.olofsson@ismobile.com">Niclas Olofsson - isMobile.com</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Tim McCune
 * @author J D Glanville
 * @author Patrick Conant Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 * Licensed under the CruiseControl BSD license
 * @author John Lussmyer
 */
public class P4 implements SourceControl {

    private static final Logger LOG = Logger.getLogger(P4.class);

    private String p4Port;
    private String p4Client;
    private String p4User;
    private String p4View;
    private String p4Passwd;
    private boolean correctForServerTime = true;
    private boolean useP4Email = true;

    private final SimpleDateFormat p4RevisionDateFormatter = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");
    private final SourceControlProperties properties = new SourceControlProperties();

    private static final String SERVER_DATE = "Server date: ";
    private static final String P4_SERVER_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    public void setPort(String p4Port) {
        this.p4Port = p4Port;
    }

    public void setClient(String p4Client) {
        this.p4Client = p4Client;
    }

    public void setUser(String p4User) {
        this.p4User = p4User;
    }

    public void setView(String p4View) {
        this.p4View = p4View;
    }

    public void setPasswd(String p4Passwd) {
        this.p4Passwd = p4Passwd;
    }

    /**
     * Indicates whether to correct for time differences between the p4 server and the CruiseControl server. Setting the
     * flag to "true" will correct for both time zone differences and for non-synchronized system clocks.
     * @param flag "true" will correct for both time zone differences and for non-synchronized system clocks.
     */
    public void setCorrectForServerTime(boolean flag) {
        correctForServerTime = flag;
    }

    /**
     * Sets if the Email address for the user should be retrieved from Perforce.
     *
     * @param flag
     *            true to retrieve email addresses from perforce.
     */
    public void setUseP4Email(boolean flag) {
        useP4Email = flag;
    }

    public void setProperty(String propertyName) {
        properties.assignPropertyName(propertyName);
    }

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(p4Port, "port", this.getClass());
        ValidationHelper.assertIsSet(p4Client, "client", this.getClass());
        ValidationHelper.assertIsSet(p4User, "user", this.getClass());
        ValidationHelper.assertIsSet(p4View, "view", this.getClass());
        ValidationHelper.assertNotEmpty(p4Passwd, "passwd", this.getClass());
    }

    /**
     * Get a List of modifications detailing all the changes between now and the last build. Return this as an element.
     * It is not necessary for sourcecontrols to actually do anything other than returning a chunk of XML data back.
     *
     * @param lastBuild
     *            time of last build
     * @param now
     *            time this build started
     * @return a list of XML elements that contains data about the modifications that took place. If no changes, this
     *         method returns an empty list.
     */
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        List<Modification> mods = new ArrayList<Modification>();
        try {
            final String[] changelistNumbers = collectChangelistSinceLastBuild(lastBuild, now);
            if (changelistNumbers.length == 0) {
                return mods;
            }
            mods = describeAllChangelistsAndBuildOutput(changelistNumbers);
        } catch (Exception e) {
            LOG.error("Log command failed to execute succesfully", e);
        }

        if (!mods.isEmpty()) {
            properties.modificationFound();
        }

        return mods;
    }

    private List<Modification> describeAllChangelistsAndBuildOutput(final String[] changelistNumbers) throws Exception {
        final Commandline command = buildDescribeCommand(changelistNumbers);
        LOG.debug(command.toString());
        final Process p = command.execute();

        final Thread error = logErrorStream(p.getErrorStream());
        final InputStream p4Stream = p.getInputStream();
        final List<Modification> mods = parseChangeDescriptions(p4Stream);
        getRidOfLeftoverData(p4Stream);

        // Get the Email address of the user for each changelist
        if ((mods.size() > 0) && useP4Email) {
            getEmailAddresses(mods);
        }

        p.waitFor();
        error.join();
        IO.close(p);

        return mods;
    }

    /**
     * Get the Email Address of the users who submitted the change lists.
     *
     * @param mods
     *            List of P4Modification structures
     * @throws IOException if something breaks
     * @throws InterruptedException if something breaks
     */
    private void getEmailAddresses(final List<Modification> mods) throws IOException, InterruptedException {
        final Iterator<Modification> iter = mods.iterator();
        final Map<String, String> users = new HashMap<String, String>();

        while (iter.hasNext()) {
            final P4Modification change = (P4Modification) iter.next();

            if ((change.userName != null) && (change.userName.length() > 0)) {
                change.emailAddress = users.get(change.userName);

                if (change.emailAddress == null) {
                    change.emailAddress = getUserEmailAddress(change.userName);
                    users.put(change.userName, change.emailAddress);
                }
            }

        }

    }

    /**
     * Get the Email Address for the given P4 User
     *
     * @param username
     *            Perforce user name
     * @return User Email address if available
     * @throws IOException if something breaks
     * @throws InterruptedException if something breaks
     */
    private String getUserEmailAddress(final String username) throws IOException, InterruptedException {
        String emailaddr = null;

        final Commandline command = buildUserCommand(username);
        LOG.debug(command.toString());
        final Process p = command.execute();

        logErrorStream(p.getErrorStream());
        final InputStream p4Stream = p.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(p4Stream));

        // Find first Changelist item if there is one.
        String line;
        while ((line = readToNotPast(reader, "info: Email:", "I really don't care")) != null) {
            final StringTokenizer st = new StringTokenizer(line);

            try {
                st.nextToken(); // skip 'info:' text
                st.nextToken(); // skip 'Email:' text
                emailaddr = st.nextToken();
            } catch (NoSuchElementException ex) {
                // No email address given
            }
        }

        getRidOfLeftoverData(p4Stream);

        p.waitFor();
        IO.close(p);

        return (emailaddr);
    }

    private String[] collectChangelistSinceLastBuild(final Date lastBuild, final Date now) throws Exception {
        final Commandline command = buildChangesCommand(lastBuild, now);
        LOG.debug(command.toString());
        final Process p = command.execute();

        final Thread error = logErrorStream(p.getErrorStream());
        final InputStream p4Stream = p.getInputStream();

        final String[] changelistNumbers = parseChangelistNumbers(p4Stream);

        p.waitFor();
        error.join();
        IO.close(p);

        return changelistNumbers;
    }

    private void getRidOfLeftoverData(final InputStream stream) {
        new StreamPumper(stream, new DiscardConsumer()).run();
    }

    protected String[] parseChangelistNumbers(final InputStream is) throws IOException {
        final ArrayList<String> changelists = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("error:")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 1")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 0")) {
                break;
            } else if (line.startsWith("info:")) {
                StringTokenizer st = new StringTokenizer(line);
                st.nextToken(); // skip 'info:' text
                st.nextToken(); // skip 'Change' text
                changelists.add(st.nextToken());
            }
        }
        if (line == null) {
            throw new IOException("Error reading P4 stream: Unexpected EOF reached");
        }
        return changelists.toArray(new String[changelists.size()]);
    }

    protected List<Modification> parseChangeDescriptions(final InputStream is) throws Exception {
        int serverOffset = 0;
        if (correctForServerTime) {
            serverOffset = (int) calculateServerTimeOffset();
        }

        final ArrayList<Modification> changelists = new ArrayList<Modification>();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // Find first Changelist item if there is one.
        String line;
        while ((line = readToNotPast(reader, "text: Change", "exit:")) != null) {

            final P4Modification changelist = new P4Modification();
            if (line.startsWith("error:")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 1")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 0")) {
                return changelists;
            } else if (line.startsWith("text: Change")) {
                final StringTokenizer st = new StringTokenizer(line);

                st.nextToken(); // skip 'text:' text
                st.nextToken(); // skip 'Change' text
                changelist.revision = st.nextToken();
                st.nextToken(); // skip 'by' text

                // split user@client
                final StringTokenizer st2 = new StringTokenizer(st.nextToken(), "@");
                changelist.userName = st2.nextToken();
                changelist.client = st2.nextToken();

                st.nextToken(); // skip 'on' text
                final String date = st.nextToken() + ":" + st.nextToken();
                try {
                    final Calendar cal = Calendar.getInstance();
                    cal.setTime(p4RevisionDateFormatter.parse(date));
                    cal.add(Calendar.MILLISECOND, -serverOffset);
                    changelist.modifiedTime = cal.getTime();
                } catch (ParseException xcp) {
                    changelist.modifiedTime = new Date();
                }
            }

            reader.readLine(); // get past a 'text:'
            final StringBuilder descriptionBuffer = new StringBuilder();

            // Use this since we don't want the final (empty) line
            String previousLine = null;
            line = reader.readLine();
            while (line != null && line.startsWith("text:") && !line.startsWith("text: Affected files ...")) {
                if (previousLine != null) {
                    if (descriptionBuffer.length() > 0) {
                        descriptionBuffer.append('\n');
                    }
                    descriptionBuffer.append(previousLine);
                }
                try {
                    previousLine = line.substring(5).trim();
                } catch (Exception e) {
                    LOG.error("Error parsing Perforce description, line that caused problem was: [" + line + "]");
                }

                line = reader.readLine();
            }

            changelist.comment = descriptionBuffer.toString();

            // Ok, read affected files if there are any.
            if (line != null) {
                reader.readLine(); // read past next 'text:'

                line = readToNotPast(reader, "info1:", "text:");
                while (line != null && line.startsWith("info1:")) {
                    String fileName = line.substring(7, line.lastIndexOf("#"));
                    Modification.ModifiedFile affectedFile = changelist.createModifiedFile(fileName, null);
                    affectedFile.action = line.substring(line.lastIndexOf(" ") + 1);
                    affectedFile.revision = line.substring(line.lastIndexOf("#") + 1, line.lastIndexOf(" "));

                    line = readToNotPast(reader, "info1:", "text:");
                }
            }
            changelists.add(changelist);
        }

        return changelists;
    }

    private Thread logErrorStream(final InputStream is) {
        final Thread errorThread = new Thread(StreamLogger.getWarnPumper(LOG, is));
        errorThread.start();
        return errorThread;
    }

    /**
     * p4 -s [-c client] [-p port] [-u user] changes -s submitted [view@lastBuildTime@now]
     *
     * @param lastBuildTime last build date
     * @param now current build date
     * @return p4 -s [-c client] [-p port] [-u user] changes -s submitted [view@lastBuildTime@now]
     * @throws CruiseControlException if somtething breaks
     */
    public Commandline buildChangesCommand(Date lastBuildTime, Date now)
            throws CruiseControlException {

        // If the Perforce server time is different from the CruiseControl
        // server time, correct the parameter dates for the difference.
        if (correctForServerTime) {
            int offset = (int) calculateServerTimeOffset();
            final Calendar cal = Calendar.getInstance();

            cal.setTime(lastBuildTime);
            cal.add(Calendar.MILLISECOND, offset);
            lastBuildTime = cal.getTime();

            cal.setTime(now);
            cal.add(Calendar.MILLISECOND, offset);
            now = cal.getTime();
        } else {
            LOG.debug("No server time offset determined.");
        }

        final Commandline commandLine = buildBaseP4Command();

        commandLine.createArgument("changes");
        commandLine.createArguments("-s", "submitted");
        commandLine.createArgument(p4View + "@" + p4RevisionDateFormatter.format(lastBuildTime) + ",@"
                + p4RevisionDateFormatter.format(now));

        return commandLine;
    }

    /**
     * @param changelistNumbers change list numbers
     * @return p4 -s [-c client] [-p port] [-u user] describe -s [change number]
     */
    public Commandline buildDescribeCommand(final String[] changelistNumbers) {
        final Commandline commandLine = buildBaseP4Command();

        // execP4Command("describe -s " + changeNumber.toString(),

        commandLine.createArgument("describe");
        commandLine.createArgument("-s");

        for (final String changelistNumber : changelistNumbers) {
            commandLine.createArgument(changelistNumber);
        }

        return commandLine;
    }

    /**
     * @param username user name
     * @return p4 -s [-c client] [-p port] [-u user] user -o [username]
     */
    public Commandline buildUserCommand(final String username) {
        final Commandline commandLine = buildBaseP4Command();
        commandLine.createArgument("user");
        commandLine.createArguments("-o", username);

        return commandLine;
    }

    /**
     * Calculate the difference in time between the Perforce server and the CruiseControl server. A negative time
     * difference indicates that the Perforce server time is later than CruiseControl server (e.g. Perforce in New York,
     * CruiseControl in San Francisco). A positive offset indicates that the Perforce server time is before the
     * CruiseControl server.
     *
     * @return the difference in time between the Perforce server and the CruiseControl server.
     * @throws CruiseControlException if something breaks
     */
    protected long calculateServerTimeOffset() throws CruiseControlException {
        final ServerInfoConsumer serverInfo = new ServerInfoConsumer();
        final CommandExecutor executor = new CommandExecutor(buildInfoCommand());
        executor.logErrorStreamTo(LOG);
        executor.setOutputConsumer(serverInfo);
        executor.executeAndWait();
        return serverInfo.getOffset();
    }

    Commandline buildInfoCommand() {
        final Commandline command = buildBaseP4Command(false);
        command.createArgument("info");
        return command;
    }

    private Commandline buildBaseP4Command() {
        final boolean prependField = true;
        return buildBaseP4Command(prependField);
    }

    private Commandline buildBaseP4Command(final boolean prependField) {
        final Commandline commandLine = new Commandline();
        commandLine.setExecutable("p4");
        if (prependField) {
            commandLine.createArgument("-s");
        }

        if (p4Client != null) {
            commandLine.createArguments("-c", p4Client);
        }

        if (p4Port != null) {
            commandLine.createArguments("-p", p4Port);
        }

        if (p4User != null) {
            commandLine.createArguments("-u", p4User);
        }

        if (p4Passwd != null) {
            commandLine.createArguments("-P", p4Passwd);
        }
        return commandLine;
    }

    /**
     * This is a modified version of the one in the CVS element. I found it far more useful if you actually return
     * either or, because otherwise it would be darn hard to use in places where I actually need the notPast line. Or
     * did I misunderstand something?
     * @param reader reader
     * @param beginsWith beginsWith
     * @param notPast notPast
     * @return a string
     * @throws IOException if something breaks.
     */
    private String readToNotPast(final BufferedReader reader, final String beginsWith, final String notPast)
            throws IOException {

        String nextLine = reader.readLine();

        // (!A && !B) || (!A && !C) || (!B && !C)
        // !A || !B || !C
        while (!(nextLine == null || nextLine.startsWith(beginsWith) || nextLine.startsWith(notPast))) {
            nextLine = reader.readLine();
        }
        return nextLine;
    }

    private static class P4Modification extends Modification {
        public String client;

        @Override
        public int compareTo(final Modification o) {
            P4Modification modification = (P4Modification) o;
            return getChangelistNumber() - modification.getChangelistNumber();
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null || !(o instanceof P4Modification)) {
                return false;
            }

            final P4Modification modification = (P4Modification) o;
            return getChangelistNumber() == modification.getChangelistNumber();
        }

        @Override
        public int hashCode() {
            return getChangelistNumber();
        }

        private int getChangelistNumber() {
            return Integer.parseInt(revision);
        }

        P4Modification() {
            super("p4");
        }

        @Override
        public Element toElement() {
            final Element element = super.toElement();
            LOG.debug("client = " + client);

            final Element clientElement = new Element("client");
            clientElement.addContent(client);
            element.addContent(clientElement);

            return element;
        }
    }

    static String getQuoteChar(final boolean isWindows) {
        return isWindows ? "\"" : "'";
    }

    protected static class ServerInfoConsumer implements StreamConsumer {
        private boolean found;
        private long offset;
        private final SimpleDateFormat p4ServerDateFormatter = new SimpleDateFormat(P4_SERVER_DATE_FORMAT);

        private Date ccServerTime = new Date();

        public void consumeLine(final String line) {
            final Date p4ServerTime;

            // Consume the full stream after we have found the offset
            if (found) {
                return;
            }

            if (line.startsWith(SERVER_DATE)) {
                try {
                    String dateString = line.substring(SERVER_DATE.length(), SERVER_DATE.length()
                            + P4_SERVER_DATE_FORMAT.length());
                    p4ServerTime = p4ServerDateFormatter.parse(dateString);
                    offset = p4ServerTime.getTime() - ccServerTime.getTime();
                    found = true;
                } catch (ParseException pe) {
                    LOG.error("Unable to parse p4 server time from line \'" + line + "\'.  " + pe.getMessage()
                            + "; Proceeding without time offset.");
                }
            }
        }

        public long getOffset() {
            LOG.info("Perforce server time offset: " + offset + " ms");
            return offset;
        }
    }
}
