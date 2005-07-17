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
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.distributed.util.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.util.ZipUtil;
import net.sourceforge.cruisecontrol.util.FileUtil;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

public class BuildAgentServiceImpl implements BuildAgentService, Serializable {

    private static final Logger LOG = Logger.getLogger(BuildAgentServiceImpl.class);

    private static final String CRUISE_BUILD_DIR = "cruise.build.dir";

    static final String DEFAULT_AGENT_PROPERTIES_FILE = "agent.properties";
    private String agentPropertiesFilename = DEFAULT_AGENT_PROPERTIES_FILE;

    static final String DEFAULT_USER_DEFINED_PROPERTIES_FILE = "user-defined.properties";

    private boolean isBusy = false;
    private Properties configProperties;
    private Properties projectProperties = new Properties();
    private String logDir = "";
    private String outputDir;
    private String buildRootDir;
    private String logsFilePath;
    private String outputFilePath;

    private synchronized String getModule() {
        return projectProperties.getProperty(PropertiesHelper.DISTRIBUTED_MODULE);
    }

    void setAgentPropertiesFilename(final String filename) {
        agentPropertiesFilename = filename;
    }
    private String getAgentPropertiesFilename() {
        return agentPropertiesFilename;
    }

    private final String busyLock = new String("busyLock");
    private void setBusy(final boolean newIsBusy) {
        synchronized (busyLock) {
            isBusy = newIsBusy;
        }
        LOG.info("agent busy status changed to: " + newIsBusy);
    }

    public Element doBuild(Element nestedBuilderElement, Map projectPropertiesMap) throws RemoteException {
        setBusy(true); // we could remove this, since claim() is called during lookup...
        try {
            projectProperties.putAll(projectPropertiesMap);
            String infoMessage = "Building module: " + getModule()
                    + "\n\tAgentLogDir: " + projectProperties.getProperty(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR)
                    + "\n\tAgentOutputDir: " + projectProperties.getProperty(
                            PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR);

            System.out.println();
            System.out.println(infoMessage);
            LOG.info(infoMessage);

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

        ProjectXMLHelper projectXMLHelper = new ProjectXMLHelper();
        PluginXMLHelper pluginXMLHelper = new PluginXMLHelper(projectXMLHelper);

        PluginRegistry plugins = PluginRegistry.createRegistry();
        Class pluginClass = plugins.getPluginClass(builderElement.getName());
        final Builder builder = (Builder) pluginXMLHelper.configure(builderElement, pluginClass, false);
        // TODO: Should overrideTarget be changed to public for Builders so that
        // we can do this?
        //            nestedBuilder.overrideTarget(projectProperties.get("distributed.overrideTarget");
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

    public String getMachineName() throws RemoteException {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            String message = "Failed to get hostname";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RemoteException(message, e);
        }
    }

    public void claim() {
        // flag this agent as busy for now. Intended to prevent mulitple builds on same agent,
        // when multiple master threads find the same agent, before any build thread has started.
        synchronized (busyLock) {
            if (isBusy()) {
                throw new IllegalStateException("Cannot claim agent that is busy building module: "
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

}
