/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.distributed.util.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.util.ZipUtil;
import net.sourceforge.cruisecontrol.util.FileUtil;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

import javax.jnlp.ServiceManager;
import javax.jnlp.BasicService;
import javax.jnlp.UnavailableServiceException;

public class BuildAgentServiceImpl implements BuildAgentService, Serializable {

    private static final Logger LOG = Logger.getLogger(BuildAgentServiceImpl.class);

    private static final String CRUISE_BUILD_DIR = "cruise.build.dir";

    static final String DEFAULT_AGENT_PROPERTIES_FILE = "agent.properties";
    private String agentPropertiesFilename = DEFAULT_AGENT_PROPERTIES_FILE;

    static final String DEFAULT_USER_DEFINED_PROPERTIES_FILE = "user-defined.properties";

    private final Date dateStarted;
    private boolean isBusy;
    private Date dateClaimed;
    private boolean isPendingKill;
    private Date pendingKillSince;
    private boolean isPendingRestart;
    private Date pendingRestartSince;

    private Properties configProperties;
    private Properties projectProperties = new Properties();
    private String logDir = "";
    private String outputDir;
    private String buildRootDir;
    private String logsFilePath;
    private String outputFilePath;

    private final List agentStatusListeners = new ArrayList();

    public BuildAgentServiceImpl() {
        dateStarted = new Date();
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public synchronized String getModule() {
        return projectProperties.getProperty(PropertiesHelper.DISTRIBUTED_MODULE);
    }

    void setAgentPropertiesFilename(final String filename) {
        agentPropertiesFilename = filename;
    }
    private String getAgentPropertiesFilename() {
        return agentPropertiesFilename;
    }

    private final String busyLock = new String("busyLock");
    void setBusy(final boolean newIsBusy) {
        if (!newIsBusy) { // means the claim is being released
            if (isPendingRestart()) {
                // restart now
                doRestart();
            } else if (isPendingKill()) {
                // kill now
                doKill();
            }

            // clear out projectProperties from last build
            projectProperties.clear();

            dateClaimed = null;
        } else {
            dateClaimed = new Date();
        }

        synchronized (busyLock) {
            isBusy = newIsBusy;
        }

        fireAgentStatusChanged();

        LOG.info("agent busy status changed to: " + newIsBusy);
    }

    public Element doBuild(Element nestedBuilderElement, Map projectPropertiesMap) throws RemoteException {
        synchronized (busyLock) {
            if (!isBusy()) {    // only reclaim if needed, since it resets the dateClaimed.
        setBusy(true); // we could remove this, since claim() is called during lookup...
            }
        }
        try {
            projectProperties.putAll(projectPropertiesMap);
            String infoMessage = "Building module: " + getModule()
                    + "\n\tAgentLogDir: " + projectProperties.getProperty(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR)
                    + "\n\tAgentOutputDir: " + projectProperties.getProperty(
                            PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR);

            System.out.println();
            System.out.println(infoMessage);
            LOG.info(infoMessage);
            // this is done only to update agent UI info regarding Module - which isn't available
            // until projectPropertiesMap has been set.
            fireAgentStatusChanged();

            final Element buildResults;
            final Builder nestedBuilder;
            try {
                nestedBuilder = createBuilder(nestedBuilderElement);
            } catch (CruiseControlException e) {
                String message = "Failed to configure nested Builder on agent";
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RemoteException(message, e);
            }

            try {
                buildResults = nestedBuilder.build(projectPropertiesMap);
            } catch (CruiseControlException e) {
                String message = "Failed to complete build on agent";
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RemoteException(message, e);
            }
            prepareLogsAndArtifacts();
            return buildResults;
        } catch (RemoteException e) {
            LOG.error("doBuild threw exception, setting busy to false.");
            setBusy(false);
            throw e; // rethrow original exception
        }
    }

    private Builder createBuilder(Element builderElement) throws CruiseControlException {

        configProperties = (Properties) PropertiesHelper.loadRequiredProperties(
                getAgentPropertiesFilename());

        final String overrideTarget = projectProperties.getProperty(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET);
        PluginXMLHelper pluginXMLHelper = PropertiesHelper.createPluginXMLHelper(overrideTarget);

        PluginRegistry plugins = PluginRegistry.createRegistry();
        Class pluginClass = plugins.getPluginClass(builderElement.getName());
        final Builder builder = (Builder) pluginXMLHelper.configure(builderElement, pluginClass, false);

        return builder;
    }

    /**
     *  
     */
    private void prepareLogsAndArtifacts() {
        String buildDirProperty = configProperties.getProperty(CRUISE_BUILD_DIR);
        try {
            buildRootDir = new File(buildDirProperty).getCanonicalPath();
        } catch (IOException e) {
            String message = "Couldn't create " + buildDirProperty;
            LOG.error(message, e);
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
        resultDir = projectProperties.getProperty(resultProperty);
        LOG.debug("Result: " + resultType + "Prop value: " + resultDir);

        if (resultDir == null || "".equals(resultDir)) {
            // use canonical behavior if attribute is not set
            resultDir = buildRootDir + File.separator + resultType + File.separator + getModule();
        }
        new File(resultDir).mkdirs();
        return resultDir;
    }

    public String getMachineName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            String message = "Failed to get hostname";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    public void claim() {
        // flag this agent as busy for now. Intended to prevent mulitple builds on same agent,
        // when multiple master threads find the same agent, before any build thread has started.
        synchronized (busyLock) {
            if (isBusy()) {
                String machineName = "unknown";
                machineName = getMachineName();
                throw new IllegalStateException("Cannot claim agent on " + machineName
                        + " that is busy building module: "
                        + getModule());
            }
            setBusy(true);
        }
    }

    public boolean isBusy() {
        synchronized (busyLock) {
            LOG.debug("Is busy called. value: " + isBusy);
            return isBusy;
        }
    }

    public Date getDateClaimed() {
        return dateClaimed;
    }

    private void setPendingKill(final boolean isPendingKill) {
        synchronized (busyLock) {
            this.isPendingKill = isPendingKill;
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


    private void setPendingRestart(final boolean isPendingRestart) {
        synchronized (busyLock) {
            this.isPendingRestart = isPendingRestart;
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


    public boolean resultsExist(String resultsType) throws RemoteException {
        if (resultsType.equals(PropertiesHelper.RESULT_TYPE_LOGS)) {
            return !(new File(logDir).list().length == 0);
        } else if (resultsType.equals(PropertiesHelper.RESULT_TYPE_OUTPUT)) {
            return !(new File(outputDir).list().length == 0);
        } else {
            return false;
        }
    }

    public byte[] retrieveResultsAsZip(String resultsType) throws RemoteException {

        final String zipFilePath = buildRootDir + File.separator + resultsType + ".zip";

        final byte[] response;
        try {
            response = FileUtil.getFileAsBytes(new File(zipFilePath));
        } catch (IOException e) {
            String message = "Unable to get file " + zipFilePath;
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
        return response;
    }

    public void clearOutputFiles() {
        try {
            LOG.debug("Deleting contents of " + logDir);
            Util.deleteFile(new File(logDir));
            if (logsFilePath != null) {
                LOG.debug("Deleting log zip " + logsFilePath);
                Util.deleteFile(new File(logsFilePath));
            } else {
                LOG.error("Skipping delete of log zip, file path is null.");
            }

            LOG.debug("Deleting contents of " + outputDir);
            Util.deleteFile(new File(outputDir));
            if (outputFilePath != null) {
                LOG.debug("Deleting output zip " + outputFilePath);
                Util.deleteFile(new File(outputFilePath));
            } else {
                LOG.error("Skipping delete of output zip, file path is null.");
            }
            setBusy(false);
        } catch (RuntimeException e) {
            LOG.error("Error cleaning agent build files.", e);
            throw e;
        }
    }


    private void doRestart() {
        LOG.info("Attempting agent restart.");

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
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        final URL codeBaseURL = basicService.getCodeBase();
        LOG.info("basicService.getCodeBase()=" + codeBaseURL.toString());

        // relaunch via new browser session
        // @todo How to close the browser after jnlp is relaunched?
        final URL relaunchURL;
        try {
            relaunchURL = new URL(codeBaseURL, "agent.jnlp");
        } catch (MalformedURLException e) {
            final String errMsg = "Error building webstart relaunch URL from " + codeBaseURL.toString();
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        if (basicService.showDocument(relaunchURL)) {
            LOG.info("Relaunched agent via URL: " + relaunchURL.toString() + ". Will kill current agent now.");
            doKill(); // don't wait for build finish, since we've already relaunched at this point.
        } else {
            final String errMsg = "Failed to relaunch agent via URL: " + relaunchURL.toString();
            LOG.error(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    private void doKill() {
        LOG.info("Attempting agent kill.");
        synchronized (busyLock) {
            if (!isBusy()) {
                // claim agent so no new build can start
                claim();
            }
        }
        BuildAgent.kill();
    }

    public void kill(final boolean afterBuildFinished) throws RemoteException {
        setPendingKill(true);

        if (!afterBuildFinished // Kill now, don't waiting for build to finish.
                || !isBusy()) { // Not busy, so kill now.

            doKill(); // calls back to this agent to terminate lookup stuff
        } else if (isBusy()) {
            ; // do nothing. When claim is released, setBusy(false) will perform the kill
        }
        fireAgentStatusChanged();
    }

    public void restart(final boolean afterBuildFinished) throws RemoteException {
        setPendingRestart(true);

        if (!afterBuildFinished // Restart now, don't waiting for build to finish.
                || !isBusy()) { // Not busy, so Restart now.
            
            doRestart();
        } else if (isBusy()) {
            ; // do nothing. When claim is released, setBusy(false) will perform the Restart
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
