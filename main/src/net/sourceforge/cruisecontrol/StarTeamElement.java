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

import java.util.*;

import org.apache.tools.ant.*;

public class StarTeamElement extends SourceControlElement {

    private Set emailAddresses = new HashSet();
    private List modifications = new ArrayList();
    private OLEDate lastBuildDate;
    private OLEDate nowDate;
    private long mostRecent = 0;
    private String folderName;
    private String url;
    private String username;
    private String password;
    private Hashtable nowFiles = new Hashtable();
    private Hashtable lastBuildFiles = new Hashtable();

    /**
     * The String prepended to log messages from the source control element.  For
     * example, CVSElement should implement this as return "[cvselement]";
     *
     * @return prefix for log messages
     */
    protected String logPrefix() {
        return "[starteamelement]";
    }

    public long getLastModified() {
        return mostRecent;
    }

    public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {

        // Clean out the modifications list.
        // Otherwise we get duplicate entries when this function is called more than once in a quiet period breach
        // We normally would need to clean out the email list as well, except we know that all entries in current list
        // will still be required
        modifications.clear();

        // Clean hashtables used to store file lists
        nowFiles.clear();
        lastBuildFiles.clear();

        // Store OLEDate equivalents of now and lastbuild for performance
        nowDate       = new OLEDate(now.getTime());
        lastBuildDate = new OLEDate(lastBuild.getTime());

        // Set up two view snapshots, one at lastbuild time, one now
        View view = StarTeamFinder.openView(this.username + ":" + this.password + "@" + this.url);
        Server server = view.getServer();

        View snapshotAtNow = new View(view, ViewConfiguration.createFromTime(nowDate));
        View snapshotAtLastBuild = new View(view, ViewConfiguration.createFromTime(lastBuildDate));

        // Visit all files in the snapshots and add to Hashtables
        nowFiles = visit(StarTeamFinder.findFolder(snapshotAtNow.getRootFolder(),
         this.folderName), nowFiles, nowDate);

        try {
      	  lastBuildFiles = visit(StarTeamFinder.findFolder(snapshotAtLastBuild.getRootFolder(),
      	    this.folderName), lastBuildFiles, lastBuildDate);
        } catch(ServerException se) {
            log("Last build view does not exist");
        } 

        // Compare old and new file lists to determine what happened
        for (Enumeration e = nowFiles.elements() ; e.hasMoreElements() ;) {

            File currentNowFile = (File)e.nextElement();
            Integer currentItemID = new Integer(currentNowFile.getItemID());

            if (lastBuildFiles.containsKey(currentItemID)) {
                File matchingLastBuildFile = (File)lastBuildFiles.get(currentItemID);

                if (currentNowFile.getContentVersion() != matchingLastBuildFile.getContentVersion()) {
                    // File has been modified
                    addRevision(currentNowFile, "modified");
                } else if (!currentNowFile.getParentFolder().getFolderHierarchy().
                equals(matchingLastBuildFile.getParentFolder().getFolderHierarchy())) {

                    // File has been moved within view folder hierarchy
                    addRevision(currentNowFile, "moved");
                }

                // Remove the identified last build file from the list of last build files.
                // It will make processing the delete check on the last builds quicker
                lastBuildFiles.remove(currentItemID);
            } else {
                // File is new
                addRevision(currentNowFile, "new");
            }
        }

        // Now examine old files.  They have to have been deleted as we know they are not in
        // the new list from the processing above
        for (Enumeration e = lastBuildFiles.elements() ; e.hasMoreElements() ;) {
            File  currentLastBuildFile = (File)e.nextElement();
            addRevision((File)currentLastBuildFile.getFromHistoryByDate(nowDate), "deleted");
        }

        log(modifications.size() + " modifications in " + folderName);
        return (ArrayList) modifications;
    }

    /**
     *
     */
    public Set getEmails() {
        return emailAddresses;
    }

    ///////////////////////
    // Setters used by Ant
    ///////////////////////
    public void setFolder(String folder) {
        this.folderName = folder;
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

    private void addRevision(File revision, String status) {

        User user = revision.getServer().getUser(revision.getModifiedBy());

        if ((user != null) && (user.getName().equals("BuildMaster")))
            return;

      /* Only get emails for users still on the system */
        if (user != null) {

        /* Try to obtain email to add.  This is only allowed if logged on user is SERVER ADMINISTRATOR */
            try {
                emailAddresses.add(user.getServer().getAdministration().findUserAccount(user.getID()).getEmailAddress());
            } catch (ServerException sx) {
          /*
           * Logged on user does not have permission to get user's email.  Return the modifying user's name instead.
           * Then use the email.properties file to map the name to an email address outside of StarTeam
           */
                emailAddresses.add(user.getName());
            }
        }

        Modification mod = new Modification();
        mod.type = status;
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

    private Hashtable visit(Folder folder, Hashtable fileList, OLEDate snapshotDate) {
        try {
            Thread.sleep(100);
        } catch(InterruptedException ignoredInterruptedException) {}

        folder.populateNow(folder.getServer().getTypeNames().FILE, null, 0);

        Item[] files = folder.getItems("File");
        for (int i = 0; i < files.length; i++) {
            File file = (File) files[i];
            visit(file, fileList, snapshotDate);
        }

        Folder[] folders = folder.getSubFolders();
        for (int i = 0; i < folders.length; i++) {
            visit(folders[i], fileList, snapshotDate);
        }

        return fileList;
    }

    private Hashtable visit(File file, Hashtable fileList, OLEDate snapshotDate) {
        File revision = (File) file.getFromHistoryByDate(snapshotDate);
        fileList.put(new Integer(revision.getItemID()), revision);

        return fileList;
    }

}
