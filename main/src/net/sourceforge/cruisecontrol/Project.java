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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.events.BuildProgressEvent;
import net.sourceforge.cruisecontrol.events.BuildProgressListener;
import net.sourceforge.cruisecontrol.events.BuildResultEvent;
import net.sourceforge.cruisecontrol.events.BuildResultListener;
import net.sourceforge.cruisecontrol.listeners.ProjectStateChangedEvent;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Represents a single logical project consisting of source code that needs to
 * be built.  Project is associated with bootstrappers that run before builds
 * and a Schedule that determines when builds occur.
 */
public class Project implements Serializable, Runnable {
    static final long serialVersionUID = 2656877748476842326L;
    private static final Logger LOG = Logger.getLogger(Project.class);

    private transient ProjectState state;

    private transient ProjectConfig projectConfig;
    private transient LabelIncrementer labelIncrementer;

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

    private int buildCounter = 0;
    private Date lastBuild = DateUtil.getMidnight();
    private Date lastSuccessfulBuild = lastBuild;
    private boolean wasLastBuildSuccessful = true;
    private String label;
    private String name;
    private boolean buildForced = false;
    private String buildTarget = null;
    private boolean isPaused = false;
    private boolean buildAfterFailed = true;
    private transient Thread projectSchedulingThread;
    private boolean stopped = true;

    public Project() {
        initializeTransientFields();
    }

    private void initializeTransientFields() {
        state = ProjectState.STOPPED;

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
        if (stopped) {
            LOG.warn("not building project " + name + " because project has been stopped.");
            buildFinished();
            return;            
        }
        
        synchronized (pausedMutex) {
            if (isPaused) {
                LOG.info("not building project " + name + " because project has been paused.");
                buildFinished();
                return;
            }
        }

        try {
            init();
            build();
        } catch (CruiseControlException e) {
            LOG.error("exception attempting build in project " + name, e);
        } finally {
            buildFinished();
        }
    }

    /**
     * Unless paused, runs any bootstrappers and then the entire build.
     */
    protected void build() throws CruiseControlException {
        if (projectConfig == null) {
            throw new IllegalStateException("projectConfig must be set on project before calling build()");
        }
        
        if (stopped) {
            LOG.warn("not building project " + name + " because project has been stopped.");
            return;
        }
        
        try {
            setBuildStartTime(new Date());
            Schedule schedule = projectConfig.getSchedule();
            if (schedule == null) {
                throw new IllegalStateException("project must have a schedule");
            }
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

            projectConfig.getLog().addContent(modifications);

            Date now = new Date();
            if (projectConfig.getModificationSet() != null) {
                now = projectConfig.getModificationSet().getTimeOfCheck();
            }

            if (getLabelIncrementer().isPreBuildIncrementer()) {
                label = getLabelIncrementer().incrementLabel(label, projectConfig.getLog().getContent());
            }

            // collect project information
            projectConfig.getLog().addContent(getProjectPropertiesElement(now));

            setState(ProjectState.BUILDING);
            Element buildLog = schedule.build(buildCounter, lastBuild, now, getProjectPropertiesMap(now));
            projectConfig.getLog().addContent(buildLog.detach());

            boolean buildSuccessful = projectConfig.getLog().wasBuildSuccessful();
            fireResultEvent(new BuildResultEvent(this, buildSuccessful));

            if (!getLabelIncrementer().isPreBuildIncrementer() && buildSuccessful) {
                label = getLabelIncrementer().incrementLabel(label, projectConfig.getLog().getContent());
            }

            setState(ProjectState.MERGING_LOGS);
            projectConfig.getLog().writeLogFile(now);

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
            projectConfig.getLog().reset();
        } finally {
            setState(ProjectState.IDLE);
        }
    }

    void setBuildStartTime(Date date) {
        buildStartTime = date;
    }

