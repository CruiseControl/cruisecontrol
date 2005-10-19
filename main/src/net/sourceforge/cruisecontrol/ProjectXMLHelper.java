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

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 *  Instantiates a project from a JDOM Element. Supports the use of Ant-like patterns in
 *  attribute values that look like this: <code>${propertyname}</code>
 */
public class ProjectXMLHelper {

    public static final String LABEL_INCREMENTER = "labelincrementer";
    private static final Logger LOG = Logger.getLogger(ProjectXMLHelper.class);
    private static final Pattern PROPERTY_PATTERN;

    public static final boolean FAIL_UPON_MISSING_PROPERTY = false;
    
    static {
        // Create a Perl 5 pattern matcher to find embedded properties
        PatternCompiler compiler = new Perl5Compiler();
        try {
            //                                    1           2         3
            PROPERTY_PATTERN = compiler.compile("(.*)\\$\\{([^${}]+)\\}(.*)");
        } catch (MalformedPatternException e) {
            // shouldn't happen
            LOG.fatal("Error compiling pattern for property matching", e);
            throw new IllegalStateException();
        }

    }

    private PluginRegistry plugins;
    private Element projectElement;
    private String projectName;
    private String overrideTarget = null;
    private Properties properties = new Properties();

    public ProjectXMLHelper() {
        this.plugins = PluginRegistry.createRegistry();
    }

    public ProjectXMLHelper(PluginRegistry registry) {
        this.plugins = registry;
    }

    /**
     * @param configFile
     * @param projName the project name, already property-resolved
     * @throws CruiseControlException
     */
    public ProjectXMLHelper(File configFile, String projName) throws CruiseControlException {
        this();

        // Load the configuration file
        Element rootElement = Util.loadConfigFile(configFile);


        Properties globalProperties = new Properties();
        // Register any global properties
        for (Iterator globProps = rootElement.getChildren("property").iterator(); globProps.hasNext(); ) {
            registerProperty(globalProperties, (Element) globProps.next(), false);
        }

        // Find this project's element within the config file
        for (Iterator projects = rootElement.getChildren("project").iterator(); projects.hasNext(); ) {
            Element currentProjectElement = (Element) projects.next();
            String parsedProjectName = parsePropertiesInString(globalProperties,
                currentProjectElement.getAttributeValue("name"), true);
            if (projName.equals(parsedProjectName)) {
                projectElement = currentProjectElement;
                // from now on, the projectName is correctly resolved.
                currentProjectElement.setAttribute("name", parsedProjectName);
                break;
            }
        }

        if (projectElement == null) {
            throw new CruiseControlException("Project not found in config file: " + projName);
        }
        projectName = projName;

        // Register the project's name as a built-in property
        setProperty(properties, "project.name", projectName);

        // Register any global properties
        for (Iterator globProps = rootElement.getChildren("property").iterator(); globProps.hasNext(); ) {
            registerProperty(properties, (Element) globProps.next(), FAIL_UPON_MISSING_PROPERTY);
        }

        // Register any project specific properties
        for (Iterator projProps = projectElement.getChildren("property").iterator(); projProps.hasNext(); ) {
            registerProperty(properties, (Element) projProps.next(), FAIL_UPON_MISSING_PROPERTY);
        }

        // Parse the entire element tree, expanding all property macros
        parsePropertiesInElement(rootElement);

        // Register any custom plugins
        for (Iterator pluginIter = projectElement.getChildren("plugin").iterator(); pluginIter.hasNext(); ) {
            plugins.register((Element) pluginIter.next());
        }

        setDateFormat(projectElement);
    }

    protected void setDateFormat(Element projElement) throws CruiseControlException {
        final Element element = projElement.getChild("dateformat");
        if (element != null) {
            CCDateFormat dateFormat = (CCDateFormat) configurePlugin(element, false);
            dateFormat.validate();
            if (dateFormat.getFormat() != null) {
                DateFormatFactory.setFormat(dateFormat.getFormat());
            }
        }
    }

    public boolean getBuildAfterFailed() {
        String buildAfterFailedAttr = projectElement.getAttributeValue("buildafterfailed");
        boolean buildafterfailed = !"false".equalsIgnoreCase(buildAfterFailedAttr);
        LOG.debug("Setting BuildAfterFailed to " + buildafterfailed);
        return buildafterfailed;
    }

    public List getBootstrappers() throws CruiseControlException {
        List bootstrappers = new ArrayList();
        Element element = projectElement.getChild("bootstrappers");
        if (element != null) {
            Iterator bootstrapperIterator = element.getChildren().iterator();
            while (bootstrapperIterator.hasNext()) {
                Element bootstrapperElement = (Element) bootstrapperIterator.next();
                Bootstrapper bootstrapper =
                        (Bootstrapper) configurePlugin(bootstrapperElement, false);
                bootstrapper.validate();
                bootstrappers.add(bootstrapper);
            }
        } else {
            LOG.debug("Project " + projectName + " has no bootstrappers");
        }
        return bootstrappers;
    }

