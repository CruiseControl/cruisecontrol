/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;

import net.sourceforge.cruisecontrol.events.BuildProgressEvent;
import net.sourceforge.cruisecontrol.events.BuildProgressListener;
import net.sourceforge.cruisecontrol.events.BuildResultEvent;
import net.sourceforge.cruisecontrol.events.BuildResultListener;
import net.sourceforge.cruisecontrol.jmx.ProjectController;
import net.sourceforge.cruisecontrol.listeners.ProjectStateChangedEvent;
import net.sourceforge.cruisecontrol.util.CVSDateUtil;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Represents a single logical project consisting of source code that needs to
 * be built.  Project is associated with bootstrappers that run before builds
 * and a Schedule that determines when builds occur.
 */
public class Project implements Serializable, Runnable, ProjectQuery {
    private static final long serialVersionUID = 2656877748476842326L;
    private static final Logger LOG = Logger.getLogger(Project.class);

    private transient ProjectState state;

    private transient ProjectConfig projectConfig;
    private transient LabelIncrementer labelIncrementer;

    /**
     * If this attribute is set, then it means that the user has overridden
     * the build interval specified in the Schedule element, probably
     * using the JMX interface.
     */
    private transient Long overrideBuildInterval;

    private transient Date buildStartTime;
    private transient Object pausedMutex;
    private transient Object scheduleMutex;
    private transient Object waitMutex;
    private transient BuildQueue queue;
    private transient List<BuildProgressListener> progressListeners;
    private transient List<BuildResultListener> resultListeners;
    private transient Progress progress;

    private int buildCounter = 0;
    private Date lastBuild = DateUtil.getMidnight();
    private Date lastSuccessfulBuild = lastBuild;
    private boolean wasLastBuildSuccessful = true;
    private String label;
    private String name;
    private transient boolean buildForced = false;
    private String buildTarget = null;
    private boolean isPaused = false;
    private boolean buildAfterFailed = true;
    private boolean stopped = true;
    private boolean forceOnly = false;
    private boolean requiremodification = true;
    private Map<String, String> additionalProperties;

    public Project() {
        initializeTransientFields();
    }

    private void initializeTransientFields() {
        state = ProjectState.STOPPED;

        pausedMutex = new Object();
        scheduleMutex = new Object();
        waitMutex = new Object();
        progressListeners = new ArrayList<BuildProgressListener>();
        resultListeners = new ArrayList<BuildResultListener>();

        progress = new ProgressImpl(this);
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initializeTransientFields();
    }

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
     * @throws CruiseControlException if an error occurs during the build
     */
    protected void build() throws CruiseControlException {
        if (projectConfig == null) {
            throw new IllegalStateException("projectConfig must be set on project before calling build()");
        }

        if (stopped) {
            LOG.warn("not building project " + name + " because project has been stopped.");
            return;
        }

        // If the force only flag is set, only build if forced
        // or if the last build faild and we want to build on failures
        if (forceOnly && !buildForced && !(!wasLastBuildSuccessful && buildAfterFailed)) {
            info("not building because project is forceOnly and build not forced.");
            return;
        }


        final boolean buildWasForced = buildForced;

        try {
            setBuildStartTime(new Date());
            final Schedule schedule = projectConfig.getSchedule();
            if (schedule == null) {
                throw new IllegalStateException("project must have a schedule");
            }
            if (schedule.isPaused(buildStartTime)) {
                // a regularly scheduled paused
                // is different than ProjectState.PAUSED
                return;
            }

            // @todo Add Progress param to Bootstrapper API?
            bootstrap();

            final String target = useAndResetBuildTargetIfBuildWasForced(buildWasForced);

            // @todo Add Progress param to ModificationSet API?
            // getModifications will only return null if we don't need to build
            final Element modifications = getModifications(buildWasForced);

            if (modifications == null) {
                return;
            }

            // Using local reference to avoid NPE if config.xml is updated during build
            final Log buildLog = projectConfig.getLog();

            buildLog.addContent(modifications);

            final Date now;
            if (projectConfig.getModificationSet() != null
                    && projectConfig.getModificationSet().getTimeOfCheck() != null) {

                now = projectConfig.getModificationSet().getTimeOfCheck();
            } else {
                now = new Date();
            }

            if (getLabelIncrementer().isPreBuildIncrementer()) {
                label = getLabelIncrementer().incrementLabel(label, buildLog.getContent());
            }

            // collect project information
            buildLog.addContent(getProjectPropertiesElement(now));

            setState(ProjectState.BUILDING);
            final Element builderLog = schedule.build(buildCounter, lastBuild, now, getProjectPropertiesMap(now),
                    target, progress);

            buildLog.addContent(builderLog.detach());

            boolean buildSuccessful = buildLog.wasBuildSuccessful();
            fireResultEvent(new BuildResultEvent(this, buildSuccessful));

            if (!getLabelIncrementer().isPreBuildIncrementer() && buildSuccessful) {
                label = getLabelIncrementer().incrementLabel(label, buildLog.getContent());
            }

            setState(ProjectState.MERGING_LOGS);
            buildLog.writeLogFile(now);

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

            // also need to reset forced flag before serializing, unless buildForced var is transient
            //resetBuildForcedOnlyIfBuildWasForced(buildWasForced);
            serializeProject();

            // @todo Add Progress param to Publisher API?
            publish(buildLog);
            buildLog.reset();
        } finally {
            resetBuildForcedOnlyIfBuildWasForced(buildWasForced);
            setState(ProjectState.IDLE);
        }
    }

