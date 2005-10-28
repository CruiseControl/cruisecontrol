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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * This class implements the SourceControlElement methods for a P4 depot. The
 * call to CVS is assumed to work without any setup. This implies that if the
 * authentication type is pserver the call to cvs login should be done prior to
 * calling this class.
 * <p/>
 * P4Element depends on the optional P4 package delivered with Ant v1.3. But
 * since it probably doesn't make much sense using the P4Element without other
 * P4 support it shouldn't be a problem.
 * <p/>
 * P4Element sets the property ${p4element.change} with the latest changelist
 * number or the changelist with the latest date. This should then be passed
 * into p4sync or other p4 commands.
 *
 * @author <a href="mailto:niclas.olofsson@ismobile.com">Niclas Olofsson - isMobile.com</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Tim McCune
 * @author J D Glanville
 * @author Patrick Conant Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 */
public class P4 implements SourceControl {

    private static final Logger LOG = Logger.getLogger(P4.class);

    private String p4Port;
    private String p4Client;
    private String p4User;
    private String p4View;
    private String p4Passwd;
    private boolean correctForServerTime = true;

    private static final SimpleDateFormat P4_REVISION_DATE =
            new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");


    private static final String SERVER_DATE = "Server date: ";
    private static final String P4_SERVER_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    private static final SimpleDateFormat P4_SERVER_DATE =
            new SimpleDateFormat(P4_SERVER_DATE_FORMAT);


    private Hashtable properties = new Hashtable();

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
     *  Indicates whether to correct for time differences between the p4
     *  server and the CruiseControl server.  Setting the flag to "true"
     *  will correct for both time zone differences and for non-synchronized
     *  system clocks.
     */
    public void setCorrectForServerTime(boolean flag) {
        this.correctForServerTime = flag;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(p4Port, "port", this.getClass());
        ValidationHelper.assertIsSet(p4Client, "client", this.getClass());
        ValidationHelper.assertIsSet(p4User, "user", this.getClass());
        ValidationHelper.assertIsSet(p4View, "view", this.getClass());
        ValidationHelper.assertNotEmpty(p4Passwd, "passwd", this.getClass());
    }

    /**
     * Get a List of modifications detailing all the changes between now and
     * the last build. Return this as an element. It is not neccessary for
     * sourcecontrols to acctually do anything other than returning a chunch
     * of XML data back.
     *
     * @param lastBuild time of last build
     * @param now       time this build started
     * @return a list of XML elements that contains data about the modifications
     *         that took place. If no changes, this method returns an empty list.
     */
    public List getModifications(Date lastBuild, Date now) {
        List mods = new ArrayList();
        try {
            String[] changelistNumbers = collectChangelistSinceLastBuild(lastBuild, now);
            if (changelistNumbers.length == 0) {
                return mods;
            }
            mods = describeAllChangelistsAndBuildOutput(changelistNumbers);
        } catch (Exception e) {
            LOG.error("Log command failed to execute succesfully", e);
        }

        return mods;
    }

    private List describeAllChangelistsAndBuildOutput(String[] changelistNumbers)
            throws IOException, InterruptedException {

        Commandline command = buildDescribeCommand(changelistNumbers);
        LOG.info(command.toString());
        Process p = Runtime.getRuntime().exec(command.getCommandline());

        logErrorStream(p.getErrorStream());
        InputStream p4Stream = p.getInputStream();
        List mods = parseChangeDescriptions(p4Stream);
        getRidOfLeftoverData(p4Stream);

        p.waitFor();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();

        return mods;
    }

    private String[] collectChangelistSinceLastBuild(Date lastBuild, Date now)
            throws IOException, InterruptedException {

        Commandline command = buildChangesCommand(lastBuild, now, Util.isWindows());
        LOG.debug("Executing: " + command.toString());
        Process p = Runtime.getRuntime().exec(command.getCommandline());

        logErrorStream(p.getErrorStream());
        InputStream p4Stream = p.getInputStream();

        String[] changelistNumbers = parseChangelistNumbers(p4Stream);

        getRidOfLeftoverData(p4Stream);
        p.waitFor();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();

        return changelistNumbers;
    }

    private void getRidOfLeftoverData(InputStream stream) {
        StreamPumper outPumper = new StreamPumper(stream, (PrintWriter) null);
        new Thread(outPumper).start();
    }

