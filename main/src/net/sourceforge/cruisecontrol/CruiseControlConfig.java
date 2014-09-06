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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.cruisecontrol.config.DashboardConfigurationPlugin;
import net.sourceforge.cruisecontrol.config.DefaultPropertiesPlugin;
import net.sourceforge.cruisecontrol.config.IncludeProjectsPlugin;
import net.sourceforge.cruisecontrol.config.PluginPlugin;
import net.sourceforge.cruisecontrol.config.PropertiesPlugin;
import net.sourceforge.cruisecontrol.config.SystemPlugin;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
@Description(
    "The root element of the configuration, acting as a container to the rest of "
    + "the configuration elements.")
public class CruiseControlConfig {
    private static final Logger LOG = Logger.getLogger(CruiseControlConfig.class);

    public static final String LABEL_INCREMENTER = "labelincrementer";

    public static final boolean FAIL_UPON_MISSING_PROPERTY = false;

    private static final Map<String, ProjectQuery> PROJECTS_REGISTRY = new HashMap<String, ProjectQuery>();
    private static final Set<String> KNOWN_ROOT_CHILD_NAMES = new HashSet<String>();
    static {
        KNOWN_ROOT_CHILD_NAMES.add("include.projects");
        KNOWN_ROOT_CHILD_NAMES.add("property");
        KNOWN_ROOT_CHILD_NAMES.add("plugin");
        KNOWN_ROOT_CHILD_NAMES.add("system");
        KNOWN_ROOT_CHILD_NAMES.add("dashboard");
    }

    private Map<String, String> rootProperties = new HashMap<String, String>();
    /**
     * Properties of a particular node. Mapped by the node name. Doesn't handle
     * rootProperties yet
     */
    private Map<String, List> templatePluginProperties = new HashMap<String, List>();
    private PluginRegistry rootPlugins = PluginRegistry.createRegistry();
    private final Map<String, ProjectInterface> projects = new LinkedHashMap<String, ProjectInterface>();
    // for test purposes only
    private final Map<String, PluginRegistry> projectPluginRegistries = new TreeMap<String, PluginRegistry>();

    private final ResolverHolder resolvers;

    private SystemPlugin system;

    /**
     * Returns the interface through which the state of a project of the given name can be queries.
     * @param name the name of project to be found
     * @return the instance of ProjectChecker for the given name
     * @throws CruiseControlException if no such project is registered
     * @note the method must be static in order to be accessible from objects not holding reference to the
     * {@link CruiseControlConfig}. It is partially duplication of {@link #getProject(String)} method.
     */
    public static ProjectQuery findProject(final String name) throws CruiseControlException {
        if (PROJECTS_REGISTRY.containsKey(name)) {
            return PROJECTS_REGISTRY.get(name);
        }
        // No such project
        throw new CruiseControlException("No project named '" + name + "'");
    }

    public int getMaxNbThreads() {
        if (system != null) {
            if (system.getConfig() != null) {
                if (system.getConfig().getThreads() != null) {
                    return system.getConfig().getThreads().getCount();
                }
            }
        }
        return 1;
    }

    private final CruiseControlController controller;

    private final Set<String> customPropertiesPlugins = new HashSet<String>();

    public CruiseControlConfig(final Element ccElement) throws CruiseControlException {
        this(ccElement, new ResolverHolder.DummeResolvers(), null);
    }

    public CruiseControlConfig(final Element ccElement, final CruiseControlController controller)
            throws CruiseControlException {

        this(ccElement, new ResolverHolder.DummeResolvers(), controller);
    }

    public CruiseControlConfig(final Element ccElement, final ResolverHolder resolvers)
            throws CruiseControlException {
        this(ccElement, resolvers, null);
    }

    public CruiseControlConfig(final Element ccElement, final ResolverHolder resolvers,
            final CruiseControlController controller) throws CruiseControlException {
        this.resolvers = resolvers;
        this.controller = controller;
        parse(ccElement);
    }

