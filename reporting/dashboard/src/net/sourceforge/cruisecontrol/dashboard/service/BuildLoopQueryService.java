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

import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.repository.ClosableProjectMBeanConnection;
import org.apache.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildLoopQueryService {

    private static final Logger LOGGER = Logger.getLogger(BuildLoopQueryService.class);

    public static final String JMXATTR_BUILD_STATUS = "Status";

    public static final String JMXCOMMAND_BUILD = "build";

    public static final String JMXCOMMAND_COMMIT_MESSAGE = "CommitMessages";

    public static final String JMXCOMMAND_BUILD_OUTPUT = "getBuildOutput";

    public static final String JMXCOMMAND_ALL_PROJECT_STATUS = "AllProjectsStatus";

    private static final String[] SUPPORTED_STATUS_ARRAY =
            new String[] {CurrentStatus.BUILDING.getCruiseStatus(),
                    CurrentStatus.WAITING.getCruiseStatus(),
                    CurrentStatus.PAUSED.getCruiseStatus(), CurrentStatus.QUEUED.getCruiseStatus()};

    private static final List SUPPORTED_STATUS_LIST = Arrays.asList(SUPPORTED_STATUS_ARRAY);

    private final EnvironmentService environmentService;

    private final BuildInformationRepository buildInformationRepository;

    public BuildLoopQueryService() {
        this(null, null);
    }

    public BuildLoopQueryService(EnvironmentService environmentService,
            BuildInformationRepository buildInformationRepository) {
        this.environmentService = environmentService;
        this.buildInformationRepository = buildInformationRepository;
    }

    public String getJmxHttpUrl(String projectName) {
        return buildInformationRepository.getBuildLoopInfo(projectName).getJmxInfo().getHttpAdpatorUrl();
    }

    private ObjectName getObjectName(String projectName) throws MalformedObjectNameException {
        return ObjectName.getInstance("CruiseControl Project:name=" + projectName);
    }

    public void forceBuild(String projectName) throws Exception {
        if (!environmentService.isForceBuildEnabled()) {
            throw new RuntimeException("Force build is disabled");
        }
        try {
            final ClosableProjectMBeanConnection closableProjectMBeanConnection = getJMXConnection(projectName);
            try {
                closableProjectMBeanConnection.getMBeanServerConnection()
                        .invoke(getObjectName(projectName), JMXCOMMAND_BUILD, null, null);
            } finally {
                closableProjectMBeanConnection.close();
            }
        } catch (Exception e) {
            LOGGER.error("Could not force build on", e);
            throw e;
        }
    }

    public List getCommitMessages(String projectName) {
        return buildInformationRepository.getProjectInfo(projectName).getModifications();
    }

    public ProjectInfo getProjectInfo(String projectName) {
        return buildInformationRepository.getProjectInfo(projectName);
    }

    public String getProjectStatus(String projectName) {
        return getProjectInfo(projectName).getStatus();
    }

    public Map<String, String> getAllProjectsStatus() {
        final Map<String, String> result = new HashMap<String, String>();
        final List<ProjectInfo> infos = buildInformationRepository.getProjectInfos();
        for (final ProjectInfo projectInfo : infos) {
            result.put(projectInfo.getName(), getSupportedStatus(projectInfo.getStatus()));
        }
        return result;
    }

    public String[] getBuildOutput(String projectName, int firstLine) {
        try {
            final ClosableProjectMBeanConnection closableProjectMBeanConnection = getJMXConnection(projectName);
            try {
                final MBeanServerConnection jmxConnection = closableProjectMBeanConnection.getMBeanServerConnection();
                return (String[]) jmxConnection.invoke(getObjectName(projectName),
                        JMXCOMMAND_BUILD_OUTPUT, new Object[] {new Integer(firstLine)},
                        new String[] {Integer.class.getName()});
            } finally {
                closableProjectMBeanConnection.close();
            }
        } catch (Exception e) {
            LOGGER.error("Problem getting build output", e);
            return new String[] {" - Unable to connect to build loop at " + getServerName(projectName)};
        }
    }

    private String getSupportedStatus(String status) {
        if (!SUPPORTED_STATUS_LIST.contains(status)) {
            return CurrentStatus.WAITING.getCruiseStatus();
        }
        return status;
    }

    private ClosableProjectMBeanConnection getJMXConnection(String projectName) throws IOException {
        return buildInformationRepository.getJmxConnection(projectName);
    }

    public Projects getProjects() {
        final List<ProjectInfo> list = buildInformationRepository.getProjectInfos();
        int i = 0;
        final String[] projectNames = new String[list.size()];
        for (final ProjectInfo projectInfo : list) {
            projectNames[i++] = projectInfo.getName();
        }
        return new Projects(environmentService.getLogDir(), environmentService.getArtifactsDir(),
                projectNames);
    }

    public String getServerName(String projectName) {
        if (buildInformationRepository.hasBuildLoopInfoFor(projectName)) {
            return buildInformationRepository.getBuildLoopInfo(projectName).getServerName();
        } else {
            return "No server name available";
        }
    }

    public boolean isDiscontinued(String projectName) {
        return !buildInformationRepository.hasBuildLoopInfoFor(projectName);
    }
}
