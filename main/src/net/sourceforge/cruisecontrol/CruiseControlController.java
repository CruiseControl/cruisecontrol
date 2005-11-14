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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.sourceforge.cruisecontrol.bootstrappers.AccurevBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.AlienBrainBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.AntBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.BootstrapperDetail;
import net.sourceforge.cruisecontrol.bootstrappers.CMSynergyBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.ClearCaseBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusFTPBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.GenericBootstrapperDetail;
import net.sourceforge.cruisecontrol.bootstrappers.P4Bootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.SVNBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.SnapshotCMBootstrapper;
import net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper;
import net.sourceforge.cruisecontrol.config.XMLConfigManager;
import net.sourceforge.cruisecontrol.publishers.AntPublisher;
import net.sourceforge.cruisecontrol.publishers.CMSynergyBaselinePublisher;
import net.sourceforge.cruisecontrol.publishers.CMSynergyTaskPublisher;
import net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusFTPPublisher;
import net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher;
import net.sourceforge.cruisecontrol.publishers.EmailPublisher;
import net.sourceforge.cruisecontrol.publishers.ExecutePublisher;
import net.sourceforge.cruisecontrol.publishers.FTPPublisher;
import net.sourceforge.cruisecontrol.publishers.GenericPublisherDetail;
import net.sourceforge.cruisecontrol.publishers.HTMLEmailPublisher;
import net.sourceforge.cruisecontrol.publishers.JabberPublisher;
import net.sourceforge.cruisecontrol.publishers.OnFailurePublisher;
import net.sourceforge.cruisecontrol.publishers.OnSuccessPublisher;
import net.sourceforge.cruisecontrol.publishers.PublisherDetail;
import net.sourceforge.cruisecontrol.publishers.RSSPublisher;
import net.sourceforge.cruisecontrol.publishers.SCPPublisher;
import net.sourceforge.cruisecontrol.publishers.SocketPublisher;
import net.sourceforge.cruisecontrol.publishers.X10Publisher;
import net.sourceforge.cruisecontrol.publishers.XSLTLogPublisher;
import net.sourceforge.cruisecontrol.sourcecontrols.Accurev;
import net.sourceforge.cruisecontrol.sourcecontrols.AlienBrain;
import net.sourceforge.cruisecontrol.sourcecontrols.AlwaysBuild;
import net.sourceforge.cruisecontrol.sourcecontrols.BuildStatus;
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.sourcecontrols.CVS;
import net.sourceforge.cruisecontrol.sourcecontrols.ClearCase;
import net.sourceforge.cruisecontrol.sourcecontrols.Compound;
import net.sourceforge.cruisecontrol.sourcecontrols.FileSystem;
import net.sourceforge.cruisecontrol.sourcecontrols.ForceOnly;
import net.sourceforge.cruisecontrol.sourcecontrols.GenericSourceControlDetail;
import net.sourceforge.cruisecontrol.sourcecontrols.HttpFile;
import net.sourceforge.cruisecontrol.sourcecontrols.MKS;
import net.sourceforge.cruisecontrol.sourcecontrols.P4;
import net.sourceforge.cruisecontrol.sourcecontrols.PVCS;
import net.sourceforge.cruisecontrol.sourcecontrols.SVN;
import net.sourceforge.cruisecontrol.sourcecontrols.SnapshotCM;
import net.sourceforge.cruisecontrol.sourcecontrols.SourceControlDetail;
import net.sourceforge.cruisecontrol.sourcecontrols.Vss;
import net.sourceforge.cruisecontrol.sourcecontrols.VssJournal;

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
    private ConfigManager configManager;

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
        if (!configFile.exists()) {
            throw new CruiseControlException("Config file not found: " + configFile.getAbsolutePath());
        }

        // TODO: we could optimize here
        // only reparse if, configFile != old or file has changed.
        this.configFile = configFile;

        configManager = new XMLConfigManager(configFile);

        List projectList = parseConfigFile();
        for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
            Project project = (Project) iterator.next();
            addProject(project);
        }
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

    private List getAllProjects(ConfigManager configManager) throws CruiseControlException {
        Set projectNames = configManager.getProjectNames();
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
        project.setProjectConfig(getConfigManager().getConfig(projectName));
        project.init();
        return project;
    }

    protected ConfigManager getConfigManager() {
        return configManager;
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

    public void addListener(Listener listener) {
        LOG.debug("Listener added");
        listeners.add(listener);
    }

    public void reloadConfigFile() {
        LOG.debug("reload config file called");
        parseConfigFileIfNecessary();
    }
    
    public void parseConfigFileIfNecessary() {
        boolean reloaded;
        try {
            reloaded = configManager.reloadIfNecessary();
        } catch (CruiseControlException e) {
            LOG.error("error parsing config file " + configFile.getAbsolutePath(), e);
            return;
        }

        if (reloaded) {
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

            } catch (CruiseControlException e) {
                LOG.error("error parsing config file " + configFile.getAbsolutePath(), e);
            }
        } else {
            LOG.debug("config file didn't change.");
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

    public BootstrapperDetail[] getAvailableBootstrappers() {
        BootstrapperDetail[] bootstrappers = {
                new GenericBootstrapperDetail(AccurevBootstrapper.class),
                new GenericBootstrapperDetail(AlienBrainBootstrapper.class),
                new GenericBootstrapperDetail(AntBootstrapper.class),
                new GenericBootstrapperDetail(ClearCaseBootstrapper.class),
                new GenericBootstrapperDetail(CMSynergyBootstrapper.class),
                new GenericBootstrapperDetail(
                        CurrentBuildStatusBootstrapper.class),
                new GenericBootstrapperDetail(
                        CurrentBuildStatusFTPBootstrapper.class),
                new GenericBootstrapperDetail(CVSBootstrapper.class),
                new GenericBootstrapperDetail(P4Bootstrapper.class),
                new GenericBootstrapperDetail(SnapshotCMBootstrapper.class),
                // new GenericBootstrapperDetail(StarTeamBootstrapper.class),
                new GenericBootstrapperDetail(SVNBootstrapper.class),
                new GenericBootstrapperDetail(VssBootstrapper.class) };
        return bootstrappers;
    }

    public PublisherDetail[] getAvailablePublishers() {
        PublisherDetail[] publishers = {
                new GenericPublisherDetail(AntPublisher.class),
                new GenericPublisherDetail(CMSynergyBaselinePublisher.class),
                new GenericPublisherDetail(CMSynergyTaskPublisher.class),
                new GenericPublisherDetail(CurrentBuildStatusPublisher.class),
                new GenericPublisherDetail(CurrentBuildStatusFTPPublisher.class),
                new GenericPublisherDetail(EmailPublisher.class),
                new GenericPublisherDetail(ExecutePublisher.class),
                new GenericPublisherDetail(FTPPublisher.class),
                new GenericPublisherDetail(HTMLEmailPublisher.class),
                new GenericPublisherDetail(JabberPublisher.class),
                new GenericPublisherDetail(OnFailurePublisher.class),
                new GenericPublisherDetail(OnSuccessPublisher.class),
                new GenericPublisherDetail(RSSPublisher.class),
                // new
                // GenericPublisherDetail(SametimeAnnouncementPublisher.class),
                new GenericPublisherDetail(SCPPublisher.class),
                new GenericPublisherDetail(SocketPublisher.class),
                new GenericPublisherDetail(X10Publisher.class),
                new GenericPublisherDetail(XSLTLogPublisher.class) };
        return publishers;
    }

    public SourceControlDetail[] getAvailableSourceControls() {
        SourceControlDetail[] srcCtrls = {
                new GenericSourceControlDetail(Accurev.class),
                new GenericSourceControlDetail(AlienBrain.class),
                new GenericSourceControlDetail(AlwaysBuild.class),
                new GenericSourceControlDetail(BuildStatus.class),
                new GenericSourceControlDetail(ClearCase.class),
                new GenericSourceControlDetail(CMSynergy.class),
                new GenericSourceControlDetail(Compound.class),
                new GenericSourceControlDetail(CVS.class),
                new GenericSourceControlDetail(FileSystem.class),
                new GenericSourceControlDetail(ForceOnly.class),
                new GenericSourceControlDetail(HttpFile.class),
                new GenericSourceControlDetail(MKS.class),
                new GenericSourceControlDetail(P4.class),
                new GenericSourceControlDetail(PVCS.class),
                new GenericSourceControlDetail(SnapshotCM.class),
                // new GenericSourceControlDetail(StarTeam.class),
                new GenericSourceControlDetail(SVN.class),
                new GenericSourceControlDetail(Vss.class),
                new GenericSourceControlDetail(VssJournal.class) };
        return srcCtrls;
    }

    public PluginDetail[] getAvailablePlugins() {
        List plugins = new LinkedList();
        plugins.addAll(Arrays.asList(getAvailableBootstrappers()));
        plugins.addAll(Arrays.asList(getAvailablePublishers()));
        plugins.addAll(Arrays.asList(getAvailableSourceControls()));
        return (PluginDetail[]) plugins
                .toArray(new PluginDetail[plugins.size()]);
    }
}
