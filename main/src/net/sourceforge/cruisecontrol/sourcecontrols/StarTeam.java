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
package net.sourceforge.cruisecontrol.sourcecontrols;

import com.starbase.starteam.*;
import com.starbase.util.OLEDate;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.NoExitSecurityManager;

import java.util.*;

import org.apache.log4j.Category;

/**
 * This class logs into StarTeam and collects information on any modifications
 * made since the last successful build.
 * <P>
 * Ant Usage:
 * <CODE><PRE>
 * <taskdef name="starteamelement"
 *     classname="net.sourceforge.cruisecontrol.StarTeamElement"/>
 * <starteamelement username="BuildMaster" password="ant"
 *     starteamurl="server:port/project/view" folder="Source"/>
 * </PRE></CODE>
 *
 * @author Christopher Charlier -- ThoughtWorks Inc. 2001
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Neill
 */
public class StarTeam extends SourceControlElement {

    /** enable logging for this class */
    private static Category log = Category.getInstance(StarTeam.class.getName());

    private String username;
    private String password;
    private String folder;
    private String url;
    private Set emailAddresses = new HashSet();
    private List modifications = new ArrayList();
    private OLEDate mostRecent = new OLEDate(0);
    private OLEDate nowDate;

    /**
     * Set StarTeam user name
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Set password for StarTeam user
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set repository folder
     */
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
        return mostRecent.getLongValue();
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
    public List getHistory(Date lastBuild, Date now, long quietPeriod) {
        // Clean out the modifications list.  Otherwise we get duplicate entries
        // when this function is called more than once in a quiet period breach
        // We normally would need to clean out the email list as well, except we
        // know that all entries in current list will still be required
        modifications.clear();

        // Store OLEDate equivalents of now and lastbuild for performance
        nowDate = new OLEDate(now.getTime());
        OLEDate lastBuildDate = new OLEDate(lastBuild.getTime());

        //StarTeam SDK does not like NoExitSecurityManager
        System.setSecurityManager(null);
        
        Server server = null;
        try {
            // Set up two view snapshots, one at lastbuild time, one now
            View view = StarTeamFinder.openView(this.username + ":"
             + this.password + "@" + this.url);
            server = view.getServer();
            
            View snapshotAtNow = new View(view,
            ViewConfiguration.createFromTime(nowDate));
            View snapshotAtLastBuild = new View(view,
            ViewConfiguration.createFromTime(lastBuildDate));

            Map nowFiles = new HashMap();
            Map lastBuildFiles = new HashMap();

            // Visit all files in the snapshots and add to Maps
            addFolderModsToList(nowFiles,
             StarTeamFinder.findFolder(snapshotAtNow.getRootFolder(),
             this.folder), nowDate);

            try {
                addFolderModsToList(lastBuildFiles,
                 StarTeamFinder.findFolder(
                 snapshotAtLastBuild.getRootFolder(),
                 this.folder), lastBuildDate);
            } catch (ServerException se) {
                log.error("Server Exception occurred visiting last build view: ", se);
            }

            compareFileLists(nowFiles, lastBuildFiles);

            // Discard cached items so memory is not eaten up
            snapshotAtNow.getRootFolder().discardItems(server.getTypeNames().FILE, -1);

            try {
                snapshotAtLastBuild.getRootFolder().discardItems(server.getTypeNames().FILE, -1);
            } catch (ServerException se) {
                log.error("Server Exception occurred discarding last build file cache: ", se);
            }

            log.info(modifications.size() + " modifications in " + this.folder);
            return (ArrayList) modifications;
        } finally {
            server.disconnect();
            System.setSecurityManager(new NoExitSecurityManager());
        }
    }

    /**
     * Compare old and new file lists to determine what happened
     */
    private void compareFileLists(Map nowFiles, Map lastBuildFiles) {
        for (Iterator iter = nowFiles.keySet().iterator(); iter.hasNext(); ) {
            Integer currentItemID  = (Integer) iter.next();
            File currentFile = (File) nowFiles.get(currentItemID);

            if (lastBuildFiles.containsKey(currentItemID)) {
                File lastBuildFile =
                (File) lastBuildFiles.get(currentItemID);

                if (fileHasBeenModified(currentFile, lastBuildFile)) {
                    addRevision(currentFile, "modified");
                } else if (fileHasBeenMoved(currentFile, lastBuildFile)) {
                    addRevision(currentFile, "moved");
                }
                // Remove the identified last build file from the list of
                // last build files.  It will make processing the delete
                // check on the last builds quicker
                lastBuildFiles.remove(currentItemID);
            } else {
                // File is new
                addRevision(currentFile, "new");
            }
        }
        examineOldFiles(lastBuildFiles);
    }

    /**
     * Now examine old files.  They have to have been deleted as we know they
     * are not in the new list from the processing above.
     */
    private void examineOldFiles(Map lastBuildFiles) {
        for (Iterator iter = lastBuildFiles.values().iterator(); iter.hasNext(); ) {
            File currentLastBuildFile = (File) iter.next();
            addRevision((File) currentLastBuildFile.getFromHistoryByDate(nowDate),
             "deleted");
        }
    }

    private boolean fileHasBeenModified(File currentFile, File lastBuildFile) {
        return currentFile.getContentVersion() != lastBuildFile.getContentVersion();
    }

    private boolean fileHasBeenMoved(File currentFile, File lastBuildFile) {
        return !currentFile.getParentFolder().getFolderHierarchy().equals(
         lastBuildFile.getParentFolder().getFolderHierarchy());
    }

    private void addFolderModsToList(Map fileList, Folder folder,
     OLEDate snapshotDate) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignoredInterruptedException) {}

        Item[] files = folder.getItems("File");
        for (int i = 0; i < files.length; i++) {
            File file = (File) files[i];
            addFileModsToList(fileList, file, snapshotDate);
        }

        Folder[] folders = folder.getSubFolders();
        for (int i = 0; i < folders.length; i++) {
            addFolderModsToList(fileList, folders[i], snapshotDate);
        }
    }

    private void addFileModsToList(Map fileList, File file,
     OLEDate snapshotDate) {
        File revision = (File) file.getFromHistoryByDate(snapshotDate);

        if (revision != null) {
            fileList.put(new Integer(revision.getItemID()), revision);
        }
    }

    /**
     * Adds the revision to the modification set.
     *
     * @param revision
     */
    private void addRevision(File revision, String status) {
        User user = revision.getServer().getUser(revision.getModifiedBy());

        if ((user != null) && (user.getName().equals("BuildMaster"))) {
            return;
        }

        //  Only get emails for users still on the system
        if (user != null) {

             // Try to obtain email to add.  This is only allowed if logged on
             // user is SERVER ADMINISTRATOR
            try {
                emailAddresses.add(
                 user.getServer().getAdministration().findUserAccount(
                 user.getID()).getEmailAddress());
            } catch (ServerException sx) {
                // Logged on user does not have permission to get user's email.
                // Return the modifying user's name instead. Then use the
                // email.properties file to map the name to an email address
                // outside of StarTeam
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

        if (revision.getModifiedTime().getLongValue() > mostRecent.getLongValue()) {
            mostRecent = revision.getModifiedTime();
        }
    }

}
