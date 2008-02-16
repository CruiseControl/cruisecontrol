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
package net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub;

import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DashboardXMLManager;
import net.sourceforge.cruisecontrol.util.DateUtil;
import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildLoopQueryServiceStub extends BuildLoopQueryService {

    private final EnvironmentService envService;

    private final ConfigXmlFileService configXmlService;

    private DateTime thritySecondsAgo;

    public BuildLoopQueryServiceStub(EnvironmentService envService, BuildInformationRepository repository) {
        super(envService, repository);
        this.envService = envService;
        this.configXmlService = new ConfigXmlFileService();
        thritySecondsAgo = new DateTime().minusSeconds(30);
    }

    private static final int DEFAULT_HTTP_PORT = 8000;

    public static final Integer BUILD_TIMES = new Integer(5);

    public static final String WAITING = "waiting for next time to build";

    public static final String BOOTSTRAPPING = "bootstrapping";

    public static final String MODIFICATIONSET = "checking for modifications";

    public static final String PROPS_CC_CONFIG_FILE = "cc.config.file";

    private Map projectStatus = new HashMap();

    private Map buildingCounts = new HashMap();

    private Integer nextBuildCount(String projectName) {
        if (!buildingCounts.containsKey(projectName)) {
            buildingCounts.put(projectName, BUILD_TIMES);
        }
        Integer current = (Integer) buildingCounts.get(projectName);
        Integer next = new Integer(current.intValue() - 1);
        buildingCounts.put(projectName, next);
        return next;
    }

    public String getProjectStatus(String projectName) {
        String status = (String) projectStatus.get(projectName);
        if ("paused".equals(projectName)) {
            return CurrentStatus.PAUSED.getCruiseStatus();
        }
        if (projectName.startsWith("queued")) {
            return CurrentStatus.QUEUED.getCruiseStatus();
        }
        if (status == null) {
            status = WAITING;
            projectStatus.put(projectName, status);
            return status;
        }
        if (status.startsWith("now building")) {
            String nextStatus = getNextStatus(projectName);
            projectStatus.put(projectName, nextStatus);
            thritySecondsAgo = new DateTime().minusSeconds(30);
            return nextStatus;
        } else {
            return WAITING;
        }
    }

    public Projects getProjects() {
        try {
            File configXmlFile = configXmlService.getConfigXmlFile(null);
            DashboardXMLManager manager = new DashboardXMLManager(configXmlFile);
            return new Projects(envService.getLogDir(), envService.getArtifactsDir(),
                    getProjectNames(manager));
        } catch (Exception e) {
            return null;
        }
    }

    private String[] getProjectNames(DashboardXMLManager manager) {
        return (String[]) new ArrayList(manager.getCruiseControlConfig().getProjectNames())
                .toArray(new String[0]);
    }

    private String getNextStatus(String projectName) {
        Integer buildCount = nextBuildCount(projectName);
        if (buildCount.intValue() == 0) {
            buildingCounts.remove(projectName);
            return WAITING;
        }
        return buildingStatus();
    }

    private String buildingStatus() {
        return "now building";
    }

    public Map getAllProjectsStatus() {
        Projects projects;
        try {
            projects = getProjects();
        } catch (Exception e) {
            projects = null;
        }

        final Projects allProjects = projects;
        return new HashMap() {
            private static final long serialVersionUID = 1L;

            public boolean containsKey(Object key) {
                if (allProjects == null) {
                    return true;
                } else {
                    return allProjects.hasProject((String) key);
                }
            }

            public Object get(Object key) {
                return getProjectStatus((String) key);
            }

            public boolean isEmpty() {
                return false;
            }
        };
    }

    public ProjectInfo getProjectInfo(String projectName) {
        return new ProjectInfo(projectName, "any", DateUtil.formatIso8601(thritySecondsAgo.toDate()));
    }

    public List getCommitMessages(String projectName) {
        List list = new ArrayList();
        if ("projectWithoutPublishers".equals(projectName)) {
            return list;
        }
        String status = (String) projectStatus.get(projectName);
        if (status.startsWith("now building")) {
            list.add(createModification("joe", "Some random change", "file1.txt", "build.xml"));
            list.add(createModification("dev", "Fixed the build456", "file2.txt", "config.xml"));
            return list;
        } else {
            return list;
        }
    }

    private Modification createModification(String name, String comment, String file, String file2) {
        List files = new ArrayList();
        Modification.ModifiedFile mfile1 = new Modification.ModifiedFile(file,  "123", "folder", "added");
        Modification.ModifiedFile mfile2 = new Modification.ModifiedFile(file2, "567", "folder2", "deleted");
        files.add(mfile1);
        files.add(mfile2);
        return new Modification("svn", name, comment, "use@email.com", new Date(), "1234", files);
    }

    public void forceBuild(String projectName) {
        projectStatus.put(projectName, buildingStatus());
    }

    public String[] getBuildOutput(String projectName, int firstLine) {
        String status = (String) projectStatus.get(projectName);
        if (status.startsWith("now building")) {
            return new String[] {"Build Failed.\nBuild Duration: 0s"};
        } else {
            return null;
        }
    }

    public String getJmxHttpUrl(String projectName) {
        return "http://localhost:" + DEFAULT_HTTP_PORT;
    }

    private static class ConfigXmlFileService {

        public File getConfigXmlFile(File configFile) {
            if (isConfigFileValid(configFile)) {
                return configFile;
            }
            File configFileFromProps = new File(System.getProperty(PROPS_CC_CONFIG_FILE));
            if (isConfigFileValid(configFileFromProps)) {
                return configFileFromProps;
            }
            return null;
        }

        private boolean isConfigFileValid(File configFile) {
            return configFile != null && configFile.exists()
                    && configFile.getName().endsWith(".xml");
        }
    }

    public String getServerName(String projectName) {
        return "localhost";
    }

    public boolean isDiscontinued(String projectName) {
        return !getProjects().hasProject(projectName);
    }
}
