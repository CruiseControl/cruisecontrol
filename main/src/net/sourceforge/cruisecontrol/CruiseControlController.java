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
package net.sourceforge.cruisecontrol;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

import com.twmacinta.util.MD5;
import com.twmacinta.util.MD5InputStream;

/**
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlController {
    private static final Logger LOG = Logger.getLogger(CruiseControlController.class);
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.xml";
    private File configFile;
    private String configMD5;
    private List projects = new ArrayList();
    private BuildQueue buildQueue = new BuildQueue();

    private List listeners = new ArrayList();
    
    public CruiseControlController() {
        buildQueue.addListener(new BuildQueueListener());
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(File configFile) throws CruiseControlException {
        this.configFile = configFile;
        if (configFile == null) {
            throw new CruiseControlException("No config file");
        }
        if (!configFile.exists()) {
            throw new CruiseControlException("Config file not found: " + configFile.getAbsolutePath());
        }
        List projectList = parseConfigFile();
        for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
            Project project = (Project) iterator.next();
            addProject(project);
        }
    }

    private List parseConfigFile() throws CruiseControlException {
        Element configRoot = Util.loadConfigFile(configFile);
        addPluginsToRootRegistry(configRoot, configFile);
        List allProjects = getAllProjects(configRoot);
        if (allProjects.size() == 0) {
            LOG.warn("no projects found in config file");
        }
        configMD5 = calculateMD5(configFile);
        return allProjects;
    }

    private String calculateMD5(File file) {
        String hash = null;
        MD5InputStream in = null;
        try {
            byte[] buf = new byte[65536];
            in = new MD5InputStream(new BufferedInputStream(new FileInputStream(file)));
            int readResult = in.read(buf);
            while (readResult != -1) {
                readResult = in.read(buf);
            }
            hash = MD5.asHex(in.hash());
        } catch (IOException e) {
            LOG.error("exception calculating MD5 of config file " + file.getAbsolutePath(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
        return hash;
    }

    static void addPluginsToRootRegistry(Element configRoot, final File configFile) {
        PluginRegistry.resetRootRegistry();
        for (Iterator pluginIter = configRoot.getChildren("plugin").iterator(); pluginIter.hasNext(); ) {
            Element pluginElement = (Element) pluginIter.next();
            String pluginName = pluginElement.getAttributeValue("name");
            if (pluginName == null) {
                LOG.warn(configFile.getName() + " contains plugin without a name-attribute, ignoring it");
                continue;
            }
            try {
                PluginRegistry.registerToRoot(pluginElement);
            } catch (CruiseControlException e) {
                LOG.warn("Can't register plugin '" + pluginName + "':\n" + e.getMessage());
            }
        }
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

    private List getAllProjects(Element configRoot) throws CruiseControlException {
        String[] projectNames = getProjectNames(configRoot);
        ArrayList allProjects = new ArrayList(projectNames.length);
        for (int i = 0; i < projectNames.length; i++) {
            String projectName = projectNames[i];
            LOG.info("projectName = [" + projectName + "]");
            Project project = configureProject(projectName);
            allProjects.add(project);
        }
        return allProjects;
    }

    protected Project configureProject(String projectName) throws CruiseControlException {
        Project project = readProject(projectName);
        project.setName(projectName);
        project.setConfigFile(configFile);
        project.init();
        return project;
    }

    /**
     * Reads project configuration from a previously serialized Project.  The
     * name of the serialized project file is equivalent to the name of the
     * project.
     *
     * @param projectName name of the serialized project file
     * @return Deserialized Project or a new Project if there are any problems
     * reading the serialized Project; should never return null
     */
    Project readProject(String projectName) {
        //look for fileName.ser first
        File serializedProjectFile = new File(projectName + ".ser");
        LOG.debug("Reading serialized project from: " + serializedProjectFile.getAbsolutePath());

        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()) {
            //filename.ser doesn't exist, try finding fileName
            serializedProjectFile = new File(projectName);
            LOG.debug(projectName + ".ser not found, looking for serialized project file: " + projectName);
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
        }

        try {
            ObjectInputStream s = new ObjectInputStream(new FileInputStream(serializedProjectFile));
            Project project = (Project) s.readObject();
            return project;
        } catch (Exception e) {
            LOG.warn("Error deserializing project file from " + serializedProjectFile.getAbsolutePath(), e);
            return new Project();
        }
    }

    private String[] getProjectNames(Element rootElement) throws CruiseControlException {
        ArrayList projectNames = new ArrayList();
        Iterator projectIterator = rootElement.getChildren("project").iterator();
        while (projectIterator.hasNext()) {
            Element projectElement = (Element) projectIterator.next();
            String projectName = projectElement.getAttributeValue("name");
            if (projectName == null) {
                // TODO: will be ignored?
                LOG.warn("configuration file contains project element with no name");
            } else {
                if (projectNames.contains(projectName)) {
                    final String message = "Duplicate entries in config file for project name " + projectName;
                    throw new CruiseControlException(message);
                }
                projectNames.add(projectName);
            }
        }
        return (String[]) projectNames.toArray(new String[]{});
    }

    public void addListener(Listener listener) {
        LOG.debug("Listener added");
        listeners.add(listener);
    }

    public void reloadConfigFile() {
        LOG.debug("reload config file called");
        configMD5 = null;
        parseConfigFileIfNecessary();
    }
    
    public void parseConfigFileIfNecessary() {
        String newMD5 = calculateMD5(configFile);
        boolean fileChanged = !newMD5.equals(configMD5);
        if (fileChanged) {
            LOG.debug("config file changed");
            try {
                List projectsFromFile = parseConfigFile();
                List removedProjects = new ArrayList(projects);
                removedProjects.removeAll(projectsFromFile);
                projectsFromFile.removeAll(projects);

                Iterator removed = removedProjects.iterator();
                while (removed.hasNext()) {
                    Project project = (Project) removed.next();
                    removeProject(project);
                }

                Iterator added = projectsFromFile.iterator();
                while (added.hasNext()) {
                    Project project = (Project) added.next();
                    addProject(project);
                }
                
                configMD5 = newMD5;
            } catch (CruiseControlException e) {
                LOG.error("error parsing config file " + configFile.getAbsolutePath(), e);
            }
        } else {
            LOG.debug("config file didn't change. MD5 of " + newMD5);
        }
    }

    public static interface Listener extends EventListener {
        void projectAdded(Project project);
        void projectRemoved(Project project);
    }

    private class BuildQueueListener implements BuildQueue.Listener {
        public void projectQueued() {
            parseConfigFileIfNecessary();
        }
    }
}
