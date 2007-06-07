package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

public class JMXFactory {
    private Map environment = new HashMap();

    private JMXServiceURL serviceUrl;

    private JMXConnector jmxConnector;

    private JMXConnectorFactory jmxConnectorFactory;

    private MBeanServerConnection mbeanConnection;

    public static final String JMXCOMMAND_ALL_PROJECT_STATUS = "AllProjectsStatus";

    private static final Logger LOGGER = Logger.getLogger(JMXFactory.class);

    public JMXFactory(EnvironmentService envService, JMXConnectorFactory jmxFactory) throws Exception {
        this.jmxConnectorFactory = jmxFactory;
        this.environment.put("java.naming.factory.initial",
                "com.sun.jndi.rmi.registry.RegistryContextFactory");
        this.environment.put("java.naming.provider.url", "rmi://localhost:" + envService.getRmiPort());
        this.serviceUrl = new JMXServiceURL("service:jmx:rmi://localhost/jndi/jrmp");
    }

    public MBeanServerConnection getJMXConnection() {
        try {
            if (jmxConnector == null) {
                jmxConnector = jmxConnectorFactory.connect(this.serviceUrl, this.environment);
                mbeanConnection = jmxConnector.getMBeanServerConnection();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get MBeanServerConnection, will close connector and set it to null", e);
            closeConnector();
        }
        return mbeanConnection;
    }

    public void closeConnector() {
        if (jmxConnector != null) {
            try {
                jmxConnector.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to call close on connector, reconnect next time anyway", e);
            } finally {
                mbeanConnection = null;
                jmxConnector = null;
            }
        }
    }

    JMXConnector getJMXConnector() {
        return jmxConnector;
    }

    JMXServiceURL getServiceURL() {
        return serviceUrl;
    }
}