    public List getPublishers() throws CruiseControlException {
        List publishers = new ArrayList();
        Element publishersElement = projectElement.getChild("publishers");
        if (publishersElement != null) {
            Iterator publisherIterator = publishersElement.getChildren().iterator();
            while (publisherIterator.hasNext()) {
                Element publisherElement = (Element) publisherIterator.next();
                Publisher publisher = (Publisher) configurePlugin(publisherElement, false);
                publisher.validate();
                publishers.add(publisher);
            }
        } else {
            LOG.debug("Project " + projectName + " has no publishers");
        }
        return publishers;
    }

    public Schedule getSchedule() throws CruiseControlException {
        Element scheduleElement = getRequiredElement(projectElement, "schedule");
        Schedule schedule = (Schedule) configurePlugin(scheduleElement, true);
        Iterator builderIterator = scheduleElement.getChildren().iterator();
        while (builderIterator.hasNext()) {
            Element builderElement = (Element) builderIterator.next();
            
            Builder builder = (Builder) configurePlugin(builderElement, false);
            if (overrideTarget != null) {
                builder.overrideTarget(overrideTarget);
            }
            builder.validate();
            // TODO: PauseBuilder should be able to be handled like any other
            // Builder
            if (builder instanceof PauseBuilder) {
                schedule.addPauseBuilder((PauseBuilder) builder);
            } else {
                schedule.addBuilder(builder);
            }
        }
        schedule.validate();

        return schedule;
    }

    public ModificationSet getModificationSet() throws CruiseControlException {
        final Element modSetElement = getRequiredElement(projectElement, "modificationset");
        ModificationSet modificationSet = (ModificationSet) configurePlugin(modSetElement, true);
        Iterator sourceControlIterator = modSetElement.getChildren().iterator();
        while (sourceControlIterator.hasNext()) {
            Element sourceControlElement = (Element) sourceControlIterator.next();
            SourceControl sourceControl = (SourceControl) configurePlugin(sourceControlElement, false);
            sourceControl.validate();
            modificationSet.addSourceControl(sourceControl);
        }
        modificationSet.validate();
        return modificationSet;
    }

    public LabelIncrementer getLabelIncrementer() throws CruiseControlException {
        LabelIncrementer incrementer;
        Element labelIncrementerElement = projectElement.getChild(LABEL_INCREMENTER);
        if (labelIncrementerElement != null) {
            incrementer = (LabelIncrementer) configurePlugin(labelIncrementerElement, false);
        } else {
            Class labelIncrClass = plugins.getPluginClass(LABEL_INCREMENTER);
            try {
                incrementer = (LabelIncrementer) labelIncrClass.newInstance();
            } catch (Exception e) {
                LOG.error(
                        "Error instantiating label incrementer named "
                        + labelIncrClass.getName()
                        + ". Using DefaultLabelIncrementer instead.",
                        e);
                incrementer = new DefaultLabelIncrementer();
            }
        }
        return incrementer;
    }

    /**
     *  returns the String value of an attribute on an element, exception if it's not set
     */
    protected String getRequiredAttribute(Element element, String attributeName)
            throws CruiseControlException {
        final String requiredAttribute = element.getAttributeValue(attributeName);
        if (requiredAttribute == null) {
            throw new CruiseControlException(
                    "Project "
                    + projectName
                    + ":  attribute "
                    + attributeName
                    + " is required on "
                    + element.getName());
        }
        return requiredAttribute;
    }

    private Element getRequiredElement(final Element parentElement, final String childName)
            throws CruiseControlException {
        final Element requiredElement = parentElement.getChild(childName);
        if (requiredElement == null) {
            throw new CruiseControlException(
                    "Project "
                    + projectName
                    + ": <"
                    + parentElement.getName()
                    + ">"
                    + " requires a <"
                    + childName
                    + "> element");
        }
        return requiredElement;
    }

