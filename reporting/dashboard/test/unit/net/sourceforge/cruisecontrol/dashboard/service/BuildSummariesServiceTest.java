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

import java.io.File;
import java.util.List;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.io.FileUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

public class BuildSummariesServiceTest extends MockObjectTestCase {
    private BuildSummariesService buildSummariesSevice;

    private String projectName;

    private File projectDirectory;

    private Mock configurationMock;

    protected void setUp() throws Exception {
        projectName = "listingProject";
        projectDirectory = FilesystemUtils.createDirectory(projectName);
        createLogFiles(projectDirectory);
        configurationMock =
                mock(Configuration.class, new Class[] {ConfigXmlFileService.class},
                        new Object[] {new ConfigXmlFileService(new EnvironmentService(new SystemService(),
                                new DashboardConfigService[] {}))});
        Configuration configuration = (Configuration) configurationMock.proxy();
        buildSummariesSevice = new BuildSummariesService(configuration, new BuildSummaryService());
    }

    private void setUpConfigurationMock() {
        configurationMock.expects(atLeastOnce()).method("getLogRoot").will(returnValue(projectDirectory));
    }

    public void createLogFiles(File directory) throws Exception {
        long base = 20060704155710L;
        int label = 489;
        for (int i = 0; i < 26; i++) {
            String fileName = "log" + (base + i) + "";
            if (i % 7 == 0) {
                fileName += "Lbuild." + (label + i);
            }
            fileName += ".xml";
            FilesystemUtils.createFile(fileName, projectDirectory);
        }
    }

    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(projectDirectory);
    }

    public void testShouldBeAbleToReturnLastest25BuildOfSpecificProject() {
        setUpConfigurationMock();
        List lastest25 = buildSummariesSevice.getLastest25(projectName);
        assertEquals(25, lastest25.size());
        assertEquals("log20060704155735.xml", ((Build) lastest25.get(0)).getBuildLogFilename());
        assertEquals("log20060704155711.xml", ((Build) lastest25.get(24)).getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnLastSuccessfulBuild() {
        setUpConfigurationMock();
        DateTime time = CCDateFormatter.formatLogName("log20060704155710Lbuild.489.xml");
        Build lastSucceed = buildSummariesSevice.getLastSucceed(projectName, time);
        assertNull(lastSucceed);
    }

    public void testShouldReturnTheTopSucceedWhenCurrentTimeIsNew() {
        setUpConfigurationMock();
        DateTime time = CCDateFormatter.formatLogName("log20060704155759Lbuild.503.xml");
        Build lastSucceed = buildSummariesSevice.getLastSucceed(projectName, time);
        assertEquals("log20060704155731Lbuild.510.xml", lastSucceed.getBuildLogFilename());
    }

    public void testShouldReturnTheBottomSucceedWhenCurrentTimeIsNew() {
        setUpConfigurationMock();
        DateTime time = CCDateFormatter.formatLogName("log20060704155730.xml");
        Build lastSucceed = buildSummariesSevice.getLastSucceed(projectName, time);
        assertEquals("log20060704155724Lbuild.503.xml", lastSucceed.getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnEarilestFailedBuild() {
        setUpConfigurationMock();
        Build earilestFailed = buildSummariesSevice.getEaliestFailed(projectName, new DateTime());
        assertEquals("log20060704155732.xml", earilestFailed.getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnAllSuccessfulBuildOfSpecificProject() {
        setUpConfigurationMock();
        List allSuccessful = buildSummariesSevice.getAllSucceed(projectName);
        assertEquals(4, allSuccessful.size());
        assertEquals("log20060704155731Lbuild.510.xml", ((Build) allSuccessful.get(0)).getBuildLogFilename());
        assertEquals("log20060704155724Lbuild.503.xml", ((Build) allSuccessful.get(1)).getBuildLogFilename());
        assertEquals("log20060704155717Lbuild.496.xml", ((Build) allSuccessful.get(2)).getBuildLogFilename());
        assertEquals("log20060704155710Lbuild.489.xml", ((Build) allSuccessful.get(3)).getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnAllBuildOfSpecificProject() {
        setUpConfigurationMock();
        List all = buildSummariesSevice.getAll(projectName);
        assertEquals(26, all.size());
        assertEquals("log20060704155735.xml", ((Build) all.get(0)).getBuildLogFilename());
        assertEquals("log20060704155710Lbuild.489.xml", ((Build) all.get(25)).getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnLatestBuildSpecificProject() {
        setUpConfigurationMock();
        Build summary = buildSummariesSevice.getLatest(projectName);
        assertNotNull(summary);
        assertEquals("log20060704155735.xml", summary.getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnAllLastestBuildOfProjects() {
        File[] files = new File[] {projectDirectory, new File("isolate_project_in_disk")};
        configurationMock.expects(once()).method("getProjectDirectoriesFromFileSystem").will(
                returnValue(files));
        File[] configFiles = new File[] {projectDirectory, new File("isolate_project_in_config_xml")};
        configurationMock.expects(once()).method("getProjectDirectoriesFromConfigFile").will(
                returnValue(configFiles));
        List allLatestOfProjects = buildSummariesSevice.getLatestOfProjects();
        assertEquals(3, allLatestOfProjects.size());
        assertEquals("isolate_project_in_config_xml", ((Build) allLatestOfProjects.get(0)).getProjectName());
        assertEquals("isolate_project_in_disk", ((Build) allLatestOfProjects.get(1)).getProjectName());
        assertEquals("listingProject", ((Build) allLatestOfProjects.get(2)).getProjectName());
    }

    public void testShouldReturnNotApplicableWhenNoSuccessfulBuildOccured() {
        setUpConfigurationMock();
        String lastSuccessfulBuild =
                buildSummariesSevice.getDurationFromLastSuccessfulBuild(projectName, new DateTime(2005, 12,
                        9, 11, 21, 3, 0));
        assertEquals("N/A", lastSuccessfulBuild);
    }

    public void testShouldReturnTimeSinceLastSucceeded() {
        setUpConfigurationMock();
        String lastSuccessfulBuild =
                buildSummariesSevice.getDurationFromLastSuccessfulBuild(projectName, new DateTime(2006, 7, 6,
                        16, 58, 32, 0));
        assertEquals("2 days 1 hours 1 minutes 1 seconds ago", lastSuccessfulBuild);
    }

    public void testShouldReturnTimeSinceLastSucceededOmittingUnnecessaryParts() {
        setUpConfigurationMock();
        String lastSuccessfulBuild =
                buildSummariesSevice.getDurationFromLastSuccessfulBuild(projectName, new DateTime(2006, 7, 4,
                        15, 58, 31, 0));
        assertEquals("1 minutes ago", lastSuccessfulBuild);
    }

    public void testGetLatestShouldReturnNullForNewProject() throws Exception {
        configurationMock.expects(atLeastOnce()).method("getLogRoot").will(
                returnValue(new File(projectDirectory, "new_project")));
        Build latest = buildSummariesSevice.getLatest("new_project");
        assertNull(latest);
    }

    public void testShouldReturnPassedStatusForNewProject() throws Exception {
        configurationMock.expects(atLeastOnce()).method("getLogRoot").will(
                returnValue(new File(projectDirectory, "new_project")));
        ProjectBuildStatus status = buildSummariesSevice.getLastBuildStatus("new_project");
        assertEquals(ProjectBuildStatus.PASSED, status);
    }
}
