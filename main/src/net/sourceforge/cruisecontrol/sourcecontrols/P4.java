/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, isMobile.com - http://www.ismobile.com
 * Aurorum 2, S-977 75 Luleå, Sweden
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
 *     + Neither the name of isMobile.com, ThoughtWorks, Inc., 
 *       CruiseControl, nor the names of its contributors may be used 
 *       to endorse or promote products derived from this software 
 *       without specific prior written permission.
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

import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Category;
import org.jdom.CDATA;
import org.jdom.Element;

import java.io.*;
import java.util.*;

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

    /** enable logging for this class */
    private static Category log = Category.getInstance(P4.class.getName());

    //P4 runtime directives

    private String _P4Port;
    private String _P4Client;
    private String _P4User;
    private String _P4View;
    private final static java.text.SimpleDateFormat P4DATE = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private final static java.text.SimpleDateFormat P4REVISIONDATE = new java.text.SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");

    private Hashtable _properties = new Hashtable();
    private String _property;
    private String _propertyOnDelete;

    /**
     *  Constructor for P4Element. Doesn't do much.
     */
    public P4() {
    }

    //Setters called by PluginXMLHelper
    public void setPort(String P4Port) {
        this._P4Port = P4Port;
    }

    public void setClient(String P4Client) {
        this._P4Client = P4Client;
    }

    public void setUser(String P4User) {
        this._P4User = P4User;
    }

    public void setView(String P4View) {
        this._P4View = P4View;
    }

    public void setProperty(String property) {
        _property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        _propertyOnDelete = propertyOnDelete;
    }

    public Hashtable getProperties() {
        return _properties;
    }

    protected List changeListsToElement(List mods) {
        List changelists = new ArrayList();
        for (Iterator iterator = mods.iterator(); iterator.hasNext();) {
            Changelist changelist = (Changelist) iterator.next();
            changelists.add(changelist.toElement());
        }
        return changelists;
    }


    /**
     * Get a List of modifications detailing all the changes between now and
     * the last build. Return this as an element. It is not neccessary for
     * sourcecontrols to acctually do anything other than returning a chunch
     * of XML data back.
     * 
     * @param lastBuild     time of last build
     * @param now           time this build started
     * @param quietPeriod   how long the repository will judge as safe
     * @return              a list of XML elements that contains data about the modifications
     *                      that took place. If no changes, this method returns an empty list.
     */
    public List getModifications(Date lastBuild, Date now, long quietPeriod) {

        List mods = new ArrayList();
        try {

            // collect changelists since last build
            String[] changelistNumbers = null;
            {
                Commandline command = buildChangesCommand(lastBuild, now);
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
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Log command failed to execute succesfully", e);
        }

        return changeListsToElement(mods);
    }

    private void getRidOfLeftoverData(InputStream stream) {
        StreamPumper outPumper = new StreamPumper(stream, null);
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
                changelist._changelistNumber = st.nextToken();
                st.nextToken(); // skip 'by' text
                {
                    // split user@client
                    StringTokenizer st2 = new StringTokenizer(st.nextToken(), "@");
                    changelist._user = st2.nextToken();
                    changelist._client = st2.nextToken();
                }
                st.nextToken(); // skip 'on' text
                changelist._dateOfSubmission = st.nextToken();
            }
            line = reader.readLine(); // get past a 'text:' otherwise the expression below will fail to match.
            String description = "";
            while ((line = readToNotPast(reader, "text:\t", "text:")) != null && line.startsWith("text:\t")) {
                description += line.substring(6);
            }
            changelist._description = description;
            
            // Ok, read affected files if there are any.
            line = readToNotPast(reader, "text: Affected files ...", "exit:");
            if (line != null) {
                reader.readLine(); // read past next 'text:'
                while ((line = readToNotPast(reader, "info1:", "text:")) != null && line.startsWith("info1:")) {
                    AffectedFile affectedFile = new AffectedFile();
                    affectedFile.filename = line.substring(7, line.lastIndexOf(" ") - 2);
                    affectedFile.action = line.substring(line.lastIndexOf(" ") + 1);
                    affectedFile.revision = line.substring(line.lastIndexOf("#") + 1, line.lastIndexOf(" "));
                    changelist._affectedFiles.add(affectedFile);
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

        if (_P4Client != null) {
            commandLine.createArgument().setValue("-c");
            commandLine.createArgument().setValue(_P4Client);
        }

        if (_P4Port != null) {
            commandLine.createArgument().setValue("-p");
            commandLine.createArgument().setValue(_P4Port);
        }

        if (_P4User != null) {
            commandLine.createArgument().setValue("-u");
            commandLine.createArgument().setValue(_P4User);
        }

//        execP4Command("changes -m 1 -s submitted " + _P4View,
        
        commandLine.createArgument().setValue("changes");
        commandLine.createArgument().setValue("-s");
        commandLine.createArgument().setValue("submitted");
        commandLine.createArgument().setValue(_P4View + "@" + P4REVISIONDATE.format(lastBuildTime) + ",@" + P4REVISIONDATE.format(now));

        return commandLine;
    }

    public Commandline buildDescribeCommand(String[] changelistNumbers) {

        Commandline commandLine = new Commandline();
        commandLine.setExecutable("p4");
        commandLine.createArgument().setValue("-s");

        if (_P4Client != null) {
            commandLine.createArgument().setValue("-c");
            commandLine.createArgument().setValue(_P4Client);
        }

        if (_P4Port != null) {
            commandLine.createArgument().setValue("-p");
            commandLine.createArgument().setValue(_P4Port);
        }

        if (_P4User != null) {
            commandLine.createArgument().setValue("-u");
            commandLine.createArgument().setValue(_P4User);
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
        String _changelistNumber;
        String _user;
        String _client;
        String _dateOfSubmission;
        String _description;
        Vector _affectedFiles = new Vector();

        public Element toElement() {
            Element changelistElement = new Element("changelist");
            changelistElement.setAttribute("type", "p4");
            changelistElement.setAttribute("changelistNumber", _changelistNumber);
            changelistElement.setAttribute("user", _user);
            changelistElement.setAttribute("client", _client);
            changelistElement.setAttribute("dateOfSubmission", _dateOfSubmission);
            Element descriptionElement = new Element("description");
            descriptionElement.addContent(new CDATA(_description));
            changelistElement.addContent(descriptionElement);
            for (int i = 0; i < _affectedFiles.size(); i++) {
                AffectedFile affectedFile = (AffectedFile) _affectedFiles.elementAt(i);
                changelistElement.addContent(affectedFile.toElement());
            }
            return changelistElement;
        }
    }

    class AffectedFile {
        String filename;
        String revision;
        String action;

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

// P4Element
