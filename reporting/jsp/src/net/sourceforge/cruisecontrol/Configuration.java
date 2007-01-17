/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import net.sourceforge.cruisecontrol.util.Util;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Communicates with the CruiseControl JMX server to allow CRUD operations on
 * the CruiseControl configuration.
 */
public class Configuration {
    private MBeanServerConnection server;
    private ObjectName ccMgr;
    private String configuration;
    private PluginDetail[] pluginDetails;

    public Configuration(String jmxServer, int rmiPort) throws IOException, MalformedObjectNameException {
        JMXServiceURL address = new JMXServiceURL("service:jmx:rmi://" + jmxServer + ":" + rmiPort + "/jndi/jrmp");

        Map environment = new HashMap();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put(Context.PROVIDER_URL, "rmi://" + jmxServer + ":" + rmiPort);

        JMXConnector cntor = JMXConnectorFactory.connect(address, environment);
        server = cntor.getMBeanServerConnection();
        ccMgr = ObjectName.getInstance("CruiseControl Manager:id=unique");
    }

    public String getConfiguration() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException, IOException, JDOMException {
        if (configuration == null) {
            load();
        }

        return configuration;
    }

    public Document getDocument() throws ReflectionException, IOException, InstanceNotFoundException, MBeanException,
            AttributeNotFoundException, JDOMException {
        return stringToDoc(getConfiguration());
    }

    public Element getElement(String name) throws ReflectionException, InstanceNotFoundException, IOException,
            MBeanException, AttributeNotFoundException, JDOMException {
        return JDOMSearcher.getElement(getDocument(), name);
    }

    public PluginDetail[] getPluginDetails() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        if (pluginDetails == null) {
            pluginDetails = (PluginDetail[]) server.getAttribute(ccMgr, "AvailablePlugins");
        }

        return pluginDetails;
    }

    public PluginDetail[] getConfiguredBootstrappers(String project) throws CruiseControlException,
            AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException,
            JDOMException {
        return getConfiguredPluginDetails(project, getProjectConfig(project).getBootstrappers());
    }

    public PluginDetail[] getConfiguredBuilders(String project) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException,
            JDOMException {
        return getConfiguredPluginDetails(project, getProjectConfig(project).getSchedule().getBuilders());
    }

    public PluginDetail[] getConfiguredListeners(String project) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException,
            JDOMException {
        return getConfiguredPluginDetails(project, getProjectConfig(project).getListeners());
    }

    public PluginDetail[] getConfiguredLoggers(String project) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException,
            JDOMException {
        return getConfiguredPluginDetails(project, Arrays.asList(getProjectConfig(project).getLog().getLoggers()));
    }

    public PluginDetail[] getConfiguredPublishers(String project) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException,
            JDOMException {
        return getConfiguredPluginDetails(project, getProjectConfig(project).getPublishers());
    }

    public PluginDetail[] getConfiguredSourceControls(String project) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException,
            JDOMException {
        return getConfiguredPluginDetails(project, getProjectConfig(project).getModificationSet().getSourceControls());
    }

    public void load() throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException, IOException, JDOMException {
        // Ensure xml is well-formed
        configuration = docToString(stringToDoc(loadConfiguration()));
    }

    public void save() throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException, IOException {
        server.setAttribute(ccMgr, new Attribute("ConfigFileContents", configuration));
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public void setConfiguration(Document doc) {
        setConfiguration(docToString(doc));
    }

    public void updatePluginConfiguration(PluginConfiguration pluginConfiguration) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, JDOMException,
            InvalidAttributeValueException {
        Element plugin = new Element(pluginConfiguration.getName());
        for (Iterator i = pluginConfiguration.getDetails().entrySet().iterator(); i.hasNext();) {
            Map.Entry element = (Map.Entry) i.next();
            String key = (String) element.getKey();
            String value = (String) element.getValue();
            if (StringUtils.isNotBlank(value)) {
                plugin.setAttribute(key, value);
            }
        }

        Document doc = getDocument();
        Element parent = JDOMSearcher.getElement(doc, pluginConfiguration.getParentElementName());
        parent.removeChild(plugin.getName());
        parent.addContent(plugin);

        setConfiguration(doc);
    }

    private static String docToString(Document doc) {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(doc).trim();
    }

    private static Document stringToDoc(String configuration) throws JDOMException, IOException {
        return new SAXBuilder().build(new StringReader(configuration));
    }

    private PluginDetail[] getConfiguredPluginDetails(String project, List plugins) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException {
        Collection details = new LinkedList();
        PluginRegistry registry = getPluginRegistry();

        for (Iterator i = plugins.iterator(); i.hasNext();) {
            Class nextClass = i.next().getClass();
            details.add(new GenericPluginDetail(registry.getPluginName(nextClass), nextClass));
        }

        return (PluginDetail[]) details.toArray(new PluginDetail[details.size()]);
    }

    private PluginRegistry getPluginRegistry() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        return (PluginRegistry) server.getAttribute(ccMgr, "PluginRegistry");
    }

    private ProjectConfig getProjectConfig(String project) throws CruiseControlException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, JDOMException {
        Element element = Util.loadRootElement(new ByteArrayInputStream(getConfiguration().getBytes()));
        CruiseControlConfig config = new CruiseControlConfig(element);
        return (ProjectConfig) config.getProject(project);
    }

    private String loadConfiguration() throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException, IOException {
        return (String) server.getAttribute(ccMgr, "ConfigFileContents");
    }
}
