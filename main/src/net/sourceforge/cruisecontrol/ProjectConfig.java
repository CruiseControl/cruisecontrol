/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServer;

import net.sourceforge.cruisecontrol.config.DefaultPropertiesPlugin;
import net.sourceforge.cruisecontrol.config.PluginPlugin;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * A plugin that represents the project node
 * 
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class ProjectConfig implements ProjectInterface {
    private static final long serialVersionUID = -893779421250033198L;

    private static final Logger LOG = Logger.getLogger(ProjectConfig.class);

    private String name;
    private boolean buildAfterFailed = true;
    private boolean forceOnly = false;
    private boolean requiremodification = true;
    private boolean forceBuildNewProject = true; // default to current behavior

    private transient Bootstrappers bootstrappers;
    private transient LabelIncrementer labelIncrementer;
    private transient Listeners listeners;
    private transient Log log;
    private transient ModificationSet modificationSet;
    private transient Publishers publishers;
    private transient Schedule schedule;

    private Project project;

    /**
     * Called after the configuration is read to make sure that all the mandatory parameters were specified..
     * 
     * @throws CruiseControlException
     *             if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(schedule != null, "project requires a schedule");
        
        if (labelIncrementer == null) {
            labelIncrementer = new DefaultLabelIncrementer();
        }

        if (bootstrappers != null) {
            bootstrappers.validate();
        }

        if (listeners != null) {
            listeners.validate();
        }

        if (log == null) {
            log = new Log();
        }
        log.setProjectName(name);
        log.validate();

        if (modificationSet != null) {
            modificationSet.validate();
        }

        if (schedule != null) {
            schedule.validate();
        }

        if (publishers != null) {
            publishers.validate();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBuildAfterFailed(boolean buildAfterFailed) {
        this.buildAfterFailed = buildAfterFailed;
    }

    public void setForceBuildNewProject(boolean forceBuildNewProject) {
        this.forceBuildNewProject = forceBuildNewProject;
    }

    /**
     * Defines a name/value pair used in configuration.
     * @param plugin plugin
     * @deprecated exists only for gendoc, should not be called.
     */
    public void add(DefaultPropertiesPlugin plugin) {
        // FIXME currently only declared for documentation generation purposes
        throw new IllegalStateException("GenDoc-only method should not be invoked.");
    }

    /**
     * Registers a classname with an alias.
     * @param plugin plugin
     * @deprecated exists only for gendoc, should not be called.
     */
    public void add(PluginPlugin plugin) {
        // currently only declared for documentation generation purposes
        throw new IllegalStateException("GenDoc-only method should not be invoked.");
    }

    public void add(LabelIncrementer labelIncrementer) {
        if (this.labelIncrementer != null) {
            LOG.warn("replacing existing label incrememnter [" + this.labelIncrementer.toString()
                    + "] with new one [" + labelIncrementer.toString() + "]");
        }
        this.labelIncrementer = labelIncrementer;
    }

    public void add(Listeners listeners) {
        this.listeners = listeners;
    }

    public void add(ModificationSet modificationSet) {
        this.modificationSet = modificationSet;
    }

    public void add(Bootstrappers bootstrappers) {
        this.bootstrappers = bootstrappers;
    }

    public void add(Publishers publishers) {
        this.publishers = publishers;
    }

    public void add(Schedule schedule) {
        this.schedule = schedule;
    }

    public void add(Log log) {
        this.log = log;
    }

    public boolean shouldBuildAfterFailed() {
        return buildAfterFailed;
    }

    public Log getLog() {
        return log;
    }

    public List<Bootstrapper> getBootstrappers() {
        return bootstrappers == null ? Collections.<Bootstrapper>emptyList() : bootstrappers.getBootstrappers();
    }

    public List<Listener> getListeners() {
        return listeners == null ? Collections.<Listener>emptyList() : listeners.getListeners();
    }

    public List<Publisher> getPublishers() {
        return publishers == null ? Collections.<Publisher>emptyList() : publishers.getPublishers();
    }

    public ModificationSet getModificationSet() {
        return modificationSet;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public LabelIncrementer getLabelIncrementer() {
        return labelIncrementer;
    }

    public String getName() {
        return name;
    }

    public static class Bootstrappers implements Serializable {
        private static final long serialVersionUID = 7428779281399848035L;
        private final List<Bootstrapper> bootstrappers = new ArrayList<Bootstrapper>();

        public void add(final Bootstrapper bootstrapper) {
            bootstrappers.add(bootstrapper);
        }

        public List<Bootstrapper> getBootstrappers() {
            return bootstrappers;
        }

        public void validate() throws CruiseControlException {
            for (final Bootstrapper nextBootstrapper : bootstrappers) {
                nextBootstrapper.validate();
            }
        }
    }

    public static class Listeners implements Serializable {
        private static final long serialVersionUID = -3816080104514876038L;
        private final List<Listener> listeners = new ArrayList<Listener>();

        public void add(Listener listener) {
            listeners.add(listener);
        }

        public List<Listener> getListeners() {
            return listeners;
        }

        public void validate() throws CruiseControlException {
            for (final Listener nextListener : listeners) {
                nextListener.validate();
            }
        }
    }

    public static class Publishers implements Serializable {
        private static final long serialVersionUID = -410933401108345152L;
        private final List<Publisher> publishers = new ArrayList<Publisher>();

        public void add(Publisher publisher) {
            publishers.add(publisher);
        }

        public List<Publisher> getPublishers() {
            return publishers;
        }

        public void validate() throws CruiseControlException {
            for (final Publisher nextPublisher : publishers) {
                nextPublisher.validate();
            }

        }
    }

    /**
     * @param forceOnly
     *            the forceOnly to set
     */
    public void setForceOnly(boolean forceOnly) {
        this.forceOnly = forceOnly;
    }

    /**
     * @return the forceOnly
     */
    public boolean isForceOnly() {
        return forceOnly;
    }

    /**
     * @return the requiremodification
     */
    public boolean isRequiremodification() {
        return requiremodification;
    }

    /**
     * @param requiremodification
     *            the requiremodification to set
     */
    public void setRequiremodification(boolean requiremodification) {
        this.requiremodification = requiremodification;
    }

    public void configureProject() throws CruiseControlException {
        Project myProject = readProject(name);
        myProject.setName(name);
        myProject.setProjectConfig(this);
        myProject.init();
        this.project = myProject;
    }

    /**
     * Reads project configuration from a previously serialized Project or creates a new instance. The name of the
     * serialized project file is derived from the name of the project.
     * 
     * @param projectName
     *            name of the serialized project
     * @return Deserialized Project or a new Project if there are any problems reading the serialized Project; should
     *         never return null
     */
    Project readProject(final String projectName) {
        File serializedProjectFile = new File(projectName + ".ser");
        LOG.debug("Reading serialized project from: " + serializedProjectFile.getAbsolutePath());

        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()) {
            serializedProjectFile = ProjectConfig.tryOldSerializedFileName(projectName);
        }

        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()
                || serializedProjectFile.isDirectory()) {
            final Project newProject = new Project();
            newProject.setName(projectName);
            if (forceBuildNewProject) {
                LOG.warn("No previously serialized project found [" + serializedProjectFile.getAbsolutePath()
                        + ".ser], forcing a build.");
                newProject.setBuildForced(true);
            } else {
                LOG.warn("No previously serialized project found  [" + serializedProjectFile.getAbsolutePath()
                         + ".ser], Not forcing build, forceBuildNewProject = false.");
            }
            return newProject;
        }

        try {
            final ObjectInputStream s = new ObjectInputStream(new FileInputStream(serializedProjectFile));
            try {
                return (Project) s.readObject();
            } finally {
                s.close();
            }
        } catch (Exception e) {
            LOG.warn("Error deserializing project file from " + serializedProjectFile.getAbsolutePath(), e);
            return new Project();
        }
    }

    private static File tryOldSerializedFileName(String projectName) {
        File serializedProjectFile;
        serializedProjectFile = new File(projectName);
        LOG.debug(projectName + ".ser not found, looking for serialized project file "
                + serializedProjectFile.getAbsolutePath());
        return serializedProjectFile;
    }

    public boolean equals(Object arg0) {
        if (arg0 == null) {
            return false;
        }

        if (arg0.getClass().getName().equals(getClass().getName())) {
            ProjectConfig thatProject = (ProjectConfig) arg0;
            return thatProject.name.equals(name);
        }

        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Need to delegate to "project" toString() to avoid breaking external jmx scripts.
     * {@inheritDoc}
     */
    public String toString() {
        if (project != null) {
            return project.toString();
        }
        return super.toString();
    }

    public void getStateFromOldProject(ProjectInterface oldProject) throws CruiseControlException {
        ProjectConfig oldProjectConfig = (ProjectConfig) oldProject;
        project = oldProjectConfig.project;
        project.setProjectConfig(this);
        project.init();
    }

    public void execute() {
        project.execute();
    }

    public void register(MBeanServer server) throws JMException {
        project.register(server);
    }

    public void setBuildQueue(BuildQueue buildQueue) {
        project.setBuildQueue(buildQueue);
    }

    public void start() {
        project.start();
    }

    public void stop() {
        project.stop();
    }

    // TODO remove this. only here till tests are fixed up.
    Project getProject() {
        return project;
    }

    public String getStatus() {
        return project.getStatus();
    }

    public String getBuildStartTime() {
        return project.getBuildStartTime();
    }

    public boolean isPaused() {
        return project.isPaused();
    }

    public List<Modification> getModifications() {
        if (getModificationSet() != null) {
            return getModificationSet().getCurrentModifications();
        } else {
            return Collections.emptyList();
        }
    }

    public boolean isInState(ProjectState state) {
        return project.getState().equals(state);
    }

    public List<String> getLogLabels() {
        return log.getLogLabels();
    }

    public String[] getLogLabelLines(final String logLabel, final int firstLine) {
        return log.getLogLabelLines(logLabel, firstLine);
    }
}
