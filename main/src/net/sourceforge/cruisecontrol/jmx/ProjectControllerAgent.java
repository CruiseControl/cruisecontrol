/*******************************************************************************
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol.jmx;

import net.sourceforge.cruisecontrol.Project;
import com.sun.jdmk.comm.HtmlAdaptorServer;

import javax.management.*;

import org.apache.log4j.Logger;

/**
 * JMX agent for a ProjectController
 *
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @see ProjectController
 */
public class ProjectControllerAgent {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(ProjectControllerAgent.class);

    private HtmlAdaptorServer adaptor = new HtmlAdaptorServer();
    private int port;
    private Project project;

    public ProjectControllerAgent(Project project, int port) {
        this.port = port;
        this.project = project;

        MBeanServer server = MBeanServerFactory.createMBeanServer();

        try {
            createAndRegisterController(server);
        } catch (Exception e) {
            log.error("Problem registering ProjectController", e);
        }

        try {
            registerHTMLAdaptor(server);
        } catch (Exception e) {
            log.error("Problem registering HTML adaptor", e);
        }
    }

    public void start() {
        adaptor.start();
    }

    public void stop() {
        adaptor.stop();
    }

    private void createAndRegisterController(MBeanServer server)
            throws Exception {

        ProjectController controller = new ProjectController(project);
        ObjectName controllerName = new ObjectName(
                "CruiseControl Manager:adminPort=" + port);
        server.registerMBean(controller, controllerName);
    }

    private void registerHTMLAdaptor(MBeanServer server) throws Exception {
        adaptor.setPort(port);

        ObjectName adaptorName = new ObjectName("Adaptor:name=html,port="
                + port);
        server.registerMBean(adaptor, adaptorName);
    }

}
