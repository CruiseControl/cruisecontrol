/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
import net.sourceforge.cruisecontrol.util.Util;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a single logical project consisting of source code that needs to
 * be built.  Project is associated with bootstrappers that run before builds
 * and a Schedule that determines when builds occur.
 */
public class Project implements Serializable, Runnable {
    static final long serialVersionUID = 2656877748476842326L;
    private static final Logger LOG = Logger.getLogger(Project.class);

    private transient ProjectState state = ProjectState.STOPPED_STATE;
    private transient Schedule schedule;
    private transient Log log;
    private transient List bootstrappers = new ArrayList();
    private transient ModificationSet modificationSet;
    private transient List publishers = new ArrayList();
    private transient LabelIncrementer labelIncrementer;
    private transient List auxLogs = new ArrayList();

    /**
     * If this attribute is set, then it means that the user has overriden
     * the build interval specified in the Schedule element, probably
     * using the JMX interface.
     */
    private transient Long overrideBuildInterval;

    private transient Date buildStartTime;
    private transient Object pausedMutex = new Object();
    private transient Object scheduleMutex = new Object();
    private transient Object waitMutex = new Object();
    private transient BuildQueue queue;

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

    private int buildCounter = 0;
    private Date lastBuild = getMidnight();
    private Date lastSuccessfulBuild = lastBuild;
    private boolean wasLastBuildSuccessful = true;
    private String label;
    private File configFile;
    private String name;
    private boolean buildForced = false;
    private boolean isPaused = false;
    private boolean buildAfterFailed = true;
    private transient Thread projectSchedulingThread;
    private boolean stopped = true;

    /**
     * <b>Note:</b> This means that the config file is re-parsed on every cycle.
     */
    public void execute() {
        checkMutex();

        synchronized (pausedMutex) {
            if (isPaused) {
                buildFinished();
                return;
            }
        }

        try {
            init();
            build();
        } catch (CruiseControlException e) {
            LOG.error("exception attempting build in project " + name, e);
        }

        buildFinished();
    }

    private void checkMutex() {
        if (pausedMutex == null) {
            pausedMutex = new Object();
        }
        if (scheduleMutex == null) {
            scheduleMutex = new Object();
        }
        if (waitMutex == null) {
            waitMutex = new Object();
        }
    }

    /**
     *  Unless paused, runs any bootstrappers and then the entire build.
     */
    public void build() throws CruiseControlException {
        buildStartTime = new Date();
        if (schedule.isPaused(buildStartTime)) {
            return; //we've paused
        }

        bootstrap();

        Element buildLog = new Element("cruisecontrol");

        Element modifications = getModifications();
        if (modifications == null) {
            setState(ProjectState.IDLE_STATE);
            return;
        }

        buildLog.addContent(modifications);

        Date now = modificationSet.getTimeOfCheck();

        if (labelIncrementer.isPreBuildIncrementer()) {
            label = labelIncrementer.incrementLabel(label, buildLog);
        }

        // collect project information
        buildLog.addContent(getProjectPropertiesElement(now));

        setState(ProjectState.BUILDING_STATE);
        buildLog.addContent(
            schedule.build(buildCounter, lastBuild, now, getProjectPropertiesMap(now)).detach());

        boolean buildSuccessful = new XMLLogHelper(buildLog).isBuildSuccessful();

        if (!labelIncrementer.isPreBuildIncrementer() && buildSuccessful) {
            label = labelIncrementer.incrementLabel(label, buildLog);
        }

        setState(ProjectState.MERGING_LOGS_STATE);
        Iterator auxLogIterator = getAuxLogElements().iterator();
        while (auxLogIterator.hasNext()) {
            buildLog.addContent((Element) auxLogIterator.next());
        }

        log.writeLogFile(buildLog, now);

        // If we only want to build after a check in, even when broken, set the last build to now,
        // regardless of success or failure (buildAfterFailed = false in config.xml)
        if (!getBuildAfterFailed()) {
            lastBuild = now;
        }

        // If this was a successful build, update both last build and last successful build
        if (buildSuccessful) {
            lastBuild = now;
            lastSuccessfulBuild = now;
        }

        buildCounter++;
        wasLastBuildSuccessful = buildSuccessful;

        serializeProject();

        setState(ProjectState.PUBLISHING_STATE);
        publish(buildLog);
        buildLog = null;

        setState(ProjectState.IDLE_STATE);
    }

