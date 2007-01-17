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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;


/**
 * Handles "registering" plugins that will be used by the CruiseControl
 * configuration file.

 * A PluginRegistry can have a parent registry, which it will query for
 * a plugin if it's not defined in the registry itself. This is used to
 * enable projects to have their own plugins and override the classname
 * for a specific plugin, like the labelincrementer.
 *
 * The root-registry contains the default list of plugins, i.e. those
 * that are already registered like AntBuilder that don't have to be registered
 * separately in the configuration file.
 *
 * The registry keeps track of the {@link #getPluginConfig(String) plugin configurations}
 * in order to allow full plugin preconfigurations (default properties + nested elements).
 *
 * @see PluginXMLHelper
 */
public final class PluginRegistry implements Serializable {

    private static final Logger LOG = Logger.getLogger(PluginRegistry.class);

    /**
     * The only instance of the root plugin registry.
     * This contains the default plugins and the plugins that are defined
     * external to projects.
     */
    private static final PluginRegistry ROOTREGISTRY = loadDefaultPluginRegistry();

    /**
     * @return PluginRegistry with the ROOTREGISTRY as its parent.
     */
    public static PluginRegistry createRegistry() {
        return createRegistry(ROOTREGISTRY);
    }

    /**
     * @return PluginRegistry with the specified registry as its parent.
     */
    public static PluginRegistry createRegistry(PluginRegistry parent) {
        return new PluginRegistry(parent);
    }

    /**
     * The parent registry that will be searched for plugin definitions
     * if they're not defined in the registry itself. May be null.
     */
    private final PluginRegistry parentRegistry;

    /**
     * Map of plugins where the key is the plugin name (e.g. ant) and the value is
     * the fully qualified classname
     * (e.g. net.sourceforge.cruisecontrol.builders.AntBuilder).
     */
    private final Map plugins = new HashMap();

    /**
     * Map that holds the DOM element representing the plugin declaration.
     * Key is the plugin name (as taken from the DOM element),
     * value is the Element representing the plugin configuration.
     */
    private final Map pluginConfigs = new HashMap();

    /**
     * Creates a new PluginRegistry with no plugins registered, with the given parent registry.
     * Only used internally for now, Projects should call createRegistry instead.
     */
    private PluginRegistry(PluginRegistry parentRegistry) {
        this.parentRegistry = parentRegistry;
    }

    /**
     * @param pluginName The name for the plugin, e.g. ant. Note that plugin
     * names are always treated as case insensitive, so Ant, ant, and AnT are
     * all treated as the same plugin.
     *
     * @param pluginClassname The fully qualified classname for the
     * plugin class, e.g. net.sourceforge.cruisecontrol.builders.AntBuilder.
     */
    public void register(String pluginName, String pluginClassname) {
        plugins.put(pluginName.toLowerCase(), pluginClassname);
    }

    /**
     * Registers the given plugin, including plugin configuration.
     *
     * @param pluginElement the JDom element that contains the plugin definition.
     */
    public void register(Element pluginElement) throws CruiseControlException {
        String pluginName = pluginElement.getAttributeValue("name").toLowerCase();
        String pluginClassName = pluginElement.getAttributeValue("classname");
        if (pluginClassName != null) {
            register(pluginName, pluginClassName);
        } else {
            // should be known plugin, then
            if (!isPluginRegistered(pluginName)) {
                throw new CruiseControlException("Unknown plugin '"
                        + pluginName + "'; maybe you forgot to specify a classname?");
            }
        }

        Element clonedPluginElement = (Element) pluginElement.clone();
        clonedPluginElement.removeAttribute("name");
        clonedPluginElement.removeAttribute("classname");
        clonedPluginElement.setName(pluginName);
        if (LOG.isDebugEnabled()) {
            LOG.debug("storing plugin configuration " + pluginName);
        }
        pluginConfigs.put(pluginName, clonedPluginElement);
    }

    /**
     * Registers the given plugin in the root registry, so it will be
     * available to all projects.
     *
     */
    static void registerToRoot(Element pluginElement) throws CruiseControlException {
        ROOTREGISTRY.register(pluginElement);
    }

