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

import java.util.*;
import java.io.*;

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
 * @author     niclas.olofsson@ismobile.com, jchyip
 * @created    den 23 april 2001
 * @version    0.1
 */
public class P4Element extends SourceControlElement {
    
    private Set _emailNames = new HashSet();
    private Date _lastModified;
    private final static java.text.SimpleDateFormat p4Date = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    //P4 runtime directives
    
    private String _P4Port;
    private String _P4Client;
    private String _P4User;
    private String _P4View;
    private int _P4lastChange;

    /**
     * The String prepended to log messages from the source control element.  For
     * example, CVSElement should implement this as return "[cvselement]";
     *
     * @return prefix for log messages
     */
    protected String logPrefix() {
        return "[p4element]";
    }
    
    protected void execP4Command(String command) throws BuildException {
        execP4Command(command, null);
    }
    
    /**
     *  Execute P4 commands. Giv a P4Handler to process the events.
     *
     * @param  command             The command to run
     * @param  handler             A P4Handler to process any input and output
     * @exception  BuildException  Description of Exception
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
            
            if (getAntTask() != null) {
                exe.setAntRun(getAntTask().getProject());
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
            getAntTask().getProject().setProperty("p4element.change", sbChangenumber.toString());
            
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
    
}// P4Element