    private void parse(final Element ccElement) throws CruiseControlException {
        // parse properties and plugins first, so their order in the config file
        // doesn't matter
        for (final Object o : ccElement.getChildren("property")) {
            handleRootProperty((Element) o);
        }
        for (final Object o : ccElement.getChildren("plugin")) {
            handleRootPlugin((Element) o);
        }

        // handle custom properties after plugin registration and before projects
        for (final Object o : ccElement.getChildren()) {
            final Element childElement = (Element) o;
            final String nodeName = childElement.getName();
            if (KNOWN_ROOT_CHILD_NAMES.contains(nodeName)
                    || "system".equals(nodeName)
                    || isProject(nodeName)) {
                continue;
            }
            if (isCustomPropertiesPlugin(nodeName)) {
                handleCustomRootProperty(childElement);
            }
        }

        for (final Object o : ccElement.getChildren("include.projects")) {
            handleIncludedProjects((Element) o);
        }
        for (final Object o : ccElement.getChildren("dashboard")) {
            handleDashboard((Element) o);
        }

        // other childNodes must be projects or the <system> node
        for (final Object o : ccElement.getChildren()) {
            final Element childElement = (Element) o;
            final String nodeName = childElement.getName();
            if (isProject(nodeName)) {
                handleProject(childElement);
            } else if ("system".equals(nodeName)) {
                add((SystemPlugin) new ProjectXMLHelper(resolvers).configurePlugin(childElement, false));
            } else if (!KNOWN_ROOT_CHILD_NAMES.contains(nodeName) && !customPropertiesPlugins.contains(nodeName)) {
                throw new CruiseControlException("cannot handle child of <" + nodeName + ">");
            }
        }
    }

    private CruiseControlConfig(final Element includedElement, final CruiseControlConfig parent)
            throws CruiseControlException {

        this.controller = parent.controller;
        resolvers = parent.resolvers;
        rootPlugins = PluginRegistry.createRegistry(parent.rootPlugins);
        rootProperties = new HashMap<String, String>(parent.rootProperties);
        templatePluginProperties = new HashMap<String, List>(parent.templatePluginProperties);

        parse(includedElement);
    }

    private void handleIncludedProjects(final Element includeElement) {
        final String path = includeElement.getAttributeValue("file");
        if (path == null) {
            LOG.warn("include.projects element missing file attribute. Skipping.");
        }
        if (resolvers == null || resolvers.getXmlResolver() == null) {
            LOG.debug("xmlResolver not available; skipping include.projects element. ok if validating config.");
            return;
        }
        if (resolvers.getXmlResolver() instanceof XmlResolver.DummyResolver) {
            LOG.debug("dummy xmlResolver available only; changes in project config will not be reflected.");
        }
        try {
            final IncludeProjectsPlugin includeProjects
                    = (IncludeProjectsPlugin) new ProjectXMLHelper(rootProperties, this
                    .getRootPlugins(), resolvers).configurePlugin(includeElement, FAIL_UPON_MISSING_PROPERTY);
            add(includeProjects);
        } catch (CruiseControlException e) {
            LOG.error("Exception including file " + path, e);
        }
    }

    private void handleDashboard(final Element dashboardElement) throws CruiseControlException {
        final DashboardConfigurationPlugin dashboard =
                (DashboardConfigurationPlugin) new ProjectXMLHelper(rootProperties, getRootPlugins(), resolvers)
                        .configurePlugin(dashboardElement, FAIL_UPON_MISSING_PROPERTY);
        dashboard.setController(controller);
        dashboard.validate();
        dashboard.startPostingToDashboard();
    }

    private boolean isCustomPropertiesPlugin(String nodeName) throws CruiseControlException {
        if (customPropertiesPlugins.contains(nodeName)) {
            return true;
        }

        boolean isPropetiesPlugin = rootPlugins.isPluginRegistered(nodeName)
                && PropertiesPlugin.class.isAssignableFrom(rootPlugins.getPluginClass(nodeName));

        if (isPropetiesPlugin) {
            customPropertiesPlugins.add(nodeName);
        }

        return isPropetiesPlugin;
    }

    private boolean isProject(String nodeName) throws CruiseControlException {
        return rootPlugins.isPluginRegistered(nodeName)
                && ProjectInterface.class.isAssignableFrom(rootPlugins.getPluginClass(nodeName));
    }