    /**
     * Clears all plugin registrations and defaults in the root registry, so they can be re-registered
     * when reloading the config file. The default-properties are re-read.
     */
    static void resetRootRegistry() {
        ROOTREGISTRY.pluginConfigs.clear();
        ROOTREGISTRY.plugins.clear();
        ROOTREGISTRY.plugins.putAll(loadDefaultPluginRegistry().plugins);
    }

    /**
     * @return Returns null if no plugin has been registered with the specified
     * name, otherwise a String representing the fully qualified classname
     * for the plugin class. Note that plugin
     * names are always treated as case insensitive, so Ant, ant, and AnT are
     * all treated as the same plugin.
     * Note: a parent name->class mapping can be overridden by children registries.
     */
    public String getPluginClassname(String pluginName) {
        pluginName = pluginName.toLowerCase();
        String className = internalGetPluginClassname(pluginName);
        if (className == null && parentRegistry != null) {
            className = parentRegistry.getPluginClassname(pluginName);
        }
        return className;
    }

    /**
     * @return the class name for this plugin on this registry. May be <code>null</code>
     * Assumes the pluginName is lower case
     */
    private String internalGetPluginClassname(String pluginName) {
        return (String) plugins.get(pluginName);
    }

    /**
     * @return Returns null if no plugin has been registered with the specified
     * name, otherwise the Class representing the plugin class. Note that
     * plugin names are always treated as case insensitive, so Ant, ant,
     * and AnT are all treated as the same plugin.
     *
     * @throws CruiseControlException If the class provided cannot be loaded.
     */
    public Class getPluginClass(String pluginName) throws CruiseControlException {
        if (!isPluginRegistered(pluginName)) {
            return null;
        }
        String pluginClassname = getPluginClassname(pluginName);
        return instanciatePluginClass(pluginClassname, pluginName);
    }