    /**
     *  TODO: also check that instantiated class implements/extends correct interface/class
     */
    protected Object configurePlugin(Element pluginElement, boolean skipChildElements)
            throws CruiseControlException {
        String name = pluginElement.getName();
        PluginXMLHelper pluginHelper = new PluginXMLHelper(this);
        String pluginName = pluginElement.getName();

        if (plugins.isPluginRegistered(pluginName)) {
            Object pluginInstance = getConfiguredPlugin(pluginHelper, pluginElement.getName());
            if (pluginInstance != null) { // preconfigured
                return pluginHelper.configure(pluginElement, pluginInstance, skipChildElements);
            }
            return pluginHelper.configure(pluginElement, plugins.getPluginClass(pluginName), skipChildElements);
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
        final Class pluginClass = this.plugins.getPluginClass(pluginName);
        if (pluginClass == null) {
            return null;
        }
        Object configuredPlugin = null;
        Element pluginElement = plugins.getPluginConfig(pluginName);
        if (pluginElement != null) {
            // Element clonedPluginElement = (Element) pluginElement.clone();
            parsePropertiesInElement(pluginElement);
            configuredPlugin = pluginHelper.configure(pluginElement, pluginClass, false);
        }
        return configuredPlugin;
    }

    /**
     * Returns a Log instance representing the Log element.
     */
    public Log getLog() throws CruiseControlException {
        Log log;

        Element logElement = projectElement.getChild("log");
        if (logElement == null) {
            log = new Log();
        } else {
            log = (Log) configurePlugin(logElement, true);
            // Get the BuildLoggers...all the children of the Log element should be
            // BuildLogger implementations
            // note: the doc specifies we only support merge elements. In fact we could support any sub-elements.
            Iterator loggerIter = logElement.getChildren().iterator();
            while (loggerIter.hasNext()) {
                Element nextLoggerElement = (Element) loggerIter.next();
                BuildLogger nextLogger = (BuildLogger) configurePlugin(nextLoggerElement, false);
                nextLogger.validate();
                log.addLogger(nextLogger);
            }
        }
        if (log.getLogDir() == null) {
            final String defaultLogDir = "logs" + File.separatorChar + projectName;
            log.setDir(defaultLogDir);
        }
        log.setProjectName(projectName);
        log.validate();
        return log;
    }

    PluginRegistry getPlugins() {
        return plugins;
    }

    public List getListeners() throws CruiseControlException {
        List listeners = new ArrayList();
        Element element = projectElement.getChild("listeners");
        if (element != null) {
            Iterator listenerIterator = element.getChildren().iterator();
            while (listenerIterator.hasNext()) {
                Element listenerElement = (Element) listenerIterator.next();
                Listener listener = (Listener) configurePlugin(listenerElement, false);
                listener.validate();
                listeners.add(listener);
            }
            LOG.debug("Project " + projectName + " has " + listeners.size() + " listeners");
        } else {
            LOG.debug("Project " + projectName + " has no listeners");
        }
        return listeners;
    }

    /**
     * Returns all properties to which this project has access
     *
     * @return The properties accessible to this project
     */
    public Properties getProperties() {
        return this.properties;
    }
    
    /**
     * @param overrideTarget An overrideTarget to set on Builders in the Schedule.
     */
    public void setOverrideTarget(String overrideTarget) {
        this.overrideTarget = overrideTarget;
    }
    
    /**
     * Registers one or more properties as defined in a property element.
     *  
     * @param propertyElement The element from which we will register properties
     * @throws CruiseControlException
     */
    private static void registerProperty(Properties props, Element propertyElement,
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

    private static void setProperty(Properties props, String name, String parsedValue) {
        LOG.debug("Setting property \"" + name + "\" to \"" + parsedValue + "\".");
        props.put(name, parsedValue);
    }

/*
    String parsePropertiesInString(String string) throws CruiseControlException {
        return parsePropertiesInString(this.properties, string, true);
    }
*/

    /**
     * Parses a string by replacing all occurences of a property macro with
     * the resolved value of the property. Nested macros are allowed - the 
     * inner most macro will be resolved first, moving out from there.
     *  
     * @param string The string to be parsed
     * @return The parsed string
     * @throws CruiseControlException if a property cannot be resolved
     */
    private static String parsePropertiesInString(Properties props, String string,
                                                  boolean failIfMissing) throws CruiseControlException {

        if (string != null) {
            PatternMatcher matcher = new Perl5Matcher();

            // Expand all (possibly nested) properties
            while (matcher.contains(string, PROPERTY_PATTERN)) {
                MatchResult result = matcher.getMatch();
                String pre = result.group(1);
                String propertyName = result.group(2);
                String post = result.group(3);
                String value = props.getProperty(propertyName);
                if (value == null && failIfMissing) {
                    throw new CruiseControlException("Property \"" + propertyName
                            + "\" is not defined. Please check the order in which you have used your properties.");
                }
                LOG.debug("Replacing the string \"" + propertyName + "\" with \"" + value + "\".");
                string = pre + value + post;
            }
        }
        return string;
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
        parsePropertiesInElement(element, this.properties, this.projectName, FAIL_UPON_MISSING_PROPERTY);
    }

    private static void parsePropertiesInElement(Element element, Properties props,
                                                 String projectName, boolean failIfMissing)
        throws CruiseControlException {
        
        // Do not attempt to parse properties from within other projects
        if (element.getName().equals("project")
            && projectName != null
            && !element.getAttributeValue("name").equals(projectName)) {
            return;
        }

        // Recurse through the element tree - depth first
        for (Iterator children = element.getChildren().iterator(); children.hasNext(); ) {
            parsePropertiesInElement((Element) children.next(), props, projectName, failIfMissing);
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

    public static void resolveProjectNames(Element rootElement) throws CruiseControlException {
        Properties props = new Properties();
        // Register any global properties
        for (Iterator globProps = rootElement.getChildren("property").iterator(); globProps.hasNext(); ) {
            registerProperty(props, (Element) globProps.next(), false);
        }

        // parsePropertiesInElement(rootElement, props, null, false);

        for (Iterator projects = rootElement.getChildren("project").iterator(); projects.hasNext(); ) {
            parsePropertiesInElement((Element) projects.next(), props, null, false);
        }
    }
}
