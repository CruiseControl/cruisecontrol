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

package net.sourceforge.cruisecontrol;

import com.starbase.starteam.*;
import com.starbase.util.*;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import org.apache.tools.ant.*;

public class StarTeamElement implements SourceControlElement {

    private Set emailAddresses = new HashSet();
    private List modifications = new ArrayList();
    private long startTime;
    private long mostRecent = 0;
    private Folder folder;
    private String folderName;
    private String targetFolderPath = "";
    private org.apache.tools.ant.Task _task;
    private String url;
    private String username;
    private String password;

    /**
     *	Set the parent task for logging purposes
     */
    public void setTask(org.apache.tools.ant.Task task) {
        _task = task;
    }

    public long getLastModified() {
        return mostRecent;
    }

    public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {

        View view = StarTeamFinder.openView(this.username + ":" + this.password + "@" + this.url);
        Server server = view.getServer();

        OLEDate snapshotDate = new OLEDate(now.getTime());
        View snapshot = new View(view, ViewConfiguration.createFromTime(snapshotDate));

		if (!"".equals(targetFolderPath)) {
			snapshot.getRootFolder().setAlternatePathFragment(this.targetFolderPath);
		}

        this.folder = StarTeamFinder.findFolder(snapshot.getRootFolder(), this.folderName);

        startTime = lastBuild.getTime();

        visit(folder, snapshotDate);

        //need to check the mostRecent and make sure it's within the quiet period...

        log(modifications.size() + " modifications in " + folderName);
        return (ArrayList) modifications;
    }



    /**
     *
     */
    public Set getEmails() {
        return emailAddresses;
    }

    public void setFolder(String folder) {
        this.folderName = folder;
    }

    public void setToDir(String toDir) {
		this.targetFolderPath = toDir.trim();
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


    public boolean isEmpty() {
        return modifications.isEmpty();
    }

    public String[] getEmailAddresses() {
        return (String[]) emailAddresses.toArray(new String[emailAddresses.size()]);
    }

    public long getMostRecent() {
        return mostRecent;
    }

    private void addRevision(File revision) {

		User user = revision.getServer().getUser(revision.getModifiedBy());

        if ((user != null) && (user.getName().equals("BuildMaster")))
            return;

        // Get the modification status before we check the current file out from StarTeam.  Must do this as afterwards the status will always be CURRENT
        Modification mod = new Modification();
        mod.type = getModificationType(revision);

        try {
            revision.checkout(Item.LockType.UNCHANGED, true, true, true);
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }

		/* Only get emails for users still on the system */
		if (user != null) {

			/* Try to obtain email to add.  This is only allowed if logged on user is SERVER ADMINISTRATOR */
			try {
				emailAddresses.add(user.getServer().getAdministration().findUserAccount(user.getID()).getEmailAddress());
			}
			catch (ServerException sx) {
				/*
				 * Logged on user does not have permission to get user's email.  Return the modifying user's name instead.
				 * Then use the email.properties file to map the name to an email address outside of StarTeam
				 */
				emailAddresses.add(user.getName());
			}
		}

        mod.fileName = revision.getName();
        mod.folderName = revision.getParentFolder().getFolderHierarchy();
        mod.modifiedTime = revision.getModifiedTime().createDate();
        mod.userName = user.getName();
        mod.comment = revision.getComment();

        modifications.add(mod);

        log("File: " + mod.fileName);

        log("userName: " + mod.userName + " Date: " + mod.modifiedTime);
        if (revision.getModifiedTime().getLongValue() > mostRecent) {
            mostRecent = revision.getModifiedTime().getLongValue();
        }
    }

    private void visit(Folder folder, OLEDate snapshotDate) {
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ignoredInterruptedException) {}
        folder.populateNow(folder.getServer().getTypeNames().FILE, new String[] {}, 0);
        Item[] files = folder.getItems("File");
        for (int i = 0; i < files.length; i++) {
            File file = (File) files[i];
            visit(file, snapshotDate);
        }

        Folder[] folders = folder.getSubFolders();
        for (int i = 0; i < folders.length; i++) {
            visit(folders[i], snapshotDate);
        }
    }

    private void visit(File file, OLEDate snapshotDate) {
        if (file.getModifiedTime().getLongValue() < startTime)
            return;

        File revision = (File) file.getFromHistoryByDate(snapshotDate);
        addRevision(revision);

    }

    /*
     * @return Empty string on IOException
     */
    private String getModificationType(File file) {

        try{
            if (file.getStatus() == Status.MERGE || file.getStatus() == Status.UNKNOWN) {
                file.updateStatus(true, true);
            }

            switch(file.getStatus()) {
                case Status.MERGE:
                    return "merge";

                case Status.MISSING:
                    return "new";

                case Status.MODIFIED:
                    return "checkin";

                case Status.NEW:
                    return "delete";

                case Status.OUTOFDATE:
                    return "outofdate";

                case Status.UNKNOWN:
                    return "unknown";

                default:
                    return "unknownfilestatus - " + file.getStatus();
            }
        } catch (java.io.IOException e) { return "";}
    }
    
    /**
     * Use Ant task to send a log message
     */
    public void log(String message) {
        if (_task != null) {
            _task.log("[starteamelement]" + message);
        }
    }
    
}
