/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import net.sourceforge.cruisecontrol.Project;

import org.apache.log4j.Logger;

import com.sun.jdmk.comm.HtmlAdaptorServer;

/**
 * JMX agent for a ProjectController
 *
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @see ProjectController
 */
public class ProjectControllerAgent {

    private static final Logger LOG = Logger.getLogger(ProjectControllerAgent.class);

    private HtmlAdaptorServer _adaptor = new HtmlAdaptorServer();
    private int _port;
    private Project _project;

    public ProjectControllerAgent(Project project, int port) {
        _port = port;
        _project = project;

        MBeanServer server = MBeanServerFactory.createMBeanServer();

        try {
            createAndRegisterController(server);
        } catch (Exception e) {
            LOG.error("Problem registering ProjectController", e);
        }

        try {
            registerHTMLAdaptor(server);
        } catch (Exception e) {
            LOG.error("Problem registering HTML adaptor", e);
        }
    }

    public void start() {
        _adaptor.start();
    }

    public void stop() {
        _adaptor.stop();
    }

    private void createAndRegisterController(MBeanServer server)
            throws Exception {

        ProjectController controller = new ProjectController(_project);
        ObjectName controllerName = new ObjectName(
                "CruiseControl Manager:adminPort=" + _port);
        server.registerMBean(controller, controllerName);
    }

    private void registerHTMLAdaptor(MBeanServer server) throws Exception {
        _adaptor.setPort(_port);

        ObjectName adaptorName = new ObjectName("Adaptor:name=html,port="
                + _port);
        server.registerMBean(_adaptor, adaptorName);
    }

}
