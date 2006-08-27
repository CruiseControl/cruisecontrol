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
package net.sourceforge.cruisecontrol.bootstrappers;

import com.starbase.starteam.commandline.StarTeamCmd;
import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.Commandline;

/**
 * Since we rely on our build.xml to handle updating our source code, there has
 * always been a problem with what happens when the build.xml file itself
 * changes.  Previous workarounds have included writing a wrapper build.xml that
 * will check out the "real" build.xml.  This class is a substitute for that
 * practice.
 *
 * The StarTeamBootstrapper will handle updating a multiple space delimited files from StarTeam before the
 * build begins.
 *
 * Usage:
 *
 *     &lt;starteambootstrapper username="" 
 *                              password="" 
 *                              server="" 
 *                              port="" 
 *                              project="" 
 *                              view=""
 *                              folder=""
 *                              files=""
 *                              localfolder=""/&gt;
 */
public class StarTeamBootstrapper implements Bootstrapper {

    private String username;
    private String password;
    private String servername;
    private String serverport;
    private String projectname;
    private String viewname;
    private String foldername;
    private String localfoldername;
    private String filenames;

    public StarTeamBootstrapper() {
    }

    public void setUsername(String name) {
        username = name;
    }

    public void setPassword(String passwd) {
        password = passwd;
    }

    public void setServer(String server) {
        servername = server;
    }

    public void setPort(String port) {
        serverport = port;
    }

    public void setProject(String project) {
        projectname = project;
    }

    public void setView(String view) {
        viewname = view;
    }

    public void setFolder(String folder) {
        foldername = folder;
    }

    public void setLocalFolder(String localfolder) {
        localfoldername = localfolder;
    }

    public void setFiles(String files) {
        filenames = files;
    }

    public void bootstrap() throws CruiseControlException {
        Commandline args = buildCheckoutCommand();
        int retVal = StarTeamCmd.run(args.getCommandline());
        if (retVal != 0) {
            throw new CruiseControlException("Error executing StarTeam checkout command: " + args.toString());
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(filenames == null
            || username == null
            || password == null
            || servername == null
            || serverport == null
            || projectname == null
            || viewname == null
            || foldername == null,
            "'files', 'username', 'password','server', 'port', 'project', 'view' and 'folder'"
              + " are all required for StarTeamBootstrapper");
    }

    private Commandline buildCheckoutCommand() {
        Commandline commandLine = new Commandline();
        commandLine.createArgument("co");
        commandLine.createArgument("-p");
        commandLine.createArgument(
            username
                + ':'
                + password
                + '@'
                + servername
                + ':'
                + serverport
                + '/'
                + projectname
                + '/'
                + viewname
                + '/'
                + foldername);
        if (localfoldername != null) {
            commandLine.createArguments("-fp", localfoldername);
        }
        commandLine.createArgument("-o");
        commandLine.createArgument("-x");
        commandLine.createArgument().setLine(filenames);
        return commandLine;
    }

    public String toString() {
        return buildCheckoutCommand().toString();
    }

    public static void main(String[] args) throws CruiseControlException {
        StarTeamBootstrapper bootstrapper = new StarTeamBootstrapper();
        bootstrapper.setUsername("ccuser");
        bootstrapper.setPassword("ccuser");
        bootstrapper.setServer("starteamserver");
        bootstrapper.setPort("4001");
        bootstrapper.setProject("ProjectName");
        bootstrapper.setView("ViewName");
        bootstrapper.setFolder("src/folder");
        bootstrapper.setLocalFolder("checkoutfolder");
        bootstrapper.setFiles("build.properties build.xml");
        System.out.println(bootstrapper);
        bootstrapper.bootstrap();
    }
}
