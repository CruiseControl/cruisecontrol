/*******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol.jmx;

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

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(ProjectController.class);

    private Project _project;

    public ProjectController(Project project) {
        _project = project;
    }

    public void pause() {
        _project.setPaused(true);
    }

    public void resume() {
        _project.setPaused(false);
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
        return _project.isPaused();
    }

    public void setConfigFileName(String fileName) {
        _project.setConfigFileName(fileName);
    }

    public String getConfigFileName() {
        return _project.getConfigFileName();
    }

    public void setLabel(String label) {
        _project.setLabel(label);
    }

    public String getLabel() {
        return _project.getLabel();
    }

    public void setLabelIncrementer(String classname) {
        LabelIncrementer incrementer = null;
        try {
            incrementer =
                    (LabelIncrementer) Class.forName(classname).newInstance();
        } catch (Exception e) {
            log.error("Error instantiating label incrementer."
                    + "  Using DefaultLabelIncrementer.", e);
            incrementer = new DefaultLabelIncrementer();
        }

        _project.setLabelIncrementer(incrementer);
    }

    public String getLabelIncrementer() {
        return _project.getLabelIncrementer().getClass().getName();
    }

    public void setLastBuild(String date) throws CruiseControlException {
        _project.setLastBuild(date);
    }

    public String getLastBuild() {
        return _project.getLastBuild();
    }

    public void setLogDir(String logdir) {
        _project.setLogDir(logdir);
    }

    public String getLogDir() {
        return _project.getLogDir();
    }

    public void setProjectName(String name) {
        _project.setName(name);
    }

    public String getProjectName() {
        return _project.getName();
    }

    public void setBuildInterval(long buildInterval) {
        _project.setSleepMillis(buildInterval);
    }

    public long getBuildInterval() {
        return _project.getSleepMilliseconds();
    }

}
