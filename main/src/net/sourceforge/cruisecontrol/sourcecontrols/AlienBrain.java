/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;

import java.util.Hashtable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * This class implements the SourceControl methods for an AlienBrain
 * repository.  It does this by taking advantage of the AlienBrain command-
 * line utility.  Obviously, the command line utility  must be installed 
 * and working in order for this class to work.
 *
 * This class is based very heavily on P4.java.
 *
 * @author <a href="mailto:scottj+cc@escherichia.net">Scott Jacobs</a>
 */
public class AlienBrain implements SourceControl {
    
    private static final Logger LOG = Logger.getLogger(AlienBrain.class);
    /*
     * The difference between January 1, 1601 0:00:00 UTC and January 1,
     * 1970 0:00:00 UTC in milliseconds.
     * ((369 years * 365 days) + 89 leap days) * 24h * 60m * 60s * 1000ms
     */ 
    private static final long FILETIME_EPOCH_DIFF = 11644473600000L;
    /* 100-ns intervals per ms */
    private static final long HUNDRED_NANO_PER_MILLI_RATIO = 10000L;
    private static final String AB_NO_SESSION = "Invalid session please logon!";
    private static final String AB_NO_MODIFICATIONS = "No files or folders found!";
    private static final String AB_MODIFICATION_SUMMARY_PREFIX = "Total of ";
    
    private Hashtable properties = new Hashtable();
    private String server;
    private String database;
    private String user;
    private String password;
    private String path;
    private String branch;


    /**
     * Sets the hostname of the server hosting the AlienBrain repository.
     *
     *@param server The AlienBrain server's hostname.
     */    
    public void setServer(String server) {
        this.server = server;
    }
    
    /**
     * Sets the name of the project database.
     *
     *@param database The name of the project database.
     */    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    /**
     * Sets the name of the AlienBrain user account used to connect.
     *
     *@param user The name of the AlienBrin user account.
     */    
    public void setUser(String user) {
        this.user = user;
    }
    
    /**
     * Sets the password of the AlienBrain user account used to connect.
     *
     *@param password The password of the AlienBrin user account.
     */    
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Sets the path to the project within the AlienBrain repository.
     *
     * @param path The path within the project database to check for 
     * modificiations.  Typically something like alienbrain://path/to/project
     */    
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Sets the path to the project within the AlienBrain repository.
     *
     *@param branch The branch within the AlienBrain project.
     */    
    public void setBranch(String branch) {
        this.branch = branch;
    }
  
    /**
     * Name of property to define if a modification is detected.
     * Currently unsupported by the AlienBrain plugin.
     */
    public void setProperty(String property) {
        throwUnsupportedException("Set property");
    }
    
    /**
     * Name of property to define if a deletion is detected. 
     * Currently unsupported by the AlienBrain plugin.
     */
    public void setPropertyOnDelete(String property) {
        throwUnsupportedException("Set property on delete");
    }
   
    private void throwUnsupportedException(String operation) {
        throw new UnsupportedOperationException(operation + " not supported by AlienBrain");
    }

    /**
     * Any properties that have been set in this sourcecontrol. 
     * Currently, this would be none.
     */
    public Hashtable getProperties() {
        return properties;
    }
   
    public void validate() throws CruiseControlException {
        if (server == null) {
            throwMissingAttributeException("'server'");
        }
        if (database == null) {
            throwMissingAttributeException("'database'");
        }
        if (user == null) {
            throwMissingAttributeException("'user'");
        }
        if (password == null) {
            throwMissingAttributeException("'password'");
        }
        if (path == null) {
            throwMissingAttributeException("'path'");
        }
    }
   
    private void throwMissingAttributeException(String attribute) throws CruiseControlException {
        throw new CruiseControlException(attribute + " is a required attribute on AlienBrain");
    }

    /**
     *  Get a List of Modifications detailing all the changes between now and
     *  the last build
     *
     *@param  lastBuild
     *@param  now
     *@return List of Modification objects
     */ 
    public List getModifications(Date lastBuild, Date now) {
        List mods = new ArrayList();
        try {
            validate();
            mods = getModificationsFromAlienBrain(lastBuild, now);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Log command failed to execute succesfully", e);
        }
        
        return mods;
    }
    
    /**
     * Convert a Java Date into an AlienBrain SCIT timestamp.
     * AlienBrain provides a 64-bit modification timestamp that is in windows
     * FILETIME format, which is a 65-bit value representing the number of 
     * 100-nanosecond intervals since January 1, 1601 (UTC).
     */
    public static long dateToFiletime(Date date) {
        long milliSecsSinceUnixEpoch = date.getTime();
        long milliSecsSinceFiletimeEpoch = milliSecsSinceUnixEpoch + FILETIME_EPOCH_DIFF;
        return milliSecsSinceFiletimeEpoch * HUNDRED_NANO_PER_MILLI_RATIO;
    }
    
    /**
     * Convert an AlienBrain SCIT timestamp into a Java Date.
     * AlienBrain provides a 64-bit modification timestamp that is in windows
     * FILETIME format, which is a 64-bit value representing the number of 
     * 100-nanosecond intervals since January 1, 1601 (UTC).
     */
    public static Date filetimeToDate(long filetime) {
        long milliSecsSinceFiletimeEpoch = filetime / HUNDRED_NANO_PER_MILLI_RATIO;
        long milliSecsSinceUnixEpoch = milliSecsSinceFiletimeEpoch - FILETIME_EPOCH_DIFF;
        return new Date(milliSecsSinceUnixEpoch);
    }
    
