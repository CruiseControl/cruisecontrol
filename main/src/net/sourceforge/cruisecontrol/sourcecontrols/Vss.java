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

import org.apache.log4j.Category;

/**
 *  This class handles all VSS-related aspects of determining the modifications
 *  since the last good build.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author Eli Tucker
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Arun Aggarwal
 */
public class Vss extends SourceControlElement {

    /** enable logging for this class */
    private static Category log = Category.getInstance(Vss.class.getName());

    private final String VSS_TEMP_FILE = "vsstempfile.txt";
    protected SimpleDateFormat vssDateTimeFormat;
    
	private String ssdir;
	private String login;
	private String property;
	private String propertyOnDelete;
    private String dateFormat;
	private long lastModified;    
    
	private ArrayList modifications = new ArrayList();
	private Set emails = new HashSet();

    /** 
     * Sets default values.
     */
    public Vss() {
        dateFormat = "MM/dd/yy";
        constructVssDateTimeFormat();
    }
    
	/**
	 *  Set the project to get history from
	 *
	 *@param  ssdir
	 */
	public void setSsDir(String ssdir) {
		this.ssdir = "$" + ssdir;
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
		this.property = property;
	}

  	/**
	 *  Choose a property to be set if the project has deletions
	 *
	 *@param  property
	 */
     public void setPropertyOnDelete(String property) {
		propertyOnDelete = property;
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
	 *  For parent modificationset to find out the time of last modification for
	 *  this project
	 *
	 *@return
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 *  Returns a Set of usernames that made any modification since the last good
	 *  build.
	 *
	 *@return
	 */
	public Set getEmails() {
		return emails;
	}

	/**
	 *  Returns a List of modifications to this project since the last good
	 *  build.
	 *
	 *@return
	 */
	public List getModifications() {
		return modifications;
	}

	/**
	 * Calls 
     * "ss history [dir] -R -Vd[now]~[lastBuild] -Y[login] -I-N -O[tempFileName]"
     * Results written to a file since VSS will start wrapping lines if read 
     * directly from the stream.
	 *
	 *@param  lastBuild
	 *@param  now
	 *@param  quietPeriod
	 *@return List of modifications
	 */
	public List getHistory(Date lastBuild, Date now, long quietPeriod) {
        //(PENDING) extract buildHistoryCommand, execHistoryCommand
        // See CVSElement
        
		//call vss, write output to intermediate file
        String[] cmdArray = {"ss.exe", "history", ssdir, "-R", "-Vd" +
                formatDateForVSS(now) + "~" + formatDateForVSS(lastBuild),
                "-Y" + login, "-I-N", "-O" + VSS_TEMP_FILE};
		try {
			Process p = Runtime.getRuntime().exec(cmdArray);
			p.waitFor();

			BufferedReader reader = new BufferedReader(new FileReader(
             new File(VSS_TEMP_FILE)));

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
					handleEntry(vssEntry);
				} else {
					currLine = reader.readLine();
				}
			}

			reader.close();
			new File(VSS_TEMP_FILE).delete();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (property != null && modifications.size() > 0) {
			//TO DO: set properties getAntTask().getProject().setProperty(property, "true");
		}

		return modifications;
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
        SimpleDateFormat sdf = new SimpleDateFormat(this.dateFormat + ";hh:mma");
		String vssFormattedDate = sdf.format(d);
		return vssFormattedDate.substring(0, vssFormattedDate.length() - 1);
	}

	// ***** the rest of this is just parsing the vss output *****

    //(PENDING) Extract class VSSEntryParser
	/**
	 *  Parse individual VSS history entry
	 *
	 *@param  historyEntry
	 */
	protected void handleEntry(List historyEntry) {
        // Ignore unusual labels of directories which cause parsing errors that 
        // look like this:
        //
        // *****  built  *****
        // Version 4
        // Label: "autobuild_test"
        // User: Etucker      Date:  6/26/01   Time: 11:53a
        // Labeled
        if ((historyEntry.size() > 4) && 
            (((String) historyEntry.get(4)).startsWith("Labeled"))) {
           return;
        }

		Modification mod = new Modification();
        String nameAndDateLine = (String) historyEntry.get(2);
		mod.userName = parseUser(nameAndDateLine);
		mod.modifiedTime = parseDate(nameAndDateLine);

        String folderLine = (String) historyEntry.get(0);
        String fileLine = (String) historyEntry.get(3);
		if (fileLine.startsWith("Checked in")) {
			mod.type = "checkin";
			mod.comment = parseComment(historyEntry);
			mod.fileName = folderLine.substring(7, folderLine.indexOf("  *"));
			mod.folderName = fileLine.substring(12);
		} else if (fileLine.endsWith("Created")) {
            mod.type = "create";
        } else {
			mod.folderName = folderLine.substring(7, folderLine.indexOf("  *"));
			mod.fileName = fileLine.substring(0, fileLine.lastIndexOf(" "));

            if (fileLine.endsWith("added")) {
				mod.type = "add";
			} else if (fileLine.endsWith("deleted")) {
				mod.type = "delete";
			} else if (fileLine.endsWith("recovered")) {
				mod.type = "recover";
			} else if (fileLine.endsWith("shared")) {
                mod.type = "branch";
            }
		}

		if (propertyOnDelete != null && "delete".equals(mod.type)) {
			//TO DO: set properties getAntTask().getProject().setProperty(propertyOnDelete, "true");
		}

        if (property != null) {
    		//TO DO: set properties getAntTask().getProject().setProperty(property, "true");
        }

		modifications.add(mod);
	}

	/**
	 *  Parse comment from VSS history (could be multi-line)
	 *
	 *@param  commentList
	 *@return
	 */
	private String parseComment(List commentList) {
		StringBuffer comment = new StringBuffer();
		comment.append(((String) commentList.get(4)) + " ");
		for (int i = 5; i < commentList.size(); i++) {
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
            Date lastModifiedDate = this.vssDateTimeFormat.parse(
             dateAndTime.trim() + "m");
            
            //(PENDING) This seems out of place
            if (lastModifiedDate.getTime() < lastModified) {
                lastModified = lastModifiedDate.getTime();
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
        final int START_OF_USER_NAME = 6;
		String userName = userLine.substring(
         START_OF_USER_NAME, userLine.indexOf("Date: ") - 1).trim();
		emails.add(userName);

		return userName;
	}

    /**
     * Constructs the vssDateTimeFormat based on the dateFormat for this element.
     * @see #setDateFormat
     */
    private void constructVssDateTimeFormat() {
        vssDateTimeFormat = new SimpleDateFormat("'Date: '" + this.dateFormat + "   'Time: 'hh:mma");        
    }
}
