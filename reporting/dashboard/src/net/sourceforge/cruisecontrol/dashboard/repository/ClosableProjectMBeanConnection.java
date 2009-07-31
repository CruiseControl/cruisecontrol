package net.sourceforge.cruisecontrol.dashboard.repository;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.io.IOException;

/**
 * Hold onto original JMXConnection after providing MBeanServerConnection, this allows the connection to be closed
 * to avoid leaks. See CC-831.
 *
 * @author Dan Rollo
 * Date: Jun 24, 2009
 * Time: 11:05:04 PM
 */
public final class ClosableProjectMBeanConnection {

    private final JMXConnector jmxConnector;
    private final MBeanServerConnection mBeanServerConnection;

    ClosableProjectMBeanConnection(final JMXConnector jmxConnector) throws IOException {

        this.jmxConnector = jmxConnector;
        this.mBeanServerConnection = jmxConnector.getMBeanServerConnection();
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return mBeanServerConnection;
    }

    public void close() throws IOException {
        jmxConnector.close();
    }
}
