/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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

import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Project;
import org.apache.log4j.Logger;

import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlControllerJMXAdaptor implements CruiseControlControllerJMXAdaptorMBean,
        CruiseControlController.Listener {
    private static final Logger LOG = Logger.getLogger(CruiseControlControllerJMXAdaptor.class);
    private final CruiseControlController controller;
    private MBeanServer server;

    public CruiseControlControllerJMXAdaptor(CruiseControlController controlController) {
        controller = controlController;
        controller.addListener(this);
    }

    public String getConfigFileName() {
        return controller.getConfigFile() != null ? controller.getConfigFile().getAbsolutePath() : "";
    }

    public void setConfigFileName(String fileName) throws InvalidAttributeValueException {
        try {
            controller.setConfigFile(fileName != null ? new File(fileName) : null);
        } catch (CruiseControlException e) {
            throw new InvalidAttributeValueException(e.getMessage());
        }
    }

    public List getProjects() {
        return controller.getProjects();
    }

    public void resume() {
        controller.resume();
    }

    public void pause() {
        controller.pause();
    }

    public String getBuildQueueStatus() {
        return controller.getBuildQueueStatus();
    }

    public void halt() {
        controller.halt();
    }

    public void register(MBeanServer server) throws JMException {
        this.server = server;
        ObjectName controllerName = new ObjectName("CruiseControl Manager:id=unique");
        server.registerMBean(this, controllerName);
        updateProjectMBeans();
    }

    private void updateProjectMBeans() {
        LOG.debug("Updating project mbeans");
        if (server != null) {
            for (Iterator iterator = controller.getProjects().iterator(); iterator.hasNext();) {
                Project project = (Project) iterator.next();
                projectAdded(project);
            }
        }
    }

    public void projectAdded(Project project) {
        try {
            LOG.debug("Registering project mbean");
            ProjectController projectController = new ProjectController(project);
            projectController.register(server);
        } catch (JMException e) {
            LOG.error("Could not register project " + project.getName(), e);
        }
    }
}
