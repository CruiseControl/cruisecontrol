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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

/**
 * This class implements the SourceControlElement methods for a P4 depot. The
 * call to CVS is assumed to work without any setup. This implies that if the
 * authentication type is pserver the call to cvs login should be done prior to
 * calling this class.
 * <p>
 * P4Element depends on the optional P4 package delivered with Ant v1.3. But
 * since it probably doesn't make much sense using the P4Element without other
 * P4 support it shouldn't be a problem.
 * <p>
 * P4Element sets the property ${p4element.change} with the latest changelist
 * number or the changelist with the latest date. This should then be passed
 * into p4sync or other p4 commands.
 *
 * @author <a href="mailto:niclas.olofsson@ismobile.com">Niclas Olofsson - isMobile.com</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Tim McCune
 */
public class P4 implements SourceControl {

    private static final Logger LOG = Logger.getLogger(P4.class);

    private String p4Port;
    private String p4Client;
    private String p4User;
    private String p4View;
    private static final SimpleDateFormat P4_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat P4_REVISION_DATE = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");

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

    /** 
     * Unsupported by P4. 
     */
    public void setProperty(String property) {
    }

    /** 
     * Unsupported by P4. 
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
    }

    public Hashtable getProperties() {
        return properties;
    }

    protected List changeListsToElement(List mods) {
        List changelists = new ArrayList();
        for (Iterator iterator = mods.iterator(); iterator.hasNext();) {
            Changelist changelist = (Changelist) iterator.next();
            changelists.add(changelist.toElement());
        }
        return changelists;
    }

    public void validate() throws CruiseControlException {
        if (p4Port == null) {
            throw new CruiseControlException("'port' is a required attribute on P4");
        }
        if (p4Client == null) {
            throw new CruiseControlException("'client' is a required attribute on P4");
        }
        if (p4User == null) {
            throw new CruiseControlException("'user' is a required attribute on P4");
        }
        if (p4View == null) {
            throw new CruiseControlException("'view' is a required attribute on P4");
        }
    }

    /**
     * Get a List of modifications detailing all the changes between now and
     * the last build. Return this as an element. It is not neccessary for
     * sourcecontrols to acctually do anything other than returning a chunch
     * of XML data back.
     *
     * @param lastBuild     time of last build
     * @param now           time this build started
     * @return              a list of XML elements that contains data about the modifications
     *                      that took place. If no changes, this method returns an empty list.
     */
    public List getModifications(Date lastBuild, Date now) {

        List mods = new ArrayList();
        try {

            // collect changelists since last build
            String[] changelistNumbers = null;
            {
                Commandline command = buildChangesCommand(lastBuild, now);
                LOG.debug("Executing: " + command.toString());
                Process p = Runtime.getRuntime().exec(command.getCommandline());

                logErrorStream(p.getErrorStream());
                InputStream p4Stream = p.getInputStream();
                changelistNumbers = parseChangelistNumbers(p4Stream);
                getRidOfLeftoverData(p4Stream);
                p.waitFor();

            }
            if (changelistNumbers.length == 0) {
                return mods;
            }
            // describe all changelists and build output.
            {
                Commandline command = buildDescribeCommand(changelistNumbers);
                System.out.println(command.toString());
                Process p = Runtime.getRuntime().exec(command.getCommandline());

                logErrorStream(p.getErrorStream());
                InputStream p4Stream = p.getInputStream();
                mods = parseChangeDescriptions(p4Stream);
                getRidOfLeftoverData(p4Stream);
                p.waitFor();
                p.getInputStream().close();
                p.getOutputStream().close();
                p.getErrorStream().close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Log command failed to execute succesfully", e);
        }

        return changeListsToElement(mods);
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
        String[] changelistNumbers = new String[0];
        return (String[]) changelists.toArray(changelistNumbers);
    }

    protected List parseChangeDescriptions(InputStream is) throws IOException {
        ArrayList changelists = new ArrayList();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // Find first Changelist item if there is one.
        String line;
        while ((line = readToNotPast(reader, "text: Change", "exit:")) != null) {
            Changelist changelist = new Changelist();
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
                changelist.changelistNumber = st.nextToken();
                st.nextToken(); // skip 'by' text
                {
                    // split user@client
                    StringTokenizer st2 = new StringTokenizer(st.nextToken(), "@");
                    changelist.user = st2.nextToken();
                    changelist.client = st2.nextToken();
                }
                st.nextToken(); // skip 'on' text
                changelist.dateOfSubmission = st.nextToken();
            }

            line = reader.readLine(); // get past a 'text:'
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
                    LOG.error("Error parsing Perforce description, line that caused problem was: [" + line + "]");
                }

            }
            changelist.description = descriptionBuffer.toString();

