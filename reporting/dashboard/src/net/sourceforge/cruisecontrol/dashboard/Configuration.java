/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.dashboard.exception.ProjectAlreadyExistException;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.VCS;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.CCProjectFolderFilter;

import org.apache.commons.lang.StringUtils;

public class Configuration {

    private ConfigXmlFileService service;

    private File configFile;

    private String logFileLocation;

    private Projects projects;

    public Configuration(ConfigXmlFileService service) {
        this.service = service;
        configFile = service.getConfigXmlFile(null);
        projects = service.getProjects(configFile);
    }

    public String getCruiseConfigLocation() {
        return configFile == null ? null : configFile.getAbsolutePath();
    }

    public String getCruiseConfigDirLocation() {
        return service.getConfigXmlFile(configFile).getAbsoluteFile().getParent();
    }

    public String getCruiseLogfileLocation() {
        if (StringUtils.isEmpty(logFileLocation)) {
            return service.getConfigXmlFile(configFile).getAbsoluteFile().getParent()
                    + File.separator + "logs";
        } else {
            return logFileLocation;
        }
    }

    public File getArtifactRoot(String projectName) {
        return projects == null ? null : projects.getArtifactRoot(projectName);
    }

    public File getLogRoot(String projectName) {
        return projects == null ? null : projects.getLogRoot(projectName);
    }

    public boolean hasProject(String projectName) {
        return projects != null && projects.hasProject(projectName);
    }

    public void updateConfigFile(String content) {
        try {
            service.writeContentToConfigXml(configFile.getAbsolutePath(), content);
            projects = service.getProjects(configFile);
        } catch (IOException e) {
            throw new RuntimeException("failed to update config xml", e);
        }
    }

    public void setCruiseConfigLocation(String cruiseConfigLocation) {
        if (service.isConfigFileValid(new File(cruiseConfigLocation))) {
            this.configFile = new File(cruiseConfigLocation);
            projects = service.getProjects(configFile);
        }
    }

    public void setCruiseLogfileLocation(String newCruiseLogLocation) {
        logFileLocation = newCruiseLogLocation;
    }

    public void addProject(String projectName, VCS vcs) throws ProjectAlreadyExistException {
        if (hasProject(projectName)) {
            throw new ProjectAlreadyExistException("Project " + projectName + "already exists.");
        }
        service.addProject(this.configFile, projectName, vcs);
        projects = service.getProjects(configFile);
    }

    public File[] getProjectDirectoriesFromConfigFile() {
        return projects.getProjectNames();
    }

    public File[] getProjectDirectoriesFromFileSystem() {
        File root = new File(getCruiseLogfileLocation());
        return root.listFiles(new CCProjectFolderFilter());
    }
}
