/**
 *  CruiseControl, a Continuous Integration Toolkit * Copyright (C) 2001
 *  ThoughtWorks, Inc. * 651 W Washington Ave. Suite 500 * Chicago, IL 60661 USA
 *  * * This program is free software; you can redistribute it and/or * modify
 *  it under the terms of the GNU General Public License * as published by the
 *  Free Software Foundation; either version 2 * of the License, or (at your
 *  option) any later version. * * This program is distributed in the hope that
 *  it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 *  warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 *  GNU General Public License for more details. * * You should have received a
 *  copy of the GNU General Public License * along with this program; if not,
 *  write to the Free Software * Foundation, Inc., 59 Temple Place - Suite 330,
 *  Boston, MA 02111-1307, USA. *
 */

package net.sourceforge.cruisecontrol;

import java.io.*;
import java.text.*;
import java.util.*;
import net.sourceforge.cruisecontrol.element.*;
import org.apache.tools.ant.*;

/**
 *  This class is designed to record the modifications made to the source
 *  control management system since the last build
 */
public class ModificationSet extends Task {

	private Date _lastBuild;
	private long _quietPeriod;
	private ArrayList _sourceControlElements = new ArrayList();

	private long _lastModified;
	private DateFormat _formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

	private Set _emails = new HashSet();

	public final static String BUILDUNNECESSARY = "modificationset.buildunnecessary";
	public final static String SNAPSHOTTIMESTAMP = "modificationset.snapshottimestamp";
	public final static String USERS = "modificationset.users";

	private final static SimpleDateFormat _simpleDateFormat =
			new SimpleDateFormat("yyyyMMddHHmmss");