    protected String[] parseChangelistNumbers(InputStream is) throws IOException {
        ArrayList changelists = new ArrayList();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            line.trim();
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
        String[] changelistNumbers = new String[ 0 ];
        return (String[]) changelists.toArray(changelistNumbers);
    }

    protected List parseChangeDescriptions(InputStream is) throws IOException {

        ArrayList changelists = new ArrayList();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // Find first Changelist item if there is one.
        String line;
        while ((line = readToNotPast(reader, "text: Change", "exit:")) != null) {

            P4Modification changelist = new P4Modification();
            if (line.startsWith("error:")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 1")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 0")) {
                return changelists;
            } else if (line.startsWith("text: Change")) {
                StringTokenizer st = new StringTokenizer(line);

                st.nextToken(); // skip 'text:' text
                st.nextToken(); // skip 'Change' text
                changelist.revision = st.nextToken();
                st.nextToken(); // skip 'by' text

                // split user@client
                StringTokenizer st2 = new StringTokenizer(st.nextToken(), "@");
                changelist.userName = st2.nextToken();
                changelist.client = st2.nextToken();

                st.nextToken(); // skip 'on' text
                String date = st.nextToken() + ":" + st.nextToken();
                try {
                    changelist.modifiedTime = P4_REVISION_DATE.parse(date);
                } catch (ParseException xcp) {
                    changelist.modifiedTime = new Date();
                }
            }

            reader.readLine(); // get past a 'text:'
            StringBuffer descriptionBuffer = new StringBuffer();
            // Use this since we don't want the final (empty) line
            String previousLine = null;
            while ((line = reader.readLine()) != null
                    && line.startsWith("text:")
                    && !line.startsWith("text: Affected files ...")) {

                if (previousLine != null) {
                    if (descriptionBuffer.length() > 0) {
                        descriptionBuffer.append('\n');
                    }
                    descriptionBuffer.append(previousLine);
                }
                try {
                    previousLine = line.substring(5).trim();
                } catch (Exception e) {
                    LOG.error("Error parsing Perforce description, line that caused problem was: ["
                            + line
                            + "]");
                }

            }

            changelist.comment = descriptionBuffer.toString();

            // Ok, read affected files if there are any.
            if (line != null) {
                reader.readLine(); // read past next 'text:'
                while ((line = readToNotPast(reader, "info1:", "text:")) != null
                        && line.startsWith("info1:")) {

                    String fileName = line.substring(7, line.lastIndexOf("#"));
                    Modification.ModifiedFile affectedFile = changelist.createModifiedFile(fileName, null);
                    affectedFile.action = line.substring(line.lastIndexOf(" ") + 1);
                    affectedFile.revision =
                            line.substring(line.lastIndexOf("#") + 1, line.lastIndexOf(" "));
                }
            }
            changelists.add(changelist);
        }

