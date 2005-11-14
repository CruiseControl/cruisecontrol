/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
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

import net.sourceforge.cruisecontrol.bootstrappers.BootstrapperDetail;
import net.sourceforge.cruisecontrol.publishers.PublisherDetail;
import net.sourceforge.cruisecontrol.sourcecontrols.SourceControlDetail;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Understands the CruiseControl configuration.
 */
public class Configuration {
    private MBeanServerConnection server;

    private ObjectName ccMgr;

    public Configuration(String jmxServer, int rmiPort) throws IOException,
            MalformedObjectNameException {
        JMXServiceURL address = new JMXServiceURL("service:jmx:rmi://"
                + jmxServer + ":" + rmiPort + "/jndi/jrmp");

        Map environment = new HashMap();
        environment.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put(Context.PROVIDER_URL, "rmi://" + jmxServer + ":"
                + rmiPort);

        JMXConnector cntor = JMXConnectorFactory.connect(address, environment);
        server = cntor.getMBeanServerConnection();
        ccMgr = ObjectName.getInstance("CruiseControl Manager:id=unique");
    }

    public String getConfiguration() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        return docToString(getDocument());
    }

    public void setConfiguration(String configuration)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException,
            ReflectionException, IOException {
        server.setAttribute(ccMgr, new Attribute("ConfigFileContents",
                URLDecoder.decode(configuration)));
    }

    public void setConfiguration(Document doc)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException,
            ReflectionException, IOException, JDOMException {
        setConfiguration(docToString(doc));
    }

    public Document getDocument() throws MBeanException,
            AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException, IOException, JDOMException {
        String xml = (String) server.getAttribute(ccMgr, "ConfigFileContents");
        return new SAXBuilder().build(new StringReader(xml));
    }

    public Element getElement(String name) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        return JDOMSearcher.getElement(getDocument(), name);
    }

    public BootstrapperDetail[] getBootstrappers()
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        return (BootstrapperDetail[]) getDetails("AvailableBootstrappers");
    }

    public PublisherDetail[] getPublishers() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        return (PublisherDetail[]) getDetails("AvailablePublishers");
    }

    public SourceControlDetail[] getSourceControls()
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        return (SourceControlDetail[]) getDetails("AvailableSourceControls");
    }

    public PluginDetail[] getPlugins() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        return getDetails("AvailablePlugins");
    }

    public void updatePlugin(PluginConfiguration pluginConfiguration)
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, JDOMException,
            InvalidAttributeValueException {
        Element plugin = new Element(pluginConfiguration.getName());
        for (Iterator i = pluginConfiguration.getDetails().entrySet()
                .iterator(); i.hasNext();) {
            Map.Entry element = (Map.Entry) i.next();
            String key = (String) element.getKey();
            String value = (String) element.getValue();
            if (StringUtils.isNotBlank(value)) {
                plugin.setAttribute(key, value);
            }
        }

        Document doc = getDocument();
        Element parent = JDOMSearcher.getElement(doc, pluginConfiguration
                .getType());
        // plugin = detachElement(plugin);
        parent.removeChild(plugin.getName());
        parent.addContent(plugin);
        setConfiguration(doc);
    }

    private String docToString(Document doc) throws MBeanException,
            AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException, IOException, JDOMException {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(doc)
                .trim();
    }

    private PluginDetail[] getDetails(String name) throws MBeanException,
            AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException, IOException {
        return (PluginDetail[]) server.getAttribute(ccMgr, name);
    }
}
