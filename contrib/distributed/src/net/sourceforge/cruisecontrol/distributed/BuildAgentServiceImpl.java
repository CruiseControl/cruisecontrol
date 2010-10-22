/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sourceforge.cruisecontrol.BuildOutputLoggerManager;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.AntScript;
import net.sourceforge.cruisecontrol.builders.CompositeBuilder;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.ZipUtil;
import net.sourceforge.cruisecontrol.distributed.core.FileUtil;
import net.sourceforge.cruisecontrol.distributed.core.CCDistVersion;
import net.sourceforge.cruisecontrol.distributed.core.ProgressRemote;
import net.sourceforge.cruisecontrol.distributed.core.RemoteResult;
import net.sourceforge.cruisecontrol.distributed.core.jnlputil.AntProgressLoggerInstaller;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.jdom.Element;

import javax.jnlp.ServiceManager;
import javax.jnlp.BasicService;
import javax.jnlp.UnavailableServiceException;

/**
 * Build Agent implementation.
 */
public class BuildAgentServiceImpl implements BuildAgentService {

    private static final Logger LOG = Logger.getLogger(BuildAgentServiceImpl.class);

    private static final long serialVersionUID = 2116738757011580074L;

    private static final String CRUISE_BUILD_DIR = "cruise.build.dir";

    static final String DEFAULT_AGENT_PROPERTIES_FILE = "agent.properties";
    private String agentPropertiesFilename = DEFAULT_AGENT_PROPERTIES_FILE;

    static final String DEFAULT_USER_DEFINED_PROPERTIES_FILE = "user-defined.properties";

    /**
     * The default number of milliseconds a restart() or kill() should delay before executing
     * in order to allow remote calls to complete, and thereby allow successful builds
     * to complete on the Distributed Master.
     */
    private static final int DEFAULT_DELAY_MS_KILLRESTART = 5 * 1000;
    /** Name of system property who's value, if defined, will override the default delay. */
    static final String SYSPROP_CCDIST_DELAY_MS_KILLRESTART = "cc.dist.delayMSKillRestart";

    /** Cache host name. */
    private final String machineName;

    private final Date dateStarted;
    private boolean isBusy;
    private Date dateClaimed;
    private boolean isPendingKill;
    private Date pendingKillSince;
    private boolean isPendingRestart;
    private Date pendingRestartSince;

    private Properties configProperties;

    private String projectName;
    private ProgressRemote buildProgressRemote;
    private final Map<String, String> distributedAgentProps = new HashMap<String, String>();

    private File logDir;
    private File outputDir;
    private RemoteResult[] remoteResults;
    private File buildRootDir;
    private File zippedLogs;
    private File zippedOutput;

    private final List<BuildAgent.AgentStatusListener> agentStatusListeners
            = new ArrayList<BuildAgent.AgentStatusListener>();

    static final String LOGMSGPREFIX_PREFIX = "Agent Host: ";
    private final String logMsgPrefix;
    /**
     * Prepends Agent machine name to error message. This is especially
     * useful when combined with an "email logger" config for Log4j using a modified
     * log4j.properties on build agents. For example:
     * <pre>
     * log4j.rootCategory=INFO,A1,FILE,Mail
     *
     * ...
     *
     * # Mail is set to be a SMTPAppender
     * log4j.appender.Mail=org.apache.log4j.net.SMTPAppender
     * log4j.appender.Mail.BufferSize=100
     * log4j.appender.Mail.From=ccbuild@yourdomain.com
     * log4j.appender.Mail.SMTPHost=yoursmtp.mailhost.com
     * log4j.appender.Mail.Subject=CC has had an error!!!
     * log4j.appender.Mail.To=youremail@yourdomain.com
     * log4j.appender.Mail.layout=org.apache.log4j.PatternLayout
     * log4j.appender.Mail.layout.ConversionPattern=%d{dd.MM.yyyy HH:mm:ss} %-5p [%x] [%c{3}] %m%n
     *
     * </pre>
     *
     * @param message  the message to log (will be prefixed with machineName).
     */
    private void logPrefixDebug(final Object message) {
        LOG.debug(logMsgPrefix + message);
    }
    private void logPrefixInfo(final Object message) {
        LOG.info(logMsgPrefix + message);
    }
    private void logPrefixError(final Object message) {
        LOG.error(logMsgPrefix + message);
    }
    private void logPrefixError(final Object message, final Throwable throwable) {
        LOG.error(logMsgPrefix + message, throwable);
    }


