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

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.NoExitSecurityManager;
import org.apache.tools.ant.*;

/**
 * This class logs into StarTeam and collects information on any modifications
 * made since the last successful build. Ant Usage: <taskdef
 * name="starteamelement" classname="net.sourceforge.cruisecontrol.StarTeamElement"/>
 * <starteamelement username="BuildMaster" password="ant"
 * starteamurl="server:port/project/view" folder="Source"/>
 *
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author Jason Yip, jcyip@thoughtworks.com
 */
//(PENDING) common StarTeam super class
public class StarTeamElement extends SourceControlElement {

    /**
     * The username for the Starteam repository.
     */
    private String username;

    /**
     * The password for this user in the Starteam repository.
     */
    private String password;

    /**
     * The Starteam folder to check out from the repository.
     */
    private String folder;

    /**
     * The url of the Starteam repository to connect to.
     */
    private String url;

    /**
     * The collection of all email addresses of anyone who made a modification.
     */
    private Set emailAddresses = new HashSet();

    /**
     * The collection of all modifications since the last successful build.
     */
    private List modificationList = new ArrayList();

    /**
     * The root folder in the StarTeam directory passed in by Ant.
     */
    private Folder rootFolder;

    /**
     * The time of the most recent modification in StarTeam.
     */
    private OLEDate mostRecentModification = new OLEDate(0);

    /**
     * The time of the last successful build.
     */
    private Date lastSuccessfulBuild;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public void setStarteamurl(String url) {
        this.url = url;
    }

    public Set getEmails() {
        return emailAddresses;
    }

    /**
     * Returns the modified time of the most recent change in StarTeam. This
     * helps to make sure that we don't start a build with someones changes half
     * checked in to StarTeam.
     *
     * @return
     */
    public long getLastModified() {
        return mostRecentModification.getLongValue();
    }

    /**
     * Populates the modification set with all appropriate information based on
     * the changes since the last successful build.
     *
     * @param lastBuild
     * @param now
     * @param quietPeriod
     * @return
     */
    public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {
        try {

            // The Starteam SDK does not like the NoExitSecurityManager that comes
            // with Cruise Control. It throws a runtime error if it is still the
            // current security manager, so we set it to null here and then set it
            // back when Starteam is done.
            System.setSecurityManager(null);
            lastSuccessfulBuild = lastBuild;

            // Get view as of the last successful build time.
            View view = StarTeamFinder.openView(this.username + ":" + this.password + "@" + this.url);
            View snapshot = new View(view, ViewConfiguration.createFromTime(new OLEDate(now.getTime())));

            rootFolder = StarTeamFinder.findFolder(snapshot.getRootFolder(), this.folder);

            if (rootFolder == null) {
                log("Root folder is null", org.apache.tools.ant.Project.MSG_ERR);
                throw new FileNotFoundException();
            }

            // Inspect everything in the root folder
            visit(rootFolder);

            // Add some info to the log file
            log(modificationList.size() + " modifications in " + this.folder);
            System.setSecurityManager(new NoExitSecurityManager());

        }
        catch (ServerException e) {
            // username or password are wrong
            log("ERROR: StarTeam is returning a ServerException.");
            log("       This is most likely caused by by a failed logon.");
            log("       Please verify the spelling of the user name and password and try again.");
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            // Folder is wrong
            log("ERROR: Unable to open the folder.");
            log("       Please verify the spelling of the folder and try again.");
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

        return (ArrayList) modificationList;
    }

    /**
     * Returns the type of the revision that took place.
     *
     * @param revision
     * @return
     */
    private String getRevisionType(File revision) {
        //(PENDING) switch-case

        try {
            // We try to update the status once to give StarTeam another chance.
            if (revision.getStatus() == Status.MERGE || revision.getStatus() == Status.UNKNOWN) {
                revision.updateStatus(true, true);
            }

            // If the File doesn't exist on the local machine, we will assume it is a new file.
            if (revision.getStatus() == Status.MISSING) {
                return "NEW";
            }
            // If the file is not in StarTeam, but is on the local machine, we assume someone deleted it.
            else if (revision.getStatus() == Status.NEW) {
                return "DELETED";
            }
            // Otherwise, we assume someone modified the file.
            else {
                return "MODIFIED";
            }
        }
        catch (java.io.IOException e) {
            log("ERROR: there was problems reading the local file : " + revision.getName() + "!!!!");
            return "ERROR";
        }
    }

    /**
     * Visits a folder to check on the files and sub folders that exist for
     * changes.
     *
     * @param folder
     */
    private void visit(Folder folder) {
        // For all Files in this folder, we need to check to see if there have been modifications.
        Item[] files = folder.getItems("File");
        for (int i = 0; i < files.length; i++) {
            File eachFile = (File) files[i];
            visit(eachFile);
        }

        // We also want to recursively call this method on all sub folders in this folder.
        Folder[] subFolders = folder.getSubFolders();
        for (int i = 0; i < subFolders.length; i++) {
            visit(subFolders[i]);
        }
    }

    /**
     * Visits a file to see if there have been changes since the last successful
     * build.
     *
     * @param file
     */
    private void visit(File file) {
        if (file.getModifiedTime().getLongValue() < lastSuccessfulBuild.getTime()) {
            return;
        }

        /**
         * There is an assumption made here. It is that the call to getHistory()
         * will return an array in Decending order sorted by the Modified time
         * starting with the most recent modification. Currently, that is what
         * Starteams SDK does, however, there is no reason, this will be the
         * same in the future. Be aware.
         */
        Item[] history = file.getHistory();
        for (int i = 0; i < history.length; i++) {
            File eachRevision = (File) history[i];

            if (eachRevision.getModifiedTime().getLongValue() > lastSuccessfulBuild.getTime()) {
                addRevision(eachRevision);
            }
            else {
                return;
            }
        }
    }

    /**
     * Adds the revision to the modification set.
     *
     * @param revision
     */
    private void addRevision(File revision) {
        Modification mod = new Modification();
        UserAccount account = revision.getServer().getAdministration().findUserAccount(revision.getModifiedBy());

        // Populate the Modification Object
        mod.type = getRevisionType(revision);
        mod.fileName = revision.getName();
        mod.folderName = revision.getParentFolder().getFolderHierarchy();
        mod.modifiedTime = revision.getModifiedTime().createDate();
        mod.userName = account.getName();
        mod.comment = revision.getComment();

        // Add this modification to our appropriate lists
        modificationList.add(mod);
        emailAddresses.add(account.getEmailAddress());

        // Update the most recent modification time, if appropriate.
        if (revision.getModifiedTime().getLongValue() > mostRecentModification.getLongValue()) {
            mostRecentModification = revision.getModifiedTime();
        }

        log("File: " + mod.fileName);
        log("userName: " + mod.userName + " Date: " + mod.modifiedTime);
    }
}
