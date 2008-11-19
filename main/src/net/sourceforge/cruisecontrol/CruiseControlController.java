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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.sourceforge.cruisecontrol.config.XMLConfigManager;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlController {
    private static final Logger LOG = Logger.getLogger(CruiseControlController.class);
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.xml";
    private File configFile;
    private final List<ProjectInterface> projects = new ArrayList<ProjectInterface>();
    private final BuildQueue buildQueue = new BuildQueue();
    private Properties versionProperties;

    private final List<Listener> listeners = new ArrayList<Listener>();
    private XMLConfigManager configManager;

    private final ParsingConfigMutex parsingConfigMutex = new ParsingConfigMutex();

    public CruiseControlController() {
        buildQueue.addListener(new BuildQueueListener());
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setVersionProperties(Properties versionProperties) {
        this.versionProperties = versionProperties;
    }

    public Properties getVersionProperties() {
       return versionProperties;
    }

    public void setConfigFile(final File configFile) throws CruiseControlException {
        if (configFile == null) {
            throw new CruiseControlException("No config file");
        }
        if (!configFile.isFile()) {
            throw new CruiseControlException("Config file not found: " + configFile.getAbsolutePath());
        }

        if (!configFile.equals(this.configFile)) {
            this.configFile = configFile;
            configManager = new XMLConfigManager(configFile, this);
        }

        loadConfig();
    }

    private void addProject(final ProjectInterface project) throws CruiseControlException {
        project.configureProject();
        projects.add(project);
        for (final Listener listener : listeners) {
            LOG.debug("Informing listener of added project " + project.getName());
            listener.projectAdded(project);
        }
        project.setBuildQueue(buildQueue);
        project.start();
    }

    private void removeProject(final ProjectInterface project) {
        projects.remove(project);
        for (final Listener listener : listeners) {
            LOG.debug("Informing listener of removed project " + project.getName());
            listener.projectRemoved(project);
        }
        project.stop();
    }

    public void resume() {
        buildQueue.start();
        for (final ProjectInterface currentProject : projects) {
            currentProject.setBuildQueue(buildQueue);
            currentProject.start();
        }
    }

    public void pause() {
        buildQueue.stop();
        for (final ProjectInterface currentProject : projects) {
            currentProject.stop();
        }
    }

    public void halt() {
        pause();
        System.exit(0);
    }

    public String getBuildQueueStatus() {
        if (buildQueue.isAlive()) {
            if (buildQueue.isWaiting()) {
                return "waiting";
            } else {
                return "alive";
            }
        } else {
            return "dead";
        }
    }

    public List<ProjectInterface> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    private List<ProjectInterface> getAllProjects(XMLConfigManager configManager) {
        final Set<String> projectNames = configManager.getCruiseControlConfig().getProjectNames();
        final List<ProjectInterface> allProjects = new ArrayList<ProjectInterface>(projectNames.size());
        for (final String projectName : projectNames) {
            LOG.info("projectName = [" + projectName + "]");
            final ProjectInterface projectConfig = getConfigManager().getProject(projectName);
            allProjects.add(projectConfig);
        }
        if (allProjects.size() == 0) {
            LOG.warn("no projects found in config file");
        }
        return allProjects;
    }

    protected XMLConfigManager getConfigManager() {
        return configManager;
    }

    public void addListener(final Listener listener) {
        LOG.debug("Listener added");
        listeners.add(listener);
    }

    public void reloadConfigFile() {
        LOG.debug("reload config file called");
        parseConfigFileIfNecessary();
    }

    /**
     * @return true if the config file was parsed.
     */
    public boolean parseConfigFileIfNecessary() {
        boolean reloaded = false;
        if (parsingConfigMutex.getPermissionToParse()) {
            try {
                try {
                    reloaded = configManager.reloadIfNecessary();
                } catch (CruiseControlException e) {
                    LOG.error("error parsing config file " + configFile.getAbsolutePath(), e);
                    return reloaded;
                }

                if (reloaded) {
                    LOG.debug("config file changed");
                    loadConfig();
                } else {
                    LOG.debug("config file didn't change.");
                }
            } finally {
                parsingConfigMutex.doneParsing();
            }
        }
        return reloaded;
    }

    private void loadConfig() {
        try {
            final List<ProjectInterface> projectsFromFile = getAllProjects(configManager);

            final List<ProjectInterface> removedProjects = new ArrayList<ProjectInterface>(projects);
            removedProjects.removeAll(projectsFromFile);

            final List<ProjectInterface> newProjects = new ArrayList<ProjectInterface>(projectsFromFile);
            newProjects.removeAll(projects);

            final List<ProjectInterface> retainedProjects = new ArrayList<ProjectInterface>(projects);
            retainedProjects.removeAll(removedProjects);

            //Handled removed projects
            for (final ProjectInterface removedProject : removedProjects) {
                removeProject(removedProject);
            }

            //Handle added projects
            for (final ProjectInterface newProject : newProjects) {
                addProject(newProject);
            }

            //Handle retained projects
            for (final ProjectInterface retainedProject : retainedProjects) {
                updateProject(retainedProject);
            }

        } catch (CruiseControlException e) {
            LOG.error("error parsing config file " + configFile.getAbsolutePath(), e);
        }
    }

    private void updateProject(ProjectInterface oldProject) throws CruiseControlException {
        ProjectInterface newProject = getConfigManager().getProject(oldProject.getName());
        projects.remove(oldProject);
        newProject.getStateFromOldProject(oldProject);
        projects.add(newProject);
    }

    public static interface Listener extends EventListener {
        void projectAdded(ProjectInterface project);
        void projectRemoved(ProjectInterface project);
    }

    private class BuildQueueListener implements BuildQueue.Listener {
        public void buildRequested() {
            parseConfigFileIfNecessary();
        }
    }

    public PluginDetail[] getAvailableBootstrappers() {
        return getPluginsByType(getAvailablePlugins(), PluginType.BOOTSTRAPPER);
    }

    public PluginDetail[] getAvailablePublishers() {
        return getPluginsByType(getAvailablePlugins(), PluginType.PUBLISHER);
    }

    public PluginDetail[] getAvailableSourceControls() {
        return getPluginsByType(getAvailablePlugins(), PluginType.SOURCE_CONTROL);
    }

    private static final PluginDetail[] EMPTY_PLUGIN_DETAIL = new PluginDetail[0];
    public PluginDetail[] getAvailablePlugins() {
        try {
            return getPluginRegistry().getPluginDetails();
        } catch (CruiseControlException e) {
            return EMPTY_PLUGIN_DETAIL;
        }
    }

    public PluginType[] getAvailablePluginTypes() {
        return getPluginRegistry().getPluginTypes();
    }

    public PluginRegistry getPluginRegistry() {
        return configManager.getCruiseControlConfig().getRootPlugins();
    }

    private static PluginDetail[] getPluginsByType(final PluginDetail[] details, final PluginType type) {
        final List<PluginDetail> plugins = new ArrayList<PluginDetail>();
        for (final PluginDetail detail : details) {
            if (detail.getType().equals(type)) {
                plugins.add(detail);
            }
        }

        return plugins.toArray(new PluginDetail[plugins.size()]);
    }

    private class ParsingConfigMutex {
        private final Object mutex = new Object();
        private boolean inUse;

        boolean getPermissionToParse() {
            synchronized (mutex) {
                if (inUse) {
                    LOG.debug("permission denied to parse config");
                    return false;
                }
                inUse = true;
                LOG.debug("permission granted to parse config");
                return true;
            }
        }

        void doneParsing() {
            LOG.debug("done parsing, allow next request permission to parse");
            inUse = false;
        }
    }
}
