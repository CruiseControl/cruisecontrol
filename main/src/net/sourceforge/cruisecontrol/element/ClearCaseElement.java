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
 *  This class implements the SourceControlElement methods for a Clear Case
 *  repository.
 *
 *@author  Thomas Leseney, jchyip
 *@created  June 11, 2001
 *@version  May 25, 2001
 */

public class ClearCaseElement extends SourceControlElement {

	/**
	 *  Set of the authors that modified files. With Clear Case, it correspond the
	 *  user names.
	 */
	private Set _emailNames = new HashSet();

	/**
	 *  This date indicates the most recent modification time.
	 */
	private Date _lastModified;

	/**
	 *  The path of the clear case view
	 */
	private String _viewPath;

	/**
	 *  The branch to check for modifications
	 */
	private String _branch = null;
	private boolean _recursive = true;

	/**
	 *  Date format required by commands passed to Clear Case
	 */
	final static SimpleDateFormat IN_DATE_FORMAT = new SimpleDateFormat("dd-MMMM-yyyy.HH:mm:ss");

	/**
	 *  Date format returned in the output of Clear Case commands.
	 */
	final static SimpleDateFormat OUT_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd.HHmmss");

	/**
	 *  Sets the local working copy to use when making queries.
	 *
	 *@param  path
	 */
	public void setViewpath(String path) {
		_viewPath = getAntTask().getProject().resolveFile(path).getAbsolutePath();
	}

	/**
	 *  Sets the branch that we're concerned about checking files into.
	 *
	 *@param  branch
	 */
	public void setBranch(String branch) {
		_branch = branch;
	}

	public void setRecursive(boolean b) {
		_recursive = b;
	}

	/**
	 *  Returns a Set of email addresses. since Clear Case doesn't track actual
	 *  email addresse, we just return the usernames, which may correspond to
	 *  emails ids.
	 *
	 *@return
	 */
	public Set getEmails() {
		return _emailNames;
	}

	/**
	 *  Gets the last modified time for the set of files queried in the {@link
	 *  #getHistory} method.
	 *
	 *@return  the lastest revision time.
	 */
	public long getLastModified() {
		if (_lastModified == null) {
			return 0;
		}
		return _lastModified.getTime();
	}

	/**
	 *  Returns an {@link java.util.ArrayList ArrayList} of {@link Modification}
	 *  detailing all the changes between now and the last build.
	 *
	 *@param  lastBuild the last build time
	 *@param  now time now, or time to check, NOT USED
	 *@param  quietPeriod NOT USED
	 *@return  the list of modifications, an empty (not null) list if no
	 *      modifications.
	 */
	public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {
		ArrayList modifications = null;
		String lastBuildDate = IN_DATE_FORMAT.format(lastBuild);
		/*
		 *  let's try a different clearcase command--this one just takes waaaaaaaay too long.
		 *  String command = "cleartool find " + _viewPath +
		 *  " -type f -exec \"cleartool lshistory" +
		 *  " -since " + lastBuildDate;
		 *  if(_branch != null)
		 *  command += " -branch " + _branch;
		 *  command += " -nco" + // exclude check out events
		 *  " -fmt \\\" %u;%Nd;%n;%o \\n \\\" \\\"%CLEARCASE_XPN%\\\" \"";
		 */
		String command = "cleartool lshistory";
		if (_branch != null) {
			command += " -branch " + _branch;
		}
		if (_recursive == true) {
			command += " -r ";
		}
		command += " -nco -since " + lastBuildDate;
		command += " -fmt \"%u;%Nd;%n;%o\\n\" " + _viewPath;

		log("Command to execute : " + command);
		try {
			Process p = Runtime.getRuntime().exec(command);

			StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
			new Thread(errorPumper).start();

			InputStream input = p.getInputStream();
			modifications = parseStream(input);

			p.waitFor();
		}
		catch (Exception e) {
			log("Error in executing the Clear Case command : " + e);
			e.printStackTrace();
		}
		if (modifications == null) {
			modifications = new ArrayList();
		}
		return modifications;
	}

	/**
	 *  Parses the input stream to construct the modifications list.
	 *
	 *@param  input the stream to parse
	 *@return  a list of modification elements
	 *@exception  IOException
	 */
	private ArrayList parseStream(InputStream input)
			 throws IOException {
		ArrayList modifications = new ArrayList();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		String line;

		while ((line = reader.readLine()) != null) {
			Modification mod = parseEntry(line);
			if (mod != null) {
				modifications.add(mod);
			}
		}
		return modifications;
	}

	/**
	 *  Parses a single line from the reader. Each line contains a signe revision
	 *  with the format : <br>
	 *  username;date_of_revision;element_name;operation_type <br>
	 *
	 *
	 *@param  line the line to parse
	 *@return  a modification element corresponding to the given line
	 */
	private Modification parseEntry(String line) {
		System.out.println("parsing entry: " + line);
		StringTokenizer st = new StringTokenizer(line, ";");
		if (st.countTokens() != 4) {
			return null;
		}
		String username = st.nextToken().trim();
		String timeStamp = st.nextToken().trim();
		String elementName = st.nextToken().trim();
		String operationType = st.nextToken().trim();

		/*
		 *  a branch event shouldn't trigger a build
		 */
		if (operationType.equals("mkbranch")) {
			return null;
		}

		Modification mod = new Modification();

		mod.userName = username;
		_emailNames.add(mod.userName);

		elementName = elementName.substring(elementName.indexOf(":\\") + 1);
		String fileName = elementName.substring(0, elementName.indexOf("@@"));

		mod.fileName = fileName.substring(fileName.lastIndexOf("\\"));
		mod.folderName = fileName.substring(0, fileName.lastIndexOf("\\"));

		try {
			mod.modifiedTime = OUT_DATE_FORMAT.parse(timeStamp);
			updateLastModified(mod.modifiedTime);
		}
		catch (ParseException e) {
			mod.modifiedTime = null;
		}

		mod.type = operationType;

		mod.comment = "";

		return mod;
	}

	/**
	 *  Updates the lastModified date if necessary (new date is after the current
	 *  lastModified date).
	 *
	 *@param  newPossible the new possible lastModified date
	 */
	private void updateLastModified(Date newPossible) {
		if (_lastModified == null || _lastModified.before(newPossible)) {
			_lastModified = newPossible;
		}
	}

	/**
	 *  Inner class to pump the error stream during Process's runtime. Copied from
	 *  the Ant built-in task.
	 *
	 *@author  jcyip
	 *@created  June 11, 2001
	 */
	class StreamPumper implements Runnable {

		private InputStream _in;
		private final static int SIZE = 128;
		private final static int SLEEP = 5;

		public StreamPumper(InputStream in) {
			_in = in;
		}

		public void run() {
			final byte[] buf = new byte[SIZE];
			int length;

			try {
				while ((length = _in.read(buf)) > 0) {
					System.err.write(buf, 0, length);
					try {
						Thread.sleep(SLEEP);
					}
					catch (InterruptedException e) {
					}
				}
			}
			catch (IOException e) {
			}
		}
	}
}
