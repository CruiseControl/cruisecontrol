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

import mx4j.adaptor.http.HttpAdaptor;
import mx4j.adaptor.rmi.RMIAdaptorMBean;
import mx4j.adaptor.rmi.jrmp.JRMPAdaptorMBean;
import mx4j.tools.naming.NamingServiceMBean;
import mx4j.util.StandardMBeanProxy;
import net.sourceforge.cruisecontrol.CruiseControlController;
import org.apache.log4j.Logger;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.naming.Context;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

/**
 * JMX agent for a ProjectController
 *
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class CruiseControlControllerAgent {

    private static final Logger LOG = Logger.getLogger(CruiseControlControllerAgent.class);

    private HttpAdaptor httpAdaptor = new HttpAdaptor();
    private int httpPort;
    private RMIAdaptorMBean rmiAdaptor;
    private int rmiPort;
    private NamingServiceMBean rmiRegistry;
    private CruiseControlControllerJMXAdaptor controllerAdaptor;
    private String path;

    public CruiseControlControllerAgent(CruiseControlController controller, int httpPort, int rmiPort, String xslPath) {
        this.httpPort = httpPort;
        this.rmiPort = rmiPort;
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
            registerRmiAdaptor(server);
        } catch (Exception e) {
            LOG.error("Problem registering RmiAdaptor", e);
        }
    }

    public void start() {
        try {
            LOG.info("starting httpAdaptor");
            httpAdaptor.start();
        } catch (IOException e) {
            LOG.error("Exception starting httpAdaptor", e);
        }
        try {
            LOG.info("starting rmiRegistry");
            rmiRegistry.start();
        } catch (RemoteException e) {
            LOG.error("Exception starting rmiRegistry", e);
        }
        try {
            LOG.info("starting rmiAdaptor");
            rmiAdaptor.start();
        } catch (Exception e) {
            LOG.error("Exception starting rmiAdaptor", e);
        }
    }

    public void stop() {
        httpAdaptor.stop();
        try {
            LOG.info("stopping rmiAdaptor");
            rmiAdaptor.stop();
        } catch (Exception e) {
            LOG.error("Exception stopping rmiAdaptor", e);
        }
        try {
            LOG.info("stopping rmiRegistry");
            rmiRegistry.stop();
        } catch (NoSuchObjectException e) {
            LOG.error("Exception stopping rmiRegistry", e);
        }
    }

    private void registerHttpAdaptor(MBeanServer server) throws Exception {
        httpAdaptor.setPort(httpPort);
        ObjectName adaptorName = new ObjectName("Adapter:name=HttpAdaptor,httpPort=" + httpPort);
        server.registerMBean(httpAdaptor, adaptorName);
        ObjectName processorName = new ObjectName("Http:name=XSLTProcessor");
        server.createMBean("mx4j.httpAdaptor.http.XSLTProcessor", processorName, null);
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

    private void registerRmiAdaptor(MBeanServer server) throws Exception {
        // Create and start the naming service
        ObjectName naming = new ObjectName("Naming:type=rmiregistry");
        server.createMBean("mx4j.tools.naming.NamingService", naming, null);
        rmiRegistry = (NamingServiceMBean) StandardMBeanProxy.create(NamingServiceMBean.class, server, naming);

        // Create the JRMP adaptor
        ObjectName adaptor = new ObjectName("Adaptor:protocol=JRMP");
        server.createMBean("mx4j.adaptor.rmi.jrmp.JRMPAdaptor", adaptor, null);
        rmiAdaptor = (JRMPAdaptorMBean) StandardMBeanProxy.create(JRMPAdaptorMBean.class, server, adaptor);

        // Set the JNDI name with which it will be registered
        String jndiName = "jrmp";
        rmiAdaptor.setJNDIName(jndiName);

        // Optionally, you can specify the JNDI properties,
        // instead of having in the classpath a jndi.properties file
        rmiAdaptor.putJNDIProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
        rmiAdaptor.putJNDIProperty(Context.PROVIDER_URL, "rmi://localhost:" + rmiPort);
    }
}
