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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;
import net.sourceforge.cruisecontrol.gendoc.PluginInfoParser;
import net.sourceforge.cruisecontrol.gendoc.html.ConfigHtmlGenerator;
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

    public List<ProjectInterface> getProjects() {
        return controller.getProjects();
    }

    public List<String> getBusyTasks() {
        return ThreadQueue.getBusyTaskNames();
    }

    public List<String> getIdleTasks() {
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

    public void register(final MBeanServer server) throws JMException {
        this.server = server;
        this.registeredName = new ObjectName("CruiseControl Manager:id=unique");
        server.registerMBean(this, this.registeredName);
        updateProjectMBeans();
    }

    private void updateProjectMBeans() {
        LOG.debug("Updating project mbeans");
        if (server != null) {
            for (final ProjectInterface project : controller.getProjects()) {
                projectAdded(project);
            }
        }
    }

    public void projectAdded(final ProjectInterface project) {
        try {
            project.register(server);
        } catch (JMException e) {
            LOG.error("Could not register project " + project.getName(), e);
        }
        final String name = "CruiseControl Project:name=" + project.getName();
        LOG.debug("Adding project " + project.getName());
        notifyChanged("projectAdded", name);
    }

    public void projectRemoved(final ProjectInterface project) {
        final String name = "CruiseControl Project:name=" + project.getName();
        LOG.debug("Removing project " + name);
        try {
            final ObjectName projectName = new ObjectName(name);
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
     * @param event event type
     * @param data event info
     */
    private void notifyChanged(final String event, final String data) {
        final Notification notification = new Notification(
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

    public Map<String, String> getAllProjectsStatus() {

        final Map<String, String> allStatus = new HashMap<String, String>();

        for (final ProjectInterface projectInterface : getProjects()) {
            // @todo Is this cast always safe, and if so, do we need to clear up the typing of these?
            final ProjectConfig projectConfig = (ProjectConfig) projectInterface;

            final String projectName = projectConfig.getName();
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

    /**
     * Gets a PluginInfo tree representing the plugins accepted by this server.
     * @param projectName Null to get the plugins from the root context. Otherwise, this should
     *        be the name of a project on the server to use as a context for loading the plugin
     *        tree. In that case, plugins defined by those projects will be included.
     * @return The PluginInfo tree.
     */
    public PluginInfo getPluginInfo(final String projectName) {
        return parsePlugins(projectName).getRootPlugin();
    }
    
    public List<PluginInfo> getAllPlugins(final String projectName) {
        return parsePlugins(projectName).getAllPlugins();
    }
    
    /**
     * Generates the HTML documentation for the plugins.
     * @param projectName Null to get the plugins from the root context. Otherwise, this should
     *        be the name of a project on the server to use as a context for loading the plugin
     *        tree. In that case, plugins defined by those projects will be included in the
     *        documentation.
     * @return The HTML content of the document.
     */
    public String getPluginHTML(final String projectName) {
        final PluginInfoParser parser = parsePlugins(projectName);
        
        try {
            final ConfigHtmlGenerator gen = new ConfigHtmlGenerator();
            return gen.generate(parser);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR; projectName: " + projectName + "; msg: " + e.getMessage();
        }
    }
    
    public String getPluginCSS() {
        // Read the CSS from the file that is bundled with the JAR, in the gendoc.html
        // package. Return an empty string if there is any kind of error.
        // TODO: should load cruisecontrol.css and configxml-gendoc.css separately and concat
        //       them together here, instead of depending on a build target to generate gendoc.css
        //       for us.
        final InputStream stream = ConfigHtmlGenerator.class.getResourceAsStream("gendoc.css");
        try {
            return IO.readText(stream);
        } catch (IOException e) {
            return "";
        }
    }
    
    /**
     * Runs a parse of all available plugins, generating the PluginInfo tree.
     * @param projectName Null to get the plugins from the root context. Otherwise, this should
     *        be the name of a project on the server to use as a context for loading the plugin
     *        tree. In that case, plugins defined by those projects will be included.
     * @return The PluginInfoParser, after it has finished parsing.
     */
    private PluginInfoParser parsePlugins(final String projectName) {
        // Get the PluginRegistry to use.
        final PluginRegistry registry;
        final CruiseControlConfig config = controller.getConfigManager().getCruiseControlConfig();
        if (projectName == null) {
            // Use the root registry.
            registry = config.getRootPlugins();
        } else {
            // Use a project-specific registry.
            registry = config.getProjectPlugins(projectName);
            if (registry == null) {
                throw new NoSuchElementException("Project " + projectName + " does not exist");
            }
        }
        
        // Create a PluginInfoParser and invoke it to parse the PluginInfo tree.
        return new PluginInfoParser(registry, PluginRegistry.ROOT_PLUGIN);
    }
    
}
