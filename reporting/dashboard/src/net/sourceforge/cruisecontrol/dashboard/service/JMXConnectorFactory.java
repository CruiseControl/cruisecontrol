package net.sourceforge.cruisecontrol.dashboard.service;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Map;

public class JMXConnectorFactory {
    public JMXConnector connect(JMXServiceURL serviceURL, Map env) throws IOException {
        return javax.management.remote.JMXConnectorFactory.connect(serviceURL, env);
    }
}
