/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit                              *
 * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
 * 651 W Washington Ave. Suite 500                                              *
 * Chicago, IL 60661 USA                                                        *
 *                                                                              *
 * This program is free software; you can redistribute it and/or                *
 * modify it under the terms of the GNU General Public License                  *
 * as published by the Free Software Foundation; either version 2               *
 * of the License, or (at your option) any later version.                       *
 *                                                                              *
 * This program is distributed in the hope that it will be useful,              *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 * GNU General Public License for more details.                                 *
 *                                                                              *
 * You should have received a copy of the GNU General Public License            *
 * along with this program; if not, write to the Free Software                  *
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
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
