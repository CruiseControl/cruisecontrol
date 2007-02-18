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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServer;

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
    private CCDateFormat dateFormat;

    private Bootstrappers bootstrappers;
    private LabelIncrementer labelIncrementer;
    private Listeners listeners;
    private Log log;
    private ModificationSet modificationSet;
    private Publishers publishers;
    private Schedule schedule;

    private Project project;

    /**
     * Called after the configuration is read to make sure that all the mandatory parameters were specified..
     * 
     * @throws CruiseControlException
     *             if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (dateFormat != null) {
            dateFormat.validate();
        }
        ValidationHelper.assertTrue(schedule != null, "project requires a schedule");

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

    public void add(CCDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void add(ModificationSet modificationSet) {
        this.modificationSet = modificationSet;
    }

    public void add(Bootstrappers bootstrappers) {
        this.bootstrappers = bootstrappers;
    }

    public void add(Listeners listeners) {
        this.listeners = listeners;
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

    public void add(LabelIncrementer labelIncrementer) {
        this.labelIncrementer = labelIncrementer;
    }

    public CCDateFormat getDateFormat() {
        return dateFormat;
    }

    public boolean shouldBuildAfterFailed() {
        return buildAfterFailed;
    }

    public Log getLog() {
        return log;
    }

    public List getBootstrappers() {
        return bootstrappers == null ? Collections.EMPTY_LIST : bootstrappers.getBootstrappers();
    }

    public List getListeners() {
        return listeners == null ? Collections.EMPTY_LIST : listeners.getListeners();
    }

    public List getPublishers() {
        return publishers == null ? Collections.EMPTY_LIST : publishers.getPublishers();
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

    public void writeLogFile(Date now) throws CruiseControlException {
        log.writeLogFile(now);
    }

    public boolean wasBuildSuccessful() {
        return log.wasBuildSuccessful();
    }

    public static class Bootstrappers implements Serializable {
        private static final long serialVersionUID = 7428779281399848035L;
        private List bootstrappers = new ArrayList();

        public void add(Bootstrapper bootstrapper) {
            bootstrappers.add(bootstrapper);
        }

        public List getBootstrappers() {
            return bootstrappers;
        }

        public void validate() throws CruiseControlException {
            for (Iterator iterator = bootstrappers.iterator(); iterator.hasNext();) {
                Bootstrapper nextBootstrapper = (Bootstrapper) iterator.next();
                nextBootstrapper.validate();
            }
        }
    }

    public static class Listeners implements Serializable {
        private static final long serialVersionUID = -3816080104514876038L;
        private List listeners = new ArrayList();

        public void add(Listener listener) {
            listeners.add(listener);
        }

        public List getListeners() {
            return listeners;
        }

        public void validate() throws CruiseControlException {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                Listener nextListener = (Listener) iterator.next();
                nextListener.validate();
            }
        }
    }

    public static class Publishers implements Serializable {
        private static final long serialVersionUID = -410933401108345152L;
        private List publishers = new ArrayList();

        public void add(Publisher publisher) {
            publishers.add(publisher);
        }

        public List getPublishers() {
            return publishers;
        }

        public void validate() throws CruiseControlException {
            for (Iterator iterator = publishers.iterator(); iterator.hasNext();) {
                Publisher nextPublisher = (Publisher) iterator.next();
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
    Project readProject(String projectName) {
        File serializedProjectFile = new File(projectName + ".ser");
        LOG.debug("Reading serialized project from: " + serializedProjectFile.getAbsolutePath());

        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()) {
            serializedProjectFile = ProjectConfig.tryOldSerializedFileName(projectName);
        }

        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()
                || serializedProjectFile.isDirectory()) {
            Project temp = new Project();
            temp.setName(projectName);
            LOG.warn("No previously serialized project found [" + serializedProjectFile.getAbsolutePath()
                    + ".ser], forcing a build.");
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
        project.start();
    }

    // TODO remove this. only here till tests are fixed up.
    Project getProject() {
        return project;
    }
}