    private final transient BuildAgent serviceContainer;

    /**
     * Constructor. 
     * @param serviceContainer the BuildAgent instance in charge of publishing this service.
     */
    BuildAgentServiceImpl(final BuildAgent serviceContainer) {
        dateStarted = new Date();

        this.serviceContainer = serviceContainer;

        try {
            machineName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            final String message = "Failed to get hostname";
            // Don't call Log helper method here since hostname is not yet set
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
        logMsgPrefix = LOGMSGPREFIX_PREFIX + machineName + "; ";
    }

    /** @return the date this Build Agent started running (not when a specific build started). */
    public Date getDateStarted() {
        return dateStarted;
    }

    /**
     * @return the project being built now, or null if no project is being built.
     */
    public String getProjectName() {
        return projectName;
    }

    void setAgentPropertiesFilename(final String filename) {
        agentPropertiesFilename = filename;
    }
    private String getAgentPropertiesFilename() {
        return agentPropertiesFilename;
    }

    private transient DelayedAction lastDelayedAction;
    DelayedAction getLastDelayedAction() { return lastDelayedAction; }
    private void setLastDelayedAction(DelayedAction lastDelayedAction) { this.lastDelayedAction = lastDelayedAction; }

    /**
     * Executes the {@link #execAction()} method after a fixed delay has expired.
     */
    abstract static class DelayedAction implements Runnable {
        /** Allow unit test to be notified when delayed action completes. */
        static interface FinishedListener {
            public void finished(final DelayedAction delayedAction);
        }

        static final class Type {
            public static final Type RESTART = new Type("restart");
            public static final Type KILL = new Type("kill");

            private final String name;

            private Type(final String name) { this.name = name; }

            public String toString() { return name; }
        }

        private Throwable thrown;
        private final int delay;
        private final Type type;
        private boolean isFinished;
        private final Thread executingThread;

        /** Allow unit test to be notified when delayed action completes. */
        private FinishedListener finishedListener;

        DelayedAction(final Type type) {
            delay = Integer.getInteger(
                    SYSPROP_CCDIST_DELAY_MS_KILLRESTART, DEFAULT_DELAY_MS_KILLRESTART);
            this.type = type;
            this.executingThread = new Thread(this, "DelayedActionThread, type: " + type.toString());
            this.executingThread.start();
        }

        public final void run() {
            try {
                LOG.info("Executing Agent " + type + " in " + delay + " milliseconds...");
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // ignore
            }

            try {
                execAction();
            } catch (Throwable t) {
                thrown = t;
                LOG.error("Error executing delayed action.", t);
            } finally {
                isFinished = true;
                if (finishedListener != null) {
                    finishedListener.finished(this);
                }
            }
        }

        void setFinishedListener(final FinishedListener finishedListener) {
            this.finishedListener = finishedListener;
        }

        public Throwable getThrown() { return thrown; }

        public Type getType() { return type; }

        public boolean isFinished() { return isFinished; }
        /**
         * Implement in order to run the desired Action
         */
        public abstract void execAction();
    }


    private final String busyLock = "busyLock";
    void setBusy(final boolean newIsBusy) {
        if (!newIsBusy) { // means the claim is being released

            if (isPendingRestart()) {
                // restart after delay to allow in progress remote calls to finish
                setLastDelayedAction(new DelayedAction(DelayedAction.Type.RESTART) {
                    public void execAction() {
                        doRestart();
                    }
                });
                // do NOT reset busy state, since action is pending
                return;
            } else if (isPendingKill()) {
                // kill now after delay to allow in progress remote calls to finish
                setLastDelayedAction(new DelayedAction(DelayedAction.Type.KILL) {
                    public void execAction() {
                        doKill();
                    }
                });
                // do NOT reset busy state, since action is pending
                return;
            }

            // clear out distributed build agent props
            distributedAgentProps.clear();

            // clear project name
            projectName = null;

            dateClaimed = null;
        } else {
            dateClaimed = new Date();
        }

        synchronized (busyLock) {
            isBusy = newIsBusy;
        }

        fireAgentStatusChanged();

        logPrefixInfo("agent busy status changed to: " + newIsBusy);
    }

    public Element doBuild(final Builder nestedBuilder, final Map<String, String> projectPropertiesMap,
                           final Map<String, String> distributedAgentProperties, final ProgressRemote progressRemote,
                           final RemoteResult[] remoteResults)
            throws RemoteException {

        synchronized (busyLock) {
            if (!isBusy()) {    // only reclaim if needed, since it resets the dateClaimed.
                setBusy(true); // we could remove this, since claim() is called during lookup...
            }
        }

        projectName = projectPropertiesMap.get(PropertiesHelper.PROJECT_NAME);
        if (null == projectName) {
            throw new RemoteException("Missing required property: " + PropertiesHelper.PROJECT_NAME
                    + " in projectProperties");
        }

        final Level origLogLevel = Logger.getRootLogger().getLevel();
        final boolean isDebugBuild = Boolean.valueOf(
                distributedAgentProperties.get(PropertiesHelper.DISTRIBUTED_AGENT_DEBUG));
        boolean isDebugOverriden = false;
        try {
            // Override log level if needed
            if (isDebugBuild && !LOG.isDebugEnabled()) {
                LOG.info("Switching Agent log level to Debug for build.");
                Logger.getRootLogger().setLevel(Level.DEBUG);
                isDebugOverriden = true;
            }

            buildProgressRemote = progressRemote;

            logPrefixDebug("Build Agent Props: " + distributedAgentProperties.toString());
            distributedAgentProps.putAll(distributedAgentProperties);


            this.remoteResults = remoteResults;

            String remoteResultsMsg = "";
            if (remoteResults != null) {
                for (final RemoteResult remoteResult : remoteResults) {
                    remoteResultsMsg += "\n\tRemoteResult: " + remoteResult.getAgentDir().getAbsolutePath();
                }
            }
            final String infoMessage = "Building project: " + projectName
                    + "\n\tAgentLogDir: " + distributedAgentProps.get(
                            PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR)
                    + "\n\tAgentOutputDir: " + distributedAgentProps.get(
                            PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR)
                    + remoteResultsMsg;

            logPrefixInfo(infoMessage);

            logPrefixDebug("Build Agent Project Props: " + projectPropertiesMap.toString());

            // this is done only to update agent UI info regarding ProjectName - which isn't available
            // until projectPropertiesMap has been set.
            fireAgentStatusChanged();


            configProperties = (Properties) PropertiesHelper.loadRequiredProperties(
                    getAgentPropertiesFilename());

            if (progressRemote != null) {
                progressRemote.setValueRemote("validating remote builder");
                fireAgentStatusChanged(); // update UI
            }

            // must do Ant Logger Lib injection before validation
            injectAntProgressLoggerLibIfNeeded(nestedBuilder);

            try {
                nestedBuilder.validate();
            } catch (CruiseControlException e) {
                final String message = "Failed to validate nested Builder on agent";
                logPrefixError(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RemoteException(message, e);
            }

            final String overrideTarget = distributedAgentProps.get(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET);

            // wrap progressRemote with a local progress
            final Progress progressLocal;
            if (progressRemote != null) {
                progressLocal = new WrappedRemoteProgress(progressRemote);
            } else {
                progressLocal = null;
            }

            if (progressRemote != null) {
                progressRemote.setValueRemote("running remote builder");
                fireAgentStatusChanged(); // update UI
            }
            final long startTime = System.currentTimeMillis();
            final Element buildResults;
            try {
                if (overrideTarget == null) {
                    buildResults = nestedBuilder.build(projectPropertiesMap, progressLocal);
                } else {
                    buildResults = nestedBuilder.buildWithTarget(projectPropertiesMap, overrideTarget, progressLocal);
                }
            } catch (CruiseControlException e) {
                final String message = "Failed to complete build on agent";
                logPrefixError(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RemoteException(message, e);
            }
            // add agent builder info to build log
            CompositeBuilder.insertBuildLogHeader(buildResults,
                    logMsgPrefix + nestedBuilder.getClass().getName() + "; agent",
                    startTime, "agent", "agent-childbuilder");

            if (progressRemote != null) {
                progressRemote.setValueRemote("preparing results");
                fireAgentStatusChanged(); // update UI
            }
            prepareLogsAndArtifacts();
            return buildResults;
        } catch (RemoteException e) {
            logPrefixError("doBuild threw exception, setting busy to false.");
            setBusy(false);
            throw e; // rethrow original exception
        } finally {
            buildProgressRemote = null;

            // restore original log level if overriden
            if (isDebugOverriden) {
                Logger.getRootLogger().setLevel(origLogLevel);
                LOG.info("Restored Agent log level to: " + origLogLevel);
            }
        }
    }

    
    private final class WrappedRemoteProgress implements Progress {

        private static final long serialVersionUID = -5980080533166620643L;

        private final ProgressRemote progressRemote;

        private WrappedRemoteProgress(final ProgressRemote progressRemote) {
            this.progressRemote = progressRemote;
        }

        public void setValue(String value) {
            try {
                progressRemote.setValueRemote(value);
                fireAgentStatusChanged(); // update UI
            } catch (RemoteException e) {
                throw new RuntimeException("Error setting progress", e);
            }
        }

        public String getValue() {
            try {
                return progressRemote.getValueRemote();
            } catch (RemoteException e) {
                throw new RuntimeException("Error getting progress", e);
            }
        }

        public Date getLastUpdated() {
            try {
                return progressRemote.getLastUpdatedRemote();
            } catch (RemoteException e) {
                throw new RuntimeException("Error getting progress", e);
            }
        }

        public String getText() {
            try {
                return progressRemote.getTextRemote();
            } catch (RemoteException e) {
                throw new RuntimeException("Error getting progress", e);
            }
        }
    }


    public String getIDRemote() {
        return BuildOutputLoggerManager.INSTANCE.lookup(getProjectName()).getID();
    }

    public String[] retrieveLinesRemote(final int firstLine) {
        return BuildOutputLoggerManager.INSTANCE.lookup(getProjectName()).retrieveLines(firstLine);
    }


    static void injectAntProgressLoggerLibIfNeeded(final Builder builder) {
        if (builder instanceof AntBuilder) {
            doInjectAntProgressLoggerLibIfNeeded((AntBuilder) builder);
        } else if (builder instanceof CompositeBuilder) {
            final Builder[] builders = ((CompositeBuilder) builder).getBuilders();
            for (final Builder childBuilder : builders) {
                injectAntProgressLoggerLibIfNeeded(childBuilder);
            }
        }
    }
    private static void doInjectAntProgressLoggerLibIfNeeded(final AntBuilder antBuilder) {

        if (antBuilder.getProgressLoggerLib() != null) {
            // path already set, so don't interfere
            LOG.debug("Agent skipping AntProgressLogger injection, already set to: "
                    + antBuilder.getProgressLoggerLib());
            return; // no kludges required
        }

        final String defaultAntProgressLoggerLib;
        try {
            defaultAntProgressLoggerLib = AntScript.findDefaultProgressLoggerLib();

            if (defaultAntProgressLoggerLib != null) {
                // AntScript will be able to find the jar on it's own, so again, don't interfere
                LOG.debug("Agent skipping AntProgressLogger injection, AntScript will set to: "
                        + defaultAntProgressLoggerLib);
                return; // no kludges required
            }
        } catch (AntScript.ProgressLibLocatorException e) {
            // This exception is expected under Webstart, so ignore and continue
            LOG.debug("Couldn't find default AntProgressLogger, will attempt injection.");
        }

        // This assumes JNLP Extension for Ant Progress Logger has been installed.
        final String jnlpMuffinAntProgressLoggerPath = AntProgressLoggerInstaller.getJNLPMuffinAntProgressLoggerPath();
        LOG.debug("jnlpMuffinAntProgressLoggerPath: " + jnlpMuffinAntProgressLoggerPath);

        final File progressLoggerJar = new File(jnlpMuffinAntProgressLoggerPath);
        if (!progressLoggerJar.exists()) {
            throw new IllegalStateException(
                    "JNLP Build Agent couldn't find progress logger lib jar in expected location: "
                    + progressLoggerJar.getAbsolutePath());
        }

        antBuilder.setProgressLoggerLib(progressLoggerJar.getAbsolutePath());
        LOG.debug("Injected AntProgressLogger lib: " + progressLoggerJar.getAbsolutePath() + " into AntBuilder.");
    }

    /**
     * Zip any build artifacts found in the logDir and/or outputDir.
     */
    void prepareLogsAndArtifacts() {
        final String buildDirProperty = configProperties.getProperty(CRUISE_BUILD_DIR);
        try {
            buildRootDir = new File(buildDirProperty).getCanonicalFile();
        } catch (IOException e) {
            final String message = "Couldn't create " + buildDirProperty;
            logPrefixError(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message);
        }


        logDir = getAgentResultDir(PropertiesHelper.RESULT_TYPE_LOGS,
                PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR);

        outputDir = getAgentResultDir(PropertiesHelper.RESULT_TYPE_OUTPUT,
                PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR);


        zippedLogs = ZipUtil.getTempResultsZipFile(buildRootDir, projectName, PropertiesHelper.RESULT_TYPE_LOGS);
        ZipUtil.zipFolderContents(zippedLogs.getAbsolutePath(), logDir.getAbsolutePath());

        zippedOutput = ZipUtil.getTempResultsZipFile(buildRootDir, projectName, PropertiesHelper.RESULT_TYPE_OUTPUT);
        ZipUtil.zipFolderContents(zippedOutput.getAbsolutePath(), outputDir.getAbsolutePath());

        if (remoteResults != null) {
            for (int i = 0; i < remoteResults.length; i++) {

                final File agentResultDir = remoteResults[i].getAgentDir();
                ensureDirExists(agentResultDir);

                remoteResults[i].storeTempZippedFile(
                        ZipUtil.getTempResultsZipFile(buildRootDir, projectName, "remoteResult" + i));

                ZipUtil.zipFolderContents(remoteResults[i].fetchTempZippedFile().getAbsolutePath(),
                        agentResultDir.getAbsolutePath());
            }
        }

    }

    private File getAgentResultDir(final String resultType, final String resultProperty) {
        String resultDir = distributedAgentProps.get(resultProperty);
        logPrefixDebug("Result: " + resultType + "Prop value: " + resultDir);

        if (resultDir == null || "".equals(resultDir)) {
            // use canonical behavior if attribute is not set
            resultDir = buildRootDir + File.separator + resultType + File.separator + projectName;
        }

        // ensure dir exists
        final File fileResultDir = new File(resultDir);
        ensureDirExists(fileResultDir);

        return fileResultDir;
    }

    private static void ensureDirExists(final File fileResultDir) {
        if (!fileResultDir.exists()) {
            if (!Util.doMkDirs(fileResultDir)) {
                final String msg = "Error creating Agent result dir: " + fileResultDir.getAbsolutePath();
                LOG.error(msg);
                throw new RuntimeException(msg);
            }
        }
    }

    public String getMachineName() {
        return machineName;
    }

    public void claim() {
        // flag this agent as busy for now. Intended to prevent mulitple builds on same agent,
        // when multiple master threads find the same agent, before any build thread has started.
        synchronized (busyLock) {
            if (isBusy()) {
                throw new IllegalStateException("Cannot claim agent on " + getMachineName()
                        + " that is busy building project: "
                        + projectName);
            }
            setBusy(true);
        }
    }

    public boolean isBusy() {
        synchronized (busyLock) {
            logPrefixDebug("Is busy called. value: " + isBusy);
            return isBusy;
        }
    }

    public Date getDateClaimed() {
        return dateClaimed;
    }

    private void setPendingKill() {
        synchronized (busyLock) {
            this.isPendingKill = true;
            pendingKillSince = new Date();
        }
    }
    public boolean isPendingKill() {
        synchronized (busyLock) {
            return isPendingKill;
        }
    }
    public Date getPendingKillSince() {
        return pendingKillSince;
    }


    private void setPendingRestart() {
        synchronized (busyLock) {
            this.isPendingRestart = true;
            pendingRestartSince = new Date();
        }
    }
    public boolean isPendingRestart() {
        synchronized (busyLock) {
            return isPendingRestart;
        }
    }
    public Date getPendingRestartSince() {
        return pendingRestartSince;
    }


    public boolean resultsExist(final String resultsType) throws RemoteException {
        if (resultsType.equals(PropertiesHelper.RESULT_TYPE_LOGS)) {
            return recursiveFilesExist(logDir);
        } else if (resultsType.equals(PropertiesHelper.RESULT_TYPE_OUTPUT)) {
            return recursiveFilesExist(outputDir);
        } else {
            throw new RemoteException("Unrecognized result type: " + resultsType);
        }
    }

    public boolean remoteResultExists(final int idx) throws RemoteException {
        if (remoteResults != null) {
            final boolean resultsExist = recursiveFilesExist(remoteResults[idx].getAgentDir());
            if (resultsExist) {
                return true;
            }
        }
        return false;
    }


    static boolean recursiveFilesExist(final File fileToCheck) {

        if (!fileToCheck.exists()) { // save time, if it's doesn't exist at all
            return false;
        } else if (fileToCheck.isFile()) { // save time, if it's a file
            return true;
        }

        final File[] dirs = fileToCheck.listFiles();
        for (final File dir : dirs) {
            if (recursiveFilesExist(dir)) {
                return true; // we found a file so return now, no need to keep looking.
            }
        }
        return false;
    }

    /**
     * Return the file containing the given type of results. Package visible for unit testing.
     * @param resultsType the type of results file
     * @return the file containing the given type of results. Package visible for unit testing.
     * @throws RemoteException if an invalid result type is given.
     */
    File getResultsZip(final String resultsType) throws RemoteException {
        final File zipFile;
        if (PropertiesHelper.RESULT_TYPE_LOGS.equals(resultsType)) {
            zipFile = zippedLogs;
        } else if (PropertiesHelper.RESULT_TYPE_OUTPUT.equals(resultsType)) {
            zipFile = zippedOutput;
        } else {
            throw new RemoteException("Unrecognized result type: " + resultsType);
        }
        return zipFile;
    }

    public byte[] retrieveResultsAsZip(final String resultsType) throws RemoteException {

        final File zipFile = getResultsZip(resultsType);

        final byte[] response;
        try {
            response = FileUtil.getFileAsBytes(zipFile);
        } catch (IOException e) {
            final String message = "Unable to get file " + zipFile.getAbsolutePath();
            logPrefixError(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }

        return response;
    }

    public byte[] retrieveRemoteResult(final int resultIdx) throws RemoteException {

        RemoteResult remoteResult = null;
        if (remoteResults != null) {
            for (final RemoteResult remoteResultTry : remoteResults) {
                if (resultIdx == remoteResultTry.getIdx()) {
                    remoteResult = remoteResultTry;
                    break;
                }
            }
        }

        if (remoteResult == null) {
            final String message = "Invalid remote result index: " + resultIdx;
            logPrefixError(message);
            System.err.println(message);
            throw new RuntimeException(message);
        }
        
        final byte[] response;
        try {
            response = FileUtil.getFileAsBytes(remoteResult.fetchTempZippedFile());
        } catch (IOException e) {
            final String message = "Unable to get remote result file: " + remoteResult.getAgentDir().getAbsolutePath();
            logPrefixError(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }

        return response;
    }

    public void clearOutputFiles() {
        try {
            if (logDir != null) {
                logPrefixDebug("Deleting contents of " + logDir);
                IO.delete(logDir);
            } else {
                logPrefixDebug("Skip delete agent logDir: " + logDir);
            }
            if (zippedLogs != null) {
                logPrefixDebug("Deleting log zip " + zippedLogs);
                zippedLogs.deleteOnExit();
                IO.delete(zippedLogs);
            } else {
                logPrefixError("Skipping delete of log zip, file path is null.");
            }

            if (outputDir != null) {
                logPrefixDebug("Deleting contents of " + outputDir);
                IO.delete(outputDir);
            } else {
                logPrefixDebug("Skip delete agent outputDir: " + outputDir);
            }
            if (zippedOutput != null) {
                logPrefixDebug("Deleting output zip " + zippedOutput);
                zippedOutput.deleteOnExit();
                IO.delete(zippedOutput);
            } else {
                logPrefixError("Skipping delete of output zip, file path is null.");
            }


            if (remoteResults != null) {
                for (final RemoteResult remoteResult : remoteResults) {
                    logPrefixDebug("Deleting contents of " + remoteResult.getAgentDir().getAbsolutePath());
                    IO.delete(remoteResult.getAgentDir());

                    final File tempZippedFile = remoteResult.fetchTempZippedFile();
                    if (tempZippedFile != null) {
                        logPrefixDebug("Deleting remote result zip " + tempZippedFile.getAbsolutePath());
                        tempZippedFile.deleteOnExit();
                        IO.delete(tempZippedFile);
                    }
                }
            }

            setBusy(false);

        } catch (RuntimeException e) {
            logPrefixError("Error cleaning agent build files.", e);
            throw e;
        }
    }


    private void doRestart() {
        logPrefixInfo("Attempting agent restart.");

        final BasicService basicService;
        try {
            basicService = (BasicService) ServiceManager.lookup(BasicService.class.getName());
        } catch (UnavailableServiceException e) {
            final String errMsg = "Couldn't find webstart Basic Service. Is Agent running outside of webstart?";
            logPrefixError(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }

        // Normally, we would set the busy lock first, but here we should only set the lock after we are certain
        // the required Webstart service is available.
        synchronized (busyLock) {
            if (!isBusy()) {
                // claim agent so no new build can start
                claim();
            }
            // set project name to informative message in case agent is found while restarting
            projectName = "executingAgentRestart";
        }

        final URL codeBaseURL = basicService.getCodeBase();
        logPrefixInfo("basicService.getCodeBase()=" + codeBaseURL.toString());

        // relaunch via new browser session
        // @todo How to close the browser after jnlp is relaunched?
        final URL relaunchURL;
        try {
            relaunchURL = new URL(codeBaseURL, "agent.jnlp");
        } catch (MalformedURLException e) {
            final String errMsg = "Error building webstart relaunch URL from " + codeBaseURL.toString();
            logPrefixError(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        if (basicService.showDocument(relaunchURL)) {
            logPrefixInfo("Relaunched agent via URL: " + relaunchURL.toString() + ". Will kill current agent now.");
            doKill(); // don't wait for build finish, since we've already relaunched at this point.
        } else {
            final String errMsg = "Failed to relaunch agent via URL: " + relaunchURL.toString();
            logPrefixError(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    private void doKill() {
        logPrefixInfo("Attempting agent kill.");
        synchronized (busyLock) {
            if (!isBusy()) {
                // claim agent so no new build can start
                claim();
            }
            // set project name to informative message in case agent is found while shutting down
            projectName = "executingAgentKill";
        }
        BuildAgent.kill();
        doKillExecuted = true;
    }

    /** Intended only for unit tests, indicating if doKill() call has completed. */
    private boolean doKillExecuted;
    boolean isDoKillExecuted() { return doKillExecuted; }


    public void kill(final boolean afterBuildFinished) throws RemoteException {
        setPendingKill();

        if (!afterBuildFinished // Kill now, don't waiting for build to finish.
                || !isBusy()) { // Not busy, so kill now.

            doKill(); // calls back to this agent to terminate lookup stuff
        } else if (isBusy()) {
            // do nothing. When claim is released, setBusy(false) will perform the kill
        }
        fireAgentStatusChanged();
    }

    public void restart(final boolean afterBuildFinished) throws RemoteException {
        setPendingRestart();

        if (!afterBuildFinished // Restart now, don't waiting for build to finish.
                || !isBusy()) { // Not busy, so Restart now.

            doRestart();
        } else if (isBusy()) {
            // do nothing. When claim is released, setBusy(false) will perform the Restart
        }
        fireAgentStatusChanged();
    }

    private static final DateFormat DF_PROGRESS_LASTUPDATED = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public String asString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Machine Name: ");
        sb.append(machineName);
        sb.append(";\t");
        sb.append("Started: ");
        sb.append(dateStarted);

        sb.append("\n\tBusy: ");
        sb.append(isBusy);
        sb.append(";\tSince: ");
        sb.append(dateClaimed);
        sb.append(";\tProject: ");
        sb.append(projectName);
        // include Progress if available
        if (buildProgressRemote != null) {
            sb.append("\n\tProgress: ");
            try {
                // preserve former date format
                //sb.append(buildProgressRemote.getValueRemote());
                sb.append(DF_PROGRESS_LASTUPDATED.format(buildProgressRemote.getLastUpdatedRemote()));
                sb.append(" ");
                sb.append(buildProgressRemote.getTextRemote());
            } catch (RemoteException e) {
                LOG.info("Error reading remote progress", e);
            }
        }

        sb.append("\n\tPending Restart: ");
        sb.append(isPendingRestart);
        sb.append(";\tPending Restart Since: ");
        sb.append(pendingRestartSince);

        sb.append("\n\tPending Kill: ");
        sb.append(isPendingKill);
        sb.append(";\tPending Kill Since: ");
        sb.append(pendingKillSince);

        sb.append("\n\tVersion: ");
        sb.append(CCDistVersion.getVersion());
        sb.append(" (Compiled: ");
        sb.append(CCDistVersion.getVersionBuildDate());
        sb.append(")");

        return sb.toString();
    }


    public void setEntryOverrides(PropertyEntry[] entryOverrides) {
        serviceContainer.setEntryOverrides(entryOverrides);
        // this is done only to update agent UI info with new entries info
        fireAgentStatusChanged();
    }

    public PropertyEntry[] getEntryOverrides() {
        return serviceContainer.getEntryOverrides();
    }


    public void addAgentStatusListener(final BuildAgent.AgentStatusListener listener) {
        agentStatusListeners.add(listener);
    }
    public void removeAgentStatusListener(final BuildAgent.AgentStatusListener listener) {
        agentStatusListeners.remove(listener);
    }
    private void fireAgentStatusChanged() {
        for (final BuildAgent.AgentStatusListener agentStatusListener : agentStatusListeners) {
            agentStatusListener.statusChanged(this);
        }
    }
}
