/******************************************************************************
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Represents a single logical project consisting of source code that needs to
 * be built.  Project is associated with bootstrappers that run before builds
 * and a Schedule that determines when builds occur.
 */
public class Project implements Serializable {

    static final long serialVersionUID = 2656877748476842326L;

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(Project.class);

    private transient Schedule _schedule;
    private transient List _bootstrappers = new ArrayList();
    private transient ModificationSet _modificationSet;
    private transient List _publishers = new ArrayList();
    private transient LabelIncrementer _labelIncrementer;
    private transient List _auxLogs = new ArrayList();
    private transient String _logXmlEncoding = null;
    private transient long _sleepMillis;
    private transient String _logFileName;
    private transient String _logDir;
    private transient String _status;
    private transient Date _buildStartTime;
    private transient Object mutex = new Object();
    private static final transient long ONE_MINUTE = 60 * 1000;

    private int _buildCounter = 0;
    private Date _lastBuild;
    private Date _lastSuccessfulBuild; // can differ from _lastBuild when _buildAfterFailed=false
    private boolean _wasLastBuildSuccessful = false;
    private String _label;
    private String _configFileName = "config.xml";
    private String _name;
    private SimpleDateFormat _formatter =
            new SimpleDateFormat("yyyyMMddHHmmss");
    private boolean _buildForced = false;
    private boolean _isPaused = false;
    private boolean _buildAfterFailed = true;

	// this variable only exists at the moment for methods needed for 
	// ProjectTest.testBuild()
	private Date __now;


    /**
     * Enters an infinite loop that initializes the project from the config
     * file, starts a build, sleeps, and repeats. <b>Note:</b> This means that
     * the config file is re-parsed on every cycle.
     */
    public void execute() {
        checkMutex();
        while (true) {
            try {
                synchronized(mutex){
                    while (_isPaused) {
                        mutex.wait(10 * ONE_MINUTE);
                    }
                }
                init();
                build();
                Date now = new Date();
                long timeTillBuild = _schedule.getTimeToNextBuild(now, _sleepMillis);
				_schedule.waitForNextBuild(timeTillBuild);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting:", e);
            } catch (CruiseControlException e) {
                log.error("", e);
            }
        }
    }

	private void checkMutex() {
		if (mutex==null) mutex = new Object();
	}

