/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.jmx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import mx4j.tools.adaptor.http.HttpAdaptor;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.Main;

import org.apache.log4j.Logger;

/**
 * JMX agent for a ProjectController
 *
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class CruiseControlControllerAgent {

    private static final Logger LOG = Logger.getLogger(CruiseControlControllerAgent.class);

    private HttpAdaptor httpAdaptor = new HttpAdaptor();
    private int httpPort;
    private JMXConnectorServer connectorServer;
    private int connectorServerPort;
    private CruiseControlControllerJMXAdaptor controllerAdaptor;
    private String path;

    public CruiseControlControllerAgent(CruiseControlController controller, int httpPort,
            int connectorServerPort, String xslPath) {
        this.httpPort = httpPort;
        this.connectorServerPort = connectorServerPort;
        path = xslPath;
        this.controllerAdaptor = new CruiseControlControllerJMXAdaptor(controller);

        MBeanServer server = MBeanServerFactory.createMBeanServer();
        try {
            controllerAdaptor.register(server);
        } catch (Exception e) {
            LOG.error("Problem registering CruiseControlController HttpAdaptor", e);
        }
        try {
            registerHttpAdaptor(server);
        } catch (Exception e) {
            LOG.error("Problem registering HttpAdaptor", e);
        }
        try {
            registerConnectorServer(server);
        } catch (Exception e) {
            LOG.error("Problem registering ConnectorServer", e);
        }
        try {
            ObjectName name = new ObjectName("Logger:name=root");
            server.registerMBean(new LoggerController(Logger.getRootLogger()), name);
        } catch (Exception e) {
            LOG.error("Problem registering LoggerController for root-Logger", e);
        }
        
    }

    public void start() {
        if (useHttpAdaptor()) {
            try {
                LOG.info("starting httpAdaptor");
                httpAdaptor.start();
            } catch (IOException e) {
                LOG.error("Exception starting httpAdaptor", e);
            }
        }
        if (useConnectorServer()) {
            try {
                LOG.info("starting connectorServer");
                connectorServer.start();
            } catch (Exception e) {
                LOG.error("Exception starting connectorServer", e);
            }
        }
    }

    public void stop() {
        if (useHttpAdaptor()) {
            httpAdaptor.stop();
        }
        if (useConnectorServer()) {
            try {
                LOG.info("stopping connectorServer");
                connectorServer.stop();
            } catch (Exception e) {
                LOG.error("Exception stopping connectorServer", e);
            }
        }
    }

    private void registerHttpAdaptor(MBeanServer server) throws Exception {
        if (useHttpAdaptor()) {
            httpAdaptor.setPort(httpPort);
            httpAdaptor.setHost("0.0.0.0");
            ObjectName adaptorName = new ObjectName("Adapter:name=HttpAdaptor,httpPort=" + httpPort);
            server.registerMBean(httpAdaptor, adaptorName);
            ObjectName processorName = new ObjectName("Http:name=XSLTProcessor");
            server.createMBean("mx4j.tools.adaptor.http.XSLTProcessor", processorName, null);
            String pathInJar = "net/sourceforge/cruisecontrol/jmx/xsl";
            if (path != null && !path.equals("")) {
                LOG.info("Starting HttpAdaptor with customized Stylesheets");
                server.setAttribute(processorName, new Attribute("File", path));
            } else {
                LOG.info("Starting HttpAdaptor with CC-Stylesheets");
                server.setAttribute(processorName, new Attribute("PathInJar", pathInJar));
            }
            server.setAttribute(adaptorName, new Attribute("ProcessorName", processorName));
        }
    }

    private boolean useHttpAdaptor() {
        return httpPort != Main.NOT_FOUND;
    }

    private void registerConnectorServer(MBeanServer server) throws Exception {
        if (useConnectorServer()) {
            final String jndiName = "/jndi/jrmp";
            JMXServiceURL address = new JMXServiceURL("rmi", "localhost", 0, jndiName);

            Map environment = new HashMap();
            final String registryContextFactory = "com.sun.jndi.rmi.registry.RegistryContextFactory";
            environment.put(Context.INITIAL_CONTEXT_FACTORY, registryContextFactory);
            environment.put(Context.PROVIDER_URL, "rmi://localhost:" + connectorServerPort);
            
            connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(address, environment, server);
            ObjectName connServerName = new ObjectName("ConnectorServer:name=" + jndiName);
            server.registerMBean(connectorServer, connServerName);
        }
    }

    private boolean useConnectorServer() {
        return connectorServerPort != Main.NOT_FOUND;
    }
}
