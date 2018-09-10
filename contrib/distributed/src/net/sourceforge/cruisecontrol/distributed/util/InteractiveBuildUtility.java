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

package net.sourceforge.cruisecontrol.distributed.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.entry.Entry;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.ResolverHolder;
import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilder;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.core.RemoteResult;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

public final class InteractiveBuildUtility {

    private static final Logger LOG = Logger.getLogger(InteractiveBuildUtility.class);

    private static final Console CONSOLE = new Console();
    
    private Element distributedBuilderElement;

    private InteractiveBuildUtility() {
        System.out.print("Enter path to Cruise Control configuration file: ");
        String configFilePath = CONSOLE.readLine();
        new InteractiveBuildUtility(configFilePath);
    }

    private InteractiveBuildUtility(String configFilePath) {
        File configFile = new File(configFilePath);
        if (!configFile.exists() || configFile.isDirectory()) {
            String message = configFilePath + " does not exist or is a directory - quitting...";
            System.err.println(message);
            LOG.error(message);
            System.exit(1);
        }
        Element project = getProjectFromConfig(configFile);
        distributedBuilderElement = getBuilderFromProject(project);
        ServiceItem[] serviceItems = findAgents(distributedBuilderElement.getAttribute("entries"));
        final BuildAgentService agent = selectAgent(serviceItems);
        final DistributedMasterBuilder distributedBuildMaster = doBuild();
        retrieveBuildArtifacts(agent, distributedBuildMaster);
    }

    private Element getProjectFromConfig(File configFile) {
        Element rootElement = null;
        Element project = null;
        try {
            rootElement = Util.loadRootElement(configFile);
        } catch (CruiseControlException e) {
            System.err.println(e.getMessage());
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }
        List projects = rootElement.getChildren("project");
        if (projects.size() == 0) {
            String message = "No projects found in configuration file at " + configFile.getAbsolutePath()
                    + " - quitting...";
            System.err.println(message);
            LOG.error(message);
            System.out.println();
            System.exit(1);
        } else if (projects.size() == 1) {
            project = (Element) projects.get(0);
            String message = "Found one project--using '" + project.getAttributeValue("name") + "'";
            System.out.println(message);
            LOG.info(message);
            System.out.println();
        } else {
            System.out.println("Found multiple projects in configuration file:");
            Iterator iter = projects.iterator();
            for (int i = 0; iter.hasNext(); i++) {
                Element tempProject = (Element) iter.next();
                System.out.println(i + 1 + ") " + tempProject.getAttributeValue("name"));
                LOG.debug("Found project: " + tempProject.getAttributeValue("name"));
            }
            System.out.print("Select project number: ");
            int projectNumber = Integer.parseInt(CONSOLE.readLine());
            if ((projectNumber > projects.size()) || (projectNumber < 1)) {
                String message = "Not a valid project number - quitting...";
                System.err.println(message);
                LOG.error(message);
                System.exit(1);
            }
            project = (Element) projects.get(projectNumber - 1);
            System.out.println();
        }
        return project;
    }

    private Element getBuilderFromProject(Element project) {
        Element builder = null;
        List schedules = project.getChildren("schedule");
        if (schedules.size() == 0) {
            String message = "No schedule for project -- quitting...";
            System.err.println(message);
            LOG.error(message);
            System.exit(1);
        } else if (schedules.size() > 1) {
            String message = "More than one schedule for project -- quitting...";
            System.err.println(message);
            LOG.error(message);
            System.exit(1);
        } else {
            List builders = ((Element) schedules.get(0)).getChildren();
            if (builders.size() == 0) {
                String message = "No builder for project -- quitting...";
                System.err.println(message);
                LOG.error(message);
                System.exit(1);
            } else if (builders.size() > 1) {
                String message = "Multiple builders found -- defaulting to first builder";
                System.out.println(message);
                LOG.warn(message);
            }
            builder = (Element) builders.get(0);
        }
        return builder;
    }

    private ServiceItem[] findAgents(Attribute configEntries) {
        final String searchEntries;
        if (configEntries == null) {
            System.out.println("Enter search entries as comma-separated name/value pairs "
                    + "(e.g. \"os.name=WinNT, fixpack=4.1\")");
            System.out.print("Search entries: ");
            searchEntries = CONSOLE.readLine();
        } else {
            searchEntries = configEntries.getValue();
        }
        LOG.debug("Searching for serviceItems matching entries: " + searchEntries);
        final Entry[] entries = ReggieUtil.convertStringEntries(searchEntries);

        MulticastDiscovery.begin();
        System.out.println("Waiting 5 seconds for registrars to report in...");
        try { Thread.sleep(5 * 1000); } catch (InterruptedException e) {
            // ignore
        }
        final ServiceItem[] serviceItems;
        try {
             serviceItems = MulticastDiscovery.findBuildAgentServices(entries,
                     MulticastDiscovery.DEFAULT_FIND_WAIT_DUR_MILLIS);
        } catch (RemoteException e) {
            e.printStackTrace();
            String message = "Problem occurred finding Build Agents: " + e.getMessage();
            LOG.error(message);
            System.err.println(message);
            throw new RuntimeException(e);
        }
        if (serviceItems.length == 0) {
            String message = "No matches for your search - quitting...";
            System.err.println(message);
            System.out.println();
            LOG.error(message);
            System.exit(1);
        }
        System.out.println();
        return serviceItems;
    }

