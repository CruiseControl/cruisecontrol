// DON'T TOUCH THIS SECTION
//
// COPYRIGHT isMobile.com AB 2000
//
// The copyright of the computer program herein is the property of
// isMobile.com AB, Sweden. The program may be used and/or copied
// only with the written permission from isMobile.com AB or in the
// accordance with the terms and conditions stipulated in the
// agreement/contract under which the program has been supplied.
//
// $Id$
//
// END DON'T TOUCH

package net.sourceforge.cruisecontrol.jmx;

import com.sun.jdmk.comm.HtmlAdaptorServer;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Main;
import net.sourceforge.cruisecontrol.Project;
import org.apache.log4j.Category;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * @author Niclas Olofsson
 */
public class ProjectController implements ProjectControllerMBean {

    /** enable logging for this class */
    private static Category log = Category.getInstance(ProjectController.class.getName());

    private Project _project = null;
    private int _port;
    private HtmlAdaptorServer _htmlAdap;

    public ProjectController(Project project, int port) {
        _project = project;
        _port = port;
    }


    /**
     * Starts the JMXController by initializing the JMX reference implementation.
     */
    public void start() {
        // CREATE the MBeanServer
        //
        log.debug("Create MBeanServer");
        MBeanServer server = MBeanServerFactory.createMBeanServer();

        // CREATE and START a new JMX Controller
        //
        log.debug("CREATE and REGISTER a new ProjectController");
        ObjectName controllerName = null;
        try {
            controllerName = new ObjectName("CruiseControl Manager:adminPort=" + _port);
            log.debug("Controller name: " + controllerName);
            server.registerMBean(this, controllerName);
        } catch (Exception e) {
            log.error("Could not create the ProjectController", e);
            return;
        }

        // CREATE and START a new HTML adaptor
        //
        log.debug("CREATE, REGISTER and START a new HTML adaptor");
        _htmlAdap = new HtmlAdaptorServer();
        _htmlAdap.setPort(_port);
        ObjectName html_name = null;
        try {
            html_name = new ObjectName("Adaptor:name=html,port=" + _port);
            log.debug("Controller name: " + html_name);
            server.registerMBean(_htmlAdap, html_name);
        } catch (Exception e) {
            log.error("Could not create the HTML Adaptor", e);
            return;
        }
        _htmlAdap.start();
    }

    /**
     * Stops any processes started by the controller.
     */
    public void stop() {
        //Kill the html adaptor
        _htmlAdap.stop();
    }

    /**
     * Pauses the controlled process.
     */
    public void pause() {
        _project.setPaused(true);
    }

    /**
     * Resumes the controlled process.
     */
    public void resume() {
        _project.setPaused(false);
    }

    /**
     * Returns the duration the managed process has been executing.
     *
     * @return Execution duration.
     */
    public long getUpTime() {
        return 0;
    }

    /**
     * Returns the number of successful builds performed by the managed
     * process.
     *
     * @return Successful build count.
     */
    public long getSuccessfulBuildCount() {
        return 0;
    }

    /**
     * Is the project paused?
     *
     * @return Pause state
     */
    public boolean isPaused() {
        return _project.isPaused();
    }

    /**
     * Parse port number from arguments.  port should always be specified
     * in arguments for ProjectController.
     *
     * @return port number;
     * @throws CruiseControlException if port is not specified
     */
    protected static int parsePort(String args[])
            throws CruiseControlException {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-port")) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CruiseControlException(
                            "'port' argument was not specified.");
                }
            }
        }
        throw new CruiseControlException(
                "'port' is a required argument to ProjectController.");
    }

    public static void main(String[] args) {
        Main main = new Main();

        Project project = null;
        int port;
        try {
            project = main.configureProject(args);
            port = parsePort(args);
        } catch (CruiseControlException e) {
            log.fatal(e.getMessage());
            usage();
            return;
        }

        ProjectController controller = new ProjectController(project, port);
        controller.start();

        project.execute();
    }

    /**
     *  Displays the standard usage message for ProjectController, and exit.
     */
    public static void usage() {
        log.info("Usage:");
        log.info("");
        log.info("Starts an http build controller");
        log.info("");
        log.info("java CruiseControl [options]");
        log.info("where options are:");
        log.info("");
        log.info("   -port number           where number is the port of the Controller web site");
        log.info("   -projectname name      where name is the name of the project");
        log.info("   -lastbuild timestamp   where timestamp is in yyyyMMddHHmmss format.  note HH is the 24 hour clock.");
        log.info("   -label label           where label is in x.y format, y being an integer.  x can be any string.");
        log.info("   -configfile file       where file is the configuration file");
        System.exit(1);
    }

}