    private boolean isProjectTemplate(Element pluginElement) {
        rootPlugins.from2classname(pluginElement);

        String pluginName = pluginElement.getAttributeValue("name");
        String pluginClassName = pluginElement.getAttributeValue("classname");
//      String pluginFrom = pluginElement.getAttributeValue("from");
//        if (pluginClassName == null && pluginFrom != null) {
//            pluginClassName = rootPlugins.getPluginClassname(pluginFrom);
//            // No standard plugin
//            if (pluginClassName == null) {
//                LOG.warn("<plugin name = '" + pluginName + "' from = '" + pluginFrom
//                       + "'> does not contain in-built element name");
//                return false;
//            }
//            // Create "standard" plugin element
//            pluginElement.setAttribute("classname", pluginClassName);
//            pluginElement.removeAttribute("from");
//        }
        if (pluginClassName == null) {
            pluginClassName = rootPlugins.getPluginClassname(pluginName);
        }
        try {
            Class pluginClass = rootPlugins.instanciatePluginClass(pluginClassName, pluginName);
            return ProjectInterface.class.isAssignableFrom(pluginClass);
        } catch (CruiseControlException e) {
            // this is only triggered by tests today, when a class is not
            // loadable.
            // I didn't want to propagate the exception
            // in case something like Distributed CC requires a class to not be
            // loadable locally at this point...
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

    private void handleNodeProperties(final Element pluginElement, final String pluginName) {
        final List<Object> properties = new ArrayList<Object>();
        final Set<String> propnodes = new HashSet<String>();

        for (final Object o : pluginElement.getChildren()) {
            final Element childElement = (Element) o;
            final String childName = childElement.getName();
            try {
                if ("property".equals(childName) || isCustomPropertiesPlugin(childName)) {
                    properties.add(o);
                    propnodes.add(childName);
                }
            } catch (CruiseControlException e) {
                LOG.error("Unable to register property " + childElement.getName(), e);
            }
        }
        if (properties.size() > 0) {
            templatePluginProperties.put(pluginName, properties);
        }
        // Remove the nodes from the element
        for (String name : propnodes) {
            pluginElement.removeChildren(name);
        }
    }

    private void handleRootProperty(final Element childElement) throws CruiseControlException {
        ProjectXMLHelper.registerProperty(rootProperties, childElement, resolvers,
                FAIL_UPON_MISSING_PROPERTY);
    }

    private void handleCustomRootProperty(final Element childElement) throws CruiseControlException {
        ProjectXMLHelper.registerCustomProperty(rootProperties, childElement, resolvers,
                FAIL_UPON_MISSING_PROPERTY, PluginRegistry.createRegistry(rootPlugins));
    }

    /**
     * @param project other project to add
     * @throws CruiseControlException when something breaks
     */
    @Description("Add projects defined in other configuration files.")
    public void add(final IncludeProjectsPlugin project) throws CruiseControlException {
        final String file = project.getFile();
        final String path = Util.parsePropertiesInString(rootProperties, file, FAIL_UPON_MISSING_PROPERTY);
        LOG.debug("getting included projects from " + path);
        final Element includedElement = resolvers.getXmlResolver().getElement(path);
        final CruiseControlConfig includedConfig = new CruiseControlConfig(includedElement, this);
        final Set<String> includedProjectNames = includedConfig.getProjectNames();
        for (final String name : includedProjectNames) {
            if (projects.containsKey(name)) {
                final String message = "Project " + name + " included from " + path + " is a duplicate name. Omitting.";
                LOG.error(message);
            }
            final ProjectInterface projobj = includedConfig.getProject(name); 
            projects.put(name, projobj);
            PROJECTS_REGISTRY.put(name, projobj);
        }
    }

    /**
     * @param system system place holder plugin
     */
    @Description(
        "Currently just a placeholder for the <code>&lt;configuration&gt;</code> element, which in "
        + "its turn is just a placeholder for the <code>&lt;threads&gt;</code> element. We expect that "
        + "in the future, more system-level features can be configured under this "
        + "element.")
    @Cardinality(min = 0, max = 1)
    public void add(final SystemPlugin system) {
        this.system = system;
    }

    /**
     * @param plugin only for gendoc
     * @deprecated exists only for gendoc, should not be called.
     */
    @Description("Registers a classname with an alias.")
    public void add(final PluginPlugin plugin) {
        // FIXME this is empty today for the documentation to be generated properly
        throw new IllegalStateException("GenDoc-only method should not be invoked.");
    }

    /**
     * @param project only for gendoc
     * @deprecated exists only for gendoc, should not be called.
     */
    @Description("Defines a basic unit of work.")
    @Cardinality(min = 1, max = -1)
    public void add(final ProjectInterface project) {
        // FIXME this is empty today for the documentation to be generated properly
        throw new IllegalStateException("GenDoc-only method should not be invoked.");
    }

    /**
     * @param plugin only for gendoc
     * @deprecated exists only for gendoc, should not be called.
     */
    @Description("Defines a name/value pair used in configuration.")
    public void add(final DefaultPropertiesPlugin plugin) {
        // FIXME currently only declared for documentation generation purposes
        throw new IllegalStateException("GenDoc-only method should not be invoked.");
    }

    /**
     * @param dashboard only for gendoc
     * @deprecated exists only for gendoc, should not be called.
     */
    @Description("Configures dashboard-related settings.")
    @Cardinality(min = 0, max = 1)
    public void add(final DashboardConfigurationPlugin dashboard) {
        // FIXME this is empty today for the documentation to be generated properly
        throw new IllegalStateException("GenDoc-only method should not be invoked.");
    }

    private void handleProject(final Element projectElement) throws CruiseControlException {

        final String projectName = getProjectName(projectElement);
        final Set<String> projectProps = new HashSet<String>();

        if (projects.containsKey(projectName)) {
            final String duplicateEntriesMessage = "Duplicate entries in config file for project name " + projectName;
            throw new CruiseControlException(duplicateEntriesMessage);
        }

        // property handling is a little bit dirty here.
        // we have a set of properties mostly resolved in the rootProperties
        // and a child set of properties
        // it is possible that the rootProperties contain references to child
        // properties
        // in particular the project.name one
        final MapWithParent nonFullyResolvedProjectProperties = new MapWithParent(rootProperties);
        // Register the project's name as a built-in property
        LOG.debug("Setting property \"project.name\" to \"" + projectName + "\".");
        nonFullyResolvedProjectProperties.put("project.name", projectName);

        // Register any project specific properties
        for (final Object o : projectElement.getChildren("property")) {
            final Element propertyElement = (Element) o;
            final String propertyName = propertyElement.getAttributeValue("name");
            ProjectXMLHelper.registerProperty(nonFullyResolvedProjectProperties, propertyElement,
                    resolvers, FAIL_UPON_MISSING_PROPERTY);
            if (propertyName != null) {
                projectProps.add(propertyElement.getAttributeValue("name"));
            }
        }
        // And custom properties plugins
        for (final Object o : projectElement.getChildren().toArray()) {
            final Element childElement = (Element) o;
            final String nodeName = childElement.getName();
            if (KNOWN_ROOT_CHILD_NAMES.contains(nodeName)) {
                continue;
            }
            if (isCustomPropertiesPlugin(nodeName)) {
                ProjectXMLHelper.registerCustomProperty(nonFullyResolvedProjectProperties, childElement,
                    resolvers, FAIL_UPON_MISSING_PROPERTY, PluginRegistry.createRegistry(rootPlugins));
                projectElement.removeContent(childElement);
            }
        }

        // handle project templates properties
        final List projectTemplateProperties = templatePluginProperties.get(projectElement.getName());
        if (projectTemplateProperties != null) {
            for (final Object projectTemplateProperty : projectTemplateProperties) {
                final Element element = (Element) projectTemplateProperty;
                final String propertyName = element.getAttributeValue("name");

                // Here it ignores properties defined in <plugin /> with the same name as those
                // defined in <project />. In this way, the project-defined properties override
                // the plugin-defined ones.
                if (propertyName != null && projectProps.contains(element.getAttributeValue("name"))) {
                    continue;
                }

                if (isCustomPropertiesPlugin(element.getName())) {
                    ProjectXMLHelper.registerCustomProperty(nonFullyResolvedProjectProperties,
                            (Element) element.clone(), resolvers, FAIL_UPON_MISSING_PROPERTY,
                            PluginRegistry.createRegistry(rootPlugins));
                } else {
                    ProjectXMLHelper.registerProperty(nonFullyResolvedProjectProperties, (Element) element.clone(),
                            resolvers, FAIL_UPON_MISSING_PROPERTY);
                }
            }
        }

        // add the resolved rootProperties to the project's properties
        final Map<String, String> thisProperties = nonFullyResolvedProjectProperties.thisMap;
        for (final String key : rootProperties.keySet()) {
            if (!thisProperties.containsKey(key)) {
                final String value = rootProperties.get(key);
                thisProperties.put(key, Util.parsePropertiesInString(thisProperties, value, false));
            }
        }

        // Parse the entire element tree, expanding all property macros
        ProjectXMLHelper.parsePropertiesInElement(projectElement, thisProperties, FAIL_UPON_MISSING_PROPERTY);

        // Register any custom plugins
        final ProjectXMLHelper helper = new ProjectXMLHelper(resolvers);
        final PluginRegistry projectPlugins = PluginRegistry.createRegistry(rootPlugins);
        // First register plugins embedded in the project template, if there are such
        final Element pparentPlugin = rootPlugins.getPluginConfig(projectElement.getName());
        if (pparentPlugin != null) {
            for (final Object o : pparentPlugin.getChildren("plugin")) {
                final Element element = (Element) o;
                projectPlugins.from2classname(element);

                final PluginPlugin plugin = (PluginPlugin) helper.configurePlugin(element, false);
                projectPlugins.register(plugin);
            }
        }

        // Register project plugins
        for (final Object o : projectElement.getChildren("plugin")) {
            final Element element = (Element) o;
            projectPlugins.from2classname(element);

            final PluginPlugin plugin = (PluginPlugin) helper.configurePlugin(element, false);
            projectPlugins.register(plugin);
        }

        projectElement.removeChildren("property");
        projectElement.removeChildren("plugin");

        LOG.debug("**************** configuring project " + projectName + " *******************");
        ProjectHelper projectHelper = new ProjectXMLHelper(thisProperties, projectPlugins, resolvers, controller);

        final ProjectInterface project;
        try {
            project = (ProjectInterface) projectHelper.configurePlugin(projectElement, false);
        } catch (CruiseControlException e) {
            throw new CruiseControlException("error configuring project " + projectName, e);
        }

        // Why call method that is a no-op, and exists only for gendoc purposes?
        //add(project);

        project.validate();
        LOG.debug("**************** end configuring project " + projectName + " *******************");

        this.projects.put(projectName, project);
        this.PROJECTS_REGISTRY.put(projectName, project);
        this.projectPluginRegistries.put(projectName, projectPlugins);
    }

    private String getProjectName(final Element childElement) throws CruiseControlException {
        if (!isProject(childElement.getName())) {
            throw new IllegalStateException("Invalid Node <" + childElement.getName() + "> (not a project)");
        }
        final String rawName = childElement.getAttribute("name").getValue();
        return Util.parsePropertiesInString(rootProperties, rawName, false);
    }

    public ProjectInterface getProject(String name) {
        return this.projects.get(name);
    }

    public Set<String> getProjectNames() {
        return Collections.unmodifiableSet(this.projects.keySet());
    }

    public PluginRegistry getRootPlugins() {
        return rootPlugins;
    }

    public PluginRegistry getProjectPlugins(String name) {
        return this.projectPluginRegistries.get(name);
    }

    // Unfortunately it seems like the commons-collection CompositeMap doesn't
    // fit that role
    // at least size is not implemented the way I want it.
    // TODO is there a clean way to do without this?
    private static class MapWithParent implements Map<String, String> {
        private final Map<String, String> parent;
        private final Map<String, String> thisMap;

        MapWithParent(Map<String, String> parent) {
            this.parent = parent;
            this.thisMap = new HashMap<String, String>();
        }

        public int size() {
            int size = thisMap.size();
            if (parent != null) {
                final Set<String> keys = parent.keySet();
                for (final String key : keys) {
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
            return thisMap.containsKey(key) || (parent != null && parent.containsKey(key));
        }

        public boolean containsValue(Object value) {
            return thisMap.containsValue(value) || (parent != null && parent.containsValue(value));
        }

        public String get(Object key) {
            String value = thisMap.get(key);
            if (value == null && parent != null) {
                value = parent.get(key);
            }
            return value;
        }

        public String put(String o, String o1) {
            return thisMap.put(o, o1);
        }


        public String remove(Object key) {
            throw new UnsupportedOperationException("'remove' not supported on MapWithParent");
        }

        public void putAll(Map< ? extends String, ? extends String> map) {
            thisMap.putAll(map);
        }

        public void clear() {
            throw new UnsupportedOperationException("'clear' not supported on MapWithParent");
        }

        public Set<String> keySet() {
            Set<String> keys = new HashSet<String>(thisMap.keySet());
            if (parent != null) {
                keys.addAll(parent.keySet());
            }
            return keys;
        }

        public Collection<String> values() {
            throw new UnsupportedOperationException("not implemented");
            /*
             * we have to support the Map contract. Back the returned values.
             * Mmmmm
             */
            /*
             * Collection values = thisMap.values(); if (parent != null) { Set
             * keys = parent.keySet(); List parentValues = new ArrayList(); for
             * (Iterator iterator = keys.iterator(); iterator.hasNext();) {
             * String key = (String) iterator.next(); if (!
             * thisMap.containsKey(key)) { parentValues.add(parent.get(key)); } } }
             * return values;
             */
        }

        public Set<Map.Entry<String, String>> entrySet() {
            final Set<Map.Entry<String, String>> entries = new HashSet<Map.Entry<String, String>>(thisMap.entrySet());
            if (parent != null) {
                entries.addAll(parent.entrySet());
            }
            return entries;
        }
    }

}
