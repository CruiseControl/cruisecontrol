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

import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Category;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class Project implements Serializable {

    /** enable logging for this class */
    private static Category log = Category.getInstance(Project.class.getName());

    private transient Schedule _schedule;
    private transient List _bootstrappers = new ArrayList();
    private transient ModificationSet _modificationSet;
    private transient List _publishers = new ArrayList();
    private transient LabelIncrementer _labelIncrementer;
    private transient List _auxLogs = new ArrayList();
    private transient long _sleepMillis;
    private transient String _logFileName;
    private transient String _logDir;

    private int _buildCounter = 0;
    private Date _lastBuild;
    private Date _now;
    private boolean _wasLastBuildSuccessful;
    private String _label;
    private String _fileName;
    private String _name;
    private SimpleDateFormat _formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    private boolean _isPaused = false;

    public void execute() {
        while (true) {
            try {
                while(_isPaused) {
                    Thread.yield();
                }
                init();
                build();
                log.info("Sleeping for " + formatTime(_sleepMillis));
                Thread.sleep(_sleepMillis);
            } catch (InterruptedException e) {
                log.error("Error sleeping.", e);
            } catch (CruiseControlException e) {
                log.error("", e);
            }
        }
    }

    protected String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        StringBuffer sb = new StringBuffer();
        if (hours > 0)
            sb.append(hours + " hours ");
        if (minutes > 0)
            sb.append(minutes + " minutes ");
        if (seconds > 0)
            sb.append(seconds + " seconds ");

        return sb.toString();

    }

    /**
     *  Run one entire build.
     */
    public void build() throws CruiseControlException {
        Element cruisecontrolElement = new Element("cruisecontrol");
        _now = new Date();
        if (_schedule.isPaused(_now)) {
            return; //we've paused
        }
        
        // bootstrap the build
        bootstrap();
        
        // check for modifications
        cruisecontrolElement.addContent(_modificationSet.getModifications(_lastBuild));
        if (!_modificationSet.isModified()) {
            log.info("No modifications found, build not necessary.");
            return; //no need to build
        }

        // increment label if nesseccary
        if (_labelIncrementer.isPreBuildIncrementer()) {
            _label = _labelIncrementer.incrementLabel(_label, cruisecontrolElement);
        }
        
        // collect project information
        cruisecontrolElement.addContent(getProjectPropertiesElement());
        
        // BUILD
        cruisecontrolElement.addContent(_schedule.build(_buildCounter, _lastBuild, _now, getProjectPropertiesMap()).detach());

        // increment label if necessary
        if (!_labelIncrementer.isPreBuildIncrementer() && (new XMLLogHelper(cruisecontrolElement)).isBuildSuccessful()) {
            _label = _labelIncrementer.incrementLabel(_label, cruisecontrolElement);
        }


        // collect log files and merge with CC log file
        Iterator auxLogIterator = getAuxLogElements().iterator();
        while (auxLogIterator.hasNext()) {
            cruisecontrolElement.addContent((Element) auxLogIterator.next());
        }
        writeLogFile(cruisecontrolElement);

        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", _logFileName.substring(_logFileName.lastIndexOf(File.separator)));
        cruisecontrolElement.getChild("info").addContent(logFileElement);

        _lastBuild = _now;
        _buildCounter++;
        write();
        publish(cruisecontrolElement);
        cruisecontrolElement = null;
    }

    /**
     * Initialize the project. Uses ProjectXMLHelper to parse a project file.
     */
    protected void init() {
        try {
            ProjectXMLHelper helper = new ProjectXMLHelper(new File(_fileName), _name);
            _sleepMillis = 1000 * helper.getBuildInterval();
            _logDir = helper.getLogDir();
            _bootstrappers = helper.getBootstrappers();
            _schedule = helper.getSchedule();
            _modificationSet = helper.getModificationSet();
            _labelIncrementer = helper.getLabelIncrementer();
            _auxLogs = helper.getAuxLogs();
            _publishers = helper.getPublishers();
        } catch (CruiseControlException e) {
            log.fatal("Error initializing project.", e);
        }
    }

    protected Element getProjectPropertiesElement() {
        Element infoElement = new Element("info");
        Element lastBuildPropertyElement = new Element("property");
        lastBuildPropertyElement.setAttribute("name", "lastbuild");
        if (_lastBuild == null)
            lastBuildPropertyElement.setAttribute("value", _formatter.format(_now));
        else
            lastBuildPropertyElement.setAttribute("value", _formatter.format(_lastBuild));
        infoElement.addContent(lastBuildPropertyElement);

        Element labelPropertyElement = new Element("property");
        labelPropertyElement.setAttribute("name", "label");
        labelPropertyElement.setAttribute("value", _label);
        infoElement.addContent(labelPropertyElement);

        Element intervalElement = new Element("property");
        intervalElement.setAttribute("name", "interval");
        intervalElement.setAttribute("value", "" + (_sleepMillis / 1000));
        infoElement.addContent(intervalElement);

        Element lastBuildSuccessfulPropertyElement = new Element("property");
        lastBuildSuccessfulPropertyElement.setAttribute("name", "lastbuildsuccessful");
        lastBuildSuccessfulPropertyElement.setAttribute("value", _wasLastBuildSuccessful + "");
        return infoElement;
    }

    protected Map getProjectPropertiesMap() {
        Map buildProperties = new HashMap();
        buildProperties.put("label", _label);
        buildProperties.put("cctimestamp", _formatter.format(_now));
        buildProperties.putAll(_modificationSet.getProperties());
        return buildProperties;
    }

    /**
     *  Iterate over all of the registered <code>Publisher</code>s and call their respective
     *  <code>publish</code> methods.
     *
     *  @param logElement JDOM Element representing the build log.
     */
    protected void publish(Element logElement) throws CruiseControlException {
        Iterator publisherIterator = _publishers.iterator();
        while (publisherIterator.hasNext()) {
            ((Publisher) publisherIterator.next()).publish(logElement);
        }
    }

    /**
     *  Iterate over all of the registered <code>Bootstrapper</code>s and call their respective
     *  <code>bootstrap</code> methods.
     */
    protected void bootstrap() throws CruiseControlException {
        Iterator bootstrapperIterator = _bootstrappers.iterator();
        while (bootstrapperIterator.hasNext()) {
            ((Bootstrapper) bootstrapperIterator.next()).bootstrap();
        }
    }

    /**
     *  Write the entire log file to disk, merging in any additional logs
     *
     *  @param logElement JDOM Element representing the build log.
     */
    protected void writeLogFile(Element logElement) throws CruiseControlException {
        BufferedWriter logWriter = null;
        try {
            XMLLogHelper helper = new XMLLogHelper(logElement);
            if (helper.isBuildSuccessful())
                _logFileName = new File(_logDir, "log" + _formatter.format(_now) + "L" + helper.getLabel() + ".xml").getAbsolutePath();
            else
                _logFileName = new File(_logDir, "log" + _formatter.format(_now) + ".xml").getAbsolutePath();
            log.debug("Writing log file: " + _logFileName);
            logWriter = new BufferedWriter(new FileWriter(_logFileName));
            XMLOutputter outputter = new XMLOutputter("   ", true);
            outputter.output(logElement, logWriter);
            logWriter = null;
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            logWriter = null;
        }
    }

    /**
     *  Builds a list of <code>Element</code>s of all of the auxilliary log files.  If the file
     *  is a directory, it will
     *
     *  @return <code>List</code> of <code>Element</code>s of all of the auxilliary log files.
     */
    protected List getAuxLogElements() {
        Iterator auxLogIterator = _auxLogs.iterator();
        List auxLogElements = new ArrayList();
        while (auxLogIterator.hasNext()) {
            String fileName = (String) auxLogIterator.next();
            File auxLogFile = new File(fileName);
            if (auxLogFile.isDirectory()) {
                String[] childFileNames = auxLogFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".xml");
                    }
                });
                for (int i = 0; i < childFileNames.length; i++) {
                    auxLogElements.add(getElementFromAuxLogFile(fileName + File.separator + childFileNames[i]).detach());
                }
            } else {
                Element auxLogElement = getElementFromAuxLogFile(fileName);
                if (auxLogElement != null) {
                    auxLogElements.add(auxLogElement.detach());
                }
            }
        }
        return auxLogElements;
    }

    /**
     *  Get a JDOM <code>Element</code> from an XML file.
     *
     *  @param fileName The file name to read.
     *  @return JDOM <code>Element</code> representing that xml file.
     */
    protected Element getElementFromAuxLogFile(String fileName) {
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            return builder.build(fileName).getRootElement();
        } catch (JDOMException e) {
            log.debug("Could not read aux log: " + fileName + ".  Skipping...");
            e.printStackTrace();
        }

        return null;
    }

    /**
     *  Serialize the project so that if we restart the process, we can resume where we were.
     */
    public void write() {
        try {
            ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(_name));
            s.writeObject(this);
            s.flush();
            s.close();
            log.debug("Serializing project to: " + _name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setModificationSet(ModificationSet modSet) {
        _modificationSet = modSet;
    }

    public void setSchedule(Schedule schedule) {
        _schedule = schedule;
    }

    public void addAuxiliaryLogFile(String fileName) {
        _auxLogs.add(fileName);
    }

    public void setLabelIncrementer(LabelIncrementer incrementer) {
        _labelIncrementer = incrementer;
    }

    public void setConfigFileName(String fileName) {
        _fileName = fileName;
    }

    public String getConfigFileName() {
        return _fileName;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setLabel(String label) {
        _label = label;
    }

    public String getLabel() {
        return _label;
    }

    public void setLastBuild(String lastBuild) {
        try {
            _lastBuild = _formatter.parse(lastBuild);
        } catch (ParseException e) {
            log.error("Error parsing last build timestamp.", e);
        }
    }

    public String getLastBuild() {
        if (_lastBuild == null)
            return null;
        return _formatter.format(_lastBuild);
    }

    public String getBuildTime() {
        return _formatter.format(_now);
    }

    public void setLogDir(String logDir) {
        _logDir = logDir;
    }

    public long getSleepMilliseconds() {
        return _sleepMillis;
    }

    protected String getLogFileName() {
        return _logFileName;
    }

    public boolean isPaused() {
        return _isPaused;
    }

    public void setPaused(boolean paused) {
        _isPaused = paused;
    }
}