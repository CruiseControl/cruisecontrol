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