    private String useAndResetBuildTargetIfBuildWasForced(final boolean buildWasForced) {
        String target = null;
        if (buildWasForced) {
            target = buildTarget;
            buildTarget = null;
        }
        return target;
    }

    private void resetBuildForcedOnlyIfBuildWasForced(final boolean buildWasForced) {
        if (buildWasForced) {
            buildForced = false;
        }
    }

    void setBuildStartTime(final Date date) {
        buildStartTime = date;
    }

    public void run() {
        LOG.info("Project " + name + " started");
        try {
            while (!stopped) {
                try {
                    waitIfPaused();
                    if (!stopped) {
                        waitForNextBuild();
                    }
                    if (!stopped) {
                        setState(ProjectState.QUEUED);
                        synchronized (scheduleMutex) {
                            queue.requestBuild(projectConfig);
                            waitForBuildToFinish();
                        }
                    }
                } catch (InterruptedException e) {
                    final String message = "Project " + name + ".run() interrupted";
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
            final String msg = "next build in " + DateUtil.formatTime(waitTime);
            info(msg);
            synchronized (waitMutex) {
                setState(ProjectState.WAITING);
                progress.setValue(msg);
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

    /** @return true if build was forced, intended for unit testing only. */
    boolean isBuildForced() {
        return buildForced;
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

    public void forceBuildWithTarget(String buildTarget, Map<String, String> addedProperties) {
        additionalProperties = addedProperties;
        forceBuildWithTarget(buildTarget);
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
     * Return modifications since the last build.  timeOfCheck will be updated according to the last modification to
     * account for time synchronisation issues.
     *
     * @param buildWasForced true if the build was forced
     * @return Element jdom element containing modification information
     */
    Element getModifications(final boolean buildWasForced) {
        setState(ProjectState.MODIFICATIONSET);

        final ModificationSet modificationSet = projectConfig.getModificationSet();
        if (modificationSet == null) {
            debug("no modification set, nothing to detect.");
            if (buildWasForced) {
                info("no modification set but build was forced");
                return new Element("modifications");
            }
            if (!requiremodification) {
                info("no modification set but no modifications required");
                return new Element("modifications");
            }
            return null;
        }

        final boolean checkNewChangesFirst = checkOnlySinceLastBuild();
        Element modifications;
        if (checkNewChangesFirst) {
            debug("getting changes since last build");
            modifications = modificationSet.retrieveModificationsAsElement(lastBuild, progress);
        } else {
            debug("getting changes since last successful build");
            modifications = modificationSet.retrieveModificationsAsElement(lastSuccessfulBuild, progress);
        }

        if (!modificationSet.isModified()) {
            info("No modifications found, build not necessary.");

            // Sometimes we want to build even though we don't have any
            // modifications:
            //   * last build failed & buildaferfailed="true"
            //   * requiremodifications="false"
            //   * build forced
            if (buildAfterFailed && !wasLastBuildSuccessful) {
                info("Building anyway, since buildAfterFailed is true and last build failed.");
            } else if (!requiremodification) {
                info("Building anyway, since modifications not required");
            } else {
                if (buildWasForced) {
                    info("Building anyway, since build was explicitly forced.");
                } else {
                    return null;
                }
            }
        }

        if (checkNewChangesFirst) {
            debug("new changes found; now getting complete set");
            modifications = modificationSet.retrieveModificationsAsElement(lastSuccessfulBuild, progress);
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

        final long lastBuildLong = lastBuild.getTime();
        final long timeDifference = lastBuildLong - lastSuccessfulBuild.getTime();
        final boolean moreThanASecond = timeDifference > DateUtil.ONE_SECOND;

        return !buildAfterFailed && moreThanASecond;
    }

    /**
     * Serialize the project to allow resumption after a process bounce
     */
    public void serializeProject() {

        final String safeProjectName = Builder.getFileSystemSafeProjectName(name);
        try {
            final ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(safeProjectName + ".ser"));
            try {
                s.writeObject(this);
                s.flush();
                debug("Serializing project to [" + safeProjectName + ".ser]");
            } finally {
                s.close();
            }
        } catch (Exception e) {
            LOG.warn("Error serializing project to [" + safeProjectName + ".ser]: "
                    + e.getMessage(), e);
        }
    }

    public void setLabelIncrementer(final LabelIncrementer incrementer) throws CruiseControlException {
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

    public void setName(final String projectName) {
        name = projectName;
    }

    public String getName() {
        return name;
    }

    public void setLabel(final String newLabel) {
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
    public void setLastBuild(final String newLastBuild) throws CruiseControlException {
        lastBuild = DateUtil.parseFormattedTime(newLastBuild, "lastBuild");
    }

    /**
     * @param newLastSuccessfulBuild string containing the build date in the format
     *                               yyyyMMddHHmmss
     * @throws CruiseControlException if the date cannot be extracted from the
     *                                input string
     */
    public void setLastSuccessfulBuild(final String newLastSuccessfulBuild)
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
     * return the Schedule's interval, otherwise the overridden value will
     * be returned.
     * @return the build interval
     */
    public long getBuildInterval() {
        if (overrideBuildInterval == null) {
            return projectConfig.getSchedule().getInterval();
        } else {
            return overrideBuildInterval;
        }
    }

    /**
     * Sets the build interval that this Project should use. This method
     * overrides the value initially specified in the Schedule attribute.
     * @param sleepMillis the number of milliseconds to sleep between build attempts
     */
    public void overrideBuildInterval(final long sleepMillis) {
        overrideBuildInterval = sleepMillis;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(final boolean paused) {
        synchronized (pausedMutex) {
            if (isPaused && !paused) {
                pausedMutex.notifyAll();
            }
            isPaused = paused;
        }
    }

    public void setBuildAfterFailed(final boolean rebuildEvenWithNoNewModifications) {
        buildAfterFailed = rebuildEvenWithNoNewModifications;
    }

    public String getStatus() {
        return getState().getDescription();
    }

    public String getStatusWithQueuePosition() {
        if (ProjectState.QUEUED.equals(getState())) {
            return getState().getDescription() + " - " + queue.findPosition(projectConfig);
        } else {
            return getState().getDescription();
        }
    }

    public ProjectState getState() {
        return state;
    }

    private void setState(final ProjectState newState) {
        state = newState;
        info(getStatus());
        notifyListeners(new ProjectStateChangedEvent(name, getState()));
        fireProgressEvent(new BuildProgressEvent(this, getState()));
    }

    public void setBuildQueue(final BuildQueue buildQueue) {
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
    protected void init() {
        if (projectConfig == null) {
            throw new IllegalStateException("projectConfig must be set on project before calling init()");
        }

        buildAfterFailed = projectConfig.shouldBuildAfterFailed();
        forceOnly = projectConfig.isForceOnly();
        requiremodification = projectConfig.isRequiremodification();

        if (lastBuild == null) {
            lastBuild = DateUtil.getMidnight();
        }

        if (lastSuccessfulBuild == null) {
            lastSuccessfulBuild = lastBuild;
        }

        if (LOG.isDebugEnabled()) {
            debug("buildInterval          = [" + getBuildInterval() + "]");
            debug("buildForced            = [" + buildForced + "]");
            debug("buildAfterFailed       = [" + buildAfterFailed + "]");
            debug("requireModifcation     = [" + requiremodification + "]");
            debug("forceOnly              = [" + forceOnly + "]");
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

    protected Element getProjectPropertiesElement(final Date now) {
        final Element infoElement = new Element("info");
        addProperty(infoElement, "projectname", name);
        final String lastBuildString = DateUtil.getFormattedTime(lastBuild == null ? now : lastBuild);
        addProperty(infoElement, "lastbuild", lastBuildString);
        final String lastSuccessfulBuildString =
                DateUtil.getFormattedTime(lastSuccessfulBuild == null ? now : lastSuccessfulBuild);
        addProperty(infoElement, "lastsuccessfulbuild", lastSuccessfulBuildString);
        addProperty(infoElement, "builddate", DateUtil.formatIso8601(now));
        addProperty(infoElement, "cctimestamp", DateUtil.getFormattedTime(now));
        addProperty(infoElement, "label", label);
        addProperty(infoElement, "interval", Long.toString(getBuildInterval() / 1000L));
        addProperty(infoElement, "lastbuildsuccessful", String.valueOf(wasLastBuildSuccessful));

        return infoElement;
    }

    private void addProperty(final Element parent, final String key, final String value) {
        final Element propertyElement = new Element("property");
        propertyElement.setAttribute("name", key);
        propertyElement.setAttribute("value", value);
        parent.addContent(propertyElement);
    }

    protected Map<String, String> getProjectPropertiesMap(final Date now) {
        final Map<String, String> buildProperties = new HashMap<String, String>();

        buildProperties.put("projectname", name);

        buildProperties.put("label", label);

        // TODO: Shouldn't have CVS specific properties here
        buildProperties.put("cvstimestamp", CVSDateUtil.formatCVSDate(now));

        buildProperties.put("cctimestamp", DateUtil.getFormattedTime(now));
        buildProperties.put("cclastgoodbuildtimestamp", getLastSuccessfulBuild());
        buildProperties.put("cclastbuildtimestamp", getLastBuild());
        buildProperties.put("lastbuildsuccessful", String.valueOf(isLastBuildSuccessful()));
        buildProperties.put("buildforced", String.valueOf(getBuildForced()));
        if (projectConfig.getModificationSet() != null) {
            buildProperties.putAll(projectConfig.getModificationSet().getProperties());
        }
        if (additionalProperties != null && !additionalProperties.isEmpty()) {
            buildProperties.putAll(additionalProperties);
            additionalProperties.clear();
            additionalProperties = null;
        }
        return buildProperties;
    }

    /**
     * Intended only for unit testing.
     * @return additional Properties variable.
     */
    Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * Iterate over all of the registered <code>Publisher</code>s and call
     * their respective <code>publish</code> methods.
     * @param buildLog the content to publish
     * @throws CruiseControlException if an error occurs during publishing
     */
    protected void publish(final Log buildLog) throws CruiseControlException {
        setState(ProjectState.PUBLISHING);
        for (final Publisher publisher : projectConfig.getPublishers()) {
            // catch all errors, Publishers shouldn't cause failures in the build method
            try {
                publisher.publish(buildLog.getContent());
            } catch (Throwable t) {
                final StringBuilder message = new StringBuilder("exception publishing results");
                message.append(" with ").append(publisher.getClass().getName());
                message.append(" for project ").append(name);
                LOG.error(message.toString(), t);
            }
        }
    }

    /**
     * Iterate over all of the registered <code>Bootstrapper</code>s and call
     * their respective <code>bootstrap</code> methods.
     * @throws CruiseControlException if an error occurs during bootstrapping
     */
    protected void bootstrap() throws CruiseControlException {
        setState(ProjectState.BOOTSTRAPPING);
        for (final Bootstrapper bootstrapper : projectConfig.getBootstrappers()) {
            bootstrapper.bootstrap();
        }
    }

    /**
     * Ensure that label is valid for the specified LabelIncrementer
     *
     * @param oldLabel    target label
     * @param incrementer target LabelIncrementer
     * @throws CruiseControlException if label is not valid
     */
    protected void validateLabel(final String oldLabel, final LabelIncrementer incrementer)
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

    void setWasLastBuildSuccessful(final boolean buildSuccessful) {
        wasLastBuildSuccessful = buildSuccessful;
    }

    /**
     * Logs a message to the application log, not to be confused with the
     * CruiseControl build log.
     * @param message the application message to log
     */
    private void warn(final String message) {
        LOG.warn("Project " + name + ":  " + message);
    }

    private void info(final String message) {
        LOG.info("Project " + name + ":  " + message);
    }

    private void debug(final String message) {
        LOG.debug("Project " + name + ":  " + message);
    }

    public void start() {
        if (stopped || getState() == ProjectState.STOPPED) {
            stopped = false;
            LOG.info("Project " + name + " starting");
            setState(ProjectState.IDLE);
            createNewSchedulingThread();
        }
    }

    protected void createNewSchedulingThread() {
        final Thread projectSchedulingThread = new Thread(this, "Project " + getName() + " thread");
        projectSchedulingThread.start();

        // brief nap to allow thread to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            LOG.warn("interrupted while waiting for scheduling thread to start", ie);
        }
    }

    public void stop() {
        LOG.info("Project " + name + " stopping");
        stopped = true;
        setState(ProjectState.STOPPED);
        synchronized (pausedMutex) {
            pausedMutex.notifyAll();
        }
        synchronized (waitMutex) {
            waitMutex.notifyAll();
        }
        synchronized (scheduleMutex) {
            scheduleMutex.notifyAll();
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("Project ");
        sb.append(getName());
        sb.append(": ");
        sb.append(getStatus());
        if (isPaused) {
            sb.append(" (paused)");
        }
        return sb.toString();
    }

    public void addBuildProgressListener(final BuildProgressListener listener) {
        synchronized (progressListeners) {
            progressListeners.add(listener);
        }
    }

    protected void fireProgressEvent(final BuildProgressEvent event) {
        synchronized (progressListeners) {
            for (final BuildProgressListener listener : progressListeners) {
                listener.handleBuildProgress(event);
            }
        }
    }

    public void addBuildResultListener(final BuildResultListener listener) {
        synchronized (resultListeners) {
            resultListeners.add(listener);
        }
    }

    protected void fireResultEvent(final BuildResultEvent event) {
        synchronized (resultListeners) {
            for (final BuildResultListener listener : resultListeners) {
                listener.handleBuildResult(event);
            }
        }
    }

    List<Listener> getListeners() {
        return projectConfig.getListeners();
    }

    public void setProjectConfig(final ProjectConfig projectConfig) throws CruiseControlException {
        if (projectConfig == null) {
            throw new IllegalArgumentException("project config can't be null");
        }
        this.projectConfig = projectConfig;
        setLabelIncrementer(projectConfig.getLabelIncrementer());
    }

    void notifyListeners(final ProjectEvent event) {
        if (projectConfig == null) {
            throw new IllegalStateException("projectConfig is null");
        }

        for (final Listener listener : projectConfig.getListeners()) {
            try {
                listener.handleEvent(event);
            } catch (CruiseControlException e) {
                final StringBuilder message = new StringBuilder("exception notifying listener ");
                message.append(listener.getClass().getName());
                message.append(" for project ").append(name);
                LOG.error(message.toString(), e);
            }
        }
    }

    public boolean equals(final Object arg0) {
        if (arg0 == null) {
            return false;
        }

        if (arg0.getClass().getName().equals(getClass().getName())) {
            final Project thatProject = (Project) arg0;
            return thatProject.name.equals(name);
        }

        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public void register(final MBeanServer server) throws JMException {
        LOG.debug("Registering project mbean");
        final ProjectController projectController = new ProjectController(this);
        projectController.register(server);
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public Date getLastBuildDate() {
        return lastBuild;
    }

    public Progress getProgress() {
        return progress;
    }

    public List<String> getLogLabels() {
        return projectConfig.getLogLabels();
    }

    public String[] getLogLabelLines(final String logLabel, final int firstLine) {
        return projectConfig.getLogLabelLines(logLabel, firstLine);
    }

    @Override
    public Map<String, String> getProperties() {
        return projectConfig.getProperties();
    }

    @Override
    public List<Modification> modificationsSinceLastBuild() {
        final ModificationSet modificationSet = projectConfig.getModificationSet();

        // modificationSet can be null when no modification set is set
        if (modificationSet == null) {
            warn("No modification set got from " + projectConfig.getName() + ", pretending 'not-modified status'");
            return Collections.emptyList();
        }
        
        info("Getting changes since last successful build");
        modificationSet.retrieveModificationsAsElement(lastSuccessfulBuild, progress);
        return modificationSet.getCurrentModifications();
    }

    @Override
    public List<Modification> modificationsSince(final Date since) {
        final List<Modification> modifications = new ArrayList<Modification>();
        final String logDirectory = getLog().getLogDir();
        final List<String> logs = getLog().getLogLabels();

        // The log directory was not set, print warning and return empty list
        if (logDirectory == null) {
            LOG.warn("Unable to get modificatiosn since " + since + " as the project[" + getName()
                    + "] has no <log /> configured");
            return modifications;
        }

        // Read all the build-successful log files since the given time
        try {
            for (final String logName : logs) {
                // Skip those not successful and those too old
                if (!Log.wasSuccessfulBuild(logName) || Log.parseDateFromLogFileName(logName).before(since)) {
                    continue;
                }
                // Read the XML
                final Element logData = Util.loadRootElement(new File(logDirectory, logName));
                final XMLLogHelper logElem = new XMLLogHelper(logData);
                modifications.addAll(logElem.getModifications());
            }
        } catch (Exception e) {
            LOG.error("Error checking for modifications", e);
        }

        return modifications;
    }

    @Override
    public Date successLastBuild() {
        return buildCounter == 0 ? new Date(0) : lastSuccessfulBuild;
    }

    @Override
    public String successLastLabel() {
        return buildCounter == 0 ? "" : label;
    }

    @Override
    public String successLastLog() {
        final List<String> labels = getLogLabels();
        final String latest = successLastLabel();

        for (String l : labels) {
             if (latest.equals(Log.parseLabelFromLogFileName(l))) {
                 return l;
             }
        }
        // Not build yet
        return "";
    }

}