        return changelists;
    }

    private void logErrorStream(InputStream is) {
        StreamPumper errorPumper = new StreamPumper(is, new PrintWriter(System.err, true));
        new Thread(errorPumper).start();
    }

    /**
     * p4 -s [-c client] [-p port] [-u user] changes -s submitted [view@lastBuildTime@now]
     */
    public Commandline buildChangesCommand(Date lastBuildTime, Date now, boolean isWindows) {

        //If the Perforce server time is different from the CruiseControl
        // server time, correct the parameter dates for the difference.
        if (this.correctForServerTime) {
            try {
                int offset = (int) calculateServerTimeOffset();
                Calendar cal = Calendar.getInstance();

                cal.setTime(lastBuildTime);
                cal.add(Calendar.MILLISECOND , offset);
                lastBuildTime = cal.getTime();

                cal.setTime(now);
                cal.add(Calendar.MILLISECOND , offset);
                now = cal.getTime();

            } catch (IOException ioe) {
                LOG.error("Unable to execute \'p4 info\' to get server time: "
                        + ioe.getMessage()
                        + "\nProceeding without time offset value.");
            }
        } else {
            LOG.debug("No server time offset determined.");
        }


        Commandline commandLine = buildBaseP4Command();

        commandLine.createArgument().setValue("changes");
        commandLine.createArgument().setValue("-s");
        commandLine.createArgument().setValue("submitted");
        commandLine.createArgument().setValue(p4View
                + "@"
                + P4_REVISION_DATE.format(lastBuildTime)
                + ",@"
                + P4_REVISION_DATE.format(now));

        return commandLine;
    }

    /**
     * p4 -s [-c client] [-p port] [-u user] describe -s [change number]
     */
    public Commandline buildDescribeCommand(String[] changelistNumbers) {
        Commandline commandLine = buildBaseP4Command();

        //        execP4Command("describe -s " + changeNumber.toString(),

        commandLine.createArgument().setValue("describe");
        commandLine.createArgument().setValue("-s");

        for (int i = 0; i < changelistNumbers.length; i++) {
            commandLine.createArgument().setValue(changelistNumbers[ i ]);
        }

        return commandLine;
    }

    /**
      * Calculate the difference in time between the Perforce server and the
      * CruiseControl server.  A negative time difference indicates that the
      * Perforce server time is later than CruiseControl server (e.g. Perforce
      * in New York, CruiseControl in San Francisco).  A negative offset
      * indicates that the Perforce server time is before the CruiseControl
      * server.
      */
    protected long calculateServerTimeOffset() throws IOException {
        Commandline command = new Commandline();
        command.setExecutable("p4");
        command.createArgument().setValue("info");

        LOG.debug("Executing: " + command.toString());
        LOG.info(command.toString());
        Process p = Runtime.getRuntime().exec(command.getCommandline());
        logErrorStream(p.getErrorStream());
        long offset = parseServerInfo(p.getInputStream());

        return offset;
    }

    protected long parseServerInfo(InputStream is) throws IOException {

        BufferedReader p4reader = new BufferedReader(new InputStreamReader(is));

        Date ccServerTime = new Date();
        Date p4ServerTime = new Date();

        String line = null;
        long offset = 0;
        while ((line = p4reader.readLine()) != null && offset == 0) {
            if (line.startsWith(SERVER_DATE)) {
                try {
                    String dateString = line.substring(SERVER_DATE.length(),
                        SERVER_DATE.length() + P4_SERVER_DATE_FORMAT.length());
                    p4ServerTime = P4_SERVER_DATE.parse(dateString);
                    offset = p4ServerTime.getTime() - ccServerTime.getTime();
                } catch (ParseException pe) {
                    LOG.error("Unable to parse p4 server time from line \'"
                            + line
                            + "\'.  " + pe.getMessage()
                            + "; Proceeding without time offset.");
                }
            }
        }

        LOG.info("Perforce server time offset: " + offset + " ms");
        return offset;
    }


    private Commandline buildBaseP4Command() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("p4");
        commandLine.createArgument().setValue("-s");

        if (p4Client != null) {
            commandLine.createArgument().setValue("-c");
            commandLine.createArgument().setValue(p4Client);
        }

        if (p4Port != null) {
            commandLine.createArgument().setValue("-p");
            commandLine.createArgument().setValue(p4Port);
        }

        if (p4User != null) {
            commandLine.createArgument().setValue("-u");
            commandLine.createArgument().setValue(p4User);
        }

        if (p4Passwd != null) {
            commandLine.createArgument().setValue("-P");
            commandLine.createArgument().setValue(p4Passwd);
        }
        return commandLine;
    }

    /**
     * This is a modified version of the one in the CVS element. I found it far
     * more useful if you acctually return either or, because otherwise it would
     * be darn hard to use in places where I acctually need the notPast line.
     * Or did I missunderatnd something?
     */
    private String readToNotPast(BufferedReader reader, String beginsWith, String notPast)
            throws IOException {

        String nextLine = reader.readLine();

        // (!A && !B) || (!A && !C) || (!B && !C)
        // !A || !B || !C
        while (!(nextLine == null
                || nextLine.startsWith(beginsWith)
                || nextLine.startsWith(notPast))) {
            nextLine = reader.readLine();
        }
        return nextLine;
    }

    private static class P4Modification extends Modification {
        public String client;

        public int compareTo(Object o) {
            P4Modification modification = (P4Modification) o;
            return getChangelistNumber() - modification.getChangelistNumber();
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof P4Modification)) {
                return false;
            }

            P4Modification modification = (P4Modification) o;
            return getChangelistNumber() == modification.getChangelistNumber();
        }

        public int hashCode() {
            return getChangelistNumber();
        }

        private int getChangelistNumber() {
            return Integer.parseInt(revision);
        }

        P4Modification() {
            super("p4");
        }

        public Element toElement(DateFormat format) {

            Element element = super.toElement(format);
            LOG.debug("client = " + client);

            Element clientElement = new Element("client");
            clientElement.addContent(client);
            element.addContent(clientElement);

            return element;
        }
    }

    static String getQuoteChar(boolean isWindows) {
        return isWindows ? "\"" : "'";
    }
}
