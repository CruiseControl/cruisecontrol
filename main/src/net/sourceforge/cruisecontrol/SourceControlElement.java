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

import java.util.*;
import org.apache.tools.ant.*;

/**
 * This abstract class defines behavior required by ModificationSet.java when 
 * gathering information about the changes made to whatever source control tool 
 * that you choose.
 *
 * @author alden almagro, ThoughtWorks, Inc. 2001, jchyip
 */

//(PENDING) move all source control elements to net.sourceforge.cruisecontrol.element
public abstract class SourceControlElement {

    private Task _task;

   /**
    * The String prepended to log messages from the source control element.  For
    * example, CVSElement should implement this as return "[cvselement]"; 
    *
    * @return prefix for log messages
    */
   protected abstract String logPrefix();
    
   /**
    * get a Set of email addresses.  depends on the source control tool.  StarTeam has a field for
    * email addresses, so we would return a set of full email addresses here.  SourceSafe doesn't have
    * the same functionality, so we'll just return the usernames here. (which should correspond to
    * email ids)  we'll tack on the suffix, i.e. @apache.org, in MasterBuild.java before mailing
    * results of the build.
    */
   public abstract Set getEmails();
    
   /**
    * get an ArrayList of Modifications detailing all the changes between now and the last build.
    */
   public abstract ArrayList getHistory(Date lastBuild, Date now, long quietPeriod);

   /** get the last modified time for this set of files */
   public abstract long getLastModified();

   /**
    * Use Ant task to send a log message.  Sends nothing if task is null.
    */
   protected void log(String message) {
        log(message, Project.MSG_INFO);
   }
   
   /**
    * Use Ant task to send a log message.  Sends nothing if task is null.
    * The msgLevel param should be on of the org.apache.tools.ant.Project static
    * variables: MSG_DEBUG, MSG_INFO, MSG_ERROR, MSG_VERBOSE, MSG_WARN
    */
   protected void log(String message, int msgLevel) {
        if (_task != null) {
            _task.log(logPrefix() + " " + message, msgLevel);
        }
   }
   
   //(PENDING) rename to get/set AntTask
   public Task getTask() {
       return _task;
   }
   
   /** 
    * Get the task from the parent element for logging purposes 
    */
   public void setTask(Task task) {
       _task = task;
   }
   
}