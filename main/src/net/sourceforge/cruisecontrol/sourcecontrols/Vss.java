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

import java.io.*;
import java.text.*;

import java.util.*;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *  This class handles all VSS-related aspects of determining the modifications
 *  since the last good build.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author Eli Tucker
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Arun Aggarwal
 */
public class Vss implements SourceControl {

	/** enable logging for this class */
	private static Logger log = Logger.getLogger(Vss.class);

	private final String VSS_TEMP_FILE = "vsstempfile.txt";
	protected SimpleDateFormat vssDateTimeFormat;

	private String ssdir;
	private String vsspath;
	private String serverPath;
	private String login;
	private String dateFormat;
	private String timeFormat;

	private Hashtable _properties = new Hashtable();
	private String _property;
	private String _propertyOnDelete;

	/**
	 * Sets default values.
	 */
	public Vss() {
		dateFormat = "MM/dd/yy";
		timeFormat = "hh:mma";
		constructVssDateTimeFormat();
	}

	/**
	 *  Set the project to get history from
	 *
	 *@param  vsspath
	 */
	public void setVsspath(String vsspath) {
		this.vsspath = "$" + vsspath;
	}

	/**
	 *  Set the path to the ss executable
	 *
	 *@param  ssdir
	 */
	public void setSsDir(String ssdir) {
		this.ssdir = ssdir;
	}

