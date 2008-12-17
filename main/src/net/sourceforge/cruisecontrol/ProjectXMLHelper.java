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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import net.sourceforge.cruisecontrol.config.DefaultPropertiesPlugin;
import net.sourceforge.cruisecontrol.config.PropertiesPlugin;
import net.sourceforge.cruisecontrol.util.Util;

/**
 *  Instantiates a project from a JDOM Element. Supports the use of Ant-like patterns in
 *  attribute values that look like this: <code>${propertyname}</code>
 */
public class ProjectXMLHelper implements ProjectHelper {
    // TODO: extract out generic Helper methods
    private static final Logger LOG = Logger.getLogger(ProjectXMLHelper.class);

    private final Map<String, String> projectProperties;
    private final PluginRegistry projectPlugins;

    private final CruiseControlController controller;

    public ProjectXMLHelper() {
        this(new HashMap<String, String>(), PluginRegistry.createRegistry(PluginRegistry.loadDefaultPluginRegistry()),
                null);
    }

    public ProjectXMLHelper(final Map<String, String> projectProperties, final PluginRegistry projectPlugins) {
        this(projectProperties, projectPlugins, null);
    }
    
    public ProjectXMLHelper(final Map<String, String> projectProperties, final PluginRegistry projectPlugins,
                            final CruiseControlController controller) {
        this.projectProperties = projectProperties;
        this.projectPlugins = projectPlugins;
        this.controller = controller;
    }

    /**
     *  TODO: also check that instantiated class implements/extends correct interface/class
     */
    public Object configurePlugin(final Element pluginElement, final boolean skipChildElements)
            throws CruiseControlException {
        final String name = pluginElement.getName();
        final PluginXMLHelper pluginHelper = new PluginXMLHelper(this, controller);
        final String pluginName = pluginElement.getName();

        LOG.debug("configuring plugin " + pluginElement.getName() + " skip " + skipChildElements);

        if (projectPlugins.isPluginRegistered(pluginName)) {
            final Object pluginInstance = getConfiguredPlugin(pluginHelper, pluginElement.getName());
            if (pluginInstance != null) { // preconfigured
                return pluginHelper.configure(pluginElement, pluginInstance, skipChildElements);
            }
            return pluginHelper.configure(pluginElement, projectPlugins.getPluginClass(pluginName), skipChildElements);
        } else {
            throw new CruiseControlException("Unknown plugin for: <" + name + ">");
        }
    }


    /**
     * Get a [partially] configured plugin instance given its plugin name.
     * @param pluginHelper xml helper
     * @param pluginName the plugin name
     * @return <code>null</code> if the plugin was never configured.
     * @throws CruiseControlException
     *   if the registered class cannot be loaded,
     *   if a property cannot be resolved,
     *   if the plugin configuration fails
     */
    private Object getConfiguredPlugin(final PluginXMLHelper pluginHelper, final String pluginName)
            throws CruiseControlException {
        
        final Class pluginClass = projectPlugins.getPluginClass(pluginName);
        if (pluginClass == null) {
            return null;
        }
        Object configuredPlugin = null;
        final Element pluginElement = projectPlugins.getPluginConfig(pluginName);
        if (pluginElement != null) {
            // FIXME
            // the only reason we have to do this here
            // is because the plugins registered in the ROOT registry have not had their properties parsed.
            // See CruiseControlController.addPluginsToRootRegistry
            parsePropertiesInElement(pluginElement);
            configuredPlugin = pluginHelper.configure(pluginElement, pluginClass, false);
        }
        return configuredPlugin;
    }

    /**
     * Recurses through an Element tree, substituting resolved values
     * for any property macros.
     *
     * @param element The Element to parse
     *
     * @throws CruiseControlException if a property cannot be resolved
     */
    private void parsePropertiesInElement(Element element) throws CruiseControlException {
        parsePropertiesInElement(element, this.projectProperties, CruiseControlConfig.FAIL_UPON_MISSING_PROPERTY);
    }

  /**
     * Registers one or more properties as defined in a property element.
     *
     * @param propertyElement The element from which we will register properties
     * @throws CruiseControlException if registraion fails
     */
    public static DefaultPropertiesPlugin registerProperty(final Map<String, String> props,
                                                           final Element propertyElement,
                                                           final boolean failIfMissing)
          throws CruiseControlException {

        parsePropertiesInElement(propertyElement, props, failIfMissing);

        final Object o = new ProjectXMLHelper().configurePlugin(propertyElement, false);
        if (!(o instanceof DefaultPropertiesPlugin)) {
          throw new CruiseControlException("Properties element does not extend DefaultPropertiesPlugin interface."
                  + " Check your CC global plugin configuration.");
        }
        final DefaultPropertiesPlugin propertiesObject = (DefaultPropertiesPlugin) o;
        propertiesObject.loadProperties(props, failIfMissing);
        return propertiesObject;
    }
    
    public static PropertiesPlugin registerCustomProperty(final Map<String, String> props,
            final Element propertyElement, final boolean failIfMissing,
            final PluginRegistry registry) throws CruiseControlException {

        parsePropertiesInElement(propertyElement, props, failIfMissing);

        final Object o = new ProjectXMLHelper(props, registry, null).configurePlugin(propertyElement, false);
        if (!(o instanceof PropertiesPlugin)) {
          throw new CruiseControlException("Element " + propertyElement.getName()
                  + " does not implement PropertiesPlugin interface."
                  + " Check your CC global plugin configuration.");
        }
        final PropertiesPlugin propertiesObject = (PropertiesPlugin) o;
        propertiesObject.loadProperties(props, failIfMissing);
        return propertiesObject;
    }


    // FIXME Helper extract ?
    public static void parsePropertiesInElement(final Element element,
                                                final Map<String, String> props,
                                                final boolean failIfMissing)
        throws CruiseControlException {

        // Recurse through the element tree - depth first
        for (Iterator children = element.getChildren().iterator(); children.hasNext(); ) {
            parsePropertiesInElement((Element) children.next(), props, failIfMissing);
        }

        // Parse the attribute value strings
        for (Iterator attributes = element.getAttributes().iterator(); attributes.hasNext(); ) {
            Attribute attribute = (Attribute) attributes.next();
            attribute.setValue(Util.parsePropertiesInString(props, attribute.getValue(), failIfMissing));
        }

        // Parse the element's text
        final String text = element.getTextTrim();
        if (text.length() > 0) {
            element.setText(Util.parsePropertiesInString(props, text, failIfMissing));
        }
    }

    public static void setProperty(final Map<String, String> props, final String name, final String parsedValue) {
        ProjectXMLHelper.LOG.debug("Setting property \"" + name + "\" to \"" + parsedValue + "\".");
        props.put(name, parsedValue);
    }

}
