package net.sourceforge.cruisecontrol.element;

import java.util.*;
import org.apache.tools.ant.*;

/**
 *  This abstract class defines behavior required by ModificationSet.java when
 *  gathering information about the changes made to whatever source control tool
 *  that you choose.
 *
 *@author  alden almagro, ThoughtWorks, Inc. 2001,
 *@author  Jason Yip, jcyip@thoughtworks.com
 *@created  June 11, 2001
 */

//(PENDING) move all source control elements to net.sourceforge.cruisecontrol.element
//(PENDING) pull up buildCommandLine
public abstract class SourceControlElement {

	private Task _task;

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

	//(PENDING) pull these up too

	/**
	 *  Get a Set of email addresses. Depends on the source control tool. StarTeam
	 *  has a field for email addresses, so we would return a set of full email
	 *  addresses here. SourceSafe doesn't have the same functionality, so we'll
	 *  just return the usernames here (which should correspond to email IDs).
	 *  We'll tack on the suffix, e.g.,
	 *
	 *@return
	 *@apache.org,  in MasterBuild.java before mailing results of the build.
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
	 *  variables: MSG_DEBUG, MSG_INFO, MSG_ERROR, MSG_VERBOSE, MSG_WARN
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
