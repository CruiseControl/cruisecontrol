/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.element.starteam;

import com.starbase.starteam.ServerException;
import com.starbase.starteam.vts.comm.CommandException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.FileNotFoundException;
import net.sourceforge.cruisecontrol.NoExitSecurityManager;

public abstract class StarTeamTask extends Task {

    /**
     * The url of the Starteam repository in the form 
     * "servername:portnum/project/view"
     */
    private String _url;
    private String _userName;
    private String _password;
    
    public void setStarTeamURL(String url) {
        _url = url;
    }

    public String getURL() {
        return _url;
    }
    
    public void setUserName(String username) {
        _userName = username;
    }
    
    public String getUserName() {
        return _userName;
    }

    public void setPassword(String password) {
        _password = password;
    }
    
    public String getPassword() {
        return _password;
    }

    /**
     * Specific StarTeamTasks should implement this method as opposed to execute
     * @throws CommandException
     * @throws java.io.FileNotFoundException
     * @throws ServerException
     */
    public abstract void taskExecute() throws CommandException, FileNotFoundException, ServerException;
    
    /**
     * Wraps specific StarTeam taskExecute() to handle setting SecurityManager
     * and exceptions
     *
     * @throws BuildException
     */
    public final void execute() throws BuildException {
        try {
            // The Starteam SDK does not like the NoExitSecurityManager that comes
            // with Cruise Control. It throws a runtime error if it is still the
            // current security manager, so we set it to null here and then set it
            // back when Starteam is done.
            System.setSecurityManager(null);
            
            taskExecute();
        } catch (ServerException e) {
            throw new BuildException("Failed logon or duplicate build label." +
             "Check spelling of user name and password.", e);
        } catch (CommandException e) {
            throw new BuildException(
             "Unsuccessful reading socket or server connection lost." +
             "Check spelling of url.", e);
        } catch (java.io.FileNotFoundException e) {
            throw new BuildException("Unable to open folder" +
             "Check spelling of folder.", e);
        } catch (NullPointerException e) {
            throw new BuildException("Unsuccessful opening project or view." + 
             "Check spelling of url.", e);
        } catch (RuntimeException e) {
            throw new BuildException("Could not find specified server name." +
             "Check spelling of server.", e);
        } finally {
            System.setSecurityManager(new NoExitSecurityManager());
        }
    }    
    
}
