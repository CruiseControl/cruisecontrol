package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

public class JMXConnectorFactory {
    public JMXConnector connect(JMXServiceURL serviceURL, Map env) throws IOException {
        return javax.management.remote.JMXConnectorFactory.connect(serviceURL, env);
    }
}
