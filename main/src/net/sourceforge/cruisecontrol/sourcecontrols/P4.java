/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, isMobile.com - http://www.ismobile.com
 * Aurorum 2, S-977 75 Luleå, Sweden
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
 *     + Neither the name of isMobile.com, ThoughtWorks, Inc., 
 *       CruiseControl, nor the names of its contributors may be used 
 *       to endorse or promote products derived from this software 
 *       without specific prior written permission.
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

import java.util.*;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;

import org.apache.oro.text.perl.*;

import org.apache.tools.ant.taskdefs.optional.perforce.*;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.BuildException;
import org.apache.log4j.Category;

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
 * @author niclas.olofsson@ismobile.com
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Tim McCune
 */
public class P4 implements SourceControl {

    /** enable logging for this class */
    private static Category log = Category.getInstance(P4.class.getName());

	//P4 runtime directives

	private String _P4Port;
	private String _P4Client;
	private String _P4User;
	private String _P4View;
	private int _P4lastChange;
	private final static java.text.SimpleDateFormat p4Date = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private Hashtable _properties;
    private String _property;
    private String _propertyOnDelete;

	/**
	 *  Constructor for P4Element. Doesn't do much.
	 */
	public P4() {
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

    public void setProperty(String property) {
        _property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        _propertyOnDelete = propertyOnDelete;
    }

    public Hashtable getProperties() {
        return _properties;
    }

	public List getModifications(Date lastBuild, Date now, long quietPeriod) {

		ArrayList mods = new ArrayList();
		final Perl5Util util = new Perl5Util();

		// next line is a trick to get the variable usable within the adhoc handler.
		final List changeNumbers = new ArrayList();
		final StringBuffer sbModifiedTime = new StringBuffer();
		//if this._P4lastChange != p4 changes -m 1 -s submitted <depotpath>
		execP4Command("changes -m 1 -s submitted " + _P4View,
			new P4HandlerAdapter() {
				public void process(String line) {
					if (util.match("/Change/", line)) {
						//Parse out the change number
						String changeNumber = util.substitute("s/Change\\s([0-9]*?)\\son\\s.*/$1/gx", line);
						changeNumbers.add(changeNumber);
						log.info("Latest change is " + changeNumber);
                    } else if (util.match("/error/", line)) {
						throw new BuildException("Perforce Error, check client settings and/or server");
					}
				}
			});

		// and collect info for this change

		Iterator iter = changeNumbers.iterator();
		while (iter.hasNext()) {
			mods.addAll(getChangeInfo((String) iter.next(), lastBuild));
		}
		return mods;
	}
	
	private List getChangeInfo(String changeNumber, Date lastBuild) {
		List rtn = new ArrayList();
		final Perl5Util util = new Perl5Util();            
            
		final StringBuffer sbDescription = new StringBuffer();
		execP4Command("describe -s " + changeNumber.toString(),
			new P4HandlerAdapter() {
				public void process(String line) {
					sbDescription.append(line);
					sbDescription.append("\n");
				}
			});
		// now, lets parse the data out
		String userName = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*/$2/s", sbDescription.toString());
		String sModifiedTime = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*/$3/s", sbDescription.toString());
		String comment = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*/$4/s", sbDescription.toString());
		comment = util.substitute("s/\\t//g", comment);

		Date modifiedTime = null;
		try {
			modifiedTime = p4Date.parse(sModifiedTime);
		} catch (Exception ex) {
			log.error("Wrong date format exception caught. Using lastModified date from project instead.", ex);
		}

		if (modifiedTime.compareTo(lastBuild) > 0) {
			// if it differs, we build,
			_P4lastChange = Integer.parseInt(changeNumber);
            _properties.put("p4element.change", changeNumber);

			// the rest should be a list of the files affected and the resp action
			String affectedFiles = util.substitute("s/Change\\s([0-9]*?)\\sby\\s(.*?)\\@.*?\\son\\s(.*?\\s.*?)\\n\\n(.*)\\n\\nAffected\\sfiles.*?\\n\\n(.*)\\n\\n/$5/s", sbDescription.toString());
			ArrayList files = new ArrayList();
			util.split(files, "/\\n/s", affectedFiles);
			Iterator iter = files.iterator();
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

				rtn.add(mod);
			}
        }

        return rtn;
	}

	/**
	 *@param  command
	 *@throws  BuildException
	 */
	protected void execP4Command(String command) throws BuildException {
		execP4Command(command, null);
	}

	/**
	 *  Execute P4 commands. Giv a P4Handler to process the events.
	 *
	 *@param  command The command to run
	 *@param  handler A P4Handler to process any input and output
	 *@throws  BuildException Description of Exception
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

			log.debug("Executing: " + commandline);

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
							log.debug(util.substitute("s/^.*: //", line));
						}
					};

			}

			Execute exe = new Execute(handler);

			/*
            if (getAntTask() != null) {
				exe.setAntRun(getAntTask().getProject());
			}
            */
			exe.setCommandline(commandline.getCommandline());

			try {
				exe.execute();
			}
			catch (IOException e) {
				throw new BuildException(e);
			}
			finally {
				try {
					handler.stop();
				}
				catch (Exception e) {
				}
			}

		}
		catch (Exception e) {
			throw new BuildException("Problem executing P4 command: " + e.getMessage());
		}
	}

}
// P4Element
