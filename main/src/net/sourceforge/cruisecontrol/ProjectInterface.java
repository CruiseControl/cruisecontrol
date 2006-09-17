package net.sourceforge.cruisecontrol;

import javax.management.JMException;
import javax.management.MBeanServer;

public interface ProjectInterface {

    String getName();

    void execute();

    void stop();

    void register(MBeanServer server) throws JMException;

    void setBuildQueue(BuildQueue buildQueue);

    void start();

}
