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

import net.sourceforge.cruisecontrol.events.BuildProgressEvent;
import net.sourceforge.cruisecontrol.events.BuildProgressListener;
import net.sourceforge.cruisecontrol.events.BuildResultEvent;
import net.sourceforge.cruisecontrol.events.BuildResultListener;
import net.sourceforge.cruisecontrol.sourcecontrols.CVS;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.listeners.ProjectStateChangedEvent;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

    private transient ProjectState state;

    private transient List bootstrappers;
    private transient ModificationSet modificationSet;
    private transient Schedule schedule;
    private transient Log log;
    private transient List publishers;
    private transient LabelIncrementer labelIncrementer;
    private transient List listeners;

    /**
     * If this attribute is set, then it means that the user has overriden
     * the build interval specified in the Schedule element, probably
     * using the JMX interface.
     */
    private transient Long overrideBuildInterval;

    private transient Date buildStartTime;
    private transient Object pausedMutex;
    private transient Object scheduleMutex;
    private transient Object waitMutex;
    private transient BuildQueue queue;
    private transient List progressListeners;
    private transient List resultListeners;

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss");

    private int buildCounter = 0;
    private Date lastBuild = Util.getMidnight();
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

    public Project() {
        initializeTransientFields();
    }

    private void initializeTransientFields() {
        state = ProjectState.STOPPED;
        
        // we never add to the following 3 Lists,
        // so we don't need to create a new List for them
        bootstrappers = Collections.EMPTY_LIST;
        publishers = Collections.EMPTY_LIST;
        listeners = Collections.EMPTY_LIST;

        pausedMutex = new Object();
        scheduleMutex = new Object();
        waitMutex = new Object();
        progressListeners = new ArrayList();
        resultListeners = new ArrayList();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initializeTransientFields();
    }

    /**
     * <b>Note:</b> This means that the config file is re-parsed on every cycle.
     */
    public void execute() {
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

    /**
     * Unless paused, runs any bootstrappers and then the entire build.
     */
    protected void build() throws CruiseControlException {
        try {
            buildStartTime = new Date();
            if (schedule.isPaused(buildStartTime)) {
                // a regularly scheduled paused
                // is different than ProjectState.PAUSED
                return;
            }

            bootstrap();

            // getModifications will only return null if we don't need to build
            Element modifications = getModifications();
            buildForced = false;

            if (modifications == null) {
                return;
            }

            log.addContent(modifications);

            Date now = modificationSet.getTimeOfCheck();

            if (labelIncrementer.isPreBuildIncrementer()) {
                label = labelIncrementer.incrementLabel(label, log.getContent());
            }

            // collect project information
            log.addContent(getProjectPropertiesElement(now));

            setState(ProjectState.BUILDING);
            log.addContent(schedule.build(buildCounter, lastBuild, now, getProjectPropertiesMap(now)).detach());

            boolean buildSuccessful = log.wasBuildSuccessful();
            fireResultEvent(new BuildResultEvent(this, wasLastBuildSuccessful));

            if (!labelIncrementer.isPreBuildIncrementer() && buildSuccessful) {
                label = labelIncrementer.incrementLabel(label, log.getContent());
            }

            setState(ProjectState.MERGING_LOGS);
            log.writeLogFile(now);

            // If we only want to build after a check in, even when broken, set the last build to now,
            // regardless of success or failure (buildAfterFailed = false in config.xml)
            if (!buildAfterFailed) {
                lastBuild = now;
            }

            // If this was a successful build, update both last build and last successful build
            if (buildSuccessful) {
                lastBuild = now;
                lastSuccessfulBuild = now;
                info("build successful");
            } else {
                info("build failed");
            }

            buildCounter++;
            setWasLastBuildSuccessful(buildSuccessful);

            serializeProject();

            publish();
            log.reset();
        } finally {
            setState(ProjectState.IDLE);
        }
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
        try {
            while (!stopped) {
                try {
                    waitIfPaused();
                    waitForNextBuild();
                    setState(ProjectState.QUEUED);
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
                setState(ProjectState.PAUSED);
                pausedMutex.wait(10 * Util.ONE_MINUTE);
            }
        }
    }

    void waitForNextBuild() throws InterruptedException {
        long waitTime = getTimeToNextBuild();
        if (needToWaitForNextBuild(waitTime)) {
            info("next build in " + Util.formatTime(waitTime));
            synchronized (waitMutex) {
                setState(ProjectState.WAITING);
                waitMutex.wait(waitTime);
            }
        }
    }

    private long getTimeToNextBuild() {
        Date now = new Date();
        long waitTime = schedule.getTimeToNextBuild(now, getBuildInterval());
        if (waitTime == 0) {
            // check for the exceptional case that we're dealing with a
            // project that has just built within a minute time
            if (buildStartTime != null) {
                long millisSinceLastBuild = now.getTime() - buildStartTime.getTime();
                if (millisSinceLastBuild < 60000L) {
                    debug("build finished within a minute, getting new time to next build");
                    waitTime = schedule.getTimeToNextBuild(new Date(now.getTime() + 60000L), getBuildInterval());
                }
            }
        }
        return waitTime;
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
        setState(ProjectState.MODIFICATIONSET);
        Element modifications;

        boolean checkNewChangesFirst = checkOnlySinceLastBuild();
        if (checkNewChangesFirst) {
            debug("getting changes since last build");
            modifications = modificationSet.getModifications(lastBuild);
        } else {
            debug("getting changes since last successful build");
            modifications = modificationSet.getModifications(lastSuccessfulBuild);
        }

        if (!modificationSet.isModified()) {
            info("No modifications found, build not necessary.");

            // Sometimes we want to build even though we don't have any
            // modifications. This is in fact current default behaviour.
            // Set by <project buildafterfailed="true/false">
            if (buildAfterFailed && !wasLastBuildSuccessful) {
                info("Building anyway, since buildAfterFailed is true and last build failed.");
            } else {
                if (buildForced) {
                    info("Building anyway, since build was explicitly forced.");
                } else {
                    return null;
                }
            }
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

    public LabelIncrementer getLabelIncrementer() {
        return labelIncrementer;
    }

    public void setLabelIncrementer(LabelIncrementer incrementer) throws CruiseControlException {
        labelIncrementer = incrementer;
        if (label == null) {
            label = labelIncrementer.getDefaultLabel();
        }
        validateLabel(label, labelIncrementer);
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
     *                     yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     *                                input string
     */
    public void setLastBuild(String newLastBuild) throws CruiseControlException {
        lastBuild = parseFormatedTime(newLastBuild, "lastBuild");
    }

    /**
     * @param newLastSuccessfulBuild string containing the build date in the format
     *                               yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     *                                input string
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
        synchronized (pausedMutex) {
            if (isPaused && !paused) {
                pausedMutex.notifyAll();
            }
            isPaused = paused;
        }
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
        info(getStatus());
        notifyListeners(new ProjectStateChangedEvent(name, getState()));
        fireProgressEvent(new BuildProgressEvent(this, getState()));
    }

    public void setBuildQueue(BuildQueue buildQueue) {
        queue = buildQueue;
    }

    public Date getBuildStartTime() {
        return buildStartTime;
    }

    public Log getLog() {
        if (this.log == null) {
            this.log = new Log(getName());
        }
        return this.log;
    }

    /**
     * Initialize the project. Uses ProjectXMLHelper to parse a project file.
     */
    protected void init() throws CruiseControlException {
        if (configFile == null) {
            throw new IllegalStateException("set config file on project before calling init()");
        }
        info("reading settings from config file [" + configFile.getAbsolutePath() + "]");
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, name);

        setListeners(helper.getListeners());
        bootstrappers = helper.getBootstrappers();
        setSchedule(helper.getSchedule());
        log = helper.getLog();
        setModificationSet(helper.getModificationSet());
        setLabelIncrementer(helper.getLabelIncrementer());
        setPublishers(helper.getPublishers());

        setBuildAfterFailed(helper.getBuildAfterFailed());

        if (lastBuild == null) {
            lastBuild = Util.getMidnight();
        }

        if (lastSuccessfulBuild == null) {
            lastSuccessfulBuild = lastBuild;
        }

        if (LOG.isDebugEnabled()) {
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
    }

    protected void setPublishers(List listOfPublishers) {
        publishers = listOfPublishers;
    }

    protected Element getProjectPropertiesElement(Date now) {
        Element infoElement = new Element("info");
        addProperty(infoElement, "projectname", name);
        String lastBuildString = getFormatedTime(lastBuild == null ? now : lastBuild); 
        addProperty(infoElement, "lastbuild", lastBuildString);
        String lastSuccessfulBuildString = getFormatedTime(lastSuccessfulBuild == null ? now : lastSuccessfulBuild);
        addProperty(infoElement, "lastsuccessfulbuild", lastSuccessfulBuildString);
        addProperty(infoElement, "builddate", new SimpleDateFormat(DateFormatFactory.getFormat()).format(now));
        if (now != null) {
            addProperty(infoElement, "cctimestamp", getFormatedTime(now));
        }
        addProperty(infoElement, "label", label);
        addProperty(infoElement, "interval", Long.toString(getBuildInterval() / 1000L));
        addProperty(infoElement, "lastbuildsuccessful", new Boolean(wasLastBuildSuccessful).toString());
        
        return infoElement;
    }

    private void addProperty(Element parent, String key, String value) {
        Element propertyElement = new Element("property");
        propertyElement.setAttribute("name", key);
        propertyElement.setAttribute("value", value);
        parent.addContent(propertyElement);
    }
    
    protected Map getProjectPropertiesMap(Date now) {
        Map buildProperties = new HashMap();
        buildProperties.put("label", label);
        buildProperties.put("cvstimestamp", CVS.formatCVSDate(now));
        buildProperties.put("cctimestamp", getFormatedTime(now));
        buildProperties.put("cclastgoodbuildtimestamp", getLastSuccessfulBuild());
        buildProperties.put("cclastbuildtimestamp", getLastBuild());
        buildProperties.put("lastbuildsuccessful", String.valueOf(isLastBuildSuccessful()));
        if (modificationSet != null) {
            buildProperties.putAll(modificationSet.getProperties());
        }
        return buildProperties;
    }

    /**
     * Iterate over all of the registered <code>Publisher</code>s and call
     * their respective <code>publish</code> methods.
     */
    protected void publish() throws CruiseControlException {
        setState(ProjectState.PUBLISHING);
        for (Iterator i = publishers.iterator(); i.hasNext(); ) {
            Publisher publisher = (Publisher) i.next();
            try {
                publisher.publish(getLog().getContent());
            } catch (CruiseControlException e) {
                StringBuffer message = new StringBuffer("exception publishing results");
                message.append(" with " + publisher.getClass().getName());
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
        setState(ProjectState.BOOTSTRAPPING);
        for (Iterator i = bootstrappers.iterator(); i.hasNext(); ) {
            ((Bootstrapper) i.next()).bootstrap();
        }
    }

    /**
     * Ensure that label is valid for the specified LabelIncrementer
     *
     * @param oldLabel    target label
     * @param incrementer target LabelIncrementer
     * @throws CruiseControlException if label is not valid
     */
    protected void validateLabel(String oldLabel, LabelIncrementer incrementer)
            throws CruiseControlException {
        if (!incrementer.isValidLabel(oldLabel)) {
            final String message = oldLabel + " is not a valid label for labelIncrementer "
                    + incrementer.getClass().getName();
            debug(message);
            throw new CruiseControlException(message);
        }
    }

    public boolean isLastBuildSuccessful() {
        return wasLastBuildSuccessful;
    }

    void setWasLastBuildSuccessful(boolean buildSuccessful) {
        wasLastBuildSuccessful = buildSuccessful;
    }

    public static String getFormatedTime(Date date) {
        if (date == null) {
            return null;
        }
        return FORMATTER.format(date);
    }

    public Date parseFormatedTime(String timeString, String description)
            throws CruiseControlException {

        Date date;
        if (timeString == null) {
            throw new IllegalArgumentException("Null date string for " + description);
        }
        try {
            date = FORMATTER.parse(timeString);
        } catch (ParseException e) {
            LOG.error("Error parsing timestamp for [" + description + "]", e);
            throw new CruiseControlException("Cannot parse string for " + description + ":" + timeString);
        }

        return date;
    }

    /**
     * Logs a message to the application log, not to be confused with the
     * CruiseControl build log.
     */
    private void info(String message) {
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
        setState(ProjectState.IDLE);
    }

    public void stop() {
        LOG.info("Project " + name + " stopping");
        stopped = true;
        setState(ProjectState.STOPPED);
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

    public void addBuildProgressListener(BuildProgressListener listener) {
        synchronized (progressListeners) {
            progressListeners.add(listener);
        }
    }

    protected void fireProgressEvent(BuildProgressEvent event) {
        synchronized (progressListeners) {
            for (Iterator i = progressListeners.iterator(); i.hasNext();) {
                BuildProgressListener listener = (BuildProgressListener) i.next();
                listener.handleBuildProgress(event);
            }
        }
    }

    public void addBuildResultListener(BuildResultListener listener) {
        synchronized (resultListeners) {
            resultListeners.add(listener);
        }
    }

    protected void fireResultEvent(BuildResultEvent event) {
        synchronized (resultListeners) {
            for (Iterator i = resultListeners.iterator(); i.hasNext();) {
                BuildResultListener listener = (BuildResultListener) i.next();
                listener.handleBuildResult(event);
            }
        }
    }

    protected void setListeners(List listeners) {
        this.listeners = listeners;
    }

    void notifyListeners(ProjectEvent event) {
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            Listener listener = (Listener) i.next();
            try {
                listener.handleEvent(event);
            } catch (CruiseControlException e) {
                StringBuffer message = new StringBuffer("exception notifying listener ");
                message.append(listener.getClass().getName());
                message.append(" for project " + name);
                LOG.error(message.toString(), e);
            }
        }
    }
}
