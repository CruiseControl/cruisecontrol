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

import java.io.File;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.JMException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.LabelIncrementer;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import org.apache.log4j.Logger;

/**
 * @author Niclas Olofsson
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class ProjectController implements ProjectControllerMBean {

    private static final Logger LOG = Logger.getLogger(ProjectController.class);

    private Project project;
    private MBeanServer server;

    public ProjectController(Project project) {
        this.project = project;
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

    public void serialize() {
        log("serializing");
        project.serializeProject();
    }

//    // TODO
//    public long getUpTime() {
//        return 0;
//    }
//
//    // TODO
//    public long getSuccessfulBuildCount() {
//        return 0;
//    }

    public boolean isPaused() {
        return project.isPaused();
    }

    public void setConfigFileName(String fileName) {
        log("setting config file to [" + fileName + "]");
        project.setConfigFile(new File(fileName));
    }

    public String getConfigFileName() {
        return project.getConfigFile().getAbsolutePath();
    }

    public void setLabel(String label) {
        log("setting label to [" + label + "]");
        project.setLabel(label);
    }

    public String getLabel() {
        return project.getLabel();
    }

    public void setLabelIncrementer(String classname) {
        log("setting label incrementer to [" + classname + "]");
        LabelIncrementer incrementer = null;
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

    public void setLogDir(String logdir) {
        log("setting log dir to [" + logdir + "]");
        project.setLogDir(logdir);
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
        project.setSleepMillis(buildInterval);
    }

    public long getBuildInterval() {
        return project.getSleepMilliseconds();
    }
    
    public String getStatus() {
        return project.getStatus();
    }

    private void log(String message) {
        LOG.info(project.getName() + " Controller: " + message);
    }

    public void register(MBeanServer server) throws JMException {
        this.server = server;
        ObjectName projectName = new ObjectName("CruiseControl Project:name=" + project.getName());
        server.registerMBean(this, projectName);
    }

}
