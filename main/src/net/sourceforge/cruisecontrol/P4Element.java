// DON'T TOUCH THIS SECTION
//
// COPYRIGHT isMobile.com AB 2000
//
// The copyright of the computer program herein is the property of
// isMobile.com AB, Sweden. The program may be used and/or copied
// only with the written permission from isMobile.com AB or in the
// accordance with the terms and conditions stipulated in the
// agreement/contract under which the program has been supplied.
//
// $Id$
//
// END DON'T TOUCH

package net.sourceforge.cruisecontrol;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.io.*;

import net.sourceforge.cruisecontrol.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;

import org.apache.tools.ant.taskdefs.optional.perforce.*;

import org.apache.oro.text.perl.*;

/**
 *  This class implements the SourceControlElement methods for a P4 depot. The
 *  call to CVS is assumed to work without any setup. This implies that if the
 *  authentication type is pserver the call to cvs login should be done prior to
 *  calling this class. <p>
 *
 *  P4Element depends on the optional P4 package delivered with Ant v1.3. But
 *  since it probably doesn't make much sense using the P4Element without other
 *  P4 support it shouldn't be a problem. <p>
 *
 *  P4Element sets the property ${p4element.change} with the current changelist
 *  number. This should then be passed into p4sync or other p4 commands.
 *
 * @author     niclas.olofsson@ismobile.com
 * @created    den 23 april 2001
 * @version    0.0
 */
public class P4Element implements SourceControlElement {

	private Set _emailNames = new HashSet();
	private Date _lastModified;
	private org.apache.tools.ant.Task _task;
	private final static java.text.SimpleDateFormat p4Date = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	//P4 runtime directives

	private String _P4Port;
	private String _P4Client;
	private String _P4User;
	private String _P4View;
	private int _P4lastChange;

	protected void execP4Command(String command) throws BuildException {
		execP4Command(command, null);
	}

	/**
	 *  Execute P4 commands. Giv a P4Handler to process the events.
	 *
	 * @param  command             The command to run
	 * @param  handler             A P4Handler to process any input and output
	 * @exception  BuildException  Description of Exception.
	 */
	protected void execP4Command(String command, P4Handler handler) throws BuildException {

		final Perl5Util util = new Perl5Util();

		try {

			String cCmd = "";
			if (_P4Client != null) {
				cCmd = "-c" + _P4Client;
			}
			String pCmd = "";
			if (_P4Port != null) {
				pCmd = "-p" + _P4Port;
			}
			String uCmd = "";
			if (_P4User != null) {
				uCmd = "-u" + _P4User;
			}

			Commandline commandline = new Commandline();
			commandline.setExecutable("p4");

			commandline.createArgument().setValue(pCmd);
			commandline.createArgument().setValue(uCmd);
			commandline.createArgument().setValue(cCmd);
			commandline.createArgument().setLine(command);

			log("Execing " + commandline);

			// Just a simple handler to record the events in question.
			//handler = null;
			if (handler == null) {
				handler =
					new P4HandlerAdapter() {
						public void process(String line) {
							if (util.match("/^exit/", line)) {
								return;
							}
							if (util.match("/error:/", line) && !util.match("/up-to-date/", line)) {
								throw new BuildException(line);
							}
							log(util.substitute("s/^.*: //", line));
						}
					};

			}

			Execute exe = new Execute(handler, null);

			if (this._task != null) {
				exe.setAntRun(this._task.getProject());
			}

			exe.setCommandline(commandline.getCommandline());

			try {
				exe.execute();
			} catch (IOException e) {
				throw new BuildException(e);
			} finally {
				try {
					handler.stop();
				} catch (Exception e) {
				}
			}

		} catch (Exception e) {
			throw new BuildException("Problem exec'ing P4 command: " + e.getMessage());
		}
	}

	//Setters called by Ant
	public void setPort(String P4Port) {
		this._P4Port = P4Port;
	}

	public void setClient(String P4Client) {
		this._P4Client = P4Client;
	}

	public void setUser(String P4User) {
		this._P4User = P4User;
	}

	public void setView(String P4View) {
		this._P4View = P4View;
	}

	/**
	 *  Constructor for P4Element. Doesn't do much.
	 */
	public P4Element() {
	}

	/**
	 *  Returns a Set of email addresses. P4 doesn't track actual email addresses,
	 *  so we'll just return the usernames here, which may correspond to email ids.
	 *  We'll tack on the suffix, i.e.
	 *
	 * @return         Set of author names; maybe empty, never null.
	 * @apache.org,    in MasterBuild.java before mailing results of the build.
	 */
	public Set getEmails() {
		if (_emailNames == null) {
			_emailNames = new HashSet();
		}
		return _emailNames;
	}

