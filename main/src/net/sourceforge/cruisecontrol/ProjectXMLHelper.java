/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 *  class to instantiate a project from a JDOM Element
 */
public class ProjectXMLHelper {

    private static final Logger LOG = Logger.getLogger(ProjectXMLHelper.class);

    private Map plugins;
    private Element projectElement;
    private String projectName;

    public ProjectXMLHelper() {
        initDefaultPluginRegistry();
    };

    public ProjectXMLHelper(File configFile, String projectName) throws CruiseControlException {
        this();
        Iterator projectIterator =
            Util.loadConfigFile(configFile).getChildren("project").iterator();
        while (projectIterator.hasNext()) {
            Element currentProjectElement = (Element) projectIterator.next();
            if (currentProjectElement.getAttributeValue("name") != null
                && currentProjectElement.getAttributeValue("name").equals(projectName)) {
                projectElement = currentProjectElement;
            }
        }
        if (projectElement == null) {
            throw new CruiseControlException("Project not found in config file: " + projectName);
        }

        this.projectName = projectName;
        setDateFormat(projectElement);

        Iterator pluginIterator = projectElement.getChildren("plugin").iterator();
        while (pluginIterator.hasNext()) {
            Element pluginElement = (Element) pluginIterator.next();
            String pluginName = pluginElement.getAttributeValue("name");
            String pluginClassName = pluginElement.getAttributeValue("classname");
            if (pluginName == null || pluginClassName == null) {
                throw new CruiseControlException("name and classname are required on <plugin>");
            }
            LOG.debug("Registering plugin: " + pluginName);
            LOG.debug("to classname: " + pluginClassName);
            LOG.debug("");
            plugins.put(pluginName.toLowerCase(), pluginClassName);
        }
    }

