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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

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

    public Properties getVersionProperties() {
        return controller.getVersionProperties();
    }

    public String getConfigFileName() {
        return controller.getConfigFile() != null ? controller.getConfigFile().getAbsolutePath() : "";
    }

    public String getConfigFileContents() {

        File theConfigFile = controller.getConfigFile();
        
        // guard clause 
        if (theConfigFile == null) {
            return "";
        }

        StringBuffer theResults = new StringBuffer();

        try {
            BufferedReader theConfigFileReader = new BufferedReader(
                    new FileReader(theConfigFile));

            // approximate the size
            theResults = new StringBuffer((int) theConfigFile.length());

            String theCurrentLine = theConfigFileReader.readLine();
            while (theCurrentLine != null) {
                theResults.append(theCurrentLine);
                theCurrentLine = theConfigFileReader.readLine();
            }
        } catch (FileNotFoundException fne) {

            LOG.error("Configuration file not found", fne);
            //throw new CruiseControlException("Configuration file not found");
        } catch (IOException ioe) {

            LOG.error("Error reading config file for JMX", ioe);
            //throw new CruiseControlException("Error reading config file");
        }

        return theResults.toString();
    }
    
    public void setConfigFileContents(String contents) throws CruiseControlException {
        
        File theConfigFile = controller.getConfigFile();

        // guard clause if config file not set
        if (theConfigFile == null) {
            return;
        }
       
        validateConfig(contents);
        
        try {
            // ensure the file exists
            theConfigFile.mkdirs();
            theConfigFile.createNewFile();
            
            BufferedWriter out = new BufferedWriter(new FileWriter(theConfigFile));
            out.write(contents);
            out.close();
        } catch (FileNotFoundException fne) {
            LOG.error("Configuration file not found", fne);
        } catch (IOException ioe) {
            LOG.error("Error storing config file for JMX", ioe);
        }
    }
    
    public void validateConfig(String contents) throws CruiseControlException {

        StringReader theInputReader = new StringReader(contents);
        
        Element theConfigRoot = Util.parseConfig(theInputReader);
        
        new CruiseControlConfig().configure(theConfigRoot);
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
    
    public void reloadConfigFile() {
        controller.reloadConfigFile();
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

    public void projectRemoved(Project project) {
        try {
            ObjectName projectName = new ObjectName("CruiseControl Project:name=" + project.getName());
            server.unregisterMBean(projectName);
        } catch (InstanceNotFoundException noProblem) {
        } catch (MBeanRegistrationException noProblem) {
        } catch (MalformedObjectNameException e) {
            LOG.error("Could not unregister project " + project.getName(), e);
        }
    }
}
