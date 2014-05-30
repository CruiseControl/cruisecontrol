package net.sourceforge.cruisecontrol;

import java.io.Serializable;

import javax.management.JMException;
import javax.management.MBeanServer;

public interface ProjectInterface extends Serializable, ProjectQuery {

    String getName();

    void execute();

    void stop();

    void register(MBeanServer server) throws JMException;

    void setBuildQueue(BuildQueue buildQueue);

    void start();

    void getStateFromOldProject(ProjectInterface project) throws CruiseControlException;

    void configureProject() throws CruiseControlException;

    void validate() throws CruiseControlException;

}