	/**
	 *  Allows the caller to set the task, which will be used for logging purposes.
	 *
	 * @param  task  Task to use.
	 */
	public void setTask(org.apache.tools.ant.Task task) {
		this._task = task;
	}

	/**
	 *  Gets the last modified time for this set of files queried in the
	 *  getHistory() method.
	 *
	 * @return    Latest revision time.
	 */
	public long getLastModified() {
		if (_lastModified == null) {
			return 0;
		}
		return _lastModified.getTime();
	}

	public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {

		ArrayList mods = null;
		final Perl5Util util = new Perl5Util();

		//Init last modified to last build date.
		_lastModified = lastBuild;

		// next line is a trick to get the variable usable within the adhoc handler.
		final StringBuffer sbChangenumber = new StringBuffer();
		final StringBuffer sbModifiedTime = new StringBuffer();
		//if this._P4lastChange != p4 changes -m 1 -s submitted <depotpath>
		execP4Command("changes -m 1 -s submitted " + _P4View,
			new P4HandlerAdapter() {
				public void process(String line) {
					if (util.match("/Change/", line)) {
						//Parse out the change number
						sbChangenumber.append(util.substitute("s/Change\\s([0-9]*?)\\son\\s.*/$1/gx", line));
						log("Latest change is " + sbChangenumber, Project.MSG_INFO);
					} else if (util.match("/error/", line)) {
						throw new BuildException("Perforce Error, check client settings and/or server");
					}
				}
			});

		// and collect info for this change

		final StringBuffer sbDescription = new StringBuffer();
		execP4Command("describe -s " + sbChangenumber.toString(),
			new P4HandlerAdapter() {
				public void process(String line) {
					if (util.match("/error/", line)) {
						throw new BuildException("Perforce Error, check client settings and/or server");
					}
					sbDescription.append(line);
					sbDescription.append("\n");
				}
			});
		// now, lets parse the data out
		String userName = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*/$2/s", sbDescription.toString());
		String sModifiedTime = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*/$3/s", sbDescription.toString());
		String comment = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*/$4/s", sbDescription.toString());
		comment = util.substitute("s/\\t//g", comment);

		java.util.Date modifiedTime;
		try {
			modifiedTime = p4Date.parse(sModifiedTime);
		} catch (Exception ex) {
			log("Wrong date format exception caught. Using lastModified date from project instead.");
			modifiedTime = _lastModified;
		}

		if (modifiedTime.compareTo(lastBuild) > 0) {
			// if it differs, we build,
			_P4lastChange = Integer.parseInt(sbChangenumber.toString());
			this._task.getProject().setProperty("p4element.change", sbChangenumber.toString());

			// the rest should be a list of the files affected and the resp action
			String affectedFiles = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*?\\n\\n(.*)\\n\\n/$5/s", sbDescription.toString());
			java.util.Vector files = new java.util.Vector();
			util.split(files, "/\\n/s", affectedFiles);
			java.util.Iterator iter = files.iterator();
			while (iter.hasNext()) {
				String file = (String) iter.next();
				String folderName = util.substitute("s/\\.\\.\\.\\s(\\/\\/.*\\/)(.*?)\\s(.*)/$1/s", file);
				String fileName = util.substitute("s/\\.\\.\\.\\s(\\/\\/.*\\/)(.*?)\\s(.*)/$2/s", file);
				String action = util.substitute("s/\\.\\.\\.\\s(\\/\\/.*\\/)(.*?)\\s(.*)/$3/s", file);
				Modification mod = new Modification();
				mod.comment = comment;
				mod.fileName = fileName;
				mod.folderName = folderName;
				mod.modifiedTime = modifiedTime;
				mod.type = action;
				mod.userName = userName;

				if (mods == null) {
					mods = new ArrayList();
				}
				mods.add(mod);
			}
		} else {
			// otherwise we don't build
		}

		if (mods == null) {
			mods = new ArrayList();
		}

		return mods;
	}

	/**
	 *  Logs the message if a task has been set.
	 *
	 * @param  message  message to log.
	 */
	private void log(String message) {
		if (_task != null) {
			this._task.getProject().log("[p4element] " + message);
		}
	}

	/**
	 *  Logs the message if a task has been set.
	 *
	 * @param  message    message to log.
	 * @param  msg_level  Description of Parameter.
	 */
	private void log(String message, int msg_level) {
		if (_task != null) {
			this._task.getProject().log("[p4element] " + message, msg_level);
		}
	}

}// P4Element
