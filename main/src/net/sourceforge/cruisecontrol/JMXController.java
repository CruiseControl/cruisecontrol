/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol;


import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;

import com.sun.management.jmx.Trace;
import com.sun.jdmk.comm.HtmlAdaptorServer;

/**
 * Implements the interface including those attributes exposed by the
 * CruiseControl main process.
 * 
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class JMXController implements JMXControllerMBean {
    
    private ManagedMasterBuild master;

    /**
     * Creates a new instance of this controller, which requires the instance
     * of ManagedMasterBuild that it controls.
     * 
     * @param master Process to control.
     */
    public JMXController(ManagedMasterBuild master) {
        this.master = master;
    }

    /**
     * Starts the JMXController by initializing the JMX reference implementation.
     */
    public void start() {
        // CREATE the MBeanServer
	//
	System.out.println("\n\tCREATE the MBeanServer.");
	MBeanServer server = MBeanServerFactory.createMBeanServer();

        // CREATE and START a new JMX Controller
	//
        System.out.println("\n\tCREATE and REGISTER a new JMXController:");
        ObjectName controllerName = null;
	try {
	    controllerName = new ObjectName("CruiseControl Manager:project=Implement Logic for Projects");
	    System.out.println("\tOBJECT NAME           = " + controllerName);
	    server.registerMBean(this, controllerName);
	} catch(Exception e) {
	    System.out.println("\t!!! Could not create the JMXController !!!");
	    e.printStackTrace();
	    return;
	}
        //Kick off the master build process.
        new Thread(master).start();

	// CREATE and START a new HTML adaptor
	//
	System.out.println("\n\tCREATE, REGISTER and START a new HTML adaptor:");
	HtmlAdaptorServer html = new HtmlAdaptorServer();
	ObjectName html_name = null;
	try {
	    html_name = new ObjectName("Adaptor:name=html,port=8082");
	    System.out.println("\tOBJECT NAME           = " + html_name);
	    server.registerMBean(html, html_name);
	} catch(Exception e) {
	    System.out.println("\t!!! Could not create the HTML adaptor !!!");
	    e.printStackTrace();
	    return;
	}
	html.start();
    }

    /**
     * Sets the number of seconds in the build interval.
     * 
     * @param buildInterval
     *               Number of seconds.
     */
    public void setBuildIntervalSeconds(long buildInterval) {
        master.getProperties().setBuildInterval(buildInterval*1000);
    }

    /**
     * Returns the number of seconds in the build interval.
     * 
     * @return Number of seconds.
     */
    public long getBuildIntervalSeconds() {
        return master.getProperties().getBuildInterval()/1000;
    }

    /**
     * Returns the number of times a build has been attempted, which should
     * include a repository check at the beginning of each.
     * 
     * @return Number of build attempts.
     */
    public long getRepositoryCheckCount() {
        return master.getRepositoryCheckCount();
    }

    /**
     * Returns the duration the managed process has been executing.
     * 
     * @return Execution duration.
     */
    public String getUpTime() {
        return master.getUpTime();
    }

    /**
     * Returns the number of successful builds performed by the managed
     * process.
     * 
     * @return Successful build count.
     */
    public long getSuccessfulBuildCount() {
        return master.getSuccessfulBuildCount();
    }

    /**
     * Tells the controlled process to run as soon as possible.
     */
    public void runAsSoonAsPossible() {
        master.buildAsap();
    }

    /**
     * Pauses the controlled process.
     */
    public void pause() {
        master.pause();
    }

    /**
     * Resumes the controlled process.
     */
    public void resume() {
        master.resume();
    }

    /**
     * Starts the JMXController, which will create a process to manage and
     * begin managing it.
     * 
     * @param args   Arguments to pass to the controlled process.
     */
    public static void main(String[] args) {
        JMXController controller = new JMXController(new ManagedMasterBuild(args));
        controller.start();
    }
}
