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
package net.sourceforge.cruisecontrol.element;

import com.starbase.starteam.*;
import com.starbase.starteam.vts.comm.CommandException;
import com.starbase.util.OLEDate;
import java.text.SimpleDateFormat;

import java.util.Date;
import net.sourceforge.cruisecontrol.NoExitSecurityManager;
import org.apache.tools.ant.*;

/**
 * This class logs into StarTeam and creates a label for the repository at the
 * time of the last successful build. Ant Usage: <taskdef name="starteamlabel"
 * classname="org.sourceforge.cruisecontrol.StarTeamLabel"/> <starteamlabel
 * label="1.0" lastbuild="20011514100000" description="Successful Build"
 * username="BuildMaster" password="ant"
 * starteamurl="server:port/project/view"/>
 *
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author Jason Yip, jcyip@thoughtworks.com
 */
public class StarTeamLabel extends org.apache.tools.ant.Task {

    /**
     * The name of the label to be set in Starteam.
     */
    private String labelName;

    /**
     * The label description to be set in Starteam.
     */
    private String description;

    /**
     * The time of the last successful. The new label will be a snapshot of the
     * repository at this time. String should be formatted as "yyyyMMddHHmmss"
     */
    private Date lastBuildTime;

    /**
     * The url of the Starteam repository to connect to.
     */
    private String url;

    /**
     * The username for the Starteam repository.
     */
    private String username;

    /**
     * The password for this user in the Starteam repository.
     */
    private String password;
    private final static SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    public void setLabel(String label) {
        this.labelName = label;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastbuild(String lastbuild) {
        try {
            lastBuildTime = _sdf.parse(lastbuild);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStarteamurl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This method does the work of creating the new view and checking it into
     * Starteam.
     *
     * @exception BuildException
     */
    public void execute() throws BuildException {
        try {

            // The Starteam SDK does not like the NoExitSecurityManager that comes
            // with Cruise Control. It throws a runtime error if it is still the
            // current security manager, so we set it to null here and then set it
            // back when Starteam is done.
            System.setSecurityManager(null);

            OLEDate buildDate = new OLEDate(lastBuildTime);

            // Get view as of the last successful build time.
            View view = StarTeamFinder.openView(this.username + ":" + this.password + "@" + this.url);
            View snapshot = new View(view, ViewConfiguration.createFromTime(buildDate));

            // Create the new label and update the repository
            new Label(snapshot, labelName, description, buildDate, true).update();

            System.setSecurityManager(new NoExitSecurityManager());

        }
        catch (ServerException e) {
            // username or password are wrong
            log("ERROR: StarTeam is returning a ServerException.");
            log("       Most likely caused by by a failed logon or a duplicate build label.");
            log("       Please verify the user name and password and try again.");
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            // Project name or view name is wrong
            log("ERROR: StarTeam is returning a NullPointerException.");
            log("       This is most likely caused by unsuccessfully opening the project or the");
            log("       view. Please verify the spelling of the url and try again.");
            e.printStackTrace();
        }
        catch (CommandException e) {
            // port number is wrong
            log("ERROR: StarTeam is returning a CommandException.");
            log("       This is most likely caused by unsuccessfully attempting to read from");
            log("       socket or Connection to server lost.Please verify the spelling of the");
            log("       url and try again.");
            e.printStackTrace();
        }
        catch (RuntimeException e) {
            // Server Name is wrong
            log("ERROR: StarTeam is returning a RuntimeException.");
            log("       This is most likely caused by not finding the server name specified.");
            log("       Please verify the spelling of the server and try again.");
            e.printStackTrace();
        }
    }
}