    /**
     * Returns just the filename from the File object, i.e. no path information
     * included. So if the File instance represents c:\java\ant\build.xml
     * this method will return build.xml.
     */
    public static String getSimpleFilename(File file) {
        String fullPath = file.getAbsolutePath();
        return fullPath.substring(fullPath.lastIndexOf(File.separator));
    }

    public void run() {
        LOG.info("Project " + name + " started");
        checkMutex();
        try {
            while (!stopped) {
                try {
                    waitIfPaused();
                    waitForNextBuild();
                    setState(ProjectState.QUEUED_STATE);
                    synchronized (scheduleMutex) {
                        queue.requestBuild(this);
                        waitForBuildToFinish();
                    }
                } catch (InterruptedException e) {
                    String message = "Project " + name + ".run() interrupted";
                    LOG.error(message, e);
                    throw new RuntimeException(message);
                }
            }
        } finally {
            stopped = true;
            LOG.info("Project " + name + " stopped");
        }
    }

    void waitIfPaused() throws InterruptedException {
        synchronized (pausedMutex) {
            while (isPaused) {
                setState(ProjectState.PAUSED_STATE);
                pausedMutex.wait(10 * Util.ONE_MINUTE);
            }
        }
    }

    void waitForNextBuild() throws InterruptedException {
        Date now = new Date();
        long waitTime = schedule.getTimeToNextBuild(now, getBuildInterval());
        if (needToWaitForNextBuild(waitTime)) {
            appLog("next build in " + Util.formatTime(waitTime));
            synchronized (waitMutex) {
                waitMutex.wait(waitTime);
            }
        }
    }

    static boolean needToWaitForNextBuild(long waitTime) {
        return waitTime > 0;
    }

    void forceBuild() {
        synchronized (waitMutex) {
            waitMutex.notify();
        }
    }

    void waitForBuildToFinish() throws InterruptedException {
        synchronized (scheduleMutex) {
            debug("waiting for build to finish");
            scheduleMutex.wait();
        }
    }

    void buildFinished() {
        synchronized (scheduleMutex) {
            debug("build finished");
            scheduleMutex.notify();
        }
    }

    /**
     * @return Element
     */
    Element getModifications() {
        setState(ProjectState.MODIFICATIONSET_STATE);
        Element modifications = null;

        boolean checkNewChangesFirst = checkOnlySinceLastBuild();
        if (checkNewChangesFirst) {
            debug("getting changes since last build");
            modifications = modificationSet.getModifications(lastBuild);
        } else {
            debug("getting changes since last successful build");
            modifications = modificationSet.getModifications(lastSuccessfulBuild);
        }

        if (!modificationSet.isModified()) {
            appLog("No modifications found, build not necessary.");

            // Sometimes we want to build even though we don't have any
            // modifications. This is in fact current default behaviour.
            // Set by <project buildafterfailed="true/false">
            if (buildAfterFailed && !wasLastBuildSuccessful) {
                appLog("Building anyway, since buildAfterFailed is true and last build failed.");
            } else {
                if (buildForced) {
                    appLog("Building anyway, since build was explicitly forced.");
                } else {
                    return null;
                }
            }
        }
        
        if (buildForced) {
            buildForced = false;
        }

        if (checkNewChangesFirst) {
            debug("new changes found; now getting complete set");
            modifications = modificationSet.getModifications(lastSuccessfulBuild);
        }

        return modifications;
    }

    /**
     * @return boolean
     */
    boolean checkOnlySinceLastBuild() {
        if (lastBuild == null || lastSuccessfulBuild == null) {
            return false;
        }

        long lastBuildLong = lastBuild.getTime();
        long timeDifference = lastBuildLong - lastSuccessfulBuild.getTime();
        boolean moreThanASecond = timeDifference > Util.ONE_SECOND;

        boolean checkNewMods = !buildAfterFailed && moreThanASecond;

        return checkNewMods;
    }

