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
package net.sourceforge.cruisecontrol;

import java.io.*;
import java.text.*;
import java.util.*;
import net.sourceforge.cruisecontrol.element.*;
import org.apache.tools.ant.*;

/**
 * This class is designed to record the modifications made to the source control
 * management system since the last build.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class ModificationSet extends Task {

    private Date _lastBuild;
    private long _quietPeriod;
    private ArrayList _sourceControlElements = new ArrayList();

    private long _lastModified;
    private DateFormat _formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    private Set _emails = new HashSet();
    
    private boolean _useServerTime = false;

    public final static String BUILDUNNECESSARY = "modificationset.buildunnecessary";
    public final static String SNAPSHOTTIMESTAMP = "modificationset.snapshottimestamp";
    public final static String USERS = "modificationset.users";

    private final static SimpleDateFormat _simpleDateFormat =
            new SimpleDateFormat("yyyyMMddHHmmss");

    public void setDateFormat(String format) {
        if (format != null && format.length() > 0) {
            _formatter = new SimpleDateFormat(format);
        }
    }

    public void setUseservertime(boolean useServerTime) {
        _useServerTime = useServerTime;
    }

    /**
     * set the timestamp of the last build time. String should be formatted as
     * "yyyyMMddHHmmss"
     *
     * @param lastBuild
     */
    public void setLastBuild(String lastBuild) {
        try {
            _lastBuild = _simpleDateFormat.parse(lastBuild);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the number of seconds that the repository has to be quiet before
     * building to avoid building while checkins are in progress
     *
     * @param seconds
     */
    public void setQuietPeriod(long seconds) {
        _quietPeriod = seconds * 1000;
    }

    /**
     * do stuff, namely get all modifications since the last build time, and
     * make sure that the appropriate quiet period is enforced so that we aren't
     * building with 1/2 of someone's checkins.
     *
     * @throws BuildException
     */
    public void execute() throws BuildException {
        List modifications = new ArrayList();
        long currentTime = 0;
        try {
            Date currentDate = new Date();
            _lastModified = _lastBuild.getTime();

            modifications = processSourceControlElements(currentDate, _lastBuild);

            currentTime = currentDate.getTime();
            while (tooMuchRepositoryActivity(currentTime)) {
                long sleepTime = calculateSleepTime(currentTime);

                log("[modificationset] Too much repository activity...sleeping for: "
                         + (sleepTime / 1000.0) + " seconds.");
                Thread.sleep(sleepTime);

                modifications =
                        processSourceControlElements(currentDate, _lastBuild);

                currentDate = new Date();
                currentTime = currentDate.getTime();
            }

            //If there aren't any modifications, then a build is not necessary, so
            //  we will terminate this build by throwing a BuildException. That will
            //  kill the Ant process and return control to MasterBuild.
            if (modifications.isEmpty()) {
                getProject().setProperty(BUILDUNNECESSARY, "true");
                throw new BuildException("No Build Necessary");
            }

            if (_useServerTime) {
                getProject().setProperty(SNAPSHOTTIMESTAMP,
                        _simpleDateFormat.format(new Date(_lastModified)));
            }
            else {
                getProject().setProperty(SNAPSHOTTIMESTAMP,
                        _simpleDateFormat.format(currentDate));
            }
            getProject().setProperty(USERS, emailsAsCommaDelimitedList());

            writeFile(modifications);
        }
        catch (InterruptedException ie) {
            throw new BuildException(ie);
        }
        catch (IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    /**
     * add a nested element for sourcesafe specific code.
     *
     * @return
     */
    public VssElement createVsselement() {
        VssElement ve = new VssElement();
        ve.setAntTask(this);
        //for logging in the sub elements
        _sourceControlElements.add(ve);

        return ve;
    }

    /**
     * add a nested element for star team specific code.
     *
     * @return
     */
    public StarTeamElement createStarteamelement() {
        StarTeamElement ste = new StarTeamElement();
        ste.setAntTask(this);
        //for logging in the sub elements
        _sourceControlElements.add(ste);

        return ste;
    }


    /**
     * add a nested element for cvs specific code.
     *
     * @return
     */
    public CVSElement createCvselement() {
        CVSElement ce = new CVSElement();
        ce.setAntTask(this);
        //for logging in the sub elements
        _sourceControlElements.add(ce);

        return ce;
    }


    /**
     * add a nested element for p4 specific code.
     *
     * @return
     */
    public P4Element createP4element() {
        P4Element p4e = new P4Element();
        p4e.setAntTask(this);
        //for logging in the sub elements
        _sourceControlElements.add(p4e);

        return p4e;
    }

    /**
     * add a nested element for clearcase specific code.
     *
     * @return
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
        }
        else {
            return _quietPeriod - (currentTime - _lastModified);
        }
    }

    /**
     * Loop over all nested source control elements and get modifications and
     * users that made modifications
     *
     * @param currentDate
     * @param lastBuild
     * @return
     */
    private List processSourceControlElements(Date currentDate, Date lastBuild) {
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
     * Write out file with all modifications. Filename is specified in the ant
     * property modificationset.file
     *
     * @param modifications
     * @exception IOException
     */
    private void writeFile(List modifications) throws IOException {
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
     * build up a string of emails of users to be notified about this build
     *
     * @return
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
