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

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.apache.log4j.Category;

import java.util.*;
import java.io.File;

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

/**
 *  class to instantiate a project from a JDOM Element
 */
public class ProjectXMLHelper {

    /** enable logging for this class */
    private static Category log = Category.getInstance(ProjectXMLHelper.class.getName());

    private Map _plugins = new HashMap();
    private Element _projectElement;

    public ProjectXMLHelper() {};

    public ProjectXMLHelper(File configFile, String projectName) throws CruiseControlException {
        Iterator projectIterator = loadConfigFile(configFile).getChildren("project").iterator();
        while(projectIterator.hasNext()) {
            Element projectElement = (Element) projectIterator.next();
            if(projectElement.getAttributeValue("name") != null && projectElement.getAttributeValue("name").equals(projectName))
                _projectElement = projectElement;
        }
        if(_projectElement == null) {
            throw new CruiseControlException("Project not found in config file: " + projectName);
        }

        Iterator pluginIterator = _projectElement.getChildren("plugin").iterator();
        while (pluginIterator.hasNext()) {
            Element pluginElement = (Element) pluginIterator.next();
            String pluginName = pluginElement.getAttributeValue("name");
            String pluginClassName = pluginElement.getAttributeValue("classname");
            if (pluginName == null || pluginClassName == null) {
                throw new CruiseControlException("name and classname are required on <plugin>");
            }
            log.debug("Registering plugin: " + pluginName);
            log.debug("to classname: " + pluginClassName);
            log.debug("");
            _plugins.put(pluginName.toLowerCase(), pluginClassName);
        }
    }

    public long getBuildInterval() {
        return Long.parseLong(_projectElement.getChild("schedule").getAttributeValue("interval"));
    }

    public String getLogDir() throws CruiseControlException {
        return getRequiredAttribute(_projectElement.getChild("log"), "dir");
    }

    public List getBootstrappers() throws CruiseControlException {
        List bootstrappers = new ArrayList();
        Iterator bootstrapperIterator = _projectElement.getChild("bootstrappers").getChildren().iterator();
        while (bootstrapperIterator.hasNext()) {
            Element bootstrapperElement = (Element) bootstrapperIterator.next();
            Bootstrapper bootstrapper = (Bootstrapper) configurePlugin(bootstrapperElement);
            bootstrappers.add(bootstrapper);
        }
        return bootstrappers;
    }

    public List getPublishers()  throws CruiseControlException {
        List publishers = new ArrayList();
        Iterator publisherIterator = _projectElement.getChild("publishers").getChildren().iterator();
        while (publisherIterator.hasNext()) {
            Element publisherElement = (Element) publisherIterator.next();
            Publisher publisher = (Publisher) configurePlugin(publisherElement);
            publishers.add(publisher);
        }
        return publishers;
    }

    public List getAuxLogs() throws CruiseControlException {
        List auxLogs = new ArrayList();
        Iterator additionalLogIterator = _projectElement.getChild("log").getChildren("merge").iterator();
        while (additionalLogIterator.hasNext()) {
            Element additionalLogElement = (Element) additionalLogIterator.next();
            auxLogs.add(parseMergeElement(additionalLogElement));
        }
        return auxLogs;
    }

    public Schedule getSchedule() throws CruiseControlException {
        Schedule schedule = new Schedule();
        Iterator builderIterator = _projectElement.getChild("schedule").getChildren().iterator();
        while(builderIterator.hasNext()) {
            Element builderElement = (Element) builderIterator.next();
            if(builderElement.getName().equalsIgnoreCase("pause")) {
                schedule.addPauseBuilder((PauseBuilder) configurePlugin(builderElement));
            } else {
                schedule.addBuilder((Builder) configurePlugin(builderElement));
            }
        }
        return schedule;
    }

    public ModificationSet getModificationSet()  throws CruiseControlException {
        ModificationSet modificationSet = new ModificationSet();
        int quietPeriod = Integer.parseInt(getRequiredAttribute(_projectElement.getChild("modificationset"), "quietperiod"));
        modificationSet.setQuietPeriod(quietPeriod);
        Iterator sourceControlIterator = _projectElement.getChild("modificationset").getChildren().iterator();
        while (sourceControlIterator.hasNext()) {
            Element sourceControlElement = (Element) sourceControlIterator.next();
            SourceControl sourceControl = (SourceControl) configurePlugin(sourceControlElement);
            modificationSet.addSourceControl(sourceControl);
        }
        return modificationSet;
    }

    public LabelIncrementer getLabelIncrementer() {
        String classname = (String) _plugins.get("labelincrementer");
        try {
            return (LabelIncrementer) Class.forName(classname).newInstance();
        } catch (Exception e) {
            log.error("Error instantiating label incrementer, using DefaultLabelIncrementer.", e);
            return new DefaultLabelIncrementer();
        }
    }

    protected Element loadConfigFile(File configFile) {
        Element cruisecontrolElement = null;
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            cruisecontrolElement = builder.build(configFile).getRootElement();
        } catch (Exception e) {
            log.fatal("", e);
        }
        return cruisecontrolElement;
    }

    /**
     *  returns the String value of an attribute on an element, exception if it's not set
     */
    protected String getRequiredAttribute(Element element, String attributeName) throws CruiseControlException {
        if (element.getAttributeValue(attributeName) != null) {
            return element.getAttributeValue(attributeName);
        } else {
            throw new CruiseControlException("Attribute " + attributeName + " is required on " + element.getName());
        }
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
    protected Object configurePlugin(Element pluginElement) throws CruiseControlException {
        String name = pluginElement.getName();
        PluginXMLHelper pluginHelper = new PluginXMLHelper();
        String lowercaseName = pluginElement.getName().toLowerCase();

        if (_plugins.containsKey(lowercaseName)) {
            return pluginHelper.configure(pluginElement, (String) _plugins.get(lowercaseName));
        } else {
            throw new CruiseControlException("Unknown plugin for: <" + name + ">");
        }
    }
}