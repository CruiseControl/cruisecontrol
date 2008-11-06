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

package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Arrays;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.entry.Entry;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.export.Exporter;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.ConfigurationException;
import net.sourceforge.cruisecontrol.Builder;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.core.ZipUtil;
import net.sourceforge.cruisecontrol.distributed.core.FileUtil;
import net.sourceforge.cruisecontrol.distributed.core.ProgressRemoteImpl;
import net.sourceforge.cruisecontrol.distributed.core.ProgressRemote;
import net.sourceforge.cruisecontrol.distributed.core.RemoteResult;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

public class DistributedMasterBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(DistributedMasterBuilder.class);

    private static final String CRUISE_PROPERTIES = "cruise.properties";
    private static final String CRUISE_RUN_DIR = "cruise.run.dir";

    // TODO: Change to property?
    private static final long DEFAULT_CACHE_MISS_WAIT = 30000;
    private boolean isFailFast;

    private String entriesRaw;
    private static final Entry[] EMPTY_ENTRIES = new Entry[] {};
    private Entry[] entries = EMPTY_ENTRIES;

    private String agentLogDir;
    private String agentOutputDir;

    private String masterLogDir;
    private String masterOutputDir;

    private RemoteResult[] remoteResults;

    private final List<Builder> tmpNestedBuilders = new ArrayList<Builder>();
    private Builder nestedBuilder;

    private String overrideTarget;
    private File rootDir;

    static final String MSG_MISSING_PROJECT_NAME = "Missing required property: " + PropertiesHelper.PROJECT_NAME
            + " in projectProperties";


    /**
     * Available agent lookup will not block until an agent is found,
     * but will return null immediately. Intended only for unit tests.
     */
    void setFailFast() {
        this.isFailFast = true;
    }
    private boolean isFailFast() {
        return isFailFast;
    }

    private void loadRequiredProps() throws CruiseControlException {
        final Properties cruiseProperties;
        try {
            cruiseProperties = (Properties) PropertiesHelper.loadRequiredProperties(CRUISE_PROPERTIES);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            System.err.println(e.getMessage());
            throw new CruiseControlException(e.getMessage(), e);
        }
        rootDir = new File(cruiseProperties.getProperty(CRUISE_RUN_DIR));
        LOG.debug("CRUISE_RUN_DIR: " + rootDir);
        if (!rootDir.exists()
                // Don't think non-existant rootDir matters if agent/master log/output dirs are set
                && (agentLogDir == null && masterLogDir == null)
                && (agentOutputDir == null && masterOutputDir == null)
        ) {
            final String message = "Could not get property " + CRUISE_RUN_DIR + " from " + CRUISE_PROPERTIES
                    + ", or run dir does not exist: " + rootDir;
            LOG.error(message);
            System.err.println(message);
            throw new CruiseControlException(message);
        }
    }


    public void validate() throws CruiseControlException {
        super.validate();

        loadRequiredProps();

        if (tmpNestedBuilders.size() == 0) {
            final String message = "A nested Builder is required for DistributedMasterBuilder";
            LOG.warn(message);
            throw new CruiseControlException(message);
        } else if (tmpNestedBuilders.size() > 1) {
            final String message = "Only one nested Builder is allowed for DistributedMasterBuilder";
            LOG.warn(message);
            throw new CruiseControlException(message);
        }
        ValidationHelper.assertHasChild(tmpNestedBuilders.get(0), Builder.class, "ant, maven2, etc.",
                DistributedMasterBuilder.class);
        nestedBuilder = tmpNestedBuilders.get(0);

        // In order to support Build Agents who's build tree does not exactly match the Master, only validate
        // the nested builder on the Build Agent (so don't validate it here).
        //nestedBuilder.validate();

        if (remoteResults != null) {
            for (final RemoteResult remoteResult : remoteResults) {
                remoteResult.validate();
            }
        }
    }


    /** Override base schedule methods to expose nested-builder values. Otherwise, schedules are not honored.*/
    public int getDay() {
        return nestedBuilder.getDay();
    }

    /** Override base schedule methods to expose nested-builder values. Otherwise, schedules are not honored.*/
    public int getTime() {
        return nestedBuilder.getTime();
    }
    /** Override base schedule methods to expose nested-builder values. Otherwise, schedules are not honored.*/
    public int getMultiple() {
        return nestedBuilder.getMultiple();
    }

    
    public Element buildWithTarget(final Map<String, String> properties, final String target, final Progress progress)
            throws CruiseControlException {
        
        final String oldOverideTarget = overrideTarget;
        overrideTarget = target;
        try {
            return build(properties, progress);
        } finally {
            overrideTarget = oldOverideTarget;
        }
    }

    public Element build(final Map<String, String> projectProperties, final Progress progressIn)
            throws CruiseControlException {

        try {
            final String projectName = projectProperties.get(PropertiesHelper.PROJECT_NAME);
            if (null == projectName) {
                throw new CruiseControlException(MSG_MISSING_PROJECT_NAME);
            }

            final Progress progress = getShowProgress() ? progressIn : null;

            final BuildAgentService agent = pickAgent(projectName, progress);
            if (agent == null) {
                throw new IllegalStateException("pickAgent() retuned without an Agent. Only valid in unit tests.");
            }
            // agent is now marked as claimed

            String agentMachine = "unknown";
            try {
                agentMachine = agent.getMachineName();
            } catch (RemoteException e1) {
                // ignored
            }

            final Element buildResults;
            try {
                final Map<String, String> distributedAgentProps = new HashMap<String, String>();
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, overrideTarget);
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR, agentLogDir);
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR, agentOutputDir);

                // set Build Agent logging to debug if the Master has debug enabled
                if (LOG.isDebugEnabled()) {
                    distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_DEBUG, "true");
                }

                LOG.debug("Distributed Agent Props: " + distributedAgentProps.toString());

                LOG.debug("Project Props: " + projectProperties.toString());

                final String msgAgentMachine = "building on agent: " + agentMachine;
                LOG.info(msgAgentMachine + ", project: " + projectName);
                final ProgressRemote progressRemote;
                // A strong reference to ProgressRemoteImpl is required to keep internal RMI refs valid.
                // For details, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4114579
                final ProgressRemoteImpl progressRemoteImpl;
                final Exporter exporter;
                if (progress != null) {
                    progress.setValue(msgAgentMachine);

                    // wrap progress object to prefix progress messages with Agent machine name
                    // and allow remote calls.
                    progressRemoteImpl = new ProgressRemoteImpl(progress, agentMachine);

                    // NOTE: Basic exported fails on nets where DNS is broken, so use config to allow override.
                    //exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                    //        new BasicILFactory(), false, true);
                    try {
                        // @todo use a new config file, ie: 'progressremote.config'. Update 'Bad DNS workaround' docs
                        final String configFilename = "transient-reggie.config";
                        final File configFile = FileUtil.getFileFromResource(configFilename);
                        final Configuration config = ConfigurationProvider.getInstance(
                                new String[] { configFile.getAbsolutePath() }, getClass().getClassLoader());

                        final Exporter defaultExporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                new BasicILFactory(), false, true);
                        final String componentName = "com.sun.jini.reggie";
                        exporter = (Exporter) config.getEntry(componentName, "serverExporter", Exporter.class,
                                defaultExporter);
                    } catch (ConfigurationException e) {
                        throw new CruiseControlException("Error configuring ProgressRemote exporter", e);
                    }

                    progressRemote = (ProgressRemote) exporter.export(progressRemoteImpl);
                } else {
                    // A strong reference to ProgressRemoteImpl is required to keep internal RMI refs valid.
                    // For details, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4114579
                    // So even though this assignment is not used, it MUST remain.
                    progressRemoteImpl = null;
                    progressRemote = null;
                    exporter = null;
                }

                try {
                    buildResults = agent.doBuild(nestedBuilder, projectProperties, distributedAgentProps,
                            progressRemote, remoteResults);
                } finally {
                    unexportProgressRemote(exporter);
                }

                final File rootDirCanon;
                try {
                    // watch out on Windoze, problems if root dir is c: instead of c:/
                    LOG.debug("rootDir: " + rootDir + "; rootDir.cp: " + rootDir.getCanonicalPath());
                    rootDirCanon = rootDir.getCanonicalFile();
                } catch (IOException e) {
                    final String message = "Error getting canonical file for: " + rootDir;
                    LOG.error(message);
                    System.err.println(message);
                    throw new CruiseControlException(message, e);
                }

                retrieveBuildArtifacts(agent, rootDirCanon, projectName, progress, agentMachine);

            } catch (RemoteException e) {
                final String message = "RemoteException from"
                        + "\nagent on: " + agentMachine
                        + "\nwhile building project: " + projectName;
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                try {
                    agent.clearOutputFiles();
                } catch (RemoteException re) {
                    LOG.error("Exception after prior exception while clearing agent output files (to set busy false).",
                            re);
                }
                throw new CruiseControlException(message, e);
            }
            return buildResults;
        } catch (RuntimeException e) {
            final String message = "Distributed build runtime exception";
            if (!isFailFast()) {
                // do not log expected error during unit test
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
            }
            throw new CruiseControlException(message, e);
        }
    }

    private void unexportProgressRemote(final Exporter exporter) {
        if (exporter != null) {
            int count = 0;
            while (!exporter.unexport(false) && count < 10) {
                LOG.info("Failed to unexport ProgressRemote, retries: " + count);
                // wait a bit and try again
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error("Interrupted while unexporting ProgressRemote");
                }
                count++;
            }
            // force unexport, even if remote calls are pending
            exporter.unexport(true);
        }
    }

    private void retrieveBuildArtifacts(final BuildAgentService agent, final File workDir, final String projectName,
                                        final Progress progress, final String agentMachine)
            throws RemoteException {

        if (progress != null) {
            progress.setValue("retrieving results from " + agentMachine);
        }

        getResultsFiles(agent, workDir, projectName, PropertiesHelper.RESULT_TYPE_LOGS,
                resolveMasterDestDir(masterLogDir, workDir, PropertiesHelper.RESULT_TYPE_LOGS));

        getResultsFiles(agent, workDir, projectName, PropertiesHelper.RESULT_TYPE_OUTPUT,
                resolveMasterDestDir(masterOutputDir, workDir, PropertiesHelper.RESULT_TYPE_OUTPUT));


        if (remoteResults != null) {

            for (final RemoteResult remoteResult : remoteResults) {
                getRemoteResult(agent, workDir, projectName, remoteResult);
            }
        }

        agent.clearOutputFiles();
    }

    
    private static File resolveMasterDestDir(final String masterDestDir, final File workDir,
                                               final String resultType) {
        final File resultDir;
        if (masterDestDir == null || "".equals(masterDestDir)) {
            resultDir = new File(workDir, resultType);
        } else {
            resultDir = new File(masterDestDir);
        }
        return resultDir;
    }

    /**
     * @param agent build agent
     * @param workDir working dir for temp files
     * @param projectName project name being built
     * @param resultsType log, output, or file (RemoteResults)
     * @param masterDestDir destination directory on master into which to expand the result files.
     * @throws RemoteException if a remote call fails
     */
    public static void getResultsFiles(final BuildAgentService agent, final File workDir, final String projectName,
                                       final String resultsType, final File masterDestDir)
            throws RemoteException {

        if (agent.resultsExist(resultsType)) {

            final byte[] remoteResultBytes = agent.retrieveResultsAsZip(resultsType);

            extractBytesToMaster(workDir, projectName, resultsType, remoteResultBytes, masterDestDir);
        } else {
            final String message = projectName + ": No results returned for " + resultsType;
            LOG.info(message);
        }
    }

    /**
     * @param agent build agent
     * @param workDir working dir for temp files
     * @param projectName project name being built
     * @param remoteResult remoteResult to retrieve from Agent
     * @throws RemoteException if a remote call fails
     */
    public static void getRemoteResult(final BuildAgentService agent, final File workDir, final String projectName,
                                       final RemoteResult remoteResult)
            throws RemoteException {

        final String resultsType = PropertiesHelper.RESULT_TYPE_DIR;

        if (agent.remoteResultExists(remoteResult.getIdx())) {

            final byte[] remoteResultBytes = agent.retrieveRemoteResult(remoteResult.getIdx());

            extractBytesToMaster(workDir, projectName, resultsType, remoteResultBytes, remoteResult.getMasterDir());
        } else {
            final String message = projectName + ": Nothing returned for remote result: " + remoteResult;
            LOG.info(message);
        }
    }

    private static void extractBytesToMaster(final File workDir, final String projectName, final String resultsType,
                                             final byte[] remoteResultBytes, final File masterDestDir) {
        
        final File zipFile = ZipUtil.getTempResultsZipFile(workDir, projectName, resultsType);

        FileUtil.bytesToFile(remoteResultBytes, zipFile);

        try {
            LOG.info("unzip " + resultsType + " (" + zipFile.getAbsolutePath() + ") to: " + masterDestDir);
            ZipUtil.unzipFileToLocation(zipFile.getAbsolutePath(), masterDestDir.getAbsolutePath());
            IO.delete(zipFile);
        } catch (IOException e) {
            // Empty zip for log results--ignore
            LOG.debug("Ignored retrieve " + resultsType + " results error:", e);
        }
    }


    BuildAgentService pickAgent(final String projectName, final Progress progress) throws CruiseControlException {
        BuildAgentService agent = null;

        if (progress != null) {
            String msgProgress = "finding agent";
            if (entriesRaw != null) {
                msgProgress += " with entries: ";
                StringTokenizer st = new StringTokenizer(entriesRaw, ",");
                while (st.hasMoreTokens()) {
                    msgProgress += ("\n" + st.nextToken());
                }
            }

            progress.setValue(msgProgress);
        }

        while (agent == null) {
            final ServiceItem serviceItem;
            try {
                serviceItem = MulticastDiscovery.findMatchingServiceAndClaim(entries,
                        // Non-zero failfast value avoids intermittent failures in unit tests
                        (isFailFast ? 2000 : MulticastDiscovery.DEFAULT_FIND_WAIT_DUR_MILLIS));

            } catch (RemoteException e) {
                throw new CruiseControlException("Error finding matching agent.", e);
            }

            if (serviceItem != null) {
                agent = (BuildAgentService) serviceItem.service;
                try {
                    LOG.info("Found available agent on: " + agent.getMachineName());
                } catch (RemoteException e) {
                    throw new CruiseControlException("Error calling agent method.", e);
                }
            } else if (isFailFast()) {
                LOG.warn("pickAgent: Agent not found. Should only occur in unit tests.");
                break;
            } else {
                // wait a bit and try again
                LOG.info("Couldn't find available agent with: "
                        + MulticastDiscovery.toStringEntries(entries) 
                        + " to build project: " + projectName + ". Waiting "
                        + (DEFAULT_CACHE_MISS_WAIT / 1000) + " seconds before retry.");
                try {
                    Thread.sleep(DEFAULT_CACHE_MISS_WAIT);
                } catch (InterruptedException e) {
                    LOG.error("Lookup Cache Miss Wait was interrupted");
                    break;
                }
            }
        }

        return agent;
    }

    public void setEntries(final String entries) {
        entriesRaw = entries;
        this.entries = ReggieUtil.convertStringEntries(entries);
    }


    public void add(final Builder builder) {
        tmpNestedBuilders.add(builder);
        nestedBuilder = builder; // can't leave this null, otherwise ProjectConfig.validate() fails
    }


    public void setAgentLogDir(final String agentLogDir) {
        this.agentLogDir = agentLogDir;
    }

    public void setAgentOutputDir(final String agentOutputDir) {
        this.agentOutputDir = agentOutputDir;
    }

    public void setMasterLogDir(final String masterLogDir) {
        this.masterLogDir = masterLogDir;
    }

    public void setMasterOutputDir(final String masterOutputDir) {
        this.masterOutputDir = masterOutputDir;
    }


    private int remoteResultIdxCounter;
    
    public Object createRemoteResult() {
        final RemoteResult remoteResult = new RemoteResult(remoteResultIdxCounter++);

        final ArrayList<RemoteResult> newList;
        if (remoteResults != null) {
            newList = new ArrayList<RemoteResult>(Arrays.asList(remoteResults));
        } else {
            newList = new ArrayList<RemoteResult>();
        }
        newList.add(remoteResult);
        
        remoteResults = newList.toArray(new RemoteResult[newList.size()]);

        return remoteResult;
    }

    public RemoteResult[] getRemoteResultsInfo() {
        return remoteResults;
    }
}
