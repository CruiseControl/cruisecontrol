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
package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ConfigurationService implements InitializingBean {

    private DashboardXmlConfigService service;

    private File configFile;

    private final BuildLoopQueryService buildLoopQueryService;

    private EnvironmentService envService;

    public ConfigurationService(EnvironmentService envService, DashboardXmlConfigService service,
            BuildLoopQueryService buildLoopQueryService) throws CruiseControlException,
            ConfigurationException {
        this.envService = envService;
        this.service = service;
        this.buildLoopQueryService = buildLoopQueryService;
    }

    public void afterPropertiesSet() throws Exception {
        configFile = service.getConfigurationFile();
    }

    public String getDashboardConfigLocation() {
        if (configFile == null) {
            return null;
        }
        try {
            return configFile.getCanonicalPath();
        } catch (IOException e) {
            return configFile.getAbsolutePath();
        }
    }

    public void setDashboardConfigLocation(String configLocation) throws CruiseControlException,
            ConfigurationException {
        if (service.isDashboardConfigFileValid(new File(configLocation))) {
            this.configFile = new File(configLocation);
        }
    }

    public boolean isForceBuildEnabled() {
        return envService.isForceBuildEnabled();
    }

    public File getLogsRoot() {
        return envService.getLogDir();
    }

    public File getArtifactsRoot() {
        return envService.getArtifactsDir();
    }

    public File getArtifactRoot(String projectName) {
        return getProjects() == null ? null : getProjects().getArtifactRoot(projectName);
    }

    public File getLogRoot(String projectName) {
        return getProjects() == null ? null : getProjects().getLogRoot(projectName);
    }

    public File[] getProjectDirectoriesFromBuildloopRepository() {
        return getProjects().getProjectsRegistedInBuildLoop();
    }

    public File[] getProjectDirectoriesFromFileSystem() {
        return getProjects().getProjectsFromFileSystem();
    }

    public Collection getActiveProjects() {
        File[] projectsInFileSystem = getProjectDirectoriesFromFileSystem();
        File[] projectsInBuildLoop = getProjectDirectoriesFromBuildloopRepository();
        return CollectionUtils.intersection(asList(projectsInFileSystem), asList(projectsInBuildLoop));
    }

    public Collection getInactiveProjects() {
        File[] projectsInFileSystem = getProjectDirectoriesFromFileSystem();
        File[] projectsInBuildLoop = getProjectDirectoriesFromBuildloopRepository();
        return CollectionUtils.subtract(asList(projectsInBuildLoop), asList(projectsInFileSystem));
    }

    public Collection getDiscontinuedProjects() {
        File[] projectsInBuildLoop = getProjectDirectoriesFromBuildloopRepository();
        File[] projectsInFileSystem = getProjectDirectoriesFromFileSystem();
        return CollectionUtils.subtract(asList(projectsInFileSystem), asList(projectsInBuildLoop));
    }

    private Collection asList(File[] ary) {
        if (ary == null) {
            return new ArrayList();
        } else {
            return Arrays.asList(ary);
        }
    }

    private Projects getProjects() {
        return buildLoopQueryService.getProjects();
    }
}
