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

package net.sourceforge.cruisecontrol.distributed.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownServiceException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import net.jini.core.lookup.ServiceRegistrar;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilder;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.util.FileUtil;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class InteractiveBuildUtility {

    private static final Logger LOG = Logger.getLogger(InteractiveBuildUtility.class);

    static Console console = new Console(System.in);
    private String searchEntries;
    private MulticastDiscovery discovery = new MulticastDiscovery();
    private List agents;
    private BuildAgentService agent;
    private Element distributedBuilderElement;

    public InteractiveBuildUtility() {
        System.out.print("Enter path to Cruise Control configuration file: ");
        String configFilePath = console.readLine();
        new InteractiveBuildUtility(configFilePath);
    }
    
    public InteractiveBuildUtility(String configFilePath) {
        File configFile = new File(configFilePath);
        if (!configFile.exists() || configFile.isDirectory()) {
            String message = configFilePath + " does not exist or is a directory - quitting...";
            System.err.println(message);
            LOG.error(message);
            System.exit(1);
        }
        Element project = getProjectFromConfig(configFile);
        distributedBuilderElement = getBuilderFromProject(project);
        agents = findAgents(distributedBuilderElement.getAttribute("entries"));
        agent = selectAgent();
        doBuild();
        retrieveResults(distributedBuilderElement.getAttribute("module").getValue());
    }

    private Element getProjectFromConfig(File configFile) {
        Element rootElement = null;
        Element project = null;
        try {
            rootElement = Util.loadConfigFile(configFile);
	    } catch (CruiseControlException e) {
	        System.err.println(e.getMessage());
	        LOG.error(e.getMessage(), e);
	        System.exit(1);
	    }
	    List projects = rootElement.getChildren("project");
	    if (projects.size() == 0) {
	        String message = "No projects found in configuration file at " + configFile.getAbsolutePath() + " - quitting...";
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
		    for (int i=0; iter.hasNext(); i++) {
	            Element tempProject = (Element) iter.next();
                System.out.println(i + 1 + ") " + tempProject.getAttributeValue("name"));
                LOG.debug("Found project: " + tempProject.getAttributeValue("name"));
		    }
		    System.out.print("Select project number: ");
	        int projectNumber = Integer.parseInt(console.readLine());
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

    private List findAgents(Attribute configEntries) {
        if (configEntries == null) {
	        System.out.println("Enter search entries as comma-separated name/value pairs "
	                + "(e.g. \"os.name=WinNT, fixpack=4.1\")");
	        System.out.print("Search entries: ");
	        searchEntries = console.readLine();
        } else {
            searchEntries = configEntries.getValue();
        }
        LOG.debug("Searching for agents matching entries: " + searchEntries);
        ServiceRegistrar registrar = null;
        try {
            registrar = (ServiceRegistrar) discovery.getRegistrar(5000);
            LOG.debug("Found registrar: " + registrar.getServiceID());
        } catch (UnknownServiceException e) {
            String message = "No registrar found after 5 seconds - quitting";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            System.exit(1);
        }
        List entriesList = ReggieUtil.convertStringEntriesToList(searchEntries);
        List agents = ReggieUtil.findServicesForEntriesList(registrar, entriesList, BuildAgentService.class);
        if (agents.size() == 0) {
            String message = "No matches for your search - quitting...";
            System.err.println(message);
            System.out.println();
            LOG.error(message);
            System.exit(1);
        }
        System.out.println();
        return agents;
    }

    private BuildAgentService selectAgent() {
        if (agents.size() < 1) {
            String message = "No matching agents found - quitting...";
            LOG.error(message);
            System.err.println(message);
            System.exit(1);
        } else if (agents.size() == 1){
            agent = (BuildAgentService) agents.get(0);
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
            System.out.println("Found agents:");
            String machineName = null;
            for (int i = 0; i < agents.size(); i++) {
                try {
                    machineName = ((BuildAgentService) agents.get(i)).getMachineName();
                    System.out.println(i + 1 + ") " + machineName);
                    LOG.debug("Found agent: " + machineName);
                } catch (RemoteException e1) {
                    String message = "Couldn't get machine name for agent - quitting...";
                    LOG.error(message, e1);
                    System.err.println(message + " - " + e1.getMessage());
                    System.exit(1);
                }
            }
	        System.out.print("Select agent # or 0 to list status of all agents: ");
	        int agentNum = Integer.parseInt(console.readLine());
	        if ((agentNum > agents.size()) || (agentNum < 0)) {
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
		        BuildAgentService agent = (BuildAgentService) agents.get(agentNum - 1);
		        System.out.println();
	        }
        }
        return agent;
    }

    private void displayAgentStatuses() {
        // TODO unimplemented
        System.out.println();
        System.err.println("Unimplemented feature - quitting...");
    }

    private void doBuild() {
        
        System.out.println("Beginning build...");
        System.out.println();
        ProjectXMLHelper projectXMLHelper = new ProjectXMLHelper();
        PluginXMLHelper pluginXMLHelper = new PluginXMLHelper(projectXMLHelper);

        try {
            DistributedMasterBuilder distributedBuildMaster = (DistributedMasterBuilder) pluginXMLHelper.configure(
                    distributedBuilderElement, DistributedMasterBuilder.class, false);
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(distributedBuildMaster.build(new Properties()), System.out);
        } catch (CruiseControlException e) {
            String message = "Oops...";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
        } catch (IOException e) {
            String message = "Oops...";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private void retrieveResults(String moduleName) {
        String resultsFileName = "logs.zip";
        String resultsType = "logs";
        try {
            if (agent.resultsExist(resultsType)) {
                String zipFilePath = FileUtil.bytesToFile(agent.retrieveResultsAsZip(resultsType), ".", resultsFileName);
            } else {
                String message = "No results returned for logs";
                LOG.debug(message);
                System.out.println(message);
            }
            resultsFileName = "output.zip";
            resultsType = "output";
            if (agent.resultsExist(resultsType)) {
                String zipFilePath = FileUtil.bytesToFile(agent.retrieveResultsAsZip(resultsType), ".", resultsFileName);
            } else {
                String message = "No results returned for output";
                LOG.debug(message);
                System.out.println(message);
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
            InteractiveBuildUtility interactiveBuildUtility = new InteractiveBuildUtility();
        } else {
            InteractiveBuildUtility interactiveController = new InteractiveBuildUtility(args[0]);
        }
    }

    public static class Console {
        private InputStream inputStream = null;

        public Console(InputStream in) {
            this.inputStream = in;
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
