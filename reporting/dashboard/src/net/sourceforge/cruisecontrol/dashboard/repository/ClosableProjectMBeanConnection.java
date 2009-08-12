package net.sourceforge.cruisecontrol.dashboard.repository;

import javax.management.MBeanServerConnection;
import java.io.IOException;

public interface ClosableProjectMBeanConnection {
    MBeanServerConnection getMBeanServerConnection();

    void close() throws IOException;
}
