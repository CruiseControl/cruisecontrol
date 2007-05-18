/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * A plugin that represents the whole XML config file.
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class CruiseControlConfig {
    private static final Logger LOG = Logger.getLogger(CruiseControlConfig.class);

    public static final String LABEL_INCREMENTER = "labelincrementer";

    public static final boolean FAIL_UPON_MISSING_PROPERTY = false;

    private static final Set KNOWN_ROOT_CHILD_NAMES = new HashSet();
    static {
        KNOWN_ROOT_CHILD_NAMES.add("include.projects");
        KNOWN_ROOT_CHILD_NAMES.add("property");
        KNOWN_ROOT_CHILD_NAMES.add("plugin");
        KNOWN_ROOT_CHILD_NAMES.add("system");
    }

    private Map rootProperties = new HashMap();
    /** Properties of a particular node. Mapped by the node name. Doesn't handle rootProperties yet */
    private Map templatePluginProperties = new HashMap();
    private PluginRegistry rootPlugins = PluginRegistry.createRegistry();
    private Map projects = new LinkedHashMap();
    // for test purposes only
    private Map projectPluginRegistries = new TreeMap();

    private XmlResolver xmlResolver;

    public CruiseControlConfig(Element ccElement) throws CruiseControlException {
        this(ccElement, (XmlResolver) null);
    }

    public CruiseControlConfig(Element ccElement, XmlResolver xmlResolver) throws CruiseControlException {
        this.xmlResolver = xmlResolver;
        parse(ccElement);
    }

    private void parse(Element ccElement) throws CruiseControlException {
        // parse properties and plugins first, so their order in the config file doesn't matter
        for (Iterator i = ccElement.getChildren("property").iterator(); i.hasNext(); ) {
            handleRootProperty((Element) i.next());
        }
        for (Iterator i = ccElement.getChildren("plugin").iterator(); i.hasNext(); ) {
            handleRootPlugin((Element) i.next());
        }
        for (Iterator i = ccElement.getChildren("include.projects").iterator(); i.hasNext(); ) {
            handleIncludedProjects((Element) i.next());
        }

        // other childNodes must be projects or the <system> node
        for (Iterator i = ccElement.getChildren().iterator(); i.hasNext(); ) {
            Element childElement = (Element) i.next();
            final String nodeName = childElement.getName();
            if (isProject(nodeName)) {
                handleProject(childElement);
            } else if (!KNOWN_ROOT_CHILD_NAMES.contains(nodeName)) {
                throw new CruiseControlException("cannot handle child of <" + nodeName + ">");
            }
        }
    }

    private CruiseControlConfig(Element includedElement, CruiseControlConfig parent) throws CruiseControlException {
        xmlResolver = parent.xmlResolver;
        rootPlugins = PluginRegistry.createRegistry(parent.rootPlugins);
        rootProperties = new HashMap(parent.rootProperties);
        templatePluginProperties = new HashMap(parent.templatePluginProperties);

        parse(includedElement);
    }

    private void handleIncludedProjects(Element includeElement) {
        String path = includeElement.getAttributeValue("file");
        if (path == null) {
            LOG.warn("include.projects element missing file attribute. Skipping.");
        }
        if (xmlResolver == null) {
            LOG.debug("xmlResolver not available; skipping include.projects element. ok if validating config.");
            return;
        }
        try {
            path = ProjectXMLHelper.parsePropertiesInString(rootProperties, path, FAIL_UPON_MISSING_PROPERTY);
            LOG.debug("getting included projects from " + path);
            Element includedElement = xmlResolver.getElement(path);
            CruiseControlConfig includedConfig = new CruiseControlConfig(includedElement, this);
            Set includedProjectNames = includedConfig.getProjectNames();
            for (Iterator iter = includedProjectNames.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                if (projects.containsKey(name)) {
                    String message = "Project " + name + " included from " + path + " is a duplicate name. Omitting.";
                    LOG.error(message);
                }
                projects.put(name, includedConfig.getProject(name));
            }
        } catch (CruiseControlException e) {
            LOG.error("Exception including file " + path, e);
        }
    }

    private boolean isProject(String nodeName) throws CruiseControlException {
        return rootPlugins.isPluginRegistered(nodeName)
            &&  ProjectInterface.class.isAssignableFrom(rootPlugins.getPluginClass(nodeName));
    }

    private boolean isProjectTemplate(Element pluginElement) {
        String pluginName = pluginElement.getAttributeValue("name");
        String pluginClassName = pluginElement.getAttributeValue("classname");
        if (pluginClassName == null) {
            pluginClassName = rootPlugins.getPluginClassname(pluginName);
        }
        try {
            Class pluginClass = rootPlugins.instanciatePluginClass(pluginClassName, pluginName);
            return ProjectInterface.class.isAssignableFrom(pluginClass);
        } catch (CruiseControlException e) {
            // this is only triggered by tests today, when a class is not loadable.
            // I didn't want to propagate the exception
            // in case something like Distributed CC requires a class to not be loadable locally at this point...
            LOG.warn("Couldn't check if the plugin " + pluginName + " is an instance of ProjectInterface", e);
            return false;
        }
    }

    private void handleRootPlugin(Element pluginElement) throws CruiseControlException {
        String pluginName = pluginElement.getAttributeValue("name");
        if (pluginName == null) {
            LOG.warn("Config contains plugin without a name-attribute, ignoring it");
            return;
        }
        if (isProjectTemplate(pluginElement)) {
            handleNodeProperties(pluginElement, pluginName);
        }
        rootPlugins.register(pluginElement);
    }

    private void handleNodeProperties(Element pluginElement, String pluginName) {
        List properties = new ArrayList();
        for (Iterator i = pluginElement.getChildren("property").iterator(); i.hasNext();) {
            properties.add(i.next());
        }
        if (properties.size() > 0) {
            templatePluginProperties.put(pluginName, properties);
        }
        pluginElement.removeChildren("property");
    }

    private void handleRootProperty(Element childElement) throws CruiseControlException {
        ProjectXMLHelper.registerProperty(rootProperties, childElement, FAIL_UPON_MISSING_PROPERTY);
    }

    private void handleProject(Element projectElement) throws CruiseControlException {

        String projectName = getProjectName(projectElement);

        if (projects.containsKey(projectName)) {
            final String duplicateEntriesMessage = "Duplicate entries in config file for project name " + projectName;
            throw new CruiseControlException(duplicateEntriesMessage);
        }

        // property handling is a little bit dirty here.
        // we have a set of properties mostly resolved in the rootProperties
        // and a child set of properties
        // it is possible that the rootProperties contain references to child properties
        // in particular the project.name one
        MapWithParent nonFullyResolvedProjectProperties = new MapWithParent(rootProperties);
        // Register the project's name as a built-in property
        LOG.debug("Setting property \"project.name\" to \"" + projectName + "\".");
        nonFullyResolvedProjectProperties.put("project.name", projectName);

        // handle project templates properties
        List projectTemplateProperties = (List) templatePluginProperties.get(projectElement.getName());
        if (projectTemplateProperties != null) {
            for (int i = 0; i < projectTemplateProperties.size(); i++) {
                Element element = (Element) projectTemplateProperties.get(i);
                ProjectXMLHelper.registerProperty(nonFullyResolvedProjectProperties,
                    element, FAIL_UPON_MISSING_PROPERTY);
            }
        }

        // Register any project specific properties
        for (Iterator projProps = projectElement.getChildren("property").iterator(); projProps.hasNext(); ) {
            final Element propertyElement = (Element) projProps.next();
            ProjectXMLHelper.registerProperty(nonFullyResolvedProjectProperties,
                propertyElement, FAIL_UPON_MISSING_PROPERTY);
        }

        // add the resolved rootProperties to the project's properties
        Map thisProperties = nonFullyResolvedProjectProperties.thisMap;
        for (Iterator iterator = rootProperties.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            if (!thisProperties.containsKey(key)) {
                String value = (String) rootProperties.get(key);
                thisProperties.put(key, ProjectXMLHelper.parsePropertiesInString(thisProperties, value, false));
            }
        }

        // Parse the entire element tree, expanding all property macros
        ProjectXMLHelper.parsePropertiesInElement(projectElement, thisProperties, FAIL_UPON_MISSING_PROPERTY);

        // Register any custom plugins
        PluginRegistry projectPlugins = PluginRegistry.createRegistry(rootPlugins);
        for (Iterator pluginIter = projectElement.getChildren("plugin").iterator(); pluginIter.hasNext(); ) {
            projectPlugins.register((Element) pluginIter.next());
        }

        projectElement.removeChildren("property");
        projectElement.removeChildren("plugin");

        LOG.debug("**************** configuring project " + projectName + " *******************");
        ProjectHelper projectHelper = new ProjectXMLHelper(thisProperties, projectPlugins);
        ProjectInterface project;
        
        try {
            project = (ProjectInterface) projectHelper.configurePlugin(projectElement, false);
        } catch (CruiseControlException e) {
            throw new CruiseControlException("error configuring project " + projectName, e);
        }

        // TODO: get rid of this ProjectConfig special case
        if (project instanceof ProjectConfig) {
            ProjectConfig projectConfig = (ProjectConfig) project;

            if (projectConfig.getLabelIncrementer() == null) {
                LabelIncrementer labelIncrementer;
                Class labelIncrClass = projectPlugins.getPluginClass(LABEL_INCREMENTER);
                try {
                    labelIncrementer = (LabelIncrementer) labelIncrClass.newInstance();
                } catch (Exception e) {
                    LOG.error("Error instantiating label incrementer named "
                        + labelIncrClass.getName()
                        + "in project "
                        + projectName
                        + ". Using DefaultLabelIncrementer instead.",
                        e);
                    labelIncrementer = new DefaultLabelIncrementer();
                }
                projectConfig.add(labelIncrementer);
            }
        }

        project.validate();
        LOG.debug("**************** end configuring project " + projectName + " *******************");

        this.projects.put(projectName, project);
        this.projectPluginRegistries.put(projectName, projectPlugins);
    }

    private String getProjectName(Element childElement) throws CruiseControlException {
        if (!isProject(childElement.getName())) {
            throw new IllegalStateException("Invalid Node <" + childElement.getName() + "> (not a project)");
        }
        String rawName = childElement.getAttribute("name").getValue();
        return ProjectXMLHelper.parsePropertiesInString(rootProperties, rawName, false);
    }

    public ProjectInterface getProject(String name) {
        return (ProjectInterface) this.projects.get(name);
    }

    public Set getProjectNames() {
        return Collections.unmodifiableSet(this.projects.keySet());
    }

    PluginRegistry getRootPlugins() {
        return rootPlugins;
    }

    PluginRegistry getProjectPlugins(String name) {
        return (PluginRegistry) this.projectPluginRegistries.get(name);
    }

    // Unfortunately it seems like the commons-collection CompositeMap doesn't fit that role
    // at least size is not implemented the way I want it.
    // TODO is there a clean way to do without this?
    private static class MapWithParent implements Map {
        private Map parent;
        private Map thisMap;

        MapWithParent(Map parent) {
            this.parent = parent;
            this.thisMap = new HashMap();
        }

        public int size() {
            int size = thisMap.size();
            if (parent != null) {
                Set keys = parent.keySet();
                for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                    String key = (String) iterator.next();
                    if (!thisMap.containsKey(key)) {
                        size++;
                    }
                }
            }
            return size;
        }

        public boolean isEmpty() {
            boolean parentIsEmpty = parent == null || parent.isEmpty();
            return parentIsEmpty && thisMap.isEmpty();
        }

        public boolean containsKey(Object key) {
            return thisMap.containsKey(key)
                || (parent != null && parent.containsKey(key));
        }

        public boolean containsValue(Object value) {
            return thisMap.containsValue(value)
                || (parent != null && parent.containsValue(value));
        }

        public Object get(Object key) {
            Object value = thisMap.get(key);
            if (value == null && parent != null) {
                value = parent.get(key);
            }
            return value;
        }

        public Object put(Object o, Object o1) {
            return thisMap.put(o, o1);
        }

        public Object remove(Object key) {
            throw new UnsupportedOperationException("'remove' not supported on MapWithParent");
        }

        public void putAll(Map map) {
            thisMap.putAll(map);
        }

        public void clear() {
            throw new UnsupportedOperationException("'clear' not supported on MapWithParent");
        }

        public Set keySet() {
            Set keys = new HashSet(thisMap.keySet());
            if (parent != null) {
                keys.addAll(parent.keySet());
            }
            return keys;
        }

        public Collection values() {
            throw new UnsupportedOperationException("not implemented");
            /* we have to support the Map contract. Back the returned values. Mmmmm */
            /*
            Collection values = thisMap.values();
            if (parent != null) {
                Set keys = parent.keySet();
                List parentValues = new ArrayList();
                for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                    String key = (String) iterator.next();
                    if (! thisMap.containsKey(key)) {
                        parentValues.add(parent.get(key));
                    }
                }
            }
            return values;
            */
        }

        public Set entrySet() {
            Set entries = new HashSet(thisMap.entrySet());
            if (parent != null) {
                entries.addAll(parent.entrySet());
            }
            return entries;
        }
    }

}
