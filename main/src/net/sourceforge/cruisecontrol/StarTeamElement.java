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
    private org.apache.tools.ant.Task task;
    private String url;
    private String username;
    private String password;



   /**
    *	set the parent task for logging purposes
    */
   public void setTask(org.apache.tools.ant.Task t) {
      this.task = t;
   }


   /**
    *
    */
   public long getLastModified() {
      return mostRecent;
   }

   /**
    *
    */
   public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {
		View view = StarTeamFinder.openView(this.username + ":" + this.password + "@" + this.url);
		Server server = view.getServer();
		View snapshot = new View(view, ViewConfiguration.createFromTime(new OLEDate(now.getTime())));

   		this.folder = StarTeamFinder.findFolder(snapshot.getRootFolder(), this.folderName);

       startTime = lastBuild.getTime();

	   visit(folder);

       //need to check the mostRecent and make sure it's within the quiet period...

       this.task.getProject().log(modifications.size() + " modifications in " + folderName);
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
    	UserAccount account = revision.getServer().getAdministration().findUserAccount(revision.getModifiedBy());
		if (account.getName().equals("BuildMaster"))
			return;

		try {
			revision.checkout(Item.LockType.UNCHANGED, true, true, true);
		} catch (java.io.IOException e) {}

		emailAddresses.add(account.getEmailAddress());

		Modification mod = new Modification();

		mod.type = getModificationType(revision);
		mod.fileName = revision.getName();
		mod.folderName = revision.getParentFolder().getFolderHierarchy();
		mod.modifiedTime = revision.getModifiedTime().createDate();
		mod.userName = account.getName();
		mod.comment = revision.getComment();

		modifications.add(mod);

    	this.task.getProject().log("File: " + mod.fileName);

    	this.task.getProject().log("userName: " + mod.userName + " Date: " + mod.modifiedTime);
		if (revision.getModifiedTime().getLongValue() > mostRecent) {
			mostRecent = revision.getModifiedTime().getLongValue();
		}
   }

   private void visit(Folder folder) {
      try {
         Thread.sleep(1000);
      } catch(InterruptedException exc)
      {}
      folder.populateNow(folder.getServer().getTypeNames().FILE, new String[] {}, 0);
      Item[] files = folder.getItems("File");
      for (int i = 0; i < files.length; i++) {
         File file = (File) files[i];
         visit(file);
      }

      Folder[] folders = folder.getSubFolders();
      for (int i = 0; i < folders.length; i++) {
         visit(folders[i]);
      }
   }

   private void visit(File file) {
      if (file.getModifiedTime().getLongValue() < startTime)
         return;

      Item[] history = file.getHistory();
      for (int i = 0; i < history.length; i++) {
         File revision = (File) history[i];

         if (revision.getModifiedTime().getLongValue() > startTime) {
            addRevision(revision);
         } else {
            return;
         }
      }
   }

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
					return "unknownfilestatus";
			}
		} catch (java.io.IOException e) { return "";}
	}



}