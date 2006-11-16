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
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.entry.Entry;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.SelfConfiguringPlugin;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.util.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.util.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.util.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.util.ZipUtil;
import net.sourceforge.cruisecontrol.util.FileUtil;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

public class DistributedMasterBuilder extends Builder implements SelfConfiguringPlugin {

    private static final Logger LOG = Logger.getLogger(DistributedMasterBuilder.class);

    private static final String CRUISE_PROPERTIES = "cruise.properties";
    private static final String CRUISE_RUN_DIR = "cruise.run.dir";

    // TODO: Change to property?
    private static final long DEFAULT_CACHE_MISS_WAIT = 30000;
    private boolean isFailFast;

    // TODO: Can we get the module from the projectProperties instead of setting
    // it via an attribute?
    //  Could be set in ModificationSet...
    private String entries;
    private String module;

    private String agentLogDir;
    private String agentOutputDir;

    private String masterLogDir;
    private String masterOutputDir;

    private Element childBuilderElement;
    private String overrideTarget;
    private MulticastDiscovery discovery;
    private File rootDir;

    protected void overrideTarget(final String target) {
        overrideTarget = target;
    }

    Element getChildBuilderElement() {
        return childBuilderElement;
    }

    /** If true, available agent lookup will not block until an agent is found,
     * but will return null immediately. */
    public synchronized void setFailFast(final boolean isFailFast) {
        this.isFailFast = isFailFast;
    }
    private synchronized  boolean isFailFast() {
        return isFailFast;
    }

    /** Intended only for use by unit tests. **/
    void setDiscovery(final MulticastDiscovery multicastDiscovery) {
        discovery = multicastDiscovery;
    }
    MulticastDiscovery getDiscovery() {
        if (discovery == null) {
            final Entry[] arrEntries = ReggieUtil.convertStringEntries(entries);
            discovery = new MulticastDiscovery(arrEntries);
        }

        return discovery;
    }

    /**
     * 
     * @param element
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     */
    public void configure(final Element element) throws CruiseControlException {
        final List children = element.getChildren();
        if (children.size() > 1) {
            final String message = "DistributedMasterBuilder can only have one nested builder";
            LOG.error(message);
            throw new CruiseControlException(message);
        } else if (children.size() == 0) {
            // @todo Clarify when configure() can be called...
            final String message = "Nested Builder required by DistributedMasterBuilder, "
                    + "ignoring and assuming this call is during plugin-preconfig";
            LOG.warn(message);
            return;
        }
        childBuilderElement = (Element) children.get(0);
        // Add default/preconfigured props to builder element
        addMissingPluginDefaults(childBuilderElement);

        // Add default/preconfigured props to distributed element
        addMissingPluginDefaults(element);

        Attribute tempAttribute = element.getAttribute("entries");
        entries = tempAttribute != null ? tempAttribute.getValue() : "";

        tempAttribute = element.getAttribute("module");
        if (tempAttribute != null) {
            module = tempAttribute.getValue();
        } else {
            // try to use project name as default value
            final Element elmProj = getElementProject(element);
            if (elmProj != null) {
                module = elmProj.getAttributeValue("name");
            } else {
                module = null;
            }
        }

        // optional attributes
        tempAttribute = element.getAttribute("agentlogdir");
        setAgentLogDir(tempAttribute != null ? tempAttribute.getValue() : null);

        tempAttribute = element.getAttribute("agentoutputdir");
        setAgentOutputDir(tempAttribute != null ? tempAttribute.getValue() : null);

        tempAttribute = element.getAttribute("masterlogdir");
        setMasterLogDir(tempAttribute != null ? tempAttribute.getValue() : null);

        tempAttribute = element.getAttribute("masteroutputdir");
        setMasterOutputDir(tempAttribute != null ? tempAttribute.getValue() : null);

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
                && (getAgentLogDir() == null && getMasterLogDir() == null)
                && (getAgentOutputDir() == null && getMasterOutputDir() == null)
        ) {
            final String message = "Could not get property " + CRUISE_RUN_DIR + " from " + CRUISE_PROPERTIES
                    + ", or run dir does not exist: " + rootDir;
            LOG.error(message);
            System.err.println(message);
            throw new CruiseControlException(message);
        }