    protected void setDateFormat(Element projectElement) {
        if (projectElement.getChild("dateformat") != null
            && projectElement.getChild("dateformat").getAttributeValue("format") != null) {
            DateFormatFactory.setFormat(
                projectElement.getChild("dateformat").getAttributeValue("format"));
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

    public String getLogDir() throws CruiseControlException {
        String logDir = "logs" + File.separatorChar + projectName;

        Element logElement = projectElement.getChild("log");
        if (logElement != null) {
            logDir = getRequiredAttribute(logElement, "dir");
        }

        return logDir;
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

    public List getAuxLogs() throws CruiseControlException {
        List auxLogs = new ArrayList();
        Element logElement = projectElement.getChild("log");
        if (logElement != null) {
            Iterator additionalLogIterator = logElement.getChildren("merge").iterator();
            while (additionalLogIterator.hasNext()) {
                Element additionalLogElement = (Element) additionalLogIterator.next();
                auxLogs.add(parseMergeElement(additionalLogElement));
            }
        }
        return auxLogs;
    }

    public String getLogXmlEncoding() throws CruiseControlException {
        String encoding = null;
        Element logElement = projectElement.getChild("log");
        if (logElement != null) {
            encoding = logElement.getAttributeValue("encoding");
        }
        return encoding;
    }

    public Schedule getSchedule() throws CruiseControlException {
        Element scheduleElement = getRequiredElement(projectElement, "schedule");
        Schedule schedule = (Schedule) configurePlugin(scheduleElement, true);
        Iterator builderIterator = scheduleElement.getChildren().iterator();
        while (builderIterator.hasNext()) {
            Element builderElement = (Element) builderIterator.next();
            if (builderElement.getName().equalsIgnoreCase("pause")) {
                PauseBuilder pauseBuilder = (PauseBuilder) configurePlugin(builderElement, false);
                pauseBuilder.validate();
                schedule.addPauseBuilder(pauseBuilder);
            } else {
                Builder builder = (Builder) configurePlugin(builderElement, false);
                builder.validate();
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

    public LabelIncrementer getLabelIncrementer() {
        String classname = (String) plugins.get("labelincrementer");
        try {
            return (LabelIncrementer) Class.forName(classname).newInstance();
        } catch (Exception e) {
            LOG.error("Error instantiating label incrementer, using DefaultLabelIncrementer.", e);
            return new DefaultLabelIncrementer();
        }
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
     *  make sure that this element has an attribute 'file' or 'dir' but not both
     */
    protected String parseMergeElement(Element mergeElement) throws CruiseControlException {
        String file = mergeElement.getAttributeValue("file");
        String dir = mergeElement.getAttributeValue("dir");
        if (file == null && dir == null) {
            throw new CruiseControlException("one of file or dir are required attributes");
        } else if (file != null && dir != null) {
            throw new CruiseControlException("only one of file or dir may be specified");
        } else if (file != null) {
            return file;
        } else {
            return dir;
        }
    }

    /**
     *  TO DO: also check that instantiated class implements/extends correct interface/class
     */
    protected Object configurePlugin(Element pluginElement, boolean skipChildElements)
        throws CruiseControlException {
        String name = pluginElement.getName();
        PluginXMLHelper pluginHelper = new PluginXMLHelper();
        String lowercaseName = pluginElement.getName().toLowerCase();

        if (plugins.containsKey(lowercaseName)) {
            return pluginHelper.configure(
                pluginElement,
                (String) plugins.get(lowercaseName),
                skipChildElements);
        } else {
            throw new CruiseControlException("Unknown plugin for: <" + name + ">");
        }
    }

    protected void initDefaultPluginRegistry() {
        plugins = new HashMap();
        plugins.put(
            "currentbuildstatusbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper");
        plugins.put(
            "cvsbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper");
        plugins.put("p4bootstrapper", "net.sourceforge.cruisecontrol.bootstrappers.P4Bootstrapper");
        plugins.put(
            "vssbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper");

        plugins.put("clearcase", "net.sourceforge.cruisecontrol.sourcecontrols.ClearCase");
        plugins.put("cvs", "net.sourceforge.cruisecontrol.sourcecontrols.CVS");
        plugins.put("filesystem", "net.sourceforge.cruisecontrol.sourcecontrols.FileSystem");
        plugins.put("mks", "net.sourceforge.cruisecontrol.sourcecontrols.MKS");
        plugins.put("p4", "net.sourceforge.cruisecontrol.sourcecontrols.P4");
        plugins.put("pvcs", "net.sourceforge.cruisecontrol.sourcecontrols.PVCS");
        plugins.put("starteam", "net.sourceforge.cruisecontrol.sourcecontrols.StarTeam");
        plugins.put("vss", "net.sourceforge.cruisecontrol.sourcecontrols.Vss");
        plugins.put("vssjournal", "net.sourceforge.cruisecontrol.sourcecontrols.VssJournal");

        plugins.put("ant", "net.sourceforge.cruisecontrol.builders.AntBuilder");
        plugins.put("maven", "net.sourceforge.cruisecontrol.builders.MavenBuilder");
        plugins.put("pause", "net.sourceforge.cruisecontrol.PauseBuilder");

        plugins.put(
            "labelincrementer",
            "net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer");

        plugins.put(
            "currentbuildstatuspublisher",
            "net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher");
        plugins.put("email", "net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher");
        plugins.put("htmlemail", "net.sourceforge.cruisecontrol.publishers.HTMLEmailPublisher");
        plugins.put("execute", "net.sourceforge.cruisecontrol.publishers.ExecutePublisher");
        plugins.put("scp", "net.sourceforge.cruisecontrol.publishers.SCPPublisher");
        plugins.put("modificationset", "net.sourceforge.cruisecontrol.ModificationSet");
        plugins.put("schedule", "net.sourceforge.cruisecontrol.Schedule");
    }

    String getClassNameForPlugin(String pluginName) {
        return (String) plugins.get(pluginName);
    }
}