    private BuildAgentService selectAgent(final ServiceItem[] serviceItems) {
        BuildAgentService agent = null;

        if (serviceItems.length < 1) {
            String message = "No matching serviceItems found - quitting...";
            LOG.error(message);
            System.err.println(message);
            System.exit(1);
        } else if (serviceItems.length == 1) {
            agent = (BuildAgentService) serviceItems[0].service;
            String agentName = null;
            try {
                agentName = agent.getMachineName();
            } catch (RemoteException e) {
                String message = "Error getting machine name from agent - quitting...";
                System.err.println(message);
                LOG.error(message, e);
                System.exit(1);
            }
            String infoMessage = "One matching agent found: " + agentName + " -- selecting it automatically";
            System.out.println(infoMessage);
            LOG.debug(infoMessage);
            System.out.println();
        } else {
            System.out.println("Found serviceItems:");
            String machineName;
            for (int i = 0; i < serviceItems.length; i++) {
                try {
                    machineName = ((BuildAgentService) serviceItems[i].service).getMachineName();
                    System.out.println(i + 1 + ") " + machineName);
                    LOG.debug("Found agent: " + machineName);
                } catch (RemoteException e1) {
                    String message = "Couldn't get machine name for agent - quitting...";
                    LOG.error(message, e1);
                    System.err.println(message + " - " + e1.getMessage());
                    System.exit(1);
                }
            }
            System.out.print("Select agent # or 0 to list status of all serviceItems: ");
            int agentNum = Integer.parseInt(CONSOLE.readLine());
            if ((agentNum > serviceItems.length) || (agentNum < 0)) {
                agent = null;
                String message = "Not a valid agent number - quitting...";
                System.err.println(message);
                LOG.error(message);
                System.exit(1);
            }
            if (agentNum == 0) {
                displayAgentStatuses();
                System.out.println("Done...");
                System.exit(0);
            } else {
                agent = (BuildAgentService) serviceItems[agentNum - 1].service;
                System.out.println();
            }
        }
        return agent;
    }

    private void displayAgentStatuses() {
        // TODO unimplemented
        System.out.println();
        System.err.println("Unimplemented feature - quitting...see BuildAgentUtility");
    }

    private DistributedMasterBuilder doBuild() {

        System.out.println("Beginning build...");
        System.out.println();
        ProjectXMLHelper projectXMLHelper = new ProjectXMLHelper(new ResolverHolder.DummyResolvers());
        PluginXMLHelper pluginXMLHelper = new PluginXMLHelper(projectXMLHelper);

        final DistributedMasterBuilder distributedBuildMaster;
        try {
            distributedBuildMaster = (DistributedMasterBuilder) pluginXMLHelper.configure(
                    distributedBuilderElement, DistributedMasterBuilder.class, false);
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(distributedBuildMaster.build(new HashMap<String, String>(), null), System.out);
        } catch (CruiseControlException e) {
            String message = "Oops...";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            return null;
        } catch (IOException e) {
            String message = "Oops...";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            return null;
        }
        System.out.println();
        return distributedBuildMaster;
    }

    private void retrieveBuildArtifacts(final BuildAgentService agent,
                                        final DistributedMasterBuilder distributedBuildMaster) {
        try {
            final File currentDir = new File(".");
            DistributedMasterBuilder.getResultsFiles(agent, currentDir, "projectInteractive",
                    PropertiesHelper.RESULT_TYPE_LOGS, currentDir);

            DistributedMasterBuilder.getResultsFiles(agent, currentDir, "projectInteractive",
                    PropertiesHelper.RESULT_TYPE_OUTPUT, currentDir);
            
            final RemoteResult[] remoteResults = distributedBuildMaster.getRemoteResultsInfo();
            if (remoteResults != null) {
                for (final RemoteResult remoteResult : remoteResults) {
                    DistributedMasterBuilder.getRemoteResult(agent, currentDir, "projectInteractive", remoteResult);
                }
            }
            agent.clearOutputFiles();
        } catch (RemoteException e) {
            String message = "Problem occurred getting or unzipping results";
            LOG.debug(message);
            System.out.println(message);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            new InteractiveBuildUtility();
        } else {
            new InteractiveBuildUtility(args[0]);
        }
    }

    public static class Console {
        private InputStream inputStream = null;

        public Console() {
            this.inputStream = System.in;
        }

        public String readLine() {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            try {
                return in.readLine().trim();
            } catch (IOException e) {
                String message = "Error reading input";
                LOG.error(message, e);
                System.err.println(message + " - " + e.getMessage());
                throw new RuntimeException(message, e);
            }
        }
    }
}
