/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, 2006, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginDetail;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.PluginType;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectInterface;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueue;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlControllerJMXAdaptor extends NotificationBroadcasterSupport
        implements CruiseControlControllerJMXAdaptorMBean, CruiseControlController.Listener {
    private static final Logger LOG = Logger.getLogger(CruiseControlControllerJMXAdaptor.class);
    private static final Object SEQUENCE_LOCK = new Object();
    private static int sequence = 0;
    private final CruiseControlController controller;
    private ObjectName registeredName;
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

        final File theConfigFile = controller.getConfigFile();

        // guard clause
        if (theConfigFile == null) {
            return "";
        }

        StringBuffer theResults = new StringBuffer();

        try {

            final BufferedReader theConfigFileReader = new BufferedReader(new FileReader(theConfigFile));
            try {
                // approximate the size
                theResults = new StringBuffer((int) theConfigFile.length());
                readConfigFileContents(theResults, theConfigFileReader);
            } finally {
                theConfigFileReader.close();
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

    /**
     * Populate the given StringBuffer with the content of the config file
     * @param theResults will contain the config file contents
     * @param theConfigFileReader reader opened on the config file
     * @throws IOException if an error occurs
     */
    static void readConfigFileContents(StringBuffer theResults, BufferedReader theConfigFileReader)
            throws IOException {

        String theCurrentLine = theConfigFileReader.readLine();
        while (theCurrentLine != null) {
            theResults.append(theCurrentLine).append('\n');
            theCurrentLine = theConfigFileReader.readLine();
        }
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
            Util.doMkDirs(theConfigFile);
            theConfigFile.createNewFile();

            IO.write(theConfigFile, contents);
        } catch (FileNotFoundException fne) {
            LOG.error("Configuration file not found", fne);
        } catch (IOException ioe) {
            LOG.error("Error storing config file for JMX", ioe);
        }
    }

    public void validateConfig(String contents) throws CruiseControlException {

        InputStream in = new ByteArrayInputStream(contents.getBytes());
        Element config = Util.loadRootElement(in);

        new CruiseControlConfig(config, controller);
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

    public List getBusyTasks() {
        return ThreadQueue.getBusyTaskNames();
    }

    public List getIdleTasks() {
        return ThreadQueue.getIdleTaskNames();
    }

    public PluginDetail[] getAvailableBootstrappers() {
        return controller.getAvailableBootstrappers();
    }

    public PluginDetail[] getAvailablePublishers() {
        return controller.getAvailablePublishers();
    }

    public PluginDetail[] getAvailableSourceControls() {
        return controller.getAvailableSourceControls();
    }

    public PluginDetail[] getAvailablePlugins() {
        return controller.getAvailablePlugins();
    }

    public PluginType[] getAvailablePluginTypes() {
        return controller.getAvailablePluginTypes();
    }

    public PluginRegistry getPluginRegistry() {
        return controller.getPluginRegistry();
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
        this.registeredName = new ObjectName("CruiseControl Manager:id=unique");
        server.registerMBean(this, this.registeredName);
        updateProjectMBeans();
    }

    private void updateProjectMBeans() {
        LOG.debug("Updating project mbeans");
        if (server != null) {
            for (Iterator iterator = controller.getProjects().iterator(); iterator.hasNext();) {
                ProjectInterface project = (ProjectInterface) iterator.next();
                projectAdded(project);
            }
        }
    }

    public void projectAdded(ProjectInterface project) {
        try {
            project.register(server);
        } catch (JMException e) {
            LOG.error("Could not register project " + project.getName(), e);
        }
        String name = "CruiseControl Project:name=" + project.getName();
        LOG.debug("Adding project " + project.getName());
        notifyChanged("projectAdded", name);
    }

    public void projectRemoved(ProjectInterface project) {
        String name = "CruiseControl Project:name=" + project.getName();
        LOG.debug("Removing project " + name);
        try {
            ObjectName projectName = new ObjectName(name);
            server.unregisterMBean(projectName);
        } catch (InstanceNotFoundException noProblem) {
        } catch (MBeanRegistrationException noProblem) {
        } catch (MalformedObjectNameException e) {
            LOG.error("Could not unregister project " + project.getName(), e);
        }
        notifyChanged("projectRemoved", name);
    }


    /**
     * Send a JMX notification that the list of projects has changed.
     * This only needs to be done when the project list changes, not
     * when a JMX server is registered.
     * <p/>
     * At the moment, we only absolutely know when a project is added or
     * removed, but not when the configuration file is reloaded or changed.
     */
    private void notifyChanged(String event, String data) {
        Notification notification = new Notification(
                "cruisecontrol." + event + ".event", this.registeredName,
                nextSequence());
        notification.setUserData(data);
        sendNotification(notification);
        LOG.debug("Sent " + event + " event.");
    }


    private int nextSequence() {
        synchronized (SEQUENCE_LOCK) {
            return ++sequence;
        }
    }

    public Map getAllProjectsStatus() {
        Map allStatus = new HashMap();
        for (Iterator iter = getProjects().iterator(); iter.hasNext();) {
            ProjectConfig projectConfig = (ProjectConfig) iter.next();

            String projectName = projectConfig.getName();
            String status = projectConfig.getStatus();
            if (ProjectState.BUILDING.hasDescription(status)) {
                status = status + " since " + projectConfig.getBuildStartTime();
            } else if (projectConfig.isPaused()) {
                status = ProjectState.PAUSED.getName();
            }
            allStatus.put(projectName, status);
        }
        return allStatus;
    }
}
