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

import java.util.*;
import org.apache.tools.ant.*;

/**
 * This abstract class defines behavior required by ModificationSet.java when
 * gathering information about the changes made to whatever source control tool
 * that you choose.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */

//(PENDING) pull up buildCommandLine
public abstract class SourceControlElement {

	private Task _task;

	/**
	 *  Get a Set of email addresses. Depends on the source control tool. StarTeam
	 *  has a field for email addresses, so we would return a set of full email
	 *  addresses here. SourceSafe doesn't have the same functionality, so we'll
	 *  just return the usernames here (which should correspond to email IDs).
	 *  We'll tack on the suffix, e.g., "@apache.org" in MasterBuild.java before 
     *  mailing results of the build.
	 *
	 *@return
	 *
	 */
	public abstract Set getEmails();

	/**
	 *  get an ArrayList of Modifications detailing all the changes between now and
	 *  the last build.
	 *
	 *@param  lastBuild
	 *@param  now
	 *@param  quietPeriod
	 *@return
	 */
	public abstract ArrayList getHistory(Date lastBuild, Date now, long quietPeriod);

	/**
	 *  get the last modified time for this set of files
	 *
	 *@return
	 */
	public abstract long getLastModified();    

    // Logging stuff 
    // (PENDING) Extract this to class that can be delegated to and change 
    // SourceControlElement to an interface
	/**
	 *  Sets Ant task which is used for logging. Also sets the task name to be
	 *  equivalent to the class name of the particular source control element
	 *  implementation.
	 *
	 *@param  task
	 */
	public void setAntTask(Task task) {
		_task = task;
		String classname = this.getClass().getName();
		_task.setTaskName(classname.substring(classname.lastIndexOf(".") + 1));
	}

	protected Task getAntTask() {
		return _task;
	}

	/**
	 *  Use Ant task to send a log message. Sends nothing if task is null.
	 *
	 *@param  message
	 */
	protected void log(String message) {
		log(message, Project.MSG_INFO);
	}

	/**
	 *  Use Ant task to send a log message. Sends nothing if task is null. The
	 *  msgLevel param should be on of the org.apache.tools.ant.Project static
	 *  variables: MSG_DEBUG, MSG_INFO, MSG_ERR, MSG_VERBOSE, MSG_WARN
	 *
	 *@param  message
	 *@param  msgLevel
	 */
	protected void log(String message, int msgLevel) {
		if (_task != null) {
			_task.log(message, msgLevel);
		}
	}

}
