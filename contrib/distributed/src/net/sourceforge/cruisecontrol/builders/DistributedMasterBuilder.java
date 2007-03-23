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

import net.jini.core.lookup.ServiceItem;
import net.jini.core.entry.Entry;
import net.sourceforge.cruisecontrol.Builder;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.core.ZipUtil;
import net.sourceforge.cruisecontrol.distributed.core.FileUtil;
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

    // TODO: Can we get the module from the projectProperties instead of setting it via an attribute?
    //  Could be set in ModificationSet...
    private String module;

    private Entry[] entries = new Entry[] {};

    private String agentLogDir;
    private String agentOutputDir;

    private String masterLogDir;
    private String masterOutputDir;

    private final List tmpNestedBuilders = new ArrayList();
    private Builder nestedBuilder;

    private String overrideTarget;
    private File rootDir;

    static final String MSG_REQUIRED_ATTRIB_MODULE = "The 'module' attribute is required for DistributedMasterBuilder."
            + "\n Consider adding module=\"${project.name}\" as a preconfigured setting in config.xml, "
            + "for example:\n\n"
            + "<plugin name=\"distributed\"\n"
            + "        classname=\"net.sourceforge.cruisecontrol.builders.DistributedMasterBuilder\"\n"
            + "        module=\"${project.name}\"\n"
            + "    />";


    /**
     * Available agent lookup will not block until an agent is found,
     * but will return null immediately. Intended only for unit tests.
     */
    synchronized void setFailFast() {
        this.isFailFast = true;
    }
    private synchronized  boolean isFailFast() {
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
        nestedBuilder = (Builder) tmpNestedBuilders.get(0);

        // In order to support Build Agents who's build tree does not exactly match the Master, only validate
        // the nested builder on the Build Agent (so don't validate it here).
        //nestedBuilder.validate();

        if (module == null) {
            LOG.warn(MSG_REQUIRED_ATTRIB_MODULE);
            throw new CruiseControlException(MSG_REQUIRED_ATTRIB_MODULE);
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

    
    public Element buildWithTarget(Map properties, String target) throws CruiseControlException {
        final String oldOverideTarget = overrideTarget;
        overrideTarget = target;
        try {
            return build(properties);
        } finally {
            overrideTarget = oldOverideTarget;
        }
    }

    public Element build(final Map projectProperties) throws CruiseControlException {
        try {
            final BuildAgentService agent = pickAgent();
            // agent is now marked as claimed

            String agentMachine = "unknown";
            try {
                agentMachine = agent.getMachineName();
            } catch (RemoteException e1) {
                // ignored
            }

            final Element buildResults;
            try {
                final Map distributedAgentProps = new HashMap();
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, overrideTarget);
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_MODULE, module);
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR, agentLogDir);
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR, agentOutputDir);

                // set Build Agent logging to debug if the Master has debug enabled
                if (LOG.isDebugEnabled()) {
                    distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_DEBUG, "true");
                }

                LOG.debug("Distributed Agent Props: " + distributedAgentProps.toString());

                LOG.debug("Project Props: " + projectProperties.toString());

                LOG.info("Starting remote build on agent: " + agent.getMachineName() + " of module: " + module);

                buildResults = agent.doBuild(nestedBuilder, projectProperties, distributedAgentProps);

                final String rootDirPath;
                try {
                    // watch out on Windoze, problems if root dir is c: instead of c:/
                    LOG.debug("rootDir: " + rootDir + "; rootDir.cp: " + rootDir.getCanonicalPath());
                    rootDirPath = rootDir.getCanonicalPath();
                } catch (IOException e) {
                    final String message = "Error getting canonical path for: " + rootDir;
                    LOG.error(message);
                    System.err.println(message);
                    throw new CruiseControlException(message, e);
                }

                retrieveBuildArtifacts(agent, rootDirPath);

            } catch (RemoteException e) {
                final String message = "RemoteException from"
                        + "\nagent on: " + agentMachine
                        + "\nwhile building module: " + module;
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
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new CruiseControlException(message, e);
        }
    }

    private void retrieveBuildArtifacts(BuildAgentService agent, String rootDirPath) throws RemoteException {
        getResultsFiles(agent, PropertiesHelper.RESULT_TYPE_LOGS, rootDirPath,
                resolveMasterDestDir(masterLogDir, rootDirPath, PropertiesHelper.RESULT_TYPE_LOGS));

        getResultsFiles(agent, PropertiesHelper.RESULT_TYPE_OUTPUT, rootDirPath,
                resolveMasterDestDir(masterOutputDir, rootDirPath, PropertiesHelper.RESULT_TYPE_OUTPUT));

        agent.clearOutputFiles();
    }

    
    private static String resolveMasterDestDir(final String masterDestDir, final String rootDirPath,
                                               final String resultType) {
        final String resultDir;
        if (masterDestDir == null || "".equals(masterDestDir)) {
            resultDir = rootDirPath + File.separator + resultType;
        } else {
            resultDir = masterDestDir;
        }
        return resultDir;
    }

    public static void getResultsFiles(final BuildAgentService agent, final String resultsType,
                                       final String rootDirPath, final String masterDestDir)
            throws RemoteException {

        if (agent.resultsExist(resultsType)) {
            final String zipFilePath = FileUtil.bytesToFile(agent.retrieveResultsAsZip(resultsType), rootDirPath,
                    resultsType + ".zip");
            try {
                LOG.info("unzip " + resultsType + " (" + zipFilePath + ") to: " + masterDestDir);
                ZipUtil.unzipFileToLocation(zipFilePath, masterDestDir);
                IO.delete(new File(zipFilePath));
            } catch (IOException e) {
                // Empty zip for log results--ignore
                LOG.debug("Ignored retrieve " + resultsType + " results error:", e);
            }
        } else {
            final String message = "No results returned for " + resultsType;
            LOG.info(message);
        }
    }

    BuildAgentService pickAgent() throws CruiseControlException {
        BuildAgentService agent = null;

        while (agent == null) {
            final ServiceItem serviceItem;
            try {
                serviceItem = MulticastDiscovery.findMatchingServiceAndClaim(entries,
                        // Non-zero failfast value avoids intermittent failures in unit tests
                        (isFailFast ? 1000 : MulticastDiscovery.DEFAULT_FIND_WAIT_DUR_MILLIS));

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
                break;
            } else {
                // wait a bit and try again
                LOG.info("Couldn't find available agent. Waiting "
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


    public void setModule(final String module) {
        this.module = module;
    }

    public void setEntries(final String entries) {
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
}
