/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * A plugin that represents the whole XML config file.
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class CruiseControlConfig implements SelfConfiguringPlugin {
    private static final Logger LOG = Logger.getLogger(CruiseControlConfig.class);

    public static final String LABEL_INCREMENTER = "labelincrementer";

    public static final boolean FAIL_UPON_MISSING_PROPERTY = false;
    
    private static final Set KNOWN_ROOT_CHILD_NAMES = new HashSet();
    static {
        KNOWN_ROOT_CHILD_NAMES.add("property");
        KNOWN_ROOT_CHILD_NAMES.add("plugin");
        KNOWN_ROOT_CHILD_NAMES.add("system");
    }

    // Unfortunately it seems like the commons-collection CompositeMap doesn't fit that role
    // at least size is not implemented the way I want it.
    // TODO is there a clean way to do without this?
    static class MapWithParent implements Map {
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

    private Map rootProperties = new HashMap();
    private PluginRegistry rootPlugins = PluginRegistry.createRegistry();
    private Map projectConfigs = new TreeMap();  // TODO: replace with LinkedHashMap when we drop 1.3 support
    private ProjectNameSet projectNames = new ProjectNameSet();  // TODO: remove when we can use LinkedHashMap
    // for test purposes only
    private Map projectPluginRegistries = new TreeMap();

    public CruiseControlConfig() {
    }

    // for testing... Could be used if we had an external property file.
    CruiseControlConfig(Properties globalProperties) {
        this.rootProperties.putAll(globalProperties);
    }

    public void configure(Element rootElement) throws CruiseControlException {
        // parse properties and plugins first, so their order in the config file doesn't matter
        for (Iterator i = rootElement.getChildren("property").iterator(); i.hasNext(); ) {
            handleRootProperty((Element) i.next());
        } 
        for (Iterator i = rootElement.getChildren("plugin").iterator(); i.hasNext(); ) {
            handleRootPlugin((Element) i.next());
        }
        
        // other childNodes must be projects or the <system> node
        for (Iterator i = rootElement.getChildren().iterator(); i.hasNext(); ) {
            Element childElement = (Element) i.next();
            final String nodeName = childElement.getName();
            if (isProject(nodeName)) {
                handleProject(childElement);
            } else if (!KNOWN_ROOT_CHILD_NAMES.contains(nodeName)) {
                throw new CruiseControlException("cannot handle child of <" + nodeName + ">");
            }
        }
    }

    private boolean isProject(String nodeName) throws CruiseControlException {
        return rootPlugins.isPluginRegistered(nodeName)
            &&  ProjectConfig.class.isAssignableFrom(rootPlugins.getPluginClass(nodeName));
    }

    private void handleRootPlugin(Element pluginElement) throws CruiseControlException {
        String pluginName = pluginElement.getAttributeValue("name");
        if (pluginName == null) {
            LOG.warn("Config contains plugin without a name-attribute, ignoring it");
            return;
        }
        rootPlugins.register(pluginElement);
    }

    private void handleRootProperty(Element childElement) throws CruiseControlException {
        ProjectXMLHelper.registerProperty(rootProperties, childElement, FAIL_UPON_MISSING_PROPERTY);
    }

    private void handleProject(Element projectElement) throws CruiseControlException {

        String projectName = getProjectName(projectElement);

        if (projectConfigs.containsKey(projectName)) {
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

        LOG.debug("**************** configuring project" + projectName + " *******************");
        ProjectHelper projectHelper = new ProjectXMLHelper(thisProperties, projectPlugins);
        ProjectConfig projectConfig = (ProjectConfig) projectHelper.configurePlugin(projectElement, false);

        projectConfig.setProperties(thisProperties);

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

        Log log = projectConfig.getLog();
        if (log == null) {
            log = new Log();
        }

        log.setProjectName(projectName);
        log.validate();
        projectConfig.add(log);

        projectConfig.validate();
        LOG.debug("**************** end configuring project" + projectName + " *******************");

        this.projectConfigs.put(projectName, projectConfig);
        projectNames.add(projectName);
        this.projectPluginRegistries.put(projectName, projectPlugins);
    }

    private String getProjectName(Element childElement) throws CruiseControlException {
        if (!isProject(childElement.getName())) {
            throw new IllegalStateException("Invalid Node <" + childElement.getName() + "> (not a project)");
        }
        String rawName = childElement.getAttribute("name").getValue();
        return ProjectXMLHelper.parsePropertiesInString(rootProperties, rawName, false);
    }

    public ProjectConfig getConfig(String name) {
        return (ProjectConfig) this.projectConfigs.get(name);
    }

    public Set getProjectNames() {
// TODO: can go to old implmentation when we drop 1.3 and can use LinkedHashMap
//        return this.projectConfigs.keySet();
        return projectNames;
    }

    PluginRegistry getRootPlugins() {
        return rootPlugins;
    }

    PluginRegistry getProjectPlugins(String name) {
        return (PluginRegistry) this.projectPluginRegistries.get(name);
    }

    /*
     * Allows CC to build projects in the order they are added, which is the
     * order they appear in the config file.
     * 
     * TODO: remove when we can drop 1.3 and use LinkedHashMap for projectConfigs
     */
    private class ProjectNameSet implements Set {
        private ArrayList list = new ArrayList();

        public int size() {
            return list.size();
        }

        public void clear() {
            list.clear();
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }

        public Object[] toArray() {
            return list.toArray();
        }

        public boolean add(Object o) {
            if (o == null) {
                throw new IllegalArgumentException("null not a valid project name");
            }
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("project names must be strings");
            }
            if (list.contains(o)) {
                return false;
            }
            list.add(o);
            return true;
        }

        public boolean contains(Object o) {
            return list.contains(o);
        }

        public boolean remove(Object o) {
            return list.remove(o);
        }

        public boolean addAll(Collection c) {
            return list.addAll(c);
        }

        public boolean containsAll(Collection c) {
            return list.containsAll(c);
        }

        public boolean removeAll(Collection c) {
            return list.removeAll(c);
        }

        public boolean retainAll(Collection c) {
            return list.retainAll(c);
        }

        public Iterator iterator() {
            return list.iterator();
        }

        public Object[] toArray(Object[] a) {
            return list.toArray(a);
        }

    }

}