        validate();
    }

    /** Package visisble since also used by unit tests to apply plugin default values. */
    static void addMissingPluginDefaults(final Element elementToAlter) {
        LOG.debug("Adding missing defaults for plugin: " + elementToAlter.getName());
        final Map pluginDefaults = getPluginDefaults(elementToAlter);
        applyPluginDefaults(pluginDefaults, elementToAlter);
    }

    private static void applyPluginDefaults(final Map pluginDefaults, final Element elementToAlter) {
        final String pluginName = elementToAlter.getName();
        // to preserve precedence, only add default attribute if it is not also defined in the tag directly
        final Set defaultAttribMapKeys = pluginDefaults.keySet();
        for (Iterator itrKeys = defaultAttribMapKeys.iterator(); itrKeys.hasNext();) {
            final String attribName = (String) itrKeys.next();
            final String attribValueExisting = elementToAlter.getAttributeValue(attribName);
            if (attribValueExisting == null) { // skip existing attribs
                final String attribValue = (String) pluginDefaults.get(attribName);
                elementToAlter.setAttribute(attribName, attribValue);
                LOG.debug("Added plugin " + pluginName + " default attribute: " + attribName + "=" + attribValue);
            } else {
                LOG.debug("Skipping plugin " + pluginName + " overidden attribute: " + attribName
                        + "=" + attribValueExisting);
            }
        }
    }

    private static Map getPluginDefaults(final Element elementToAlter) {

        final PluginRegistry pluginsRegistry = PluginRegistry.createRegistry();
        final Map pluginDefaults = new HashMap();
        // note: the map returned here is "unmodifiable"
        pluginDefaults.putAll(pluginsRegistry.getDefaultProperties(elementToAlter.getName()));

        if (pluginDefaults.size() == 0) { // maybe we're in a unit test
            // @todo Remove this kludge when we figure out how to make PluginRegistry work in unit test
            LOG.warn("Unit Test kludge for plugin default values. "
                    + "Should happen only if no default plugin settings exist OR during unit tests.");
            final Element elemCC = getElementCruiseControl(elementToAlter);
            // bail out if we can't find CruiseControl element, since there may actually
            // be no defaults for this element
            if (elemCC == null) {
                return pluginDefaults;
            }

            final List plugins = elemCC.getChildren("plugin");
            final Map pluginDefaultsHack = new HashMap();
            for (int i = 0; i < plugins.size(); i++) {
                final Element plugin = (Element) plugins.get(i);
                if (elementToAlter.getName().equals(plugin.getAttributeValue("name"))) {
                    // iterate attribs
                    final List attribs = plugin.getAttributes();
                    for (int j = 0; j < attribs.size(); j++) {
                        final Attribute attribute = (Attribute) attribs.get(j);
                        final String attribName = attribute.getName();
                        // skip certain attribs
                        if (!"name".equals(attribName)) { // ignore "name" attrib of default plugin declaration
                            pluginDefaultsHack.put(attribName, attribute.getValue());
                        }
                    }

                    //@todo Handle preconfigured Child elements
                    // handle any child elements, like <property> elements
                    final List pluginChildren = plugin.getChildren();
                    for (int k = 0; k < pluginChildren.size(); k++) {
                        final Element child = (Element) pluginChildren.get(k);
                        final Attribute childAttrName = child.getAttribute("name");
                        LOG.error("WARNING!!! Distributed Builders do not yet handle nested child elements! Element: "
                                + childAttrName.getValue() + " will not work on the BuildAgent.");
//                        final List childAttribs = child.getAttributes();
//                        for (int j = 0; j < childAttribs.size(); j++) {
//                            final Attribute childAttribute = (Attribute) childAttribs.get(j);
//                            final String childAttribName = childAttribute.getName();
//                            // skip certain attribs
//                            if (!"name".equals(childAttribName)) { // ignore "name" attrib 
//                                pluginDefaultsHack.put(childAttribName, childAttribute.getValue());
//                            }
//                        }
                    }
                }
            }
            // put kludge results into returned map
            pluginDefaults.putAll(pluginDefaultsHack);
        }

        return Collections.unmodifiableMap(pluginDefaults);
    }

    private static Element getElementCruiseControl(Element element) {
        LOG.debug("Searching for CC root element, starting at: " + element.toString());
        while (!"cruisecontrol".equals(element.getName().toLowerCase())) {
            element = element.getParentElement();
            LOG.debug("Searching for CC root element, moved up to: "
                    + (element != null ? element.toString() : "Augh! parent element is null"));
            if (element == null) {
                LOG.warn("Searching for CC root element, not found.");
                break;
            }
        }
        return element;
    }

    /** Used to get default value for "module" attribute if not given. */
    private static Element getElementProject(Element element) {
        LOG.debug("Searching for Project element, starting at: " + element.toString());
        while (!"project".equals(element.getName().toLowerCase())) {
            element = element.getParentElement();
            LOG.debug("Searching for Project element, moved up to: "
                    + (element != null ? element.toString() : "Augh! parent element is null"));
            if (element == null) {
                LOG.warn("Searching for Project element, not found.");
                break;
            }
        }
        return element;
    }

    public void validate() throws CruiseControlException {
        super.validate();
        final Element elmChildBuilder = getChildBuilderElement();
        if (elmChildBuilder == null) {
            final String message = "A nested Builder is required for DistributedMasterBuilder";
            LOG.warn(message);
            throw new CruiseControlException(message);
        }

        // Add default/preconfigured props to builder element
        addMissingPluginDefaults(elmChildBuilder);

        /* @todo Should we call validate() on the child builder?
        // If so, figure out how to do in unit tests.
        // One problem is config file properties, like: "anthome="${env.ANT_HOME}" don't get expanded...
        final PluginXMLHelper pluginXMLHelper = PropertiesHelper.createPluginXMLHelper(overrideTarget);
        PluginRegistry plugins = PluginRegistry.createRegistry();
        Class pluginClass = plugins.getPluginClass(elmChildBuilder.getName());
        // this dies due to "anthome="${env.ANT_HOME}" in config file not being expanded...
        final Builder builder = (Builder) pluginXMLHelper.configure(elmChildBuilder, pluginClass, false);
        builder.validate();
        //*/

        if (module == null) {
            final String message = "The 'module' attribute is required for DistributedMasterBuilder";
            LOG.warn(message);
            throw new CruiseControlException(message);
        }
    }

    /* Override base schedule methods to expose child-builder values. Otherwise, schedules are not honored.*/
    public int getDay() {
        // @todo Replace with real Builder object if possible
        final String value = childBuilderElement.getAttributeValue("day");
        final int retVal;
        if (value == null) {
            retVal = NOT_SET;
        } else {
            retVal = Integer.parseInt(value);
        }
        return retVal;
    }
    /* Override base schedule methods to expose child-builder values. Otherwise, schedules are not honored.*/
    public int getTime() {
        // @todo Replace with real Builder object if possible
        final String value = childBuilderElement.getAttributeValue("time");
        final int retVal;
        if (value == null) {
            retVal = NOT_SET;
        } else {
            retVal = Integer.parseInt(value);
        }
        return retVal;
    }
    /* Override base schedule methods to expose child-builder values. Otherwise, schedules are not honored.*/
    public int getMultiple() {
        // @todo Replace with real Builder object if possible
        final String value = childBuilderElement.getAttributeValue("multiple");
        final int retVal;
        if (getTime() != NOT_SET) {
            // can't use both time and multiple
            retVal = NOT_SET;
        } else if (value == null) {
            // no multiple attribute is set
            // use default multiple value
            retVal = 1;
        } else {
            retVal = Integer.parseInt(value);
        }
        return retVal;
    }

    public Element buildWithTarget(Map properties, String target) throws CruiseControlException {
        String oldOverideTarget = overrideTarget;
        overrideTarget(target);
        try {
            return build(properties);
        } finally {
            overrideTarget(oldOverideTarget);
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
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR, getAgentLogDir());
                distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_OUTPUTDIR, getAgentOutputDir());

                // set Build Agent logging to debug if the Master has debug enabled
                if (LOG.isDebugEnabled()) {
                    distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_DEBUG, "true");
                }

                LOG.debug("Distributed Agent Props: " + distributedAgentProps.toString());
                
                LOG.debug("Project Props: " + projectProperties.toString());
                
                LOG.info("Starting remote build on agent: " + agent.getMachineName() + " of module: " + module);
                buildResults = agent.doBuild(getChildBuilderElement(), projectProperties, distributedAgentProps);

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


                String masterDir;
                if (getMasterLogDir() == null || "".equals(getMasterLogDir())) {
                    masterDir = rootDirPath + File.separator + PropertiesHelper.RESULT_TYPE_LOGS;
                } else {
                    masterDir = getMasterLogDir();
                }
                getResultsFiles(agent, PropertiesHelper.RESULT_TYPE_LOGS, rootDirPath, masterDir);


                if (getMasterOutputDir() == null || "".equals(getMasterOutputDir())) {
                    masterDir = rootDirPath + File.separator + PropertiesHelper.RESULT_TYPE_OUTPUT;
                } else {
                    masterDir = getMasterOutputDir();
                }
                getResultsFiles(agent, PropertiesHelper.RESULT_TYPE_OUTPUT, rootDirPath, masterDir);

                agent.clearOutputFiles();
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

    public static void getResultsFiles(final BuildAgentService agent, final String resultsType,
                                       final String rootDirPath, final String masterDir)
            throws RemoteException {

        if (agent.resultsExist(resultsType)) {
            final String zipFilePath = FileUtil.bytesToFile(agent.retrieveResultsAsZip(resultsType), rootDirPath,
                    resultsType + ".zip");
            try {
                LOG.info("unzip " + resultsType + " (" + zipFilePath + ") to: " + masterDir);
                ZipUtil.unzipFileToLocation(zipFilePath, masterDir);
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

    protected BuildAgentService pickAgent() throws CruiseControlException {
        BuildAgentService agent = null;

        while (agent == null) {
            final ServiceItem serviceItem;
            try {
                serviceItem = getDiscovery().findMatchingService();
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

    public String getAgentLogDir() {
        return agentLogDir;
    }

    public void setAgentLogDir(final String agentLogDir) {
        this.agentLogDir = agentLogDir;
    }

    public String getAgentOutputDir() {
        return agentOutputDir;
    }

    public void setAgentOutputDir(final String agentOutputDir) {
        this.agentOutputDir = agentOutputDir;
    }

    public String getMasterLogDir() {
        return masterLogDir;
    }

    public void setMasterLogDir(final String masterLogDir) {
        this.masterLogDir = masterLogDir;
    }

    public String getMasterOutputDir() {
        return masterOutputDir;
    }

    public void setMasterOutputDir(final String masterOutputDir) {
        this.masterOutputDir = masterOutputDir;
    }
}
