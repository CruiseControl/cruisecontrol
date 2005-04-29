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

    private static final String AGENT_PROPERTIES_FILE = "agent.properties";
    private static final String CRUISE_BUILD_DIR = "cruise.build.dir";
    private static final String DISTRIBUTED_MODULE = "distributed.module";

    private boolean isBusy = false;
    private Properties configProperties;
    private Properties projectProperties = new Properties();
    private String logDir="";
    private String outputDir;
    private long lastBusyTime = 0;
    private String buildRootDir;
    private String logsFilePath;
    private String outputFilePath;

    private final String _busyLock = new String("busyLock");
    private void setBusy(final boolean newIsBusy) {
        synchronized (_busyLock)
        {
            isBusy = newIsBusy;
        }
    }

    public Element doBuild(Element nestedBuilderElement, Map projectPropertiesMap) throws RemoteException {
        setBusy(true);

        projectProperties.putAll(projectPropertiesMap);
        String infoMessage = "Building module: " + projectProperties.getProperty("distributed.module");
        System.out.println();
        System.out.println(infoMessage);
        LOG.info(infoMessage);

        Element buildResults = null;
        Builder nestedBuilder = null;
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
    }

    private Builder createBuilder(Element builderElement) throws CruiseControlException {
        Element buildResults = null;

        configProperties = (Properties) PropertiesHelper.loadRequiredProperties(AGENT_PROPERTIES_FILE);

        ProjectXMLHelper projectXMLHelper = new ProjectXMLHelper();
        PluginXMLHelper pluginXMLHelper = new PluginXMLHelper(projectXMLHelper);

        Builder builder = null;

        PluginRegistry plugins = PluginRegistry.createRegistry();
        Class pluginClass = plugins.getPluginClass(builderElement.getName());
        builder = (Builder) pluginXMLHelper.configure(builderElement, pluginClass, false);
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
        String module = projectProperties.getProperty(DISTRIBUTED_MODULE);
        logDir = buildRootDir + File.separator + "logs" + File.separator + module;
        new File(logDir).mkdirs();
        outputDir = buildRootDir + File.separator + "output" + File.separator + module;
        new File(outputDir).mkdirs();

        logsFilePath = buildRootDir + File.separator + "logs.zip";
        ZipUtil.zipFolderContents(logsFilePath, logDir);
        outputFilePath = buildRootDir + File.separator + "output.zip";
        ZipUtil.zipFolderContents(outputFilePath, outputDir);
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
        setBusy(true);
    }

    public boolean isBusy() {
        //        if( !isBusy ) {
        //            if( System.currentTimeMillis() > lastBusyTime + 1000 ) {
        //	            lastBusyTime = System.currentTimeMillis();
        //	            return true;
        //	        } else {
        //                return false;
        //            }
        //        }
        //        return isBusy;
        //return false;
        synchronized (_busyLock)
        {
            return isBusy;
        }
    }

    public boolean resultsExist(String resultsType) throws RemoteException {
        if (resultsType.equals("logs")) {
            return !(new File(logDir).list().length == 0);
        } else if (resultsType.equals("output")) {
            return !(new File(outputDir).list().length == 0);
        } else
            return false;
    }

    public byte[] retrieveResultsAsZip(String resultsType) throws RemoteException {
        String zipFilePath = null;
        if (resultsType.equals("logs")) {
            zipFilePath = buildRootDir + File.separator + "logs.zip";
        } else if (resultsType.equals("output")) {
            zipFilePath = buildRootDir + File.separator + "output.zip";
        } else {
            String message = "Unknown results type '" + resultsType + "'";
            LOG.debug(message);
            System.err.println(message);
            throw new RemoteException(message);
        }
        byte[] response = null;
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
        LOG.debug("Deleting contents of " + logDir);
        Util.deleteFile(new File(logDir));
        Util.deleteFile(new File(logsFilePath));
        LOG.debug("Deleting contents of " + outputDir);
        Util.deleteFile(new File(outputDir));
        Util.deleteFile(new File(outputFilePath));
        setBusy(false);
    }

}
