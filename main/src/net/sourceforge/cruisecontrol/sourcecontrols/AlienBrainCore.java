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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;

import org.apache.log4j.Logger;

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
public class AlienBrainCore {
    
    protected static final Logger LOG = Logger.getLogger(AlienBrainCore.class);
    
    protected static final String AB_NO_SESSION = "Invalid session please logon!";
    
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

    public String getServer() {
        return server;
    }
    
    /**
     * Sets the name of the project database.
     *
     *@param database The name of the project database.
     */    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getDatabase() {
        return database;
    }
    
    /**
     * Sets the name of the AlienBrain user account used to connect.
     *
     *@param user The name of the AlienBrin user account.
     */    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getUser() {
        return user;
    }
    
    /**
     * Sets the password of the AlienBrain user account used to connect.
     *
     *@param password The password of the AlienBrin user account.
     */    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getPassword() {
        return password;
    }
    
    /**
     * Sets the path to the project within the AlienBrain repository.
     *
     * @param path The path within the project database to check for 
     * modifications.  Typically something like alienbrain://path/to/project
     */    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    /**
     * Sets the path to the project within the AlienBrain repository.
     *
     *@param branch The branch within the AlienBrain project.
     */    
    public void setBranch(String branch) {
        this.branch = branch;
    }
  
    public String getBranch() {
        return branch;
    }
  
    /**
     * Try to add a flag to a Commandline
     *
     *@param cmdLine The Commandline object to possibly add arguments
     *@param flagValue Whether or not to add the flag.
     *@param flagName The flag to use if the argument is added.
     */
    protected void addFlagIfSet(Commandline cmdLine, boolean flagValue, String flagName) {
        if (flagValue) {
            cmdLine.createArgument(flagName);
        }
    }

    /**
     * Try to add flagged argument to a Commandline
     *
     *@param cmdLine The Commandline object to possibly add arguments
     *@param argument The argument to possibly add.
     *@param flag The flag to use if the argument is added.
     */
    protected void addArgumentIfSet(Commandline cmdLine, String argument, String flag) {
        if (argument != null) {
            cmdLine.createArguments(flag, argument);
        }
    }

    /**
     * Construct a ManagedCommandline preset with arguments applicable to
     * any AlienBrain command that we wish to run.
     */
    protected ManagedCommandline buildCommonCommand() {
        ManagedCommandline cmdLine = new ManagedCommandline();
        cmdLine.setExecutable("ab");
        addArgumentIfSet(cmdLine, user, "-u");
        addArgumentIfSet(cmdLine, password, "-p");
        addArgumentIfSet(cmdLine, server, "-s");
        addArgumentIfSet(cmdLine, database, "-d");

        return cmdLine;
    }

    
    /**
     * Sets the active branch to the provided branch name.
     *
     *@param branch The branch name.
     * @throws CruiseControlException 
     */
    protected void setActiveBranch(String branch) throws IOException, CruiseControlException {
        ManagedCommandline cmdLine = buildCommonCommand();
        cmdLine.createArguments("setactivebranch", branch);
        LOG.debug("Executing: " + cmdLine.toString());
        cmdLine.execute();
        cmdLine.assertExitCode(0);
    }        
}
