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
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class StarTeamLabel extends StarTeamTask {

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

    private final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyyMMddHHmmss");

    public void setLabel(String label) {
        this.labelName = label;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastBuild(String lastbuild) {
        try {
            lastBuildTime = DATE_FORMAT.parse(lastbuild);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method does the work of creating the new view and checking it into
     * Starteam.
     *
     * @exception java.io.FileNotFoundException
     * @throws CommandException
     * @throws FileNotFoundException
     * @throws ServerException
     */
    public void taskExecute() throws CommandException, java.io.FileNotFoundException, ServerException {
        OLEDate buildDate = new OLEDate(lastBuildTime);

        // Get view as of the last successful build time.
        View view = StarTeamFinder.openView(getUserName() + ":" + getPassword()
                 + "@" + getURL());
        View snapshot = new View(view, ViewConfiguration.createFromTime(buildDate));

        // Create the new label and update the repository
        new Label(snapshot, labelName, description, buildDate, true).update();
    }

}
