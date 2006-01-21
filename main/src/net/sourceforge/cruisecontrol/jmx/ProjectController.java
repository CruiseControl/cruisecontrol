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

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.LabelIncrementer;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.events.BuildProgressEvent;
import net.sourceforge.cruisecontrol.events.BuildProgressListener;
import net.sourceforge.cruisecontrol.events.BuildResultEvent;
import net.sourceforge.cruisecontrol.events.BuildResultListener;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

import org.apache.log4j.Logger;

/**
 * @author Niclas Olofsson
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class ProjectController extends NotificationBroadcasterSupport
                               implements ProjectControllerMBean, BuildProgressListener, BuildResultListener {

    private static final Logger LOG = Logger.getLogger(ProjectController.class);

    private Project project;
    private static int sequence = 0;
    private static final Object SEQUENCE_LOCK = new Object();

    public ProjectController(Project project) {
        this.project = project;
        project.addBuildProgressListener(this);
        project.addBuildResultListener(this);
    }

    private int nextSequence() {
        synchronized (SEQUENCE_LOCK) {
            return ++sequence;
        }
    }

    public void handleBuildProgress(BuildProgressEvent event) {
        log("build progress event: " + event.getState().getDescription());
        if (checkSourceProject(event.getProject())) {
            Notification notification = new Notification("cruisecontrol.progress.event", this, nextSequence());
            notification.setUserData(event.getState().getName());
            sendNotification(notification);
        }
    }

    public void handleBuildResult(BuildResultEvent event) {
        log("build result event: build " + String.valueOf(event.isBuildSuccessful() ? "successful" : "failed"));
        if (checkSourceProject(event.getProject())) {
            Notification notification = new Notification("cruisecontrol.result.event", this, nextSequence());
            notification.setUserData((event.isBuildSuccessful()) ? Boolean.TRUE : Boolean.FALSE);
            sendNotification(notification);
        }
    }

    private boolean checkSourceProject(Project sourceProject) {
        boolean projectsMatch = false;
        if (project == sourceProject) {
            projectsMatch = true;
        } else {
            if (sourceProject == null) {
                LOG.warn("source project was null");
            } else {
                LOG.warn("source project " + sourceProject.getName()
                        + " didn't match internal project " + project.getName());
            }
        }
        return projectsMatch;
    }

    public void pause() {
        log("pausing");
        project.setPaused(true);
    }

    public void resume() {
        log("resuming");
        project.setPaused(false);
    }

    public void build() {
        log("forcing build");
        project.setBuildForced(true);
    }

    public void buildWithTarget(String buildTarget) {
        log("forcing build with target \"" + buildTarget + "\"");
        project.forceBuildWithTarget(buildTarget);
    }
    
    public void serialize() {
        log("serializing");
        project.serializeProject();
    }

    public boolean isPaused() {
        return project.isPaused();
    }

    public void setLabel(String label) {
        log("setting label to [" + label + "]");
        project.setLabel(label);
    }

    public String getLabel() {
        return project.getLabel();
    }

    public void setLabelIncrementer(String classname) throws CruiseControlException {
        log("setting label incrementer to [" + classname + "]");
        LabelIncrementer incrementer;
        try {
            incrementer =
                (LabelIncrementer) Class.forName(classname).newInstance();
        } catch (Exception e) {
            LOG.error(
                "Error instantiating label incrementer."
                    + "  Using DefaultLabelIncrementer.",
                e);
            incrementer = new DefaultLabelIncrementer();
        }

        project.setLabelIncrementer(incrementer);
    }

    public String getLabelIncrementer() {
        return project.getLabelIncrementer().getClass().getName();
    }

    public void setLastBuild(String date) throws CruiseControlException {
        log("setting last build to [" + date + "]");
        project.setLastBuild(date);
    }

    public String getLastBuild() {
        return project.getLastBuild();
    }

    public void setLastSuccessfulBuild(String date)
        throws CruiseControlException {
        log("setting last successful build to [" + date + "]");
        project.setLastSuccessfulBuild(date);
    }

    public String getLastSuccessfulBuild() {
        return project.getLastSuccessfulBuild();
    }

    public String getBuildStartTime() {
        String buildStartTime = project.getBuildStartTime();
        return buildStartTime == null ? "" : buildStartTime;
    }

    public void setLogDir(String logdir) throws CruiseControlException {
        log("setting log dir to [" + logdir + "]");
        project.getLog().setDir(logdir);
    }

    public String getLogDir() {
        return project.getLogDir();
    }

    public void setProjectName(String name) {
        log("setting project name to [" + name + "]");
        project.setName(name);
    }

    public String getProjectName() {
        return project.getName();
    }

    public void setBuildInterval(long buildInterval) {
        log("setting build interval to [" + buildInterval + "]");
        project.overrideBuildInterval(buildInterval);
    }

    public long getBuildInterval() {
        return project.getBuildInterval();
    }
    
    public String getStatus() {
        return project.getStatusWithQueuePosition();
    }

    private void log(String message) {
        LOG.info(project.getName() + " Controller: " + message);
    }

    public void register(MBeanServer server) throws JMException {
        ObjectName projectName = new ObjectName("CruiseControl Project:name=" + project.getName());

        // Need to attempt to unregister the old mbean with the same name since
        // CruiseControlControllerJMXAdaptor keeps calling every time a change
        // is made to the config.xml file via JMX.
        try {
            server.unregisterMBean(projectName);
        } catch (InstanceNotFoundException noProblem) {
        } catch (MBeanRegistrationException noProblem) {
        }

        server.registerMBean(this, projectName);
    }

}
