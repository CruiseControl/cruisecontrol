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
import java.io.File;

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

    public static void main(String[] args) {
        String[] correctArgs = new String[]{"-lastbuild", "20020310120000", "-label", "1.2.2", "-projectname", "myproject", "-configfile", "config.xml"};

        File myProjFile = new File("myproject");
        if (myProjFile.exists()) {
            myProjFile.delete();
        }
        Main main = new Main();

        Project project = null;
        try {
            project = main.configureProject(correctArgs);
        } catch (CruiseControlException e) {
            e.printStackTrace();
        }

        ProjectController controller = new ProjectController(project, 1010);
        controller.start();
    }
}
