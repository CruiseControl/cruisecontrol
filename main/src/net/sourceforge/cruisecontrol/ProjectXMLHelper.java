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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.cruisecontrol.util.OSEnvironment;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 *  Instantiates a project from a JDOM Element. Supports the use of Ant-like patterns in
 *  attribute values that look like this: <code>${propertyname}</code>
 */
public class ProjectXMLHelper implements ProjectHelper {
    // TODO: extract out generic Helper methods
    private static final Logger LOG = Logger.getLogger(ProjectXMLHelper.class);

    private Map projectProperties;
    private PluginRegistry projectPlugins;

    public ProjectXMLHelper() {
        this.projectProperties = new HashMap();
        this.projectPlugins = PluginRegistry.createRegistry(PluginRegistry.loadDefaultPluginRegistry());
    }

    public ProjectXMLHelper(Map projectProperties, PluginRegistry projectPlugins) {
        this.projectProperties = projectProperties;
        this.projectPlugins = projectPlugins;
    }

    /**
     *  TODO: also check that instantiated class implements/extends correct interface/class
     */
    public Object configurePlugin(Element pluginElement, boolean skipChildElements)
            throws CruiseControlException {
        String name = pluginElement.getName();
        PluginXMLHelper pluginHelper = new PluginXMLHelper(this);
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
    Object getConfiguredPlugin(PluginXMLHelper pluginHelper, String pluginName) throws CruiseControlException {
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
    public static void registerProperty(Map props, Element propertyElement,
                                         boolean failIfMissing) throws CruiseControlException {
        // Determine which attributes were set in the element
        String fileName = parsePropertiesInString(props, propertyElement.getAttributeValue("file"),
                                                  failIfMissing);
        String environment = parsePropertiesInString(props,
                                                     propertyElement.getAttributeValue("environment"),
                                                     failIfMissing);
        String propName = parsePropertiesInString(props, propertyElement.getAttributeValue("name"),
                                                  failIfMissing);
        String propValue = propertyElement.getAttributeValue("value");
        String toUpperValue = parsePropertiesInString(props,
                                                      propertyElement.getAttributeValue("toupper"),
                                                      failIfMissing);
        boolean toupper = "true".equalsIgnoreCase(toUpperValue);

        // If the file attribute was set, try to read properties
        // from the given filename.
        if (fileName != null && fileName.trim().length() > 0) {
            File file = new File(fileName);
            // FIXME add exists check.
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                // Read the file line by line, expanding macros
                // as we go. We must do this manually to preserve the
                // order of the properties.
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    int index = line.indexOf('=');
                    if (index < 0) {
                        continue;
                    }
                    String parsedName
                        = parsePropertiesInString(props, line.substring(0, index).trim(), failIfMissing);
                    String parsedValue
                        = parsePropertiesInString(props, line.substring(index + 1).trim(), failIfMissing);
                    setProperty(props, parsedName, parsedValue);
                }
                reader.close();
            } catch (FileNotFoundException e) {
                throw new CruiseControlException(
                        "Could not load properties from file \"" + fileName
                                + "\". The file does not exist", e);
            } catch (IOException e) {
                throw new CruiseControlException(
                        "Could not load properties from file \"" + fileName
                                + "\".", e);
            }
        } else if (environment != null) {
            // Load the environment into the project's properties
            Iterator variables = new OSEnvironment().getEnvironment().iterator();
            while (variables.hasNext()) {
                String line = (String) variables.next();
                int index = line.indexOf('=');
                if (index < 0) {
                    continue;
                }
                // If the toupper attribute was set, upcase the variables
                StringBuffer name = new StringBuffer(environment);
                name.append(".");
                if (toupper) {
                    name.append(line.substring(0, index).toUpperCase());
                } else {
                    name.append(line.substring(0, index));
                }
                String parsedValue = parsePropertiesInString(props, line.substring(index + 1), failIfMissing);
                setProperty(props, name.toString(), parsedValue);
            }
        } else {
            // Try to get a name value pair
            if (propName == null) {
                throw new CruiseControlException("Bad property definition - "
                        + new XMLOutputter().outputString(propertyElement));
            }
            if (propValue == null) {
                throw new CruiseControlException(
                        "No value provided for property \"" + propName + "\" - "
                        + new XMLOutputter().outputString(propertyElement));
            }
            String parsedValue = parsePropertiesInString(props, propValue, failIfMissing);
            setProperty(props, propName, parsedValue);
        }
    }

    // FIXME Helper extract ?
    private static void setProperty(Map props, String name, String parsedValue) {
        LOG.debug("Setting property \"" + name + "\" to \"" + parsedValue + "\".");
        props.put(name, parsedValue);
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
    static String parsePropertiesInString(Map props, String string,
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
}