            // Ok, read affected files if there are any.
            if (line != null) {
                reader.readLine(); // read past next 'text:'
                while ((line = readToNotPast(reader, "info1:", "text:")) != null && line.startsWith("info1:")) {
                    AffectedFile affectedFile = new AffectedFile();
                    affectedFile.filename = line.substring(7, line.lastIndexOf(" ") - 2);
                    affectedFile.action = line.substring(line.lastIndexOf(" ") + 1);
                    affectedFile.revision = line.substring(line.lastIndexOf("#") + 1, line.lastIndexOf(" "));
                    changelist.affectedFiles.add(affectedFile);
                }
            }
            changelists.add(changelist);
        }

        return changelists;
    }

    private boolean preJava13() {
        String javaVersion = System.getProperty("java.version");
        return javaVersion.startsWith("1.1") || javaVersion.startsWith("1.2");
    }

    private void logErrorStream(InputStream is) {
        StreamPumper errorPumper = new StreamPumper(is, new PrintWriter(System.err, true));
        new Thread(errorPumper).start();
    }

    /**
     *@param lastBuildTime
     */
    public Commandline buildChangesCommand(Date lastBuildTime, Date now) {
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

//        execP4Command("changes -m 1 -s submitted " + _P4View,

        commandLine.createArgument().setValue("changes");
        commandLine.createArgument().setValue("-s");
        commandLine.createArgument().setValue("submitted");
        commandLine.createArgument().setValue(
            p4View
                + "@"
                + P4_REVISION_DATE.format(lastBuildTime)
                + ",@"
                + P4_REVISION_DATE.format(now));

        return commandLine;
    }

    public Commandline buildDescribeCommand(String[] changelistNumbers) {

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

//        execP4Command("describe -s " + changeNumber.toString(),

        commandLine.createArgument().setValue("describe");
        commandLine.createArgument().setValue("-s");

        for (int i = 0; i < changelistNumbers.length; i++) {
            commandLine.createArgument().setValue(changelistNumbers[i]);
        }

        return commandLine;
    }

    /**
     * This is a modified version of the one in the CVS element. I found it far
     * more useful if you acctually return either or, because otherwise it would
     * be darn hard to use in places where I acctually need the notPast line.
     * Or did I missunderatnd something?
     */
    private String readToNotPast(BufferedReader reader, String beginsWith,
                                 String notPast) throws IOException {
        boolean checkingNotPast = notPast != null;

        String nextLine = reader.readLine();

        // (!A && !B) || (!A && !C) || (!B && !C)
        // !A || !B || !C
        while (!(nextLine == null || nextLine.startsWith(beginsWith) || nextLine.startsWith(notPast))) {
            nextLine = reader.readLine();
        }
        return nextLine;
    }

    /**
     * Contains a changelist description, including the changelist number,
     * user, client, date of submission, textual description, list
     * of affected files
     */
    class Changelist {
        public String changelistNumber;
        public String user;
        public String client;
        public String dateOfSubmission;
        public String description;
        public Vector affectedFiles = new Vector();

        public Element toElement() {
            Element changelistElement = new Element("changelist");
            changelistElement.setAttribute("type", "p4");
            changelistElement.setAttribute("changelistNumber", changelistNumber);
            changelistElement.setAttribute("user", user);
            changelistElement.setAttribute("client", client);
            changelistElement.setAttribute("dateOfSubmission", dateOfSubmission);
            Element descriptionElement = new Element("description");
            descriptionElement.addContent(new CDATA(description));
            changelistElement.addContent(descriptionElement);
            for (int i = 0; i < affectedFiles.size(); i++) {
                AffectedFile affectedFile = (AffectedFile) affectedFiles.elementAt(i);
                changelistElement.addContent(affectedFile.toElement());
            }
            return changelistElement;
        }
        
    }

    class AffectedFile {
        public String filename;
        public String revision;
        public String action;

        public Element toElement() {
            Element affectedFileElement = new Element("affectedfile");
            affectedFileElement.setAttribute("filename", filename);
            affectedFileElement.setAttribute("revision", revision);
            affectedFileElement.setAttribute("action", action);
            return affectedFileElement;
        }

        public void log() {
            System.out.println("Filename:\t<" + filename + ">");
            System.out.println("Revision:\t<" + revision + ">");
            System.out.println("Action:\t<" + action + ">");
        }
    }

}