	/**
	 *  Set the path to the directory containing the srcsafe.ini file.
	 *
	 *  @param serverPath
	 */
	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}

	/**
	 *  Login for vss
	 *
	 *@param  login
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	/**
	 *  Choose a property to be set if the project has modifications if we have a
	 *  change that only requires repackaging, i.e. jsp, we don't need to recompile
	 *  everything, just rejar.
	 *
	 *@param  property
	 */
	public void setProperty(String property) {
		_property = property;
	}

	/**
	 *  Choose a property to be set if the project has deletions
	 *
	 *  @param  propertyOnDelete
	 */
	public void setPropertyOnDelete(String propertyOnDelete) {
		_propertyOnDelete = propertyOnDelete;
	}

	/**
	 * Sets the date format to use for querying VSS and processing reports.
	 *
	 * The default date format is <code>MM/dd/yy</code> .  If your computer
	 * is set to a different region, you may wish to use a format such
	 * as <code>dd/MM/yy</code> .
	 *
	 * @see java.text.SimpleDateFormat
	 */
	public void setDateFormat(String format) {
		dateFormat = format;
		constructVssDateTimeFormat();
	}


	/**
	 * Sets the time format to use for querying VSS and processing reports.
	 *
	 * The default time format is <code>hh:mma</code> .  If your computer
	 * is set to a different region, you may wish to use a format such
	 * as <code>HH:mm</code> .
	 *
	 * @see java.text.SimpleDateFormat
	 */
	public void setTimeFormat(String format) {
		timeFormat = format;
		constructVssDateTimeFormat();
	}


	public Hashtable getProperties() {
		return _properties;
	}

	public void validate() throws CruiseControlException {
		if(vsspath == null)
		throw new CruiseControlException("'vsspath' is a required attribute on Vss");
	if(login == null)
	 throw new CruiseControlException("'login' is a required attribute on Vss");
	}

	/**
	 * Calls
	 * "ss history [dir] -R -Vd[now]~[lastBuild] -Y[login] -I-N -O[tempFileName]"
	 * Results written to a file since VSS will start wrapping lines if read
	 * directly from the stream.
	 *
	 *@param  lastBuild
	 *@param  now
	 *@return List of modifications
	 */
	public List getModifications(Date lastBuild, Date now) {
		//(PENDING) extract buildHistoryCommand, execHistoryCommand
		// See CVSElement
		ArrayList modifications = new ArrayList();
		try {
			Properties systemProps = System.getProperties();
			if(serverPath != null) {
				systemProps.put("SSDIR", serverPath);
			}
			String[] env = new String[systemProps.size()];
			int index = 0;
			Iterator systemPropIterator = systemProps.keySet().iterator();
			while(systemPropIterator.hasNext()) {
				String propName = (String) systemPropIterator.next();
				env[index] = propName + "=" + systemProps.get(propName);
				index++;
			}

			log.info("Vss: getting modifications for " + vsspath);
			Process p = Runtime.getRuntime().exec(getCommandLine(lastBuild, now), env);
			p.waitFor();
			p.getInputStream().close();
			p.getOutputStream().close();
			p.getErrorStream().close();

			parseTempFile(modifications);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (_property != null && modifications.size() > 0) {
			_properties.put(_property, "true");
		}

		return modifications;
	}

	private void parseTempFile(ArrayList modifications) throws IOException {
		Level loggingLevel = log.getEffectiveLevel();
		if(Level.DEBUG.equals(loggingLevel)) {
			logVSS_TEMP_FILE();
		}

		File tempFile = new File(VSS_TEMP_FILE);
		BufferedReader reader = new BufferedReader(new FileReader(tempFile));

		parseHistoryEntries(modifications, reader);

		reader.close();
		tempFile.delete();
	}

	private void logVSS_TEMP_FILE() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				new File(VSS_TEMP_FILE)));
		String currLine = reader.readLine();
		log.debug(" ");
		while (currLine != null) {
			log.debug(VSS_TEMP_FILE+": "+currLine);
			currLine = reader.readLine();
		}
		log.debug(" ");
		reader.close();
	}

	void parseHistoryEntries(ArrayList modifications, BufferedReader reader) throws IOException {
		String currLine = reader.readLine();
		while (currLine != null) {
			if (currLine.startsWith("***** ")) {
				ArrayList vssEntry = new ArrayList();
				vssEntry.add(currLine);
				currLine = reader.readLine();
				while (currLine != null && !currLine.startsWith("***** ")) {
					vssEntry.add(currLine);
					currLine = reader.readLine();
				}
				Modification mod = handleEntry(vssEntry);
				if(mod != null) modifications.add(mod);
			} else {
				currLine = reader.readLine();
			}
		}
	}

	protected String[] getCommandLine(Date lastBuild, Date now) throws CruiseControlException {
		String execCommand = null;
		try {
			execCommand = (ssdir != null) ? new File(ssdir, "ss.exe").getCanonicalPath() : "ss.exe";
		} catch (IOException e) {
			throw new CruiseControlException(e);
		}

		String[] commandLine = new String[]{execCommand, "history", vsspath, "-R", "-Vd" +
			formatDateForVSS(now) + "~" + formatDateForVSS(lastBuild),
			"-Y" + login, "-I-N", "-O" + VSS_TEMP_FILE};

		log.debug(" ");
		for(int i=0; i<commandLine.length; i++) {
			log.debug("Vss command line arguments: "+commandLine[i]);
		}
		log.debug(" ");

		return commandLine;
	}

	/**
	 *  Format a date for vss in the format specified by the dateFormat.
	 *  By default, this is in the form <code>12/21/2000;8:14A</code> (vss doesn't
	 *  like the m in am or pm).  This format can be changed with <code>setDateFormat()</code>
	 *
	 *  @param d Date to format.
	 *  @return String of date in format that VSS requires.
	 *  @see #setDateFormat
	 */
	private String formatDateForVSS(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat(this.dateFormat + ";" + this.timeFormat);
		String vssFormattedDate = sdf.format(d);
		if (this.timeFormat.endsWith("a")) {
			return vssFormattedDate.substring(0, vssFormattedDate.length() - 1);
		} else {
			return vssFormattedDate;
		}
	}

	// ***** the rest of this is just parsing the vss output *****

	//(PENDING) Extract class VSSEntryParser
	/**
	 *  Parse individual VSS history entry
	 *
	 *@param  entry
	 */
	protected Modification handleEntry(List entry) {
		log.debug("VSS history entry BEGIN");
		for(Iterator i = entry.iterator(); i.hasNext(); log.debug("entry: "+i.next())) {}
		log.debug("VSS history entry END");

		try {
			// Ignore unusual labels of directories which cause parsing errors that
			// look like this:
			//
			// *****  built  *****
			// Version 4
			// Label: "autobuild_test"
			// User: Etucker      Date:  6/26/01   Time: 11:53a
			// Labeled
			if ((entry.size() > 4) &&
					(((String) entry.get(4)).startsWith("Labeled"))) {
				log.debug("this is a label; ignoring this entry");
				return null;
			}

			// but need to adjust for cases where Label: line exists
			//
			// *****  DateChooser.java  *****
			// Version 8
			// Label: "Completely new version!"
			// User: Arass        Date: 10/21/02   Time: 12:48p
			// Checked in $/code/development/src/org/ets/cbtidg/common/gui
			// Comment: This is where I add a completely new, but alot nicer version of the date chooser.
			// Label comment:

			int nameAndDateIndex = 2;
			String nameAndDateLine = (String) entry.get(nameAndDateIndex);
			if (nameAndDateLine.startsWith("Label:")) {
				nameAndDateIndex++;
				nameAndDateLine = (String) entry.get(nameAndDateIndex);
				log.debug("adjusting for the line that starts with Label");
			}

			Modification modification = new Modification();
			modification.userName = parseUser(nameAndDateLine);
			modification.modifiedTime = parseDate(nameAndDateLine);

			String folderLine = (String) entry.get(0);
			int fileIndex = nameAndDateIndex+1;
			String fileLine = (String) entry.get(fileIndex);
			if (fileLine.startsWith("Checked in")) {
				modification.type = "checkin";
				log.debug("this is a checkin");
				int commentIndex = fileIndex+1;
				modification.comment = parseComment(entry, commentIndex);
				modification.fileName = folderLine.substring(7, folderLine.indexOf("  *"));
				modification.folderName = fileLine.substring(12);
			} else if (fileLine.endsWith("Created")) {
				modification.type = "create";
				log.debug("this folder was created");
			} else {
				modification.folderName = folderLine.substring(7, folderLine.indexOf("  *"));
				int lastSpace = fileLine.lastIndexOf(" ");
				if ( lastSpace != -1 ) {
					modification.fileName = fileLine.substring(0, lastSpace);
				} else {
					modification.fileName = fileLine;
				}

				if (fileLine.endsWith("added")) {
					modification.type = "add";
					log.debug("this file was added");
				} else if (fileLine.endsWith("deleted")) {
					modification.type = "delete";
					log.debug("this file was deleted");
					addPropertyOnDelete();
                } else if (fileLine.endsWith("destroyed")) {
                    modification.type = "destroy";
                    log.debug("this file was destroyed");
                    addPropertyOnDelete();
				} else if (fileLine.endsWith("recovered")) {
					modification.type = "recover";
					log.debug("this file was recovered");
				} else if (fileLine.endsWith("shared")) {
					modification.type = "branch";
					log.debug("this file was branched");
				} else if (fileLine.indexOf(" renamed to ") != -1){
					modification.fileName = fileLine;
					modification.type = "rename";
					log.debug("this file was renamed");
					addPropertyOnDelete();
				}
				else {
					log.debug("action for this vss entry ("+fileLine+") is unknown");
				}
			}

			if (_property != null) {
				_properties.put(_property,  "true");
				log.debug("setting property " + _property + " to be true");
			}

			log.debug(" ");

			return modification;

		} catch (RuntimeException e) {
			log.fatal("RuntimeException handling VSS entry:");
			for (int i=0; i < entry.size(); i++) {
				log.fatal(entry.get(i));
			}
			throw e;
		}
	}

	private void addPropertyOnDelete() {
		if (_propertyOnDelete != null) {
			_properties.put(_propertyOnDelete, "true");
			log.debug("setting property " + _propertyOnDelete + " to be true");
		}
	}

	/**
	 *  Parse comment from VSS history (could be multi-line)
	 *
	 *@param  commentList
	 *@return
	 */
	private String parseComment(List commentList, int commentIndex) {
		StringBuffer comment = new StringBuffer();
		comment.append(((String) commentList.get(commentIndex)) + " ");
		for (int i = commentIndex+1; i < commentList.size(); i++) {
			comment.append(((String) commentList.get(i)) + " ");
		}

		return comment.toString().trim();
	}

	/**
	 * Parse date/time from VSS file history
	 *
	 * The nameAndDateLine will look like <br>
	 * <code>User: Etucker      Date:  6/26/01   Time: 11:53a</code><br>
	 * Sometimes also this<br>
	 * <code>User: Aaggarwa     Date:  6/29/:1   Time:  3:40p</code><br>
	 * Note the ":" instead of a "0"
	 *
	 *@param  nameAndDateLine
	 *@return Date in form "'Date: 'MM/dd/yy   'Time:  'hh:mma", or a different form based on dateFormat
	 *@see #setDateFormat
	 */
	public Date parseDate(String nameAndDateLine) {
		String dateAndTime =
				nameAndDateLine.substring(nameAndDateLine.indexOf("Date: "));

		int indexOfColon = dateAndTime.indexOf("/:");
		if(indexOfColon != -1) {
			dateAndTime = dateAndTime.substring(0, indexOfColon)
							 + dateAndTime.substring(indexOfColon, indexOfColon + 2).replace(':','0')
							 + dateAndTime.substring(indexOfColon + 2);
		}

		try {
			Date lastModifiedDate = null;
			if (this.timeFormat.endsWith("a")) {
				lastModifiedDate = this.vssDateTimeFormat.parse(dateAndTime.trim() + "m");
			} else {
				lastModifiedDate = this.vssDateTimeFormat.parse(dateAndTime.trim());
			}


			return lastModifiedDate;
		} catch (ParseException pe) {
			pe.printStackTrace();
			return null;
		}
	}

	/**
	 *  Parse username from VSS file history
	 *
	 *@param  userLine
	 *@return the user name who made the modification
	 */
	public String parseUser(String userLine) {
		final int USER_INDEX = "User: ".length();
		String userName = userLine.substring(USER_INDEX, userLine.indexOf("Date: ") - 1).trim();

		return userName;
	}

	/**
	 * Constructs the vssDateTimeFormat based on the dateFormat for this element.
	 * @see #setDateFormat
	 */
	private void constructVssDateTimeFormat() {
		vssDateTimeFormat = new SimpleDateFormat("'Date: '" + this.dateFormat + "   'Time: '" + this.timeFormat);
	}
}

