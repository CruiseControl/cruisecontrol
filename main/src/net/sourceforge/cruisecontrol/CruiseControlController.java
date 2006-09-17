/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
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
    private List projects = new ArrayList();
    private BuildQueue buildQueue = new BuildQueue();
    private Properties versionProperties;

    private List listeners = new ArrayList();
    private XMLConfigManager configManager;
    
    private ParsingConfigMutex parsingConfigMutex = new ParsingConfigMutex();

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

    public void setConfigFile(File configFile) throws CruiseControlException {
        if (configFile == null) {
            throw new CruiseControlException("No config file");
        }
        if (!configFile.isFile()) {
            throw new CruiseControlException("Config file not found: " + configFile.getAbsolutePath());
        }
        
        if (!configFile.equals(this.configFile)) {
            this.configFile = configFile;        
            configManager = new XMLConfigManager(configFile);
        }

        loadConfigFromConfigManager();
    }

    private List parseConfigFile() throws CruiseControlException {
        List allProjects = getAllProjects(configManager);
        if (allProjects.size() == 0) {
            LOG.warn("no projects found in config file");
        }
        return allProjects;
    }

    private void addProject(Project project) {
        projects.add(project);
        for (Iterator listenIter = listeners.iterator(); listenIter.hasNext();) {
            LOG.debug("Informing listener of added project " + project.getName());
            Listener listener = (Listener) listenIter.next();
            listener.projectAdded(project);
        }
        project.setBuildQueue(buildQueue);
        project.start();
    }

    private void removeProject(Project project) {
        projects.remove(project);
        for (Iterator listenIter = listeners.iterator(); listenIter.hasNext();) {
            LOG.debug("Informing listener of removed project " + project.getName());
            Listener listener = (Listener) listenIter.next();
            listener.projectRemoved(project);
        }
        project.stop();
    }

    public void resume() {
        buildQueue.start();
        for (Iterator iterator = projects.iterator(); iterator.hasNext();) {
            Project currentProject = (Project) iterator.next();
            currentProject.setBuildQueue(buildQueue);
            currentProject.start();
        }
    }

    public void pause() {
        buildQueue.stop();
        for (Iterator iterator = projects.iterator(); iterator.hasNext();) {
            Project currentProject = (Project) iterator.next();
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

    public List getProjects() {
        return Collections.unmodifiableList(projects);
    }

    private List getAllProjects(XMLConfigManager configManager) throws CruiseControlException {
        Set projectNames = configManager.getCruiseControlConfig().getProjectNames();
        List allProjects = new ArrayList(projectNames.size());
        for (Iterator it = projectNames.iterator(); it.hasNext();) {
            String projectName = (String) it.next();
            LOG.info("projectName = [" + projectName + "]");
            Project project = configureProject(projectName);
            allProjects.add(project);
        }
        return allProjects;
    }

    protected Project configureProject(String projectName) throws CruiseControlException {
        Project project = readProject(projectName);
        project.setName(projectName);
        project.setProjectConfig(getConfigManager().getProjectConfig(projectName));
        project.init();
        return project;
    }

    protected XMLConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Reads project configuration from a previously serialized Project or creates a new
     * instance.  The name of the serialized project file is derived from the name of
     * the project.
     *
     * @param projectName name of the serialized project
     * @return Deserialized Project or a new Project if there are any problems
     * reading the serialized Project; should never return null
     */
    Project readProject(String projectName) {
        File serializedProjectFile = new File(projectName + ".ser");
        LOG.debug("Reading serialized project from: " + serializedProjectFile.getAbsolutePath());

        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()) {
            serializedProjectFile = tryOldSerializedFileName(projectName);
        }
        
        if (!serializedProjectFile.exists()
                || !serializedProjectFile.canRead()
                || serializedProjectFile.isDirectory()) {
            Project temp = new Project();
            temp.setName(projectName);
            if (!projects.contains(temp)) {
                LOG.warn("No previously serialized project found ["
                        + serializedProjectFile.getAbsolutePath()
                        + ".ser], forcing a build.");
            }
            Project newProject = new Project();
            newProject.setBuildForced(true);
            return newProject;
        }

        try {
            ObjectInputStream s = new ObjectInputStream(new FileInputStream(serializedProjectFile));
            return (Project) s.readObject();
        } catch (Exception e) {
            LOG.warn("Error deserializing project file from " + serializedProjectFile.getAbsolutePath(), e);
            return new Project();
        }
    }

    private File tryOldSerializedFileName(String projectName) {
        File serializedProjectFile;
        serializedProjectFile = new File(projectName);
        LOG.debug(projectName + ".ser not found, looking for serialized project file "
                + serializedProjectFile.getAbsolutePath());
        return serializedProjectFile;
    }

    public void addListener(Listener listener) {
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
                    loadConfigFromConfigManager();
                } else {
                    LOG.debug("config file didn't change.");
                }
            } finally {
                parsingConfigMutex.doneParsing();
            }
        }
        return reloaded;
    }

    private void loadConfigFromConfigManager() {
        try {
            List projectsFromFile = parseConfigFile();

            List removedProjects = new ArrayList(projects);
            removedProjects.removeAll(projectsFromFile);

            List newProjects = new ArrayList(projectsFromFile);
            newProjects.removeAll(projects);

            List retainedProjects = new ArrayList(projectsFromFile);
            retainedProjects.removeAll(newProjects);

            //Handled removed projects
            Iterator removed = removedProjects.iterator();
            while (removed.hasNext()) {
                removeProject((Project) removed.next());
            }

            //Handle added projects
            Iterator added = newProjects.iterator();
            while (added.hasNext()) {
                addProject((Project) added.next());
            }

            //Handle retained projects
            Iterator retained = retainedProjects.iterator();
            while (retained.hasNext()) {
                updateProject((Project) retained.next());
            }

        } catch (CruiseControlException e) {
            LOG.error("error parsing config file " + configFile.getAbsolutePath(), e);
        }
    }

    private void updateProject(Project project) throws CruiseControlException {
        Project matchingProject = (Project) projects.get(projects.indexOf(project));
        matchingProject.setProjectConfig(getConfigManager().getProjectConfig(matchingProject.getName()));
        matchingProject.init();
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

    public PluginDetail[] getAvailablePlugins() {
        try {
            return getPluginRegistry().getPluginDetails();
        } catch (CruiseControlException e) {
            return new PluginDetail[0];
        }
    }

    public PluginType[] getAvailablePluginTypes() {
        return getPluginRegistry().getPluginTypes();
    }

    public PluginRegistry getPluginRegistry() {
        return ((XMLConfigManager) configManager).getCruiseControlConfig().getRootPlugins();
    }

    private static PluginDetail[] getPluginsByType(PluginDetail[] details, PluginType type) {
        List plugins = new ArrayList();
        for (int i = 0; i < details.length; i++) {
            if (details[i].getType().equals(type)) {
                plugins.add(details[i]);
            }
        }

        return (PluginDetail[]) plugins.toArray(new PluginDetail[plugins.size()]);
    }
    
    private class ParsingConfigMutex {
        private Object mutex = new Object();
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