    /**
     * Serialize the project to allow resumption after a process bounce
     */
    public void serializeProject() {
        try {
            ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(name + ".ser"));
            s.writeObject(this);
            s.flush();
            s.close();
            debug("Serializing project to [" + name + ".ser]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setModificationSet(ModificationSet modSet) {
        modificationSet = modSet;
    }

    public void setSchedule(Schedule newSchedule) {
        schedule = newSchedule;
    }

    public void addAuxiliaryLogFile(String fileName) {
        auxLogs.add(fileName);
    }

    public LabelIncrementer getLabelIncrementer() {
        return labelIncrementer;
    }

    public void setLabelIncrementer(LabelIncrementer incrementer) {
        labelIncrementer = incrementer;
    }

    public void setLogXmlEncoding(String encoding) {
        log.setLogXmlEncoding(encoding);
    }

    public void setConfigFile(File fileName) {
        debug("Config file set to [" + fileName + "]");
        configFile = fileName;
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setName(String projectName) {
        name = projectName;
    }

    public String getName() {
        return name;
    }

    public void setLabel(String newLabel) {
        label = newLabel;
    }

    public String getLabel() {
        return label;
    }

    /**
     * @param newLastBuild string containing the build date in the format
     * yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     * input string
     */
    public void setLastBuild(String newLastBuild) throws CruiseControlException {
        lastBuild = parseFormatedTime(newLastBuild, "lastBuild");
    }

    /**
     * @param newLastSuccessfulBuild string containing the build date in the format
     * yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     * input string
     */
    public void setLastSuccessfulBuild(String newLastSuccessfulBuild)
        throws CruiseControlException {
        lastSuccessfulBuild = parseFormatedTime(newLastSuccessfulBuild, "lastSuccessfulBuild");
    }

    public String getLastBuild() {
        if (lastBuild == null) {
            return null;
        }
        return getFormatedTime(lastBuild);
    }

    public boolean getBuildForced() {
        return buildForced;
    }

    public void setBuildForced(boolean forceNewBuildNow) {
        buildForced = forceNewBuildNow;
        if (forceNewBuildNow) {
            forceBuild();
        }
    }

    public String getLastSuccessfulBuild() {
        if (lastSuccessfulBuild == null) {
            return null;
        }
        return getFormatedTime(lastSuccessfulBuild);
    }

    public String getLogDir() {
        return log.getLogDir();
    }

    /**
     * Returns the build interval. This value is initially specified on the
     * schedule, but the user may override that value using the JMX interface.
     * If the user hasn't override the Schedule, then this method will
     * return the Schedule's interval, otherwise the overriden value will
     * be returned.
     */
    public long getBuildInterval() {
        if (overrideBuildInterval == null) {
            return schedule.getInterval();
        } else {
            return overrideBuildInterval.longValue();
        }
    }

    /**
     * Sets the build interval that this Project should use. This method
     * overrides the value initially specified in the Schedule attribute.
     */
    public void overrideBuildInterval(long sleepMillis) {
        overrideBuildInterval = new Long(sleepMillis);
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        checkMutex();
        synchronized (pausedMutex) {
            if (isPaused && !paused) {
                pausedMutex.notifyAll();
            }
            isPaused = paused;
        }
    }

    public boolean getBuildAfterFailed() {
        return buildAfterFailed;
    }

    public void setBuildAfterFailed(boolean rebuildEvenWithNoNewModifications) {
        buildAfterFailed = rebuildEvenWithNoNewModifications;
    }

    public String getStatus() {
        return getState().getDescription();
    }

    public ProjectState getState() {
        return state;
    }

    private void setState(ProjectState newState) {
        state = newState;
        appLog(getStatus());
    }

    public void setBuildQueue(BuildQueue buildQueue) {
        queue = buildQueue;
    }

    public Date getBuildStartTime() {
        return buildStartTime;
    }

    /**
     * Initialize the project. Uses ProjectXMLHelper to parse a project file.
     */
    protected void init() throws CruiseControlException {
        appLog("reading settings from config file [" + configFile.getAbsolutePath() + "]");
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, name);

        bootstrappers = helper.getBootstrappers();
        schedule = helper.getSchedule();
        log = helper.getLog();

        modificationSet = helper.getModificationSet();

        labelIncrementer = helper.getLabelIncrementer();
        if (label == null) {
            label = labelIncrementer.getDefaultLabel();
        }
        validateLabel(label, labelIncrementer);

        auxLogs = helper.getAuxLogs();
        setPublishers(helper.getPublishers());

        buildAfterFailed = helper.getBuildAfterFailed();

        if (lastBuild == null) {
            lastBuild = getMidnight();
        }

        if (lastSuccessfulBuild == null) {
            lastSuccessfulBuild = lastBuild;
        }

        debug("buildInterval          = [" + getBuildInterval() + "]");
        debug("buildForced            = [" + buildForced + "]");
        debug("buildAfterFailed       = [" + buildAfterFailed + "]");
        debug("buildCounter           = [" + buildCounter + "]");
        debug("isPaused               = [" + isPaused + "]");
        debug("label                  = [" + label + "]");
        debug("lastBuild              = [" + getFormatedTime(lastBuild) + "]");
        debug("lastSuccessfulBuild    = [" + getFormatedTime(lastSuccessfulBuild) + "]");
        debug("logDir                 = [" + log.getLogDir() + "]");
        debug("logXmlEncoding         = [" + log.getLogXmlEncoding() + "]");
        debug("wasLastBuildSuccessful = [" + wasLastBuildSuccessful + "]");
    }

    protected void setPublishers(List listOfPublishers) {
        publishers = listOfPublishers;
        
    }

    protected Element getProjectPropertiesElement(Date now) {
        Element infoElement = new Element("info");
        Element projectNameElement = new Element("property");
        projectNameElement.setAttribute("name", "projectname");
        projectNameElement.setAttribute("value", name);
        infoElement.addContent(projectNameElement);

        Element lastBuildPropertyElement = new Element("property");
        lastBuildPropertyElement.setAttribute("name", "lastbuild");
        if (lastBuild == null) {
            lastBuildPropertyElement.setAttribute("value", getFormatedTime(now));
        } else {
            lastBuildPropertyElement.setAttribute("value", getFormatedTime(lastBuild));
        }
        infoElement.addContent(lastBuildPropertyElement);

        Element lastSuccessfulBuildPropertyElement = new Element("property");
        lastSuccessfulBuildPropertyElement.setAttribute("name", "lastsuccessfulbuild");
        if (lastSuccessfulBuild == null) {
            lastSuccessfulBuildPropertyElement.setAttribute("value", getFormatedTime(now));
        } else {
            lastSuccessfulBuildPropertyElement.setAttribute(
                "value",
                getFormatedTime(lastSuccessfulBuild));
        }
        infoElement.addContent(lastSuccessfulBuildPropertyElement);

        Element buildDateElement = new Element("property");
        buildDateElement.setAttribute("name", "builddate");
        buildDateElement.setAttribute(
            "value",
            new SimpleDateFormat(DateFormatFactory.getFormat()).format(now));
        infoElement.addContent(buildDateElement);

        if (now != null) {
            Element ccTimeStampPropertyElement = createBuildTimestampElement(now);
            infoElement.addContent(ccTimeStampPropertyElement);
        }

        Element labelPropertyElement = new Element("property");
        labelPropertyElement.setAttribute("name", "label");
        labelPropertyElement.setAttribute("value", label);
        infoElement.addContent(labelPropertyElement);

        Element intervalElement = new Element("property");
        intervalElement.setAttribute("name", "interval");
        intervalElement.setAttribute("value", "" + (getBuildInterval() / 1000));
        infoElement.addContent(intervalElement);

        Element lastBuildSuccessfulPropertyElement = new Element("property");
        lastBuildSuccessfulPropertyElement.setAttribute("name", "lastbuildsuccessful");
        lastBuildSuccessfulPropertyElement.setAttribute("value", wasLastBuildSuccessful + "");
        infoElement.addContent(lastBuildSuccessfulPropertyElement);

        return infoElement;
    }

    public static Element createBuildTimestampElement(Date now) {
        Element ccTimeStampPropertyElement = new Element("property");
        ccTimeStampPropertyElement.setAttribute("name", "cctimestamp");
        ccTimeStampPropertyElement.setAttribute("value", getFormatedTime(now));
        return ccTimeStampPropertyElement;
    }

    protected Map getProjectPropertiesMap(Date now) {
        Map buildProperties = new HashMap();
        buildProperties.put("label", label);
        buildProperties.put("cctimestamp", getFormatedTime(now));
        buildProperties.putAll(modificationSet.getProperties());
        return buildProperties;
    }

    /**
     * Iterate over all of the registered <code>Publisher</code>s and call
     * their respective <code>publish</code> methods.
     *
     *  @param logElement JDOM Element representing the build log.
     */
    protected void publish(Element logElement) throws CruiseControlException {
        Iterator publisherIterator = publishers.iterator();
        Publisher publisher = null;
        while (publisherIterator.hasNext()) {
            try {
                publisher = (Publisher) publisherIterator.next(); 
                publisher.publish(logElement);
            } catch (CruiseControlException e) {
                StringBuffer message = new StringBuffer("exception publishing results");
                if (publisher != null) {
                    message.append(" with " + publisher.getClass().getName());
                }
                message.append(" for project " + name);
                LOG.error(message.toString(), e);
            }
        }
    }

    /**
     * Iterate over all of the registered <code>Bootstrapper</code>s and call
     * their respective <code>bootstrap</code> methods.
     */
    protected void bootstrap() throws CruiseControlException {
        setState(ProjectState.BOOTSTRAPPING_STATE);
        Iterator bootstrapperIterator = bootstrappers.iterator();
        while (bootstrapperIterator.hasNext()) {
            ((Bootstrapper) bootstrapperIterator.next()).bootstrap();
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
        Iterator auxLogIterator = auxLogs.iterator();
        List auxLogElements = new ArrayList();
        while (auxLogIterator.hasNext()) {
            String fileName = (String) auxLogIterator.next();
            File auxLogFile = new File(fileName);
            if (auxLogFile.isDirectory()) {
                String[] childFileNames = auxLogFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(".xml");
                    }
                });
                for (int i = 0; i < childFileNames.length; i++) {
                    Element auxLogElement =
                        getElementFromAuxLogFile(fileName + File.separator + childFileNames[i]);
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
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element element = builder.build(fileName).getRootElement();
            if (element.getName().equals("testsuite")) {
                if (element.getChild("properties") != null) {
                    element.getChild("properties").detach();
                }
            }
            return element;
        } catch (JDOMException e) {
            LOG.warn("Could not read aux log: " + fileName + ".  Skipping...", e);
        }

        return null;
    }

    /**
     * Ensure that label is valid for the specified LabelIncrementer
     *
     * @param oldLabel target label
     * @param incrementer target LabelIncrementer
     * @throws CruiseControlException if label is not valid
     */
    protected void validateLabel(String oldLabel, LabelIncrementer incrementer)
        throws CruiseControlException {
        if (!incrementer.isValidLabel(oldLabel)) {
            throw new CruiseControlException(
                oldLabel
                    + " is not a valid label for labelIncrementer "
                    + incrementer.getClass().getName());
        }
    }

    protected boolean isLastBuildSuccessful() {
        return wasLastBuildSuccessful;
    }

    public static String getFormatedTime(Date date) {
        if (date == null) {
            return null;
        }
        return formatter.format(date);
    }

    public Date parseFormatedTime(String timeString, String description)
        throws CruiseControlException {

        Date date = null;
        if (timeString == null) {
            throw new IllegalArgumentException("Null date string for " + description);
        }
        try {
            date = formatter.parse(timeString);
        } catch (ParseException e) {
            LOG.error("Error parsing timestamp for [" + description + "]", e);
            throw new CruiseControlException(
                "Cannot parse string for " + description + ":" + timeString);
        }

        return date;
    }

    private void appLog(String message) {
        LOG.info("Project " + name + ":  " + message);
    }

    private void debug(String message) {
        LOG.debug("Project " + name + ":  " + message);
    }

    public void start() {
        stopped = false;
        projectSchedulingThread = new Thread(this, "Project " + getName() + " thread");
        projectSchedulingThread.start();
        LOG.info("Project " + name + " starting");
        setState(ProjectState.IDLE_STATE);
    }

    public void stop() {
        LOG.info("Project " + name + " stopping");
        stopped = true;
        setState(ProjectState.STOPPED_STATE);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Project ");
        sb.append(getName());
        sb.append(": ");
        sb.append(getStatus());
        if (isPaused) {
            sb.append(" (paused)");
        }
        return sb.toString();
    }

    /**
     * @return midnight on today's date
     */
    public static Date getMidnight() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight.getTime();
    }

    public Log getLog() {
        if (this.log == null) {
            this.log = new Log(getName());
        }
        return this.log;
    }
}
