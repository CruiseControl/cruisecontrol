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
import java.lang.NullPointerException;
import java.lang.RuntimeException;
import java.text.SimpleDateFormat;

import java.util.*;
import net.sourceforge.cruisecontrol.NoExitSecurityManager;
import org.apache.tools.ant.*;

/**
 * This class logs into StarTeam checks out any changes that have occurred since
 * the last successful build. It also creates all working directories on the
 * local directory if appropriate. Ant Usage: <taskdef name="starteamcheckout"
 * classname="org.apache.tools.ant.taskdefs.StarTeamCheckout"/>
 * <starteamcheckout username="BuildMaster" password="ant" folder="Source"
 * starteamurl="servername:portnum/project/view"
 * createworkingdirectories="true"/>
 *
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author Jason Yip, jcyip@thoughtworks.com
 */
public class StarTeamCheckout extends org.apache.tools.ant.Task {
    /**
     * The root folder in the StarTeam directory passed in by Ant.
     */
    private Folder rootFolder;

    /**
     * The username for the Starteam repository.
     */
    private String username;

    /**
     * The password for this user in the Starteam repository.
     */
    private String password;

    /**
     * Set the folder in the Starteam repository to check out the code from. All
     * subdirectories of the folder will be checked out as well.
     */
    private String folder;

    /**
     * The url of the Starteam repository to connect to.
     */
    private String url;

    /**
     * Set the boolean value that tells us if we want to create all directories
     * that are in the Starteam repository regardless if they are empty.
     */
    private String createDirs;

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

    public void setCreateWorkingDirectories(String createWorkingDirectories) {
        this.createDirs = createWorkingDirectories;
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

            // Get view as of the last successful build time.
            View view = StarTeamFinder.openView(this.username + ":" + this.password + "@" + this.url);
            View snapshot = new View(view, ViewConfiguration.createFromTime(new OLEDate()));
            rootFolder = StarTeamFinder.findFolder(snapshot.getRootFolder(), this.folder);

            if (rootFolder == null) {
                throw new FileNotFoundException();
            }

            // Inspect everything in the root folder
            visit(rootFolder);

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
    }

    /**
     * Visits a folder to check on the files and sub folders that exist for
     * changes.
     *
     * @param folder
     */
    private void visit(Folder folder) {
        try {

            Set localFiles = getLocalFiles(folder);

            // If we have been told to create the working folders
            if ((createDirs == "true") || (createDirs == "yes")) {
                // Create if it doesn't exist
                java.io.File f = new java.io.File(folder.getPath());
                if (!f.exists()) {
                    f.mkdir();
                }
            }

            // For all Files in this folder, we need to check to see if there have been modifications.
            Item[] files = folder.getItems("File");
            for (int i = 0; i < files.length; i++) {
                File eachFile = (File) files[i];
                localFiles.remove(eachFile.getFullName());
                int fileStatus = (eachFile.getStatus());
                // We try to update the status once to give StarTeam another chance.
                if (fileStatus == Status.MERGE || fileStatus == Status.UNKNOWN) {
                    eachFile.updateStatus(true, true);
                }

                // If the file is current then skip it.
                if (fileStatus == Status.CURRENT) {
                    continue;
                }

                // Checkout anything else.
                // Just a note: StarTeam has a status for NEW which implies that their is
                // Something on your local machine that is not in the repository. These
                // are the items that show up as NOT IN VIEW in the Starteam GUI. One
                // would think that we would want to perhaps checkin the NEW items.
                // Unfortunately, the sdk doesn't really work, and we cant actually see
                // anything with a status of NEW. That is why we can just checkout everything
                // here and we don't need to worry about losing anything.
                System.out.println("Checking Out: " + (eachFile.getFullName()));
                eachFile.checkout(Item.LockType.UNCHANGED, true, true, true);
            }

            // We also want to recursively call this method on all sub folders in this folder.
            Folder[] subFolders = folder.getSubFolders();
            for (int i = 0; i < subFolders.length; i++) {
                localFiles.remove(subFolders[i].getPath());
                visit(subFolders[i]);
            }

            // Delete all folders or files that are not in Starteam.
            if (!localFiles.isEmpty()) {
                delete(localFiles);
            }
        }
        catch (java.io.IOException e) {
            System.out.println("ERROR: Error occurred while reading file.");
        }
    }

    /**
     * Deletes everything on the local machine that is not in Starteam.
     *
     * @param files
     */
    private void delete(Collection files) {
        try {

            Iterator itr = files.iterator();
            while (itr.hasNext()) {
                java.io.File file = new java.io.File(itr.next().toString());
                delete(file);
            }

        }
        catch (SecurityException e) {
            System.out.println("Error deleting file: " + e);
        }
    }

    /**
     * Deletes the file from the local drive.
     *
     * @param file
     * @return
     */
    private boolean delete(java.io.File file) {
        // If the current file is a Directory, we need to delete all its children as well.
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }

        System.out.println("Deleting: " + file.getAbsolutePath());
        return file.delete();
    }

    /**
     * Gets the collection of the local file names in the current directory We
     * need to check this collection against what we find in Starteam to
     * understand what we need to delete in order to synch with the repos.
     *
     * @param folder
     * @return
     */
    private static Set getLocalFiles(Folder folder) {
        java.io.File localFolder = new java.io.File(folder.getPath());
        if (localFolder.exists()) {
            String[] localFiles = localFolder.list();
            for (int i = 0; i < localFiles.length; i++) {
                localFiles[i] = folder.getPath() + "\\" + localFiles[i];
            }
            return new HashSet(Arrays.asList(localFiles));
        }
        else {
            return Collections.EMPTY_SET;
        }
    }
}
