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

import java.util.Date;
import java.util.Set;
import java.util.ArrayList;

import org.apache.tools.ant.Task;

/**
 *	This interface defines behavior required by ModificationSet.java when gathering information
 *	about the changes made to whatever source control tool that you choose.
 *
 *	@author alden almagro, ThoughtWorks, Inc. 2001
 */
public interface SourceControlElement {

   /** get the task from the parent element for logging purposes */
   public void setTask(Task t);
   
   /** get the last modified time for this set of files */
   public long getLastModified();

   /**
    * get an ArrayList of Modifications detailing all the changes between now and the last build.
    */
   public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod);

   /**
    * get a Set of email addresses.  depends on the source control tool.  StarTeam has a field for
    * email addresses, so we would return a set of full email addresses here.  SourceSafe doesn't have
    * the same functionality, so we'll just return the usernames here. (which should correspond to
    * email ids)  we'll tack on the suffix, i.e. @apache.org, in MasterBuild.java before mailing
    * results of the build.
    */
   public Set getEmails();
   
   /**
    * Use Ant task to send a log message
    */
   public void log(String message);
   
}