    public void run() {
        LOG.info("Project " + name + " started");
        try {
            while (!stopped) {
                try {
                    waitIfPaused();
                    waitForNextBuild();
                    if (!stopped) {
                        setState(ProjectState.QUEUED);
                        synchronized (scheduleMutex) {
                            queue.requestBuild(this);
                            waitForBuildToFinish();
                        }
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
                pausedMutex.wait(10 * DateUtil.ONE_MINUTE);
            }
        }
    }

    void waitForNextBuild() throws InterruptedException {
        long waitTime = getTimeToNextBuild(new Date());
        if (needToWaitForNextBuild(waitTime) && !buildForced) {
            info("next build in " + DateUtil.formatTime(waitTime));
            synchronized (waitMutex) {
                setState(ProjectState.WAITING);
                waitMutex.wait(waitTime);
            }
        }
    }

    long getTimeToNextBuild(Date now) {
        long waitTime = projectConfig.getSchedule().getTimeToNextBuild(now, getBuildInterval());
        if (waitTime == 0) {
            // check for the exceptional case that we're dealing with a
            // project that has just built within a minute time
            if (buildStartTime != null) {
                long millisSinceLastBuild = now.getTime() - buildStartTime.getTime();
                if (millisSinceLastBuild < Schedule.ONE_MINUTE) {
                    debug("build finished within a minute, getting new time to next build");
                    Date oneMinuteInFuture = new Date(now.getTime() + Schedule.ONE_MINUTE);
                    waitTime = projectConfig.getSchedule().getTimeToNextBuild(oneMinuteInFuture, getBuildInterval());
                    waitTime += Schedule.ONE_MINUTE;
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
    
    public void forceBuildWithTarget(String buildTarget) {
        this.buildTarget = buildTarget;
        setBuildForced(true);
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

        ModificationSet modificationSet = projectConfig.getModificationSet();
        if (modificationSet == null) {
            debug("no modification set, nothing to detect.");
            if (buildForced) {
                info("no modification set but build was forced");
                return new Element("modifications");
            }
            return null;
        }
        
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
        boolean moreThanASecond = timeDifference > DateUtil.ONE_SECOND;

        boolean checkNewMods = !buildAfterFailed && moreThanASecond;

        return checkNewMods;
    }

    /**
     * Serialize the project to allow resumption after a process bounce
     */
    public void serializeProject() {
        ObjectOutputStream s = null;
        try {
            s = new ObjectOutputStream(new FileOutputStream(name + ".ser"));
            s.writeObject(this);
            s.flush();
            debug("Serializing project to [" + name + ".ser]");
        } catch (Exception e) {
            LOG.warn("Error serializing project to [" + name + ".ser]: "
                    + e.getMessage(), e);
        } finally {
           if (s != null) {
               try {
                   s.close();
               } catch (Exception ignore) {
               }
           }
        }
    }

    public void setLabelIncrementer(LabelIncrementer incrementer) throws CruiseControlException {
        if (incrementer == null) {
            throw new IllegalArgumentException("label incrementer can't be null");
        }
        labelIncrementer = incrementer;
        if (label == null) {
            label = labelIncrementer.getDefaultLabel();
        }
        validateLabel(label, labelIncrementer);
    }

    public LabelIncrementer getLabelIncrementer() {
        return labelIncrementer;
    }

    /** deprecated */
    public void setLogXmlEncoding(String encoding) {
        projectConfig.getLog().setEncoding(encoding);
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
        lastBuild = DateUtil.parseFormattedTime(newLastBuild, "lastBuild");
    }

    /**
     * @param newLastSuccessfulBuild string containing the build date in the format
     *                               yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     *                                input string
     */
    public void setLastSuccessfulBuild(String newLastSuccessfulBuild)
            throws CruiseControlException {
        lastSuccessfulBuild = DateUtil.parseFormattedTime(newLastSuccessfulBuild, "lastSuccessfulBuild");
    }

    public String getLastBuild() {
        if (lastBuild == null) {
            return null;
        }
        return DateUtil.getFormattedTime(lastBuild);
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
        return DateUtil.getFormattedTime(lastSuccessfulBuild);
    }

    public String getLogDir() {
        return projectConfig.getLog().getLogDir();
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
            return projectConfig.getSchedule().getInterval();
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

    public String getBuildStartTime() {
        return DateUtil.getFormattedTime(buildStartTime);
    }

    public Log getLog() {
        return this.projectConfig.getLog();
    }

    /**
     * Initialize the project. Uses ProjectXMLHelper to parse a project file.
     */
    protected void init() throws CruiseControlException {
        if (projectConfig == null) {
            throw new IllegalStateException("projectConfig must be set on project before calling init()");
        }
        
        if (buildTarget != null) {
            // tell the helper to set the given buildTarget
            // on all Builders, just for this run (so we need to reset it)
            info("Overriding build target with \"" + buildTarget + "\"");
            projectConfig.getSchedule().overrideTargets(buildTarget);
            buildTarget = null;
        }

        if (lastBuild == null) {
            lastBuild = DateUtil.getMidnight();
        }

        if (lastSuccessfulBuild == null) {
            lastSuccessfulBuild = lastBuild;
        }

        // should we move this to build? This singleton thingy is scaring me..
        setDateFormat(projectConfig.getDateFormat());

        if (LOG.isDebugEnabled()) {
            debug("buildInterval          = [" + getBuildInterval() + "]");
            debug("buildForced            = [" + buildForced + "]");
            debug("buildAfterFailed       = [" + buildAfterFailed + "]");
            debug("buildCounter           = [" + buildCounter + "]");
            debug("isPaused               = [" + isPaused + "]");
            debug("label                  = [" + label + "]");
            debug("lastBuild              = [" + DateUtil.getFormattedTime(lastBuild) + "]");
            debug("lastSuccessfulBuild    = [" + DateUtil.getFormattedTime(lastSuccessfulBuild) + "]");
            debug("logDir                 = [" + projectConfig.getLog().getLogDir() + "]");
            debug("logXmlEncoding         = [" + projectConfig.getLog().getLogXmlEncoding() + "]");
            debug("wasLastBuildSuccessful = [" + wasLastBuildSuccessful + "]");
        }
    }

    private void setDateFormat(CCDateFormat dateFormat) {
        if (dateFormat != null && dateFormat.getFormat() != null) {
            DateFormatFactory.setFormat(dateFormat.getFormat());
        }
    }

    protected Element getProjectPropertiesElement(Date now) {
        Element infoElement = new Element("info");
        addProperty(infoElement, "projectname", name);
        String lastBuildString = DateUtil.getFormattedTime(lastBuild == null ? now : lastBuild); 
        addProperty(infoElement, "lastbuild", lastBuildString);
        String lastSuccessfulBuildString = 
                DateUtil.getFormattedTime(lastSuccessfulBuild == null ? now : lastSuccessfulBuild);
        addProperty(infoElement, "lastsuccessfulbuild", lastSuccessfulBuildString);
        addProperty(infoElement, "builddate", DateFormatFactory.getDateFormat().format(now));
        if (now != null) {
            addProperty(infoElement, "cctimestamp", DateUtil.getFormattedTime(now));
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
        buildProperties.put("cvstimestamp", DateUtil.formatCVSDate(now));
        buildProperties.put("cctimestamp", DateUtil.getFormattedTime(now));
        buildProperties.put("cclastgoodbuildtimestamp", getLastSuccessfulBuild());
        buildProperties.put("cclastbuildtimestamp", getLastBuild());
        buildProperties.put("lastbuildsuccessful", String.valueOf(isLastBuildSuccessful()));
        if (projectConfig.getModificationSet() != null) {
            buildProperties.putAll(projectConfig.getModificationSet().getProperties());
        }
        return buildProperties;
    }

    /**
     * Iterate over all of the registered <code>Publisher</code>s and call
     * their respective <code>publish</code> methods.
     */
    protected void publish() throws CruiseControlException {
        setState(ProjectState.PUBLISHING);
        for (Iterator i = projectConfig.getPublishers().iterator(); i.hasNext(); ) {
            Publisher publisher = (Publisher) i.next();
            // catch all errors, Publishers shouldn't cause failures in the build method
            try {
                publisher.publish(projectConfig.getLog().getContent());
            } catch (Throwable t) {
                StringBuffer message = new StringBuffer("exception publishing results");
                message.append(" with " + publisher.getClass().getName());
                message.append(" for project " + name);
                LOG.error(message.toString(), t);
            }
        }
    }

    /**
     * Iterate over all of the registered <code>Bootstrapper</code>s and call
     * their respective <code>bootstrap</code> methods.
     */
    protected void bootstrap() throws CruiseControlException {
        setState(ProjectState.BOOTSTRAPPING);
        for (Iterator i = projectConfig.getBootstrappers().iterator(); i.hasNext(); ) {
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
        if (stopped || getState() == ProjectState.STOPPED) {
            stopped = false;
            createNewSchedulingThread();
            LOG.info("Project " + name + " starting");
            setState(ProjectState.IDLE);
        }
    }

    protected void createNewSchedulingThread() {
        projectSchedulingThread = new Thread(this, "Project " + getName() + " thread");
        projectSchedulingThread.start();
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

    List getListeners() {
        return projectConfig.getListeners();
    }

    public void setProjectConfig(ProjectConfig projectConfig) throws CruiseControlException {
        if (projectConfig == null) {
            throw new IllegalArgumentException("project config can't be null");
        }
        this.projectConfig = projectConfig;
        setLabelIncrementer(projectConfig.getLabelIncrementer());
    }

    void notifyListeners(ProjectEvent event) {
        if (projectConfig == null) {
            throw new IllegalStateException("projectConfig is null");
        }
        
        for (Iterator i = projectConfig.getListeners().iterator(); i.hasNext(); ) {
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

    public boolean equals(Object arg0) {
        if (arg0 == null) {
            return false;
        }

        if (arg0.getClass().getName().equals(getClass().getName())) {
            Project thatProject = (Project) arg0;
            return thatProject.name.equals(name);
        }

        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }

}
