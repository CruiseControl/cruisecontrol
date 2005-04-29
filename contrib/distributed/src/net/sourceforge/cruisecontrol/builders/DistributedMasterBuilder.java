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

package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.jini.core.lookup.ServiceRegistrar;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SelfConfiguringPlugin;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.util.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.util.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.util.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.util.ZipUtil;
import net.sourceforge.cruisecontrol.util.FileUtil;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

public class DistributedMasterBuilder extends Builder implements SelfConfiguringPlugin {

    private static final Logger LOG = Logger.getLogger(DistributedMasterBuilder.class);

    private static final String CRUISE_PROPERTIES = "cruise.properties";
    private static final String CRUISE_RUN_DIR = "cruise.run.dir";
    // TODO: Change to property timeout?
    private static final long DEFAULT_TIMEOUT = 30000;

    // TODO: Can we get the module from the projectProperties instead of setting
    // it via an attribute?
    //		 Could be set in ModificationSet...
    private String entries = "";
    private String module = "";
    private Element thisElement;
    private Element childBuilderElement = null;
    private String overrideTarget = "";
    private MulticastDiscovery discovery = new MulticastDiscovery();
    private Properties cruiseProperties;
    private File rootDir;

    protected void overrideTarget(String target) {
        overrideTarget = target;
    }

    /**
     * 
     * @param element
     * @throws CruiseControlException
     */
    public void configure(Element element) throws CruiseControlException {
        this.thisElement = element;
        List children = element.getChildren();
        if (children.size() > 1) {
            String message = "DistributedMasterBuilder can only have one nested builder";
            LOG.error(message);
            throw new CruiseControlException(message);
        }
        childBuilderElement = (Element) children.get(0);

        Attribute tempAttribute = thisElement.getAttribute("entries");
        entries = tempAttribute != null ? tempAttribute.getValue() : "";
        tempAttribute = thisElement.getAttribute("module");
        module = tempAttribute != null ? tempAttribute.getValue() : null;

        try {
            cruiseProperties = (Properties) PropertiesHelper.loadRequiredProperties(CRUISE_PROPERTIES);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            System.err.println(e.getMessage());
            throw new CruiseControlException(e.getMessage(), e);
        }
        rootDir = new File(cruiseProperties.getProperty(CRUISE_RUN_DIR));
        if (!rootDir.exists()) {
            String message = "Could not get property " + CRUISE_RUN_DIR + " from " + CRUISE_PROPERTIES;
            LOG.error(message);
            System.err.println(message);
            throw new CruiseControlException(message);
        }

        validate();
    }

    public void validate() throws CruiseControlException {
        super.validate();
        if (childBuilderElement == null) {
            String message = "A nested Builder is required for DistributedMasterBuilder";
            LOG.warn(message);
            throw new CruiseControlException(message);
        }
        if (module == null) {
            String message = "The 'module' attribute is required for DistributedMasterBuilder";
            LOG.warn(message);
            throw new CruiseControlException(message);
        }
    }

    public Element build(Map projectProperties) throws CruiseControlException {
        try {
            BuildAgentService agent = pickAgent();

            Element buildResults = null;

            try {
                projectProperties.put("distributed.overrideTarget", overrideTarget);
                projectProperties.put("distributed.module", module);

                buildResults = agent.doBuild(childBuilderElement, projectProperties);

                String rootDirPath = null;
                try {
                    rootDirPath = rootDir.getCanonicalPath();
                } catch (IOException e1) {
                    String message = "Error getting canonical path for: " + rootDir;
                    LOG.error(message);
                    System.err.println(message);
                    throw new CruiseControlException(message, e1);
                }

                String resultsFileName = "logs.zip";
                String resultsType = "logs";
                if (agent.resultsExist(resultsType)) {
                    String zipFilePath = FileUtil.bytesToFile(agent.retrieveResultsAsZip(resultsType), rootDirPath, resultsFileName);
                    try {
                        ZipUtil.unzipFileToLocation(zipFilePath, rootDirPath + File.separator + resultsType);
                        Util.deleteFile(new File(zipFilePath));
                    } catch (IOException e2) {
                        // Empty zip for log results--ignore
                    }
                } else {
                    String message = "No results returned for logs";
                    LOG.debug(message);
                    System.out.println(message);
                }
                resultsFileName = "output.zip";
                resultsType = "output";
                if (agent.resultsExist(resultsType)) {
                    String zipFilePath = FileUtil.bytesToFile(agent.retrieveResultsAsZip(resultsType), rootDirPath, resultsFileName);
                    try {
                        ZipUtil.unzipFileToLocation(zipFilePath, rootDirPath + File.separator + resultsType);
                        Util.deleteFile(new File(zipFilePath));
                    } catch (IOException e2) {
                        // Empty zip for output results--ignore
                    }
                } else {
                    String message = "No results returned for output";
                    LOG.debug(message);
                    System.out.println(message);
                }
                agent.clearOutputFiles();
            } catch (RemoteException e) {
                String message = "Distributed build failed with RemoteException";
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new CruiseControlException(message, e);
            }
            return buildResults;
        } catch(RuntimeException e) {
            String message = "Distributed build runtime exception";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new CruiseControlException(message, e);
        }
    }

    protected BuildAgentService pickAgent() throws CruiseControlException {
        List entriesList = ReggieUtil.convertStringEntriesToList(entries);
        ServiceRegistrar registrar;
        BuildAgentService agent = null;
        do {
            try {
                registrar = discovery.getRegistrar(DEFAULT_TIMEOUT);
            } catch (UnknownServiceException e) {
                String message = "Could not find a registrar before timeout";
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new CruiseControlException(message, e);
            }
            List agents = null;
            try {
                agents = ReggieUtil.findServicesForEntriesList(registrar, entriesList, BuildAgentService.class,
                        DEFAULT_TIMEOUT);
            } catch (UnknownServiceException e1) {
                String message = "Could not find a BuildAgent matching entries before timeout";
                LOG.debug(message, e1);
                System.err.println(message);
                throw new CruiseControlException(message, e1);
            }
            agent = findAvailableAgent(agents);
            if (agent != null) {
                break;
            }
            try {
                // TODO: Change to property timeout?
                Thread.sleep(5000);
            } catch (InterruptedException e2) {
            }
        } while (true);

        return agent;
    }

    /**
     * @param agents
     * @return
     * @throws CruiseControlException
     */
    protected BuildAgentService findAvailableAgent(List agents) throws CruiseControlException {
        BuildAgentService agent = null;
        boolean foundBusyAgent;
        for (Iterator iter = agents.iterator(); iter.hasNext();) {
            agent = (BuildAgentService) iter.next();
            try {
                if (!agent.isBusy()) {
                    LOG.debug("Found matching agent on " + agent.getMachineName());
                    // flag agent as claimed (for now same as busy) to prevent other build
                    // thread from using same agent before this build gets started.
                    agent.claim();
                    break;
                } else {
                    LOG.debug("Found matching busy agent on " + agent.getMachineName());
                    //break;
                    agent = null;
                }
            } catch (RemoteException e) {
                String message = "Couldn't determine agent busy state";
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new CruiseControlException(message, e);
            }
        }
        return agent;
    }

}
