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
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  Instantiates a project from a JDOM Element
 */
public class ProjectXMLHelper {

    private static final Logger LOG = Logger.getLogger(ProjectXMLHelper.class);

    private PluginRegistry plugins;
    private Element projectElement;
    private String projectName;
    private String overrideTarget = null;
    public static final String LABEL_INCREMENTER = "labelincrementer";

    public ProjectXMLHelper() throws CruiseControlException {
        plugins = PluginRegistry.createRegistry();
    }

    public ProjectXMLHelper(File configFile, String projName) throws CruiseControlException {
        this();
        Iterator projectIterator =
                Util.loadConfigFile(configFile).getChildren("project").iterator();
        while (projectIterator.hasNext()) {
            Element currentProjectElement = (Element) projectIterator.next();
            if (currentProjectElement.getAttributeValue("name") != null
                    && currentProjectElement.getAttributeValue("name").equals(projName)) {
                projectElement = currentProjectElement;
            }
        }
        if (projectElement == null) {
            throw new CruiseControlException("Project not found in config file: " + projName);
        }

        projectName = projName;
        setDateFormat(projectElement);

        Iterator pluginIterator = projectElement.getChildren("plugin").iterator();
        while (pluginIterator.hasNext()) {
            Element pluginElement = (Element) pluginIterator.next();
            String pluginName = pluginElement.getAttributeValue("name");
            String pluginClassName = pluginElement.getAttributeValue("classname");
            if (pluginName == null || pluginClassName == null) {
                throw new CruiseControlException("name and classname are required on <plugin>");
            }
            LOG.debug("Registering plugin '" + pluginName
                    + "' to classname '" + pluginClassName
                    + "' for project " + projectName);
            LOG.debug("");
            plugins.register(pluginName, pluginClassName);
        }
    }

    protected void setDateFormat(Element projElement) {
        if (projElement.getChild("dateformat") != null
                && projElement.getChild("dateformat").getAttributeValue("format") != null) {
            DateFormatFactory.setFormat(
                    projElement.getChild("dateformat").getAttributeValue("format"));
        }
    }

    public boolean getBuildAfterFailed() {
        String buildAfterFailedAttr = projectElement.getAttributeValue("buildafterfailed");
        if (!"false".equalsIgnoreCase(buildAfterFailedAttr)) {
            // default if not specified and all other cases
            buildAfterFailedAttr = "true";
        }
        boolean buildafterfailed = Boolean.valueOf(buildAfterFailedAttr).booleanValue();
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
            SourceControl sourceControl =
                    (SourceControl) configurePlugin(sourceControlElement, false);
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
        if (element.getAttributeValue(attributeName) != null) {
            return element.getAttributeValue(attributeName);
        } else {
            throw new CruiseControlException(
                    "Project "
                    + projectName
                    + ":  attribute "
                    + attributeName
                    + " is required on "
                    + element.getName());
        }
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
            return pluginHelper.configure(
                    pluginElement,
                    plugins.getPluginClass(pluginName),
                    skipChildElements);
        } else {
            throw new CruiseControlException("Unknown plugin for: <" + name + ">");
        }
    }

    /**
     * Returns a Log instance representing the Log element.
     */
    public Log getLog() throws CruiseControlException {
        Log log = new Log(this.projectName);
        String logDir = "logs" + File.separatorChar + projectName;

        Element logElement = projectElement.getChild("log");
        if (logElement != null) {
            String dirValue = logElement.getAttributeValue("dir");
            if (dirValue != null) {
                logDir = dirValue;
            }
            log.setLogXmlEncoding(logElement.getAttributeValue("encoding"));

            //Get the BuildLoggers...all the children of the Log element should be
            //  BuildLogger implementations
            Iterator loggerIter = logElement.getChildren().iterator();
            while (loggerIter.hasNext()) {
                Element nextLoggerElement = (Element) loggerIter.next();
                BuildLogger nextLogger = (BuildLogger) configurePlugin(nextLoggerElement, false);
                nextLogger.validate();
                log.addLogger(nextLogger);
            }
        }

        log.setLogDir(logDir);
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
     * @param overrideTarget An overrideTarget to set on Builders in the Schedule.
     */
    public void setOverrideTarget(String overrideTarget) {
        this.overrideTarget = overrideTarget;
    }
}