	/**
	 *  set the timestamp of the last build time. String should be formatted as
	 *  "yyyyMMddHHmmss"
	 *
	 *@param  lastBuild
	 */
	public void setLastBuild(String lastBuild) {
		try {
			_lastBuild = _simpleDateFormat.parse(lastBuild);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Set the number of seconds that the repository has to be quiet before
	 *  building to avoid building while checkins are in progress
	 *
	 *@param  seconds
	 */
	public void setQuietPeriod(long seconds) {
		_quietPeriod = seconds * 1000;
	}

	public void setDateFormat(String format) {
		if (format != null && format.length() > 0) {
			_formatter = new SimpleDateFormat(format);
		}
	}

	/**
	 *  do stuff, namely get all modifications since the last build time, and make
	 *  sure that the appropriate quiet period is enforced so that we aren't
	 *  building with 1/2 of someone's checkins.
	 *
	 *@throws  BuildException
	 */
	public void execute() throws BuildException {
		try {
			Date currentDate = new Date();
			_lastModified = _lastBuild.getTime();
            
            long currentTime = currentDate.getTime();
			while (tooMuchRepositoryActivity(currentTime)) {
                long sleepTime = calculateSleepTime(currentTime);

				log("[modificationset] Too much repository activity...sleeping for: "
						 + (sleepTime / 1000.0) + " seconds.");
				Thread.sleep(sleepTime);

				currentDate = new Date();
			}

			ArrayList modifications =
					processSourceControlElements(currentDate, _lastBuild);

			//If there aren't any modifications, then a build is not necessary, so
			//  we will terminate this build by throwing a BuildException. That will
			//  kill the Ant process and return control to MasterBuild.
			if (modifications.isEmpty()) {
				getProject().setProperty(BUILDUNNECESSARY, "true");
				throw new BuildException("No Build Necessary");
			}

			getProject().setProperty(SNAPSHOTTIMESTAMP,
					_simpleDateFormat.format(currentDate));
			getProject().setProperty(USERS, emailsAsCommaDelimitedList());

			writeFile(modifications);
		} catch (InterruptedException ie) {
			throw new BuildException(ie);
		} catch (IOException ioe) {
			throw new BuildException(ioe);
		}
	}

	/**
	 *  add a nested element for sourcesafe specific code.
	 *
	 *@return
	 */
	public VssElement createVsselement() {
		VssElement ve = new VssElement();
		ve.setAntTask(this);
		//for logging in the sub elements
		_sourceControlElements.add(ve);

		return ve;
	}

	/**
	 *  add a nested element for star team specific code.
	 *
	 *@return
	 */
	public StarTeamElement createStarteamelement() {
		StarTeamElement ste = new StarTeamElement();
		ste.setAntTask(this);
		//for logging in the sub elements
		_sourceControlElements.add(ste);

		return ste;
	}


	/**
	 *  add a nested element for cvs specific code.
	 *
	 *@return
	 */
	public CVSElement createCvselement() {
		CVSElement ce = new CVSElement();
		ce.setAntTask(this);
		//for logging in the sub elements
		_sourceControlElements.add(ce);

		return ce;
	}


	/**
	 *  add a nested element for p4 specific code.
	 *
	 *@return
	 */
	public P4Element createP4element() {
		P4Element p4e = new P4Element();
		p4e.setAntTask(this);
		//for logging in the sub elements
		_sourceControlElements.add(p4e);

		return p4e;
	}

	/**
	 *  add a nested element for clearcase specific code.
	 *
	 *@return
	 */
	public ClearCaseElement createClearcaseelement() {
		ClearCaseElement cce = new ClearCaseElement();
		cce.setAntTask(this);
		//for logging in the sub elements
		_sourceControlElements.add(cce);

		return cce;
	}

	private boolean tooMuchRepositoryActivity(long currentTime) {
        if (_lastModified > currentTime) {
            return true;
        }
		return (_lastModified > (currentTime - _quietPeriod));
	}

    private long calculateSleepTime(long currentTime) {
        if (_lastModified > currentTime) {
            return _lastModified - currentTime + _quietPeriod;
        } else {
            return _quietPeriod - currentTime - _lastModified;                    
        }        
    }    
    
	/**
	 *  Loop over all nested source control elements and get modifications and
	 *  users that made modifications
	 *
	 *@param  currentDate
	 *@param  lastBuild
	 *@return
	 */
	private ArrayList processSourceControlElements(Date currentDate, Date lastBuild) {
		ArrayList mods = new ArrayList();

		for (int i = 0; i < _sourceControlElements.size(); i++) {
			SourceControlElement sce =
					(SourceControlElement) _sourceControlElements.get(i);
			mods.addAll(sce.getHistory(lastBuild, currentDate, _quietPeriod));

			if (!mods.isEmpty()) {
				if (sce.getLastModified() > lastBuild.getTime()) {
					_lastModified = sce.getLastModified();
				}

				_emails.addAll(sce.getEmails());
			}
		}

		return mods;
	}

	/**
	 *  Write out file with all modifications. Filename is specified in the ant
	 *  property modificationset.file
	 *
	 *@param  modifications
	 *@exception  IOException
	 */
	private void writeFile(ArrayList modifications) throws IOException {
		Project p = getProject();
		String modFileName = getProject().getProperty("modificationset.file");
		if (modFileName == null) {
			modFileName = "modificationset.xml";
			getProject().setProperty("modificationset.file", modFileName);
		}

		FileWriter fw = new FileWriter(new File(modFileName));
		fw.write("<modifications>\n");
		for (int i = 0; i < modifications.size(); i++) {
			fw.write(((Modification) modifications.get(i)).toXml(_formatter));
		}
		fw.write("</modifications>\n");
		fw.close();
	}

	/**
	 *  build up a string of emails of users to be notified about this build
	 *
	 *@return
	 */
	private String emailsAsCommaDelimitedList() {
		StringBuffer sb = new StringBuffer();
		Iterator i = _emails.iterator();
		while (i.hasNext()) {
			sb.append(((String) i.next()));
			if (i.hasNext()) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

}