    /**
     * Construct a Commanline which will run the AlienBrain command-line 
     * client in such a way that it will return a list of modifications.
     *
     *@param  lastBuild
     *@param  now
     */
    protected Commandline buildGetModificationsCommand(Date lastBuild, Date now) {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("find");
        cmdLine.createArgument().setValue(path);
        cmdLine.createArgument().setValue("-regex");
        cmdLine.createArgument().setValue("\"SCIT > " + dateToFiletime(lastBuild) + "\"");
        cmdLine.createArgument().setValue("-format");
        cmdLine.createArgument().setValue("\"#SCIT#|#DbPath#|#Changed By#|#CheckInComment#\"");
        
        return cmdLine;
    }
   
    /**
     * Run the AlienBrain command-line client and return a list of 
     * Modifications since lastBuild, if any. 
     *@param  lastBuild
     *@param  now
     */ 
    protected List getModificationsFromAlienBrain(Date lastBuild, Date now) 
        throws IOException, InterruptedException {
            
        if (!isConnected()) {
            connect();
        }
        
        if (branch != null) {
            setActiveBranch(branch);
        }
            
        Commandline cmdLine = buildGetModificationsCommand(lastBuild, now);
        LOG.debug("Executing: " + cmdLine.toString());
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        
        logErrorStream(p.getErrorStream());
        InputStream abStream = p.getInputStream();
        
        List mods = parseModifications(abStream);
        
        p.waitFor();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();
        
        return mods;
    }
   
    /**
     * Turn a stream containing the results of running the AlienBrain 
     * command-line client into a list of Modifications.
     */ 
    protected List parseModifications(InputStream is) throws IOException {
        List mods = new ArrayList();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.equals(AB_NO_SESSION)) {
                LOG.error(AB_NO_SESSION);
                continue;
            } else if (line.equals(AB_NO_MODIFICATIONS)) {
                continue;
            } else if (line.startsWith(AB_MODIFICATION_SUMMARY_PREFIX)) {
                continue;
            } else if (line.startsWith("|")) {
                //Folders don't seem to always have a checked-in time, so
                //fake one.
                line = "0" + line;
            }
            
            Modification m = parseModificationDescription(line);
            mods.add(m);
        }
        return mods;
    }
    
    /**
     * Turns a string, most likely provided from the AlienBrain command-line
     * client, into a Modification.
     */
    protected static Modification parseModificationDescription(String description) {
        Modification m = new Modification("AlienBrain");
        
        StringTokenizer st = new StringTokenizer(description, "|");
        
        m.modifiedTime = AlienBrain.filetimeToDate(Long.parseLong(st.nextToken()));
        m.createModifiedFile(st.nextToken(), null);
        m.userName = st.nextToken();
        while (st.hasMoreTokens()) {
            m.comment += st.nextToken();
        }
                
        return m;
    }
   
    /**
     * Copied from P4.java.
     */ 
    private void logErrorStream(InputStream is) {
        StreamPumper errorPumper = new StreamPumper(is, new PrintWriter(
                                                        System.err, true));
        new Thread(errorPumper).start();
    }
   
    /**
     * Checks to see if the AlienBrain JXDK bridge is running.
     * This is accomplished by running 'ab isbridgerunning'
     * @return boolean True if the bridge is running.
     */ 
    protected static boolean isBridgeRunning() 
        throws IOException, InterruptedException {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("isbridgerunning");
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        LOG.debug("Executing: " + cmdLine.toString());
        p.waitFor();
        LOG.debug("Exit value:" + p.exitValue());
        return p.exitValue() == 0;
    }
   
    /**
     * Checks to see if there is a currently a user connected to an
     * AlienBrain project via the JXDK bridge.
     * This is accomplished by first checking for the bridge, then running
     * 'ab isconnected'
     * @return boolean True if there is a connection.
     */ 
    protected static boolean isConnected() throws IOException, InterruptedException {
        if (!isBridgeRunning()) {
            return false;
        }

        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("isconnected");
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        LOG.debug("Executing: " + cmdLine.toString());
        p.waitFor();
        LOG.debug("Exit value:" + p.exitValue());
        return p.exitValue() == 0;
    }
   
    /**
     * Connects to an AlienBrain server using the parameters stored in
     * this class.
     */ 
    protected boolean connect() 
        throws IOException, InterruptedException {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("logon");
        cmdLine.createArgument().setValue("-s");
        cmdLine.createArgument().setValue(server);
        cmdLine.createArgument().setValue("-d");
        cmdLine.createArgument().setValue(database);
        cmdLine.createArgument().setValue("-u");
        cmdLine.createArgument().setValue(user);
        cmdLine.createArgument().setValue("-p");
        cmdLine.createArgument().setValue(password);
        LOG.debug("Executing: " + cmdLine.toString());
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        p.waitFor();
        return p.exitValue() == 0;
    }
    
    /**
     * Sets the active branch to the provided branch name.
     *
     *@param String The branch name.
     */
    protected boolean setActiveBranch(String branch)
        throws IOException, InterruptedException {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable("ab");
        cmdLine.createArgument().setValue("setactivebranch");
        cmdLine.createArgument().setValue(branch);
        LOG.debug("Executing: " + cmdLine.toString());
        Process p = Runtime.getRuntime().exec(cmdLine.getCommandline());
        p.waitFor();
        return p.exitValue() == 0;
    }        
}
