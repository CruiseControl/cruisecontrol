/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.PropertyNames;
import com.starbase.starteam.Server;
import com.starbase.starteam.ServerException;
import com.starbase.starteam.StarTeamFinder;
import com.starbase.starteam.User;
import com.starbase.starteam.UserAccount;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
import com.starbase.util.OLEDate;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class logs into StarTeam and collects information on any modifications
 * made since the last successful build.
 *
 * @author Christopher Charlier -- ThoughtWorks Inc. 2001
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Neill
 */
public class StarTeam implements SourceControl {

    private static final Logger LOG = Logger.getLogger(StarTeam.class);

    private String userName;
    private String password;
    private String folder;
    private String url;
    private List modifications = new ArrayList();
    private OLEDate nowDate;

    private Hashtable properties = new Hashtable();
    private String property;
    private String propertyOnDelete;

    private boolean preloadFileInformation = false;
    private boolean canLookupEmails = true;

    /**
     * Set StarTeam user name
     */
    public void setUsername(String userName) {
        this.userName = userName;
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

    public void setPreloadFileInformation(boolean preloadFileInformation) {
        this.preloadFileInformation = preloadFileInformation;
    }

    public void setStarteamurl(String url) {
        this.url = url;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        this.propertyOnDelete = propertyOnDelete;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        if (folder == null) {
            throw new CruiseControlException("'folder' is a required attribute on StarTeam.");
        }
        if (url == null) {
            throw new CruiseControlException("'url' is a required attribute on StarTeam.");
        }
        if (userName == null) {
            throw new CruiseControlException("'username' is a required attribute on StarTeam.");
        }
        if (password == null) {
            throw new CruiseControlException("'password' is a required attribute on StarTeam.");
        }
    }

    /**
     * Populates the modification set with all appropriate information based on
     * the changes since the last successful build.
     *
     * @param lastBuild
     * @param now
     * @return
     */
    public List getModifications(Date lastBuild, Date now) {
        // Clean out the modifications list.  Otherwise we get duplicate entries
        // when this function is called more than once in a quiet period breach
        // We normally would need to clean out the email list as well, except we
        // know that all entries in current list will still be required
        modifications.clear();

        // Store OLEDate equivalents of now and lastbuild for performance
        nowDate = new OLEDate(now.getTime());
        OLEDate lastBuildDate = new OLEDate(lastBuild.getTime());

        Server server = null;
        try {
            // Set up two view snapshots, one at lastbuild time, one now
            View view = StarTeamFinder.openView(userName + ":" + password + "@" + url);
            server = view.getServer();

            View snapshotAtNow = new View(view, ViewConfiguration.createFromTime(nowDate));
            View snapshotAtLastBuild =
                new View(view, ViewConfiguration.createFromTime(lastBuildDate));

            Map nowFiles = new HashMap();
            Map lastBuildFiles = new HashMap();

            Folder nowRoot = StarTeamFinder.findFolder(snapshotAtNow.getRootFolder(), folder);

            PropertyNames stPropertyNames = server.getPropertyNames();
            // properties to fetch immediately and cache
            final String[] propertiesToCache =
                new String[] {
                    stPropertyNames.FILE_CONTENT_REVISION,
                    stPropertyNames.MODIFIED_TIME,
                    stPropertyNames.FILE_FILE_TIME_AT_CHECKIN,
                    stPropertyNames.COMMENT,
                    stPropertyNames.MODIFIED_USER_ID,
                    stPropertyNames.FILE_NAME };

            if (preloadFileInformation) {
                // cache information for now
                nowRoot.populateNow(server.getTypeNames().FILE, propertiesToCache, -1);
            }

            // Visit all files in the snapshots and add to Maps
            addFolderModsToList(nowFiles, nowRoot);

            try {
                Folder lastBuildRoot =
                    StarTeamFinder.findFolder(snapshotAtLastBuild.getRootFolder(), folder);

                if (preloadFileInformation) {
                    // cache information for last build
                    lastBuildRoot.populateNow(server.getTypeNames().FILE, propertiesToCache, -1);
                }

                addFolderModsToList(lastBuildFiles, lastBuildRoot);
            } catch (ServerException se) {
                LOG.error("Server Exception occurred visiting last build view: ", se);
            }

            compareFileLists(nowFiles, lastBuildFiles);

            // Discard cached items so memory is not eaten up
            snapshotAtNow.getRootFolder().discardItems(server.getTypeNames().FILE, -1);

            try {
                snapshotAtLastBuild.getRootFolder().discardItems(server.getTypeNames().FILE, -1);
            } catch (ServerException se) {
                LOG.error("Server Exception occurred discarding last build file cache: ", se);
            }

            LOG.info(modifications.size() + " modifications in " + folder);
            return modifications;
        } catch (Exception e) {
            LOG.error("Problem looking up modifications in StarTeam.", e);
            modifications.clear();
            return modifications;
        } finally {
            if (server != null) {
                server.disconnect();
            }
        }
    }

    /**
     * Compare old and new file lists to determine what happened
     */
    private void compareFileLists(Map nowFiles, Map lastBuildFiles) {
        for (Iterator iter = nowFiles.keySet().iterator(); iter.hasNext();) {
            Integer currentItemID = (Integer) iter.next();
            File currentFile = (File) nowFiles.get(currentItemID);

            if (lastBuildFiles.containsKey(currentItemID)) {
                File lastBuildFile = (File) lastBuildFiles.get(currentItemID);

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
        for (Iterator iter = lastBuildFiles.values().iterator(); iter.hasNext();) {
            File currentLastBuildFile = (File) iter.next();
            addRevision((File) currentLastBuildFile.getFromHistoryByDate(nowDate), "deleted");
        }
    }

    private boolean fileHasBeenModified(File currentFile, File lastBuildFile) {
        return currentFile.getContentVersion() != lastBuildFile.getContentVersion();
    }

    private boolean fileHasBeenMoved(File currentFile, File lastBuildFile) {
        return !currentFile.getParentFolder().getFolderHierarchy().equals(
            lastBuildFile.getParentFolder().getFolderHierarchy());
    }

    private void addFolderModsToList(Map fileList, Folder folder) {
        //try {
        //    Thread.sleep(100);
        //} catch (InterruptedException ignoredInterruptedException) {}

        Item[] files = folder.getItems("File");
        for (int i = 0; i < files.length; i++) {
            File file = (File) files[i];
            fileList.put(new Integer(file.getItemID()), file);
        }

        Folder[] folders = folder.getSubFolders();
        for (int i = 0; i < folders.length; i++) {
            addFolderModsToList(fileList, folders[i]);
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

        Modification mod = new Modification();
        mod.type = "StarTeam";
        String fileName = revision.getName();
        String folderName = revision.getParentFolder().getFolderHierarchy();
        Modification.ModifiedFile modFile = mod.createModifiedFile(fileName, folderName);
        modFile.action = status;
        mod.modifiedTime = revision.getModifiedTime().createDate();
        mod.userName = user.getName();
        mod.comment = revision.getComment();

        //  Only get emails for users still on the system
        if (user != null && canLookupEmails) {

            // Try to obtain email to add.  This is only allowed if logged on
            // user is SERVER ADMINISTRATOR
            try {
                // check if user account exists
                UserAccount useracct =
                    user.getServer().getAdministration().findUserAccount(user.getID());
                if (useracct == null) {
                    LOG.warn("User account " + user.getID() + " not found for email address.");
                } else {
                    mod.emailAddress = useracct.getEmailAddress();
                }
            } catch (ServerException sx) {
                // Logged on user does not have permission to get user's email.
                // Return the modifying user's name instead. Then use the
                // email.properties file to map the name to an email address
                // outside of StarTeam
                LOG.debug("Error looking up user email address.", sx);
                canLookupEmails = false;
            }
        }

        modifications.add(mod);
        if (status.equals("deleted")) {
            if (propertyOnDelete != null) {
                properties.put(propertyOnDelete, "true");
            }
        }
        if (property != null) {
            properties.put(property, "true");
        }
    }

}
