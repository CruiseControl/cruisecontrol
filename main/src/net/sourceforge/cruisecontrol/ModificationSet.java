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

import java.util.ArrayList;
import java.util.Date;
import java.io.*;
import java.text.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.tools.ant.*;

/**
 *  This class is designed to record the modifications made to the
 *  source control management system since the last build
 */
public class ModificationSet extends Task {


    private Date _lastBuild;
    private long _quietPeriod;
    private ArrayList _scElements = new ArrayList();

    private Date _now;
    long _lastModified;
    private DateFormat _formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    private ArrayList _modifications = new ArrayList();
    private Set _emails = new HashSet();

    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    public static final String BUILDUNNECESSARY = "modificationset.buildunnecessary";
    public static final String SNAPSHOTTIMESTAMP = "modificationset.snapshottimestamp";
    public static final String USERS = "modificationset.users";

    /**
     *	set the timestamp of the last build time.
     *	String should be formatted as "yyyyMMddHHmmss"
     */
    public void setLastbuild(String s) {
        try {
            _lastBuild = _sdf.parse(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *	Set the number of seconds that the repository has to be
     *   quiet before building to avoid building while checkins
     *	are in progress
     */
    public void setQuietperiod(long seconds) {
        _quietPeriod = seconds * 1000;
    }


    public void setDateformat(String format) {
        if (format != null && format.length() > 0) {
            _formatter = new SimpleDateFormat(format);
        }
    }

    /**
     *   add a nested element for sourcesafe specific code.
     */
    public VssElement createVsselement() {
        VssElement ve = new VssElement();
        ve.setTask(this); //for logging in the sub elements
        _scElements.add(ve);

        return ve;
    }

    /**
     *   add a nested element for star team specific code.
     */
       public StarTeamElement createStarteamelement() {
           StarTeamElement ste = new StarTeamElement();
           ste.setTask(this); //for logging in the sub elements
           _scElements.add(ste);

           return ste;
       }


    /**
     *   add a nested element for cvs specific code.
     */
    public CVSElement createCvselement() {
        CVSElement ce = new CVSElement();
        ce.setTask(this); //for logging in the sub elements
        _scElements.add(ce);

        return ce;
    }


    /**
     *   add a nested element for p4 specific code.
     */
    public P4Element createP4element() {
        P4Element p4e = new P4Element();
        p4e.setTask(this); //for logging in the sub elements
        _scElements.add(p4e);

        return p4e;
    }

    /**
     *   add a nested element for clearcase specific code.
     */
    public ClearCaseElement createClearcaseelement() {
        ClearCaseElement cce = new ClearCaseElement();
        cce.setTask(this); //for logging in the sub elements
        _scElements.add(cce);

        return cce;
    }


    /**
     *  loop over all nested source control elements and get modifications and
     *	users that made modifications
     */
    private void processSourceControlElements() {
        for (int i=0; i < _scElements.size(); i++) {
            SourceControlElement sce = (SourceControlElement) _scElements.get(i);
            ArrayList mods = sce.getHistory(_lastBuild, _now, _quietPeriod);

            if (!mods.isEmpty()) {
                _modifications.addAll(mods);
                if (sce.getLastModified() > _lastModified)
                    _lastModified = sce.getLastModified();

                _emails.addAll(sce.getEmails());
            }
        }
    }

    /**
     *	do stuff, namely get all modifications since the last build time, and
     *	make sure that the appropriate quiet period is enforced so that we aren't
     *   building with 1/2 of someone's checkins.
     */
    public void execute() throws BuildException {
        try {
            _now = new Date();
            _lastModified = _lastBuild.getTime();

            processSourceControlElements();

            //If there aren't any modifications, then a build is not necessary, so
            //  we will terminate this build by throwing a BuildException. That will
            //  kill the Ant process and return control to MasterBuild.
            if (_modifications.isEmpty()) {
                this.getProject().setProperty(BUILDUNNECESSARY, "true");
                throw new BuildException("No Build Necessary");
            }

            //If a modification occured within our quietPeriod, we need to sleep
            //  until at least the end of the quiet period, then check again.
            while (_lastModified > (_now.getTime() - _quietPeriod)) {
                long sleepTime = _quietPeriod - (_now.getTime() - _lastModified);

                this.getProject().log("Too much repository activity...sleeping for: " + (sleepTime/1000.0) + " seconds.");
                Thread.sleep(sleepTime);

                _now = new Date();
                _modifications = new ArrayList();
                processSourceControlElements();
            }

            this.getProject().setProperty(SNAPSHOTTIMESTAMP, _sdf.format(_now));
            this.getProject().setProperty(USERS, emailsAsCommaDelimitedList());

            writeFile();
        } catch (InterruptedException ie) {
            throw new BuildException(ie);
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    /**
     *	write out file with all modifications.  filename is specified in the ant property
     *	modificationset.file
     */
    private void writeFile() throws IOException {
        Project p = this.getProject();
        String modFileName = this.getProject().getProperty("modificationset.file");
        if (modFileName == null) {
            modFileName = "modificationset.xml";
            this.getProject().setProperty("modificationset.file", modFileName);
        }

        FileWriter fw = new FileWriter(new File(modFileName));
        fw.write("<modifications>\n");
        for (int i=0; i < _modifications.size(); i++)
            fw.write(((Modification) _modifications.get(i)).toXml(_formatter));
        fw.write("</modifications>\n");
        fw.close();
    }

    /**
     *	build up a string of emails of users to be notified about this build
     */
    private String emailsAsCommaDelimitedList() {
        StringBuffer sb = new StringBuffer();
        Iterator i = _emails.iterator();
        while (i.hasNext()) {
            sb.append(((String) i.next()));
            if (i.hasNext())
                sb.append(",");
        }
        return sb.toString();
    }
}