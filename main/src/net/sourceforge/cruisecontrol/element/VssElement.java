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

import java.io.*;
import java.text.*;

import java.util.*;
import net.sourceforge.cruisecontrol.Modification;

import org.apache.tools.ant.Task;

/**
 *  This class handles all vss-related aspects of determining the modifications
 *  since the last good build.
 *
 * @author Alden Almagro, ThoughtWorks, Inc. 2001
 * @author Eli Tucker
 * @author Jason Yip, jcyip@thoughtworks.com
 */
public class VssElement extends SourceControlElement {

    private final String VSS_TEMP_FILE = "vsstempfile.txt";
    
	private String _ssDir;
	private String _login;
	private String _property;
	private String _propertyOnDelete;

	private long _lastModified;

	private ArrayList _modifications = new ArrayList();
	private Set _emails = new HashSet();

	private SimpleDateFormat _vssOutFormat = new SimpleDateFormat("'Date:'MM/dd/yy   'Time: 'hh:mma");

	/**
	 *  set the project to get history
	 *
	 *@param  s
	 */
	public void setSsDir(String s) {
		_ssDir = "$" + s;
	}

	/**
	 *  login for vss
	 *
	 *@param  s
	 */
	public void setLogin(String s) {
		_login = s;
	}

	/**
	 *  choose a property to be set if the project has modifications if we have a
	 *  change that only requires repackaging, i.e. jsp, we don't need to recompile
	 *  everything, just rejar.
	 *
	 *@param  s
	 */
	public void setProperty(String s) {
		_property = s;
	}

	public void setPropertyondelete(String s) {
		_propertyOnDelete = s;
	}

	/**
	 *  for parent modificationset to find out the time of last modification for
	 *  this project
	 *
	 *@return
	 */
	public long getLastModified() {
		return _lastModified;
	}

	/**
	 *  returns a Set of usernames that made any modification since the last good
	 *  build.
	 *
	 *@return
	 */
	public Set getEmails() {
		return _emails;
	}

	/**
	 *  returns an ArrayList of modifications to this project since the last good
	 *  build.
	 *
	 *@return
	 */
	public ArrayList getModifications() {
		return _modifications;
	}

	/**
	 *  Do the work... I'm writing to a file since VSS will start wrapping lines 
     * if I read directly from the stream.
	 *
	 *@param  lastBuild
	 *@param  now
	 *@param  quietPeriod
	 *@return
	 */
	public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {
        //(PENDING) buildHistoryCommand, execHistoryCommand
		//call vss, write output to intermediate file
		try {
			String[] cmdArray = {"ss.exe", "history", _ssDir, "-R", "-Vd" +
					formatDateForVSS(now) + "~" + formatDateForVSS(lastBuild), 
                    "-Y" + _login, "-I-N", "-O" + VSS_TEMP_FILE};
			Process p = Runtime.getRuntime().exec(cmdArray);
			p.waitFor();

			BufferedReader br = new BufferedReader(new FileReader(
             new File(VSS_TEMP_FILE)));

			String s = br.readLine();
			while (s != null) {
				if (s.startsWith("***** ")) {
					ArrayList a = new ArrayList();
					a.add(s);
					s = br.readLine();
					while (s != null && !s.startsWith("***** ")) {
						a.add(s);
						s = br.readLine();
					}
					handleEntry(a);
				}
				else {
					s = br.readLine();
				}
			}

			br.close();
			(new File(VSS_TEMP_FILE)).delete();

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		if (_modifications.size() > 0) {
			getAntTask().getProject().setProperty(_property, "true");
		}

		return _modifications;
	}

	/**
	 *  format a date for vss in 12/21/2000;8:14A format (vss doesn't like the m in
	 *  am or pm)
	 *
	 *@param  d
	 *@return
	 */
	private String formatDateForVSS(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy;hh:mma");
		String vssFormattedDate = sdf.format(d);
		return vssFormattedDate.substring(0, vssFormattedDate.length() - 1);
	}

	// ***** the rest of this is just parsing the vss output *****

	/**
	 *  parse individual vss history entry
	 *
	 *@param  historyEntry
	 */
	private void handleEntry(ArrayList historyEntry) {
		Modification mod = new Modification();
		mod.userName = parseUser(historyEntry);
		mod.modifiedTime = parseDate(historyEntry);

		if (((String) historyEntry.get(3)).startsWith("Checked in")) {
			mod.type = "checkin";
			mod.comment = parseComment(historyEntry);
			mod.fileName = ((String) historyEntry.get(0)).substring(7, ((String) historyEntry.get(0)).indexOf("  *"));
			mod.folderName = ((String) historyEntry.get(3)).substring(12);
		} else {
			String folderLine = (String) historyEntry.get(0);
			String fileLine = (String) historyEntry.get(3);
			mod.folderName = folderLine.substring(7, folderLine.indexOf("  *"));
			mod.fileName = fileLine.substring(0, fileLine.lastIndexOf(" "));

			if (fileLine.endsWith("Created")) {
                mod.type = "create";
            } else if (fileLine.endsWith("added")) {
				mod.type = "add";
			} else if (fileLine.endsWith("deleted")) {
				mod.type = "delete";
			} else if (fileLine.endsWith("recovered")) {
				mod.type = "recover";
			} else if (fileLine.endsWith("shared")) {
                mod.type = "branch";
            } else {
                mod.type = "unknown";
            }
		}

		if ("delete".equals(mod.type)) {
			getAntTask().getProject().setProperty(_propertyOnDelete, "true");
		}

		getAntTask().getProject().setProperty(_property, "true");
		_modifications.add(mod);
		logModification(mod);
	}

	/**
	 *  parse comment from vss history (could be multiline)
	 *
	 *@param  a
	 *@return
	 */
	private String parseComment(ArrayList a) {
		StringBuffer comment = new StringBuffer();
		comment.append(((String) a.get(4)) + " ");
		for (int i = 5; i < a.size(); i++) {
			comment.append(((String) a.get(i)) + " ");
		}

		return comment.toString().trim();
	}

	/**
	 *  parse date/time from vss file history
	 *
	 *@param  a
	 *@return
	 */
	private Date parseDate(ArrayList a) {
		String s = (String) a.get(2);

		try {
			Date lastModifiedDate = _vssOutFormat.parse(s.substring(16).trim() + "m");
			if (lastModifiedDate.getTime() < _lastModified) {
				_lastModified = lastModifiedDate.getTime();
			}
			return lastModifiedDate;
		}
		catch (ParseException pe) {
			pe.printStackTrace();
			return null;
		}
	}

	/**
	 *  parse username from vss file history
	 *
	 *@param  a
	 *@return
	 */
	private String parseUser(ArrayList a) {
		String userLine = (String) a.get(2);
		String userName = userLine.substring(6, 16).trim();
		_emails.add(userName);

		return userName;
	}

	/**
	 *  pretty logging
	 *
	 *@param  mod
	 */
	private void logModification(Modification mod) {
		log("Type: " + mod.type + " " + mod.fileName);
		log("User: " + mod.userName + " Date: " + mod.modifiedTime);
		log("");
	}

}