    /**
     *  Unless paused, runs any bootstrappers and then the entire build.
     */
    public void build() throws CruiseControlException {
        _buildStartTime = new Date();
        Date now = _buildStartTime;
        if (_schedule.isPaused(now)) {
            return; //we've paused
        }

        _status = "Bootstrapping";
        bootstrap();

        Element cruisecontrolElement = new Element("cruisecontrol");

        // check for modifications since _lastBuild, but don't save them
        _status = "Scanning for modified code";
        Element modifications = _modificationSet.getModifications(_lastBuild);
        if (!_modificationSet.isModified()) {
            log.info("No modifications found, build not necessary.");

            // Sometimes we want to build even though we don't have any
            // modifications. This is in fact current default behaviour.
            // Set by <project buildafterfailed="true/false">
            if (_buildAfterFailed && !_wasLastBuildSuccessful) {
                log.info("Building anyway, since buildAfterFailed is true and last build failed.");
            } else {
                if (_buildForced) {
                    log.info("Building anyway, since build was explicitly forced.");
                } else {
                    return;
                }
            }
        }

        // Store modifications from last successful build, if they are different
        if (_lastBuild != null && !_lastBuild.equals(_lastSuccessfulBuild)) {
            modifications = _modificationSet.getModifications(_lastSuccessfulBuild);
        }
        cruisecontrolElement.addContent(modifications);

        now = _modificationSet.getNow();

        if (_labelIncrementer.isPreBuildIncrementer()) {
            _label = _labelIncrementer.incrementLabel(_label,
                    cruisecontrolElement);
        }

        // collect project information
        cruisecontrolElement.addContent(getProjectPropertiesElement(now));

        // BUILD
        _status = "Building";
        cruisecontrolElement.addContent(_schedule.build(_buildCounter,
                _lastBuild, now, getProjectPropertiesMap(now)).detach());

        boolean buildSuccessful =
                new XMLLogHelper(cruisecontrolElement).isBuildSuccessful();

        if (!_labelIncrementer.isPreBuildIncrementer() && buildSuccessful) {
            _label = _labelIncrementer.incrementLabel(_label,
                    cruisecontrolElement);
        }

        // collect log files and merge with CC log file
        _status = "Merging additional XML files";
        Iterator auxLogIterator = getAuxLogElements().iterator();
        while (auxLogIterator.hasNext()) {
            cruisecontrolElement.addContent((Element) auxLogIterator.next());
        }

        createLogFileName(cruisecontrolElement, now);

        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", _logFileName.substring(
                _logFileName.lastIndexOf(File.separator)));
        cruisecontrolElement.getChild("info").addContent(logFileElement);

        writeLogFile(cruisecontrolElement);

        // If we only want to build after a check in, even when broken, set the last build to now,
        // regardless of success or failure (buildAfterFailed = false in config.xml)
        if (!getBuildAfterFailed()) {
            _lastBuild = now;
        }

        // If this was a successful build, update both last build and last successful build
        if (buildSuccessful) {
            _lastBuild = now;
            _lastSuccessfulBuild = now;
        }

        _buildCounter++;
        _wasLastBuildSuccessful = buildSuccessful;

        serializeProject();

        _status = "Publishing build results";
        publish(cruisecontrolElement);
        cruisecontrolElement = null;

        _status = "";
        
        __now = now; // see comment on __now declaration
    }

    /**
     * Serialize the project to allow resumption after a process bounce
     */
    public void serializeProject() {
        try {
            ObjectOutputStream s = new ObjectOutputStream(
                    new FileOutputStream(_name));
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

    public LabelIncrementer getLabelIncrementer() {
        return _labelIncrementer;
    }

    public void setLabelIncrementer(LabelIncrementer incrementer) {
        _labelIncrementer = incrementer;
    }

    public void setLogXmlEncoding(String logXmlEncoding) {
        _logXmlEncoding = logXmlEncoding;
    }

    public void setConfigFileName(String fileName) {
        log.debug("Config file set to: " + fileName);
        _configFileName = fileName;
    }

    public String getConfigFileName() {
        return _configFileName;
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

    /**
     * @param lastBuild string containing the build date in the format
     * yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     * input string
     */
    public void setLastBuild(String lastBuild) throws CruiseControlException {
        if (lastBuild == null) {
            throw new IllegalArgumentException("Null last build date string");
        }
        try {
            _lastBuild = _formatter.parse(lastBuild);
        } catch (ParseException e) {
            log.error("Error parsing last build timestamp.", e);
            throw new CruiseControlException("Cannot parse last build string: "
                    + lastBuild);
        }
    }

    /**
     * @param lastSuccessfulBuild string containing the build date in the format
     * yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     * input string
     */
    public void setLastSuccessfulBuild(String lastSuccessfulBuild) throws CruiseControlException {
        if (lastSuccessfulBuild == null) {
            throw new IllegalArgumentException("Null last successful build date string");
        }
        try {
            _lastSuccessfulBuild = _formatter.parse(lastSuccessfulBuild);
        } catch (ParseException e) {
            log.error("Error parsing last successful build timestamp.", e);
            throw new CruiseControlException("Cannot parse last successful build string: "
                    + lastSuccessfulBuild);
        }
    }

    public String getLastBuild() {
        if (_lastBuild == null)
            return null;
        return _formatter.format(_lastBuild);
    }

    public boolean getBuildForced() {
        return _buildForced;
    }

    public void setBuildForced(boolean buildForced) {
        _buildForced = buildForced;
        if (buildForced) _schedule.forceBuild();
    }

    public String getLastSuccessfulBuild() {
        if (_lastSuccessfulBuild == null)
            return null;
        return _formatter.format(_lastSuccessfulBuild);
    }

    public String getLogDir() {
        return _logDir;
    }

    public void setLogDir(String logDir) {
        _logDir = logDir;
    }

    public long getSleepMilliseconds() {
        return _sleepMillis;
    }

    public void setSleepMillis(long sleepMillis) {
        _sleepMillis = sleepMillis;
    }

    protected String getLogFileName() {
        return _logFileName;
    }

    public boolean isPaused() {
		return _isPaused;
    }

    public void setPaused(boolean paused) {
        checkMutex();
        synchronized(mutex) {
        	_isPaused = paused;
        	if (!paused) mutex.notifyAll();
        }
    }

    public boolean getBuildAfterFailed() {
        return _buildAfterFailed;
    }

    public void setBuildAfterFailed(boolean buildAfterFailed) {
        _buildAfterFailed = buildAfterFailed;
    }

    public String getStatus() {
        return _status;
    }

    public Date getBuildStartTime() {
        return _buildStartTime;
    }

    /**
     * Initialize the project. Uses ProjectXMLHelper to parse a project file.
     */
    protected void init() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(new File(_configFileName), _name);
        _sleepMillis = 1000 * helper.getBuildInterval();
        _logDir = helper.getLogDir();
        _logXmlEncoding = helper.getLogXmlEncoding();
        File logDir = new File(_logDir);
        if (!logDir.exists()) {
            throw new CruiseControlException(
                    "Log Directory specified in config file does not exist: " + logDir.getAbsolutePath());
        }
        if (!logDir.isDirectory()) {
            throw new CruiseControlException(
                    "Log Directory specified in config file is not a directory: " + logDir.getAbsolutePath());
        }
        _bootstrappers = helper.getBootstrappers();
        _schedule = helper.getSchedule();
        _modificationSet = helper.getModificationSet();

        _labelIncrementer = helper.getLabelIncrementer();
        validateLabel(_label, _labelIncrementer);

        _auxLogs = helper.getAuxLogs();
        _publishers = helper.getPublishers();

        _buildAfterFailed = helper.getBuildAfterFailed();
    }

    protected Element getProjectPropertiesElement(Date now) {
        Element infoElement = new Element("info");
        Element projectNameElement = new Element("property");
        projectNameElement.setAttribute("name", "projectname");
        projectNameElement.setAttribute("value", _name);
        infoElement.addContent(projectNameElement);

        Element lastBuildPropertyElement = new Element("property");
        lastBuildPropertyElement.setAttribute("name", "lastbuild");
        if (_lastBuild == null) {
            lastBuildPropertyElement.setAttribute("value",
                    _formatter.format(now));
        } else {
            lastBuildPropertyElement.setAttribute("value",
                    _formatter.format(_lastBuild));
        }
        infoElement.addContent(lastBuildPropertyElement);

        Element lastSuccessfulBuildPropertyElement = new Element("property");
        lastSuccessfulBuildPropertyElement.setAttribute("name", "lastsuccessfulbuild");
        if (_lastSuccessfulBuild == null) {
            lastSuccessfulBuildPropertyElement.setAttribute("value",
                    _formatter.format(now));
        } else {
            lastSuccessfulBuildPropertyElement.setAttribute("value",
                    _formatter.format(_lastSuccessfulBuild));
        }
        infoElement.addContent(lastSuccessfulBuildPropertyElement);

        Element buildDateElement = new Element("property");
        buildDateElement.setAttribute("name", "builddate");
        buildDateElement.setAttribute("value", new SimpleDateFormat(DateFormatFactory.getFormat()).format(now));
        infoElement.addContent(buildDateElement);

        Element labelPropertyElement = new Element("property");
        labelPropertyElement.setAttribute("name", "label");
        labelPropertyElement.setAttribute("value", _label);
        infoElement.addContent(labelPropertyElement);

        Element intervalElement = new Element("property");
        intervalElement.setAttribute("name", "interval");
        intervalElement.setAttribute("value", "" + (_sleepMillis / 1000));
        infoElement.addContent(intervalElement);

        Element lastBuildSuccessfulPropertyElement = new Element("property");
        lastBuildSuccessfulPropertyElement.setAttribute("name",
                "lastbuildsuccessful");
        lastBuildSuccessfulPropertyElement.setAttribute("value",
                _wasLastBuildSuccessful + "");
        infoElement.addContent(lastBuildSuccessfulPropertyElement);

        return infoElement;
    }

    protected Map getProjectPropertiesMap(Date now) {
        Map buildProperties = new HashMap();
        buildProperties.put("label", _label);
        buildProperties.put("cctimestamp", _formatter.format(now));
        buildProperties.putAll(_modificationSet.getProperties());
        return buildProperties;
    }

    /**
     * Iterate over all of the registered <code>Publisher</code>s and call
     * their respective <code>publish</code> methods.
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
     * Iterate over all of the registered <code>Bootstrapper</code>s and call
     * their respective <code>bootstrap</code> methods.
     */
    protected void bootstrap() throws CruiseControlException {
        Iterator bootstrapperIterator = _bootstrappers.iterator();
        while (bootstrapperIterator.hasNext()) {
            ((Bootstrapper) bootstrapperIterator.next()).bootstrap();
        }
    }

    protected void createLogFileName(Element logElement, Date now) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(logElement);
        if (helper.isBuildSuccessful())
            _logFileName = new File(_logDir, "log" + _formatter.format(now)
                    + "L" + helper.getLabel() + ".xml").getAbsolutePath();
        else
            _logFileName = new File(_logDir, "log" + _formatter.format(now)
                    + ".xml").getAbsolutePath();
    }

    /**
     *  Write the entire log file to disk, merging in any additional logs
     *
     *  @param logElement JDOM Element representing the build log.
     */
    protected void writeLogFile(Element logElement)
            throws CruiseControlException {
        BufferedWriter logWriter = null;
        try {
            log.debug("Writing log file: " + _logFileName);
            XMLOutputter outputter = null;
            if (_logXmlEncoding == null) {
                outputter = new XMLOutputter("   ", true);
                logWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_logFileName)));
            } else {
                outputter = new XMLOutputter("   ", true, _logXmlEncoding);
                logWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_logFileName), _logXmlEncoding));
            }
            outputter.output(new Document(logElement), logWriter);
            logWriter = null;
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            logWriter = null;
        }
    }

    /**
     * Builds a list of <code>Element</code>s of all of the auxilliary log
     * files.  If the file is a directory, it will
     *
     * @return <code>List</code> of <code>Element</code>s of all of the
     * auxilliary log files.
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
                    Element auxLogElement = getElementFromAuxLogFile(fileName + File.separator + childFileNames[i]);
                    if (auxLogElement != null) {
                        auxLogElements.add(auxLogElement.detach());
                    }
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
            SAXBuilder builder =
                    new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element element = builder.build(fileName).getRootElement();
            if (element.getName().equals("testsuite")) {
                if (element.getChild("properties") != null) {
                    element.getChild("properties").detach();
                }
            }
            return element;
        } catch (JDOMException e) {
            log.debug("Could not read aux log: " + fileName + ".  Skipping...", e);
        }

        return null;
    }

    /**
     * Ensure that label is valid for the specified LabelIncrementer
     *
     * @param label target label
     * @param labelIncrementer target LabelIncrementer
     * @throws CruiseControlException if label is not valid
     */
    protected void validateLabel(String label,
                                 LabelIncrementer labelIncrementer)
            throws CruiseControlException {

        if (!labelIncrementer.isValidLabel(label)) {
            throw new CruiseControlException(label + " is not a valid label");
        }
    }

    protected boolean isLastBuildSuccessful() {
        return _wasLastBuildSuccessful;
    }

	public String getBuildTime() {
		return _formatter.format(__now);
	}

    protected Date getNow() {
        return __now;
    }
}
