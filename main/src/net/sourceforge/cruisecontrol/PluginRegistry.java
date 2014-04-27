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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import net.sourceforge.cruisecontrol.config.PluginPlugin;


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
public final class PluginRegistry implements Serializable, Iterable<String> {

    private static final long serialVersionUID = 5941716771646177086L;

    /** Root plugin name. */
    public static final String ROOT_PLUGIN = "cruisecontrol";
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
     * @param parent parent registry of the new plugin registry
     * @return PluginRegistry with the specified registry as its parent.
     */
    public static PluginRegistry createRegistry(final PluginRegistry parent) {
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
    private final Map<String, String> plugins = new HashMap<String, String>();

    /**
     * Map that holds the DOM element representing the plugin declaration.
     * Key is the plugin name (as taken from the DOM element),
     * value is the Element representing the plugin configuration.
     */
    private final Map<String, Element> pluginConfigs = new HashMap<String, Element>();

    /**
     * Creates a new PluginRegistry with no plugins registered, with the given parent registry.
     * Only used internally for now, Projects should call createRegistry instead.
     * @param parentRegistry parent registry of the new plugin registry
     */
    private PluginRegistry(final PluginRegistry parentRegistry) {
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
    void register(final String pluginName, final String pluginClassname) {
        plugins.put(pluginName.toLowerCase(), pluginClassname);
    }

    public void register(final PluginPlugin plugin) throws CruiseControlException {
      final String pluginName = plugin.getName();
      final String pluginClassName = plugin.getClassname();
      Element transformedElement = plugin.getTransformedElement();

      // Resolve inheritance
      final Attribute parentPlugin = transformedElement.getAttribute("inherits");
      if (parentPlugin != null) {
          // find the plugin to inherrit from
          final Element parentConfig = this.getPluginConfig(parentPlugin.getValue());
          if (parentConfig == null) {
              throw new CruiseControlException("Unknown plugin '"
                      + parentPlugin.getValue() + "' to inherit from.");
          }
          transformedElement = overridePluginConfig(parentPlugin.getValue(), pluginClassName, transformedElement);
          transformedElement.removeAttribute("inherits");
      }

      if (pluginClassName != null) {
        register(pluginName, pluginClassName);
      } else {
        // should be known plugin, then
        if (!isPluginRegistered(pluginName)) {
          throw new CruiseControlException("Unknown plugin '"
                  + pluginName + "'; maybe you forgot to specify a classname?");
        }
        register(pluginName, getPluginClassname(pluginName));
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("storing plugin configuration " + pluginName);
      }
      pluginConfigs.put(pluginName, transformedElement);
    }

  /**
     * Registers the given plugin, including plugin configuration.
     *
     * @param pluginElement the JDom element that contains the plugin definition.
     * @throws CruiseControlException if operation fails
     * @deprecated use {@link #register(PluginPlugin)}
     */
    public void register(Element pluginElement) throws CruiseControlException {
      // wants to inherit the plugin
      final Attribute parentPlugin = pluginElement.getAttribute("inherits");
      if (parentPlugin != null) {
          /* Check the name of plugin to inherit from */
          if (getPluginClassname(parentPlugin.getValue()) == null) {
              throw new CruiseControlException("Unknown plugin '"
                      + parentPlugin.getValue() + "' to inherit from.");
          }

          Attribute pluginName = pluginElement.getAttribute("name").detach();
          Attribute pluginClass = pluginElement.getAttribute("classname");

          if (pluginClass == null) {
              throw new CruiseControlException("Unknown plugin '"
                      + pluginName.getValue() + "'; maybe you forgot to specify a classname?");
          }

          pluginElement = overridePluginConfig(parentPlugin.getValue(), pluginClass.getValue(), pluginElement);
          pluginElement.setAttribute(pluginName);
          pluginElement.removeAttribute(parentPlugin.getName());
      }

      final PluginPlugin plugin = (PluginPlugin) new ProjectXMLHelper(
            new ResolverHolder.DummeResolvers()).configurePlugin(pluginElement, false);
      register(plugin);
    }

    /**
     * For plugins defining <code>from="type"</code> element, it finds the class for the
     * given type, sets it to the <code>classname=""</code> attribute and removes the
     * <code>from</code> attribute.
     *
     * @param pluginElement the XML element with plugin configuration.
     */
    public void from2classname(Element pluginElement) {
        if (!"plugin".equals(pluginElement.getName())) {
            LOG.warn("Node <" + pluginElement.getName() + "> is not plugin");
            return;
        }

        String pluginName = pluginElement.getAttributeValue("name");
        String pluginFrom = pluginElement.getAttributeValue("from");
        String pluginClassName = pluginElement.getAttributeValue("classname");
        if (pluginClassName == null && pluginFrom != null) {
            pluginClassName = getPluginClassname(pluginFrom);
            // No standard plugin
            if (pluginClassName == null) {
                LOG.warn("<plugin name = '" + pluginName + "' from = '" + pluginFrom
                       + "'> does not contain in-built element name");
                return;
            }
            // Create "standard" plugin element
            pluginElement.setAttribute("classname", pluginClassName);
            pluginElement.removeAttribute("from");
        }
    }

   /**
     * Registers the given plugin in the root registry, so it will be
     * available to all projects.
     * @param pluginElement the plugin to register
     * @throws CruiseControlException if operation fails
     */
    static void registerToRoot(final Element pluginElement) throws CruiseControlException {
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
     * @param pluginName the case insensitive plugin name.
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
     * @param pluginName lower case plugin name.
     * @return the class name for this plugin on this registry. May be <code>null</code>
     * Assumes the pluginName is lower case
     */
    private String internalGetPluginClassname(final String pluginName) {
        return plugins.get(pluginName);
    }

    /**
     * @param pluginName the plugin name
     * @return Returns null if no plugin has been registered with the specified
     * name, otherwise the Class representing the plugin class. Note that
     * plugin names are always treated as case insensitive, so Ant, ant,
     * and AnT are all treated as the same plugin.
     *
     * @throws CruiseControlException If the class provided cannot be loaded.
     */
    public Class getPluginClass(final String pluginName) throws CruiseControlException {
        if (!isPluginRegistered(pluginName)) {
            return null;
        }
        final String pluginClassname = getPluginClassname(pluginName);
        return instanciatePluginClass(pluginClassname, pluginName);
    }

    /**
     * @param pluginClassname fully qualified plugin class name
     * @param pluginName plugin name
     * @return instantiate the Class representing the plugin class name.
     * @throws CruiseControlException If the class provided cannot be loaded.
     */
    public Class instanciatePluginClass(final String pluginClassname, final String pluginName)
            throws CruiseControlException {

        try {
            return Class.forName(pluginClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Attemping to load plugin named [" + pluginName
                    + "], but couldn't load corresponding class ["
                    + pluginClassname + "].";
            throw new CruiseControlException(msg);
        }
    }

    public String getPluginName(final Class pluginClass) {
        String pluginName = null;

        if (parentRegistry != null) {
            pluginName = parentRegistry.getPluginName(pluginClass);
        }

        if (pluginName == null) {
            for (final Map.Entry<String, String> entry : plugins.entrySet()) {
                final String value = entry.getValue();
                if (value.equals(pluginClass.getName())) {
                    pluginName = entry.getKey();
                    break;
                }
            }
        }

        return pluginName;
    }

    public PluginDetail[] getPluginDetails() throws CruiseControlException {
        final List<PluginDetail> availablePlugins = new LinkedList<PluginDetail>();

        if (parentRegistry != null) {
            availablePlugins.addAll(Arrays.asList(parentRegistry.getPluginDetails()));
        }

        for (final String pluginName : plugins.keySet()) {
            try {
                Class pluginClass = getPluginClass(pluginName);
                availablePlugins.add(new GenericPluginDetail(pluginName, pluginClass));
            } catch (CruiseControlException e) {
                String message = e.getMessage();
                // TODO: handle these potential unloadable plugins in a better way
                if (message.indexOf("starteam") == -1 && message.indexOf("harvest") == -1) {
                    throw e;
                }
            }
        }

        return availablePlugins.toArray(new PluginDetail[availablePlugins.size()]);
    }

    public PluginType[] getPluginTypes() {
        return PluginType.getTypes();
    }

    /**
     * @param pluginName the short name for the plugin.
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
    public boolean isPluginRegistered(final String pluginName) {
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
     * @return a PluginRegistry containing all the default plugins.
     * @throws RuntimeException in case of IOException during the reading of the properties-file
     */
    static PluginRegistry loadDefaultPluginRegistry() {
        final PluginRegistry rootRegistry = new PluginRegistry(null);
        final Properties pluginDefinitions = new Properties();
        try {
            pluginDefinitions.load(PluginRegistry.class.getResourceAsStream("default-plugins.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load plugin-definitions from default-plugins.properties: " + e);
        }
        for (final Map.Entry entry : pluginDefinitions.entrySet()) {
            rootRegistry.register((String) entry.getKey(), (String) entry.getValue());
        }
        return rootRegistry;
    }

    /**
     * @param pluginName the plugin name
     * @return the plugin configuration particular to this plugin, merged with the parents
     * @throws NullPointerException if pluginName is null
     */
    public Element getPluginConfig(String pluginName) {
        pluginName = pluginName.toLowerCase();
        final String className = getPluginClassname(pluginName);
        return overridePluginConfig(pluginName, className, null);
    }

    /**
     * Return a merged plugin configuration, taking into account parent classes where appropriate.
     *
     * This method is used recursively to fill up the specified pluginConfig Element.
     *
     * Properties are taken from parent plugins if they have not been defined in the child.
     * Nested elements of the parent are always added to the beginning of child's config (in order to
     * the later child's config values being able to overwrite the parent's values).
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
    @SuppressWarnings("unchecked")
    private Element overridePluginConfig(final String pluginName, final String pluginClass, Element pluginConfig) {
        Element pluginElement = this.pluginConfigs.get(pluginName);
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
                final List<Attribute> attributes = (List<Attribute>) pluginElement.getAttributes();
                for (final Attribute attribute : attributes) {
                    final String name = attribute.getName();
                    if (pluginConfig.getAttribute(name) == null) {
                        pluginConfig.setAttribute(name, attribute.getValue());
                    }
                }
                // combine child elements
                final List<Element> children = (List<Element>) pluginElement.getChildren();
                int index = 0;
                for (final Element child : children) {
                    pluginConfig.addContent(index, (Element) child.clone());
                    index++;
                }
            }
        }
        if (this.parentRegistry != null) {
            pluginConfig = this.parentRegistry.overridePluginConfig(pluginName, pluginClass, pluginConfig);
        }
        return pluginConfig;
    }

    /**
     * Gets an iterator for iterating over all the plugin class names in this registry and
     * its parents.
     * @return The iterator object. Iteration over the class names is not guaranteed to take
     *         place in any particular order.
     */
    public Iterator<String> iterator() {
        return new PluginIterator();
    }

    /**
     * Iterator for compositing this registry's class names with its parent's class names.
     * @author pollens
     */
    private class PluginIterator implements Iterator<String> {

        /** Iterator over this registry's class names. */
        private final Iterator<String> myClassNames;

        /** Iterator over parent registry's classes. */
        private final Iterator<String> parentClassNames;

        /**
         * Instantiates a PluginIterator for iterating over the classes in this PluginRegistry.
         */
        public PluginIterator() {
            myClassNames = PluginRegistry.this.plugins.values().iterator();
            if (PluginRegistry.this.parentRegistry != null) {
                parentClassNames = PluginRegistry.this.parentRegistry.iterator();
            } else {
                parentClassNames = null;
            }
        }

        public boolean hasNext() {
            return myClassNames.hasNext()
                    || (parentClassNames != null && parentClassNames.hasNext());
        }

        public String next() {
            // First, use up this registry's classes.
            if (myClassNames.hasNext()) {
                return myClassNames.next();
            } else if (parentClassNames != null) {
                // This registry has run out of classes, so fall back on the parent registry.
                return parentClassNames.next();
            } else {
                // We have run out of plugins.
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Removal not supported by this iterator.");
        }

    }
}
