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
import java.io.Serializable;
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

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.ZipUtil;
import net.sourceforge.cruisecontrol.distributed.core.FileUtil;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.jdom.Element;

import javax.jnlp.ServiceManager;
import javax.jnlp.BasicService;
import javax.jnlp.UnavailableServiceException;

/**
 * Build Agent implementation.
 */
public class BuildAgentServiceImpl implements BuildAgentService, Serializable {

    private static final Logger LOG = Logger.getLogger(BuildAgentServiceImpl.class);

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
    private final Map distributedAgentProps = new HashMap();
    private String logDir;
    private String outputDir;
    private String buildRootDir;
    private String logsFilePath;
    private String outputFilePath;

    private final List agentStatusListeners = new ArrayList();

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


    /** Constructor. */
    public BuildAgentServiceImpl() {
        dateStarted = new Date();

        try {
            machineName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            final String message = "Failed to get hostname";
            // Don't call Log helper method here since hostname is not yet set
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
        logMsgPrefix = "Agent Host: " + machineName + "; ";
    }

    /** @return the date this Build Agent started running (not when a specific build started). */
    public Date getDateStarted() {
        return dateStarted;
    }

    /** @return the module being built now, or null if no module is being built. */
    public synchronized String getModule() {
        return (String) distributedAgentProps.get(PropertiesHelper.DISTRIBUTED_MODULE);
    }

    void setAgentPropertiesFilename(final String filename) {
        agentPropertiesFilename = filename;
    }
    private String getAgentPropertiesFilename() {
        return agentPropertiesFilename;
    }

    private DelayedAction lastDelayedAction;
    DelayedAction getLastDelayedAction() { return lastDelayedAction; }
    private void setLastDelayedAction(DelayedAction lastDelayedAction) { this.lastDelayedAction = lastDelayedAction; }

    /**
     * Executes the {@link #execAction()} method after a fixed delay has expired.
     */
    abstract static class DelayedAction extends Thread {
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

        DelayedAction(final Type type) {
            delay = Integer.getInteger(
                    SYSPROP_CCDIST_DELAY_MS_KILLRESTART, DEFAULT_DELAY_MS_KILLRESTART).intValue();
            this.type = type;
            start();
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
            }
        }

        public Throwable getThrown() { return thrown; }

        public Type getType() { return type; }

        /**
         * Implement in order to run the desired Action
         */
        public abstract void execAction() ;
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

    public Element doBuild(final Element nestedBuilderElement, final Map projectPropertiesMap,
                           final Map distributedAgentProperties) throws RemoteException {

        synchronized (busyLock) {
            if (!isBusy()) {    // only reclaim if needed, since it resets the dateClaimed.
                setBusy(true); // we could remove this, since claim() is called during lookup...
            }
        }

        final Level origLogLevel = Logger.getRootLogger().getLevel();
        final boolean isDebugBuild = Boolean.valueOf(
                (String) distributedAgentProperties.get(PropertiesHelper.DISTRIBUTED_AGENT_DEBUG)).booleanValue();
        boolean isDebugOverriden = false;
        try {
            // Override log level if needed
            if (isDebugBuild && !LOG.isDebugEnabled()) {
                LOG.info("Switching Agent log level to Debug for build.");
                Logger.getRootLogger().setLevel(Level.DEBUG);
                isDebugOverriden = true;
            }

            logPrefixDebug("Build Agent Props: " + distributedAgentProperties.toString());
            distributedAgentProps.putAll(distributedAgentProperties);

            final String infoMessage = "Building module: " + getModule()
                    + "\n\tAgentLogDir: " + distributedAgentProps.get(
                            PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR)
                    + "\n\tAgentOutputDir: " + distributedAgentProps.get(
                            PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR);

            System.out.println();
            System.out.println(infoMessage);
            logPrefixInfo(infoMessage);

            logPrefixDebug("Build Agent Project Props: " + projectPropertiesMap.toString());

            // this is done only to update agent UI info regarding Module - which isn't available
            // until projectPropertiesMap has been set.
            fireAgentStatusChanged();

            final Element buildResults;
            final Builder nestedBuilder;
            try {
                nestedBuilder = createBuilder(nestedBuilderElement);
                nestedBuilder.validate();
            } catch (CruiseControlException e) {
                final String message = "Failed to configure nested Builder on agent";
                logPrefixError(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RemoteException(message, e);
            }

            try {
                buildResults = nestedBuilder.build(projectPropertiesMap);
            } catch (CruiseControlException e) {
                final String message = "Failed to complete build on agent";
                logPrefixError(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RemoteException(message, e);
            }
            prepareLogsAndArtifacts();
            return buildResults;
        } catch (RemoteException e) {
            logPrefixError("doBuild threw exception, setting busy to false.");
            setBusy(false);
            throw e; // rethrow original exception
        } finally {
            // restore original log level if overriden
            if (isDebugOverriden) {
                Logger.getRootLogger().setLevel(origLogLevel);
                LOG.info("Restored Agent log level to: " + origLogLevel);
            }
        }
    }

    private Builder createBuilder(final Element builderElement) throws CruiseControlException {

        configProperties = (Properties) PropertiesHelper.loadRequiredProperties(
                getAgentPropertiesFilename());

        final String overrideTarget
                = (String) distributedAgentProps.get(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET);
        final PluginXMLHelper pluginXMLHelper = PropertiesHelper.createPluginXMLHelper(overrideTarget);

        final PluginRegistry plugins = PluginRegistry.createRegistry();
        final Class pluginClass = plugins.getPluginClass(builderElement.getName());

        return (Builder) pluginXMLHelper.configure(builderElement, pluginClass, false);
    }

    /**
     * Zip any build artifacts found in the logDir and/or outputDir.
     */
    void prepareLogsAndArtifacts() {
        final String buildDirProperty = configProperties.getProperty(CRUISE_BUILD_DIR);
        try {
            buildRootDir = new File(buildDirProperty).getCanonicalPath();
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


        logsFilePath = buildRootDir + File.separator + PropertiesHelper.RESULT_TYPE_LOGS + ".zip";
        ZipUtil.zipFolderContents(logsFilePath, logDir);
        outputFilePath = buildRootDir + File.separator + PropertiesHelper.RESULT_TYPE_OUTPUT + ".zip";
        ZipUtil.zipFolderContents(outputFilePath, outputDir);
    }

    private String getAgentResultDir(final String resultType, final String resultProperty) {
        String resultDir;
        resultDir = (String) distributedAgentProps.get(resultProperty);
        logPrefixDebug("Result: " + resultType + "Prop value: " + resultDir);

        if (resultDir == null || "".equals(resultDir)) {
            // use canonical behavior if attribute is not set
            resultDir = buildRootDir + File.separator + resultType + File.separator + getModule();
        }
        new File(resultDir).mkdirs();
        return resultDir;
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
                        + " that is busy building module: "
                        + getModule());
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
            return recursiveFilesExist(new File(logDir));
        } else {
            return resultsType.equals(PropertiesHelper.RESULT_TYPE_OUTPUT) && recursiveFilesExist(new File(outputDir));
        }
    }

    static boolean recursiveFilesExist(final File fileToCheck) {

        if (!fileToCheck.exists()) { // save time, if it's doesn't exist at all
            return false;
        } else if (fileToCheck.isFile()) { // save time, if it's a file
            return true;
        }

        final File[] dirs = fileToCheck.listFiles();
        for (int i = 0; i < dirs.length; i++) {
            if (recursiveFilesExist(dirs[i])) {
                return true; // we found a file so return now, no need to keep looking.
            }
        }
        return false;
    }

    public byte[] retrieveResultsAsZip(final String resultsType) throws RemoteException {

        final String zipFilePath = buildRootDir + File.separator + resultsType + ".zip";

        final byte[] response;
        try {
            response = FileUtil.getFileAsBytes(new File(zipFilePath));
        } catch (IOException e) {
            final String message = "Unable to get file " + zipFilePath;
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
                IO.delete(new File(logDir));
            } else {
                logPrefixDebug("Skip delete agent logDir: " + logDir);
            }
            if (logsFilePath != null) {
                logPrefixDebug("Deleting log zip " + logsFilePath);
                IO.delete(new File(logsFilePath));
            } else {
                logPrefixError("Skipping delete of log zip, file path is null.");
            }

            if (outputDir != null) {
                logPrefixDebug("Deleting contents of " + outputDir);
                IO.delete(new File(outputDir));
            } else {
                logPrefixDebug("Skip delete agent outputDir: " + outputDir);
            }
            if (outputFilePath != null) {
                logPrefixDebug("Deleting output zip " + outputFilePath);
                IO.delete(new File(outputFilePath));
            } else {
                logPrefixError("Skipping delete of output zip, file path is null.");
            }

            setBusy(false);

        } catch (RuntimeException e) {
            logPrefixError("Error cleaning agent build files.", e);
            throw e;
        }
    }


    private void doRestart() {
        logPrefixInfo("Attempting agent restart.");

        synchronized (busyLock) {
            if (!isBusy()) {
                // claim agent so no new build can start
                claim();
            }
        }

        final BasicService basicService;
        try {
            basicService = (BasicService) ServiceManager.lookup(BasicService.class.getName());
        } catch (UnavailableServiceException e) {
            final String errMsg = "Couldn't find webstart Basic Service. Is Agent running outside of webstart?";
            logPrefixError(errMsg, e);
            throw new RuntimeException(errMsg, e);
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

    public String asString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Machine Name: ");
        sb.append(getMachineName());
        sb.append(";\t");
        sb.append("Started: ");
        sb.append(getDateStarted());

        sb.append("\n\tBusy: ");
        sb.append(isBusy());
        sb.append(";\tSince: ");
        sb.append(getDateClaimed());
        sb.append(";\tModule: ");
        sb.append(getModule());

        sb.append("\n\tPending Restart: ");
        sb.append(isPendingRestart());
        sb.append(";\tPending Restart Since: ");
        sb.append(getPendingRestartSince());

        sb.append("\n\tPending Kill: ");
        sb.append(isPendingKill());
        sb.append(";\tPending Kill Since: ");
        sb.append(getPendingKillSince());

        return sb.toString();
    }

    public void addAgentStatusListener(final BuildAgent.AgentStatusListener listener) {
        agentStatusListeners.add(listener);
    }
    public void removeAgentStatusListener(final BuildAgent.AgentStatusListener listener) {
        agentStatusListeners.remove(listener);
    }
    private void fireAgentStatusChanged() {
        for (int i = 0; i < agentStatusListeners.size(); i++) {
            ((BuildAgent.AgentStatusListener) agentStatusListeners.get(i)).statusChanged(this);
        }
    }
}
