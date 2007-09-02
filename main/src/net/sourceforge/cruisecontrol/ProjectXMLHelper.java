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

/**
 *  Instantiates a project from a JDOM Element. Supports the use of Ant-like patterns in
 *  attribute values that look like this: <code>${propertyname}</code>
 */
public class ProjectXMLHelper implements ProjectHelper {
    // TODO: extract out generic Helper methods
    private static final Logger LOG = Logger.getLogger(ProjectXMLHelper.class);

    private Map projectProperties;
    private PluginRegistry projectPlugins;

    private final CruiseControlController controller;

    public ProjectXMLHelper() {
        this(new HashMap(), PluginRegistry.createRegistry(PluginRegistry.loadDefaultPluginRegistry()), null);
    }

    public ProjectXMLHelper(Map projectProperties, PluginRegistry projectPlugins) {
        this(projectProperties, projectPlugins, null);
    }
    
    public ProjectXMLHelper(Map projectProperties, PluginRegistry projectPlugins, CruiseControlController controller) {
        this.projectProperties = projectProperties;
        this.projectPlugins = projectPlugins;
        this.controller = controller;
    }

    /**
     *  TODO: also check that instantiated class implements/extends correct interface/class
     */
    public Object configurePlugin(Element pluginElement, boolean skipChildElements)
            throws CruiseControlException {
        String name = pluginElement.getName();
        PluginXMLHelper pluginHelper = new PluginXMLHelper(this, controller);
        String pluginName = pluginElement.getName();

        LOG.debug("configuring plugin " + pluginElement.getName() + " skip " + skipChildElements);

        if (projectPlugins.isPluginRegistered(pluginName)) {
            Object pluginInstance = getConfiguredPlugin(pluginHelper, pluginElement.getName());
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
     * @param pluginName
     * @return <code>null</code> if the plugin was never configured.
     * @throws CruiseControlException
     *   if the registered class cannot be loaded,
     *   if a property cannot be resolved,
     *   if the plugin configuration fails
     */
    private Object getConfiguredPlugin(PluginXMLHelper pluginHelper, String pluginName) throws CruiseControlException {
        final Class pluginClass = projectPlugins.getPluginClass(pluginName);
        if (pluginClass == null) {
            return null;
        }
        Object configuredPlugin = null;
        Element pluginElement = projectPlugins.getPluginConfig(pluginName);
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
     * @throws CruiseControlException
     */
    public static DefaultPropertiesPlugin registerProperty(Map props, Element propertyElement,
                                         boolean failIfMissing) throws CruiseControlException {

        parsePropertiesInElement(propertyElement, props, failIfMissing);

        Object o = new ProjectXMLHelper().configurePlugin(propertyElement, false);
        if (!(o instanceof DefaultPropertiesPlugin)) {
          throw new CruiseControlException("Properties element does not extend DefaultPropertiesPlugin interface."
                  + " Check your CC global plugin configuration.");
        }
        DefaultPropertiesPlugin propertiesObject = (DefaultPropertiesPlugin) o;
        propertiesObject.loadProperties(props, failIfMissing);
        return propertiesObject;
    }


    // FIXME Helper extract ?
    /**
     * Parses a string by replacing all occurrences of a property macro with
     * the resolved value of the property. Nested macros are allowed - the
     * inner most macro will be resolved first, moving out from there.
     *
     * @param string The string to be parsed
     * @return The parsed string
     * @throws CruiseControlException if a property cannot be resolved
     */
    public static String parsePropertiesInString(Map props, String string,
                                          boolean failIfMissing) throws CruiseControlException {
        if (string != null) {
            int startIndex = string.indexOf("${");
            if (startIndex != -1) {
                int openedBrackets = 1;
                int lastStartIndex = startIndex + 2;
                int endIndex;
                do {
                    endIndex = string.indexOf("}", lastStartIndex);
                    int otherStartIndex = string.indexOf("${", lastStartIndex);
                    if (otherStartIndex != -1 && otherStartIndex < endIndex) {
                        openedBrackets++;
                        lastStartIndex = otherStartIndex + 2;
                    } else {
                        openedBrackets--;
                        if (openedBrackets == 0) {
                            break;
                        }
                        lastStartIndex = endIndex + 1;
                    }
                } while (true);
                if (endIndex < startIndex + 2) {
                    throw new CruiseControlException("Unclosed brackets in " + string);
                }
                String property = string.substring(startIndex + 2, endIndex);
                // not necessarily resolved
                String propertyName = parsePropertiesInString(props, property, failIfMissing);
                String value = "".equals(propertyName) ? "" : (String) props.get(propertyName);
                if (value == null) {
                    if (failIfMissing) {
                        throw new CruiseControlException("Property \"" + propertyName
                                + "\" is not defined. Please check the order in which you have used your properties.");
                    } else {
                        // we don't resolve missing properties
                        value = "${" + propertyName + "}";
                    }
                }
                LOG.debug("Replacing the string \"" + propertyName + "\" with \"" + value + "\".");
                string = string.substring(0, startIndex) + value
                    + parsePropertiesInString(props, string.substring(endIndex + 1), failIfMissing);
            }
        }
        return string;

    }

    // FIXME Helper extract ?
    public static void parsePropertiesInElement(Element element,
                                                Map props,
                                                boolean failIfMissing)
        throws CruiseControlException {

        // Recurse through the element tree - depth first
        for (Iterator children = element.getChildren().iterator(); children.hasNext(); ) {
            parsePropertiesInElement((Element) children.next(), props, failIfMissing);
        }

        // Parse the attribute value strings
        for (Iterator attributes = element.getAttributes().iterator(); attributes.hasNext(); ) {
            Attribute attribute = (Attribute) attributes.next();
            attribute.setValue(parsePropertiesInString(props, attribute.getValue(), failIfMissing));
        }

        // Parse the element's text
        String text = element.getTextTrim();
        if (text.length() > 0) {
            element.setText(parsePropertiesInString(props, text, failIfMissing));
        }
    }

    public static void setProperty(Map props, String name, String parsedValue) {
        ProjectXMLHelper.LOG.debug("Setting property \"" + name + "\" to \"" + parsedValue + "\".");
        props.put(name, parsedValue);
    }
}