    /**
     * @param pluginClassname
     * @param pluginName
     * @return instantiate the Class representing the plugin class name.
     * @throws CruiseControlException If the class provided cannot be loaded.
     */
    Class instanciatePluginClass(String pluginClassname, String pluginName) throws CruiseControlException {
        try {
            return Class.forName(pluginClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Attemping to load plugin named [" + pluginName
                    + "], but couldn't load corresponding class ["
                    + pluginClassname + "].";
        //            LOG.error(msg, e);
            throw new CruiseControlException(msg);
        }
    }

    public String getPluginName(Class pluginClass) {
        String pluginName = null;

        if (parentRegistry != null) {
            pluginName = parentRegistry.getPluginName(pluginClass);
        }

        if (pluginName == null) {
            for (Iterator i = plugins.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String value = (String) entry.getValue();
                if (value.equals(pluginClass.getName())) {
                    pluginName = ((String) entry.getKey());
                    break;
                }
            }
        }

        return pluginName;
    }

    public PluginDetail[] getPluginDetails() throws CruiseControlException {
        List availablePlugins = new LinkedList();

        if (parentRegistry != null) {
            availablePlugins.addAll(Arrays.asList(parentRegistry.getPluginDetails()));
        }

        for (Iterator i = plugins.keySet().iterator(); i.hasNext();) {
            String pluginName = (String) i.next();
            try {
                Class pluginClass = getPluginClass(pluginName);
                availablePlugins.add(new GenericPluginDetail(pluginName, pluginClass));
            } catch (CruiseControlException e) {
                String message = e.getMessage();
                if (message.indexOf("starteam") < 0) {
                    throw e;
                }
            }
        }

        return (PluginDetail[]) availablePlugins.toArray(new PluginDetail[availablePlugins.size()]);
    }

    public PluginType[] getPluginTypes() {
        return PluginType.getTypes();
    }

    /**
     * @return True if this registry or its parent contains
     * an entry for the plugin specified by the name.
     * The name is the short name for the plugin, not
     * the classname, e.g. ant. Note that plugin
     * names are always treated as case insensitive, so Ant, ant, and AnT are
     * all treated as the same plugin.
     *
     * @throws NullPointerException If a null pluginName is passed, then
     * a NullPointerException will occur. It's recommended to not pass a
     * null pluginName.
     */
    public boolean isPluginRegistered(String pluginName) {
        boolean isRegistered = plugins.containsKey(pluginName.toLowerCase());
        if (!isRegistered && parentRegistry != null) {
            isRegistered = parentRegistry.isPluginRegistered(pluginName);
        }
        return isRegistered;
    }

    /**
     * Returns a PluginRegistry containing all the default plugins.
     * The key is the plugin name (e.g. ant) and the value is
     * the fully qualified classname
     * (e.g. net.sourceforge.cruisecontrol.builders.AntBuilder).
     * @throws RuntimeException in case of IOException during the reading of the properties-file
     */
    static PluginRegistry loadDefaultPluginRegistry() {
        PluginRegistry rootRegistry = new PluginRegistry(null);
        Properties pluginDefinitions = new Properties();
        try {
            pluginDefinitions.load(PluginRegistry.class.getResourceAsStream("default-plugins.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load plugin-definitions from default-plugins.properties: " + e);
        }
        for (Iterator iter = pluginDefinitions.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            rootRegistry.register((String) entry.getKey(), (String) entry.getValue());
        }
        return rootRegistry;
    }

    /**
     * Get the plugin configuration particular to this plugin, merged with the parents
     * @throws NullPointerException if pluginName is null
     */
    public Element getPluginConfig(String pluginName) {
        pluginName = pluginName.toLowerCase();
        String className = getPluginClassname(pluginName);
        return overridePluginConfig(pluginName, className, null);
    }

    /**
     * Return a merged plugin configuration, taking into account parent classes where appropriate.
     *
     * This method is used recursively to fill up the specified pluginConfig Element.
     *
     * Properties are taken from parent plugins if they have not been defined in the child.
     * Nested elements of the parent are always added to the child's config.
     *
     * Note: as we have no way to enforce the cardinality of the nested elements, the parent/default nested
     * elements are always added to the config of the child. The validity of the resulting config then
     * depends on the config to be correctly specified.
     *
     * @param pluginName the name of the plugin to create a config for (must be lower case)
     * @param pluginClass the mapped class name for the plugin
     * @param pluginConfig the current config, passed up for completion
     * @return an Element representing the combination of the various plugin configurations for
     * the same plugin, following the hierarchy.
     */
    private Element overridePluginConfig(final String pluginName, final String pluginClass, Element pluginConfig) {
        Element pluginElement = (Element) this.pluginConfigs.get(pluginName);
        // clone the first found plugin config
        if (pluginElement != null && pluginConfig == null) {
            pluginElement = (Element) pluginElement.clone();
        }
        if (pluginConfig == null) {
            pluginConfig = pluginElement;
        } else {
            // do not override if class names do not match
            if (pluginElement != null && pluginClass.equals(this.internalGetPluginClassname(pluginName))) {
                // override properties
                List attributes = pluginElement.getAttributes();
                for (int i = 0; i < attributes.size(); i++) {
                    Attribute attribute = (Attribute) attributes.get(i);
                    String name = attribute.getName();
                    if (pluginConfig.getAttribute(name) == null) {
                        pluginConfig.setAttribute(name, attribute.getValue());
                    }
                }
                // combine child elements
                List children = pluginElement.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    Element child = (Element) children.get(i);
                    pluginConfig.addContent((Element) child.clone());
                }
            }
        }
        if (this.parentRegistry != null) {
            pluginConfig = this.parentRegistry.overridePluginConfig(pluginName, pluginClass, pluginConfig);
        }
        return pluginConfig;
    }

    /**
     * Returns a Map containing the default properties for the plugin
     * with the given name. If there's no such plugin, an empty
     * Map will be returned. The default properties can be inherited
     * from a parent registry.
     * @deprecated use FIXME that also supports preconfiguration of nested elements
     */
    public Map getDefaultProperties(String pluginName) {
        Map defaultProperties = new HashMap();
        Element pluginConfig = this.getPluginConfig(pluginName);
        if (pluginConfig != null) {
            List attributes = pluginConfig.getAttributes();
            for (Iterator iter = attributes.iterator(); iter.hasNext(); ) {
                Attribute attr = (Attribute) iter.next();
                String name = attr.getName();
                if (name.equals("name") || name.equals("classname")) {
                    continue;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("setting default property " + name + " to '" + attr.getValue()
                       + "' for " + pluginName);
                }
                defaultProperties.put(name, attr.getValue());
            }
        }
        return Collections.unmodifiableMap(defaultProperties);
    }
}
