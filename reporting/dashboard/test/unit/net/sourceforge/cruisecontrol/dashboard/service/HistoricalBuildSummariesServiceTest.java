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
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.io.FileUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

public class HistoricalBuildSummariesServiceTest extends MockObjectTestCase {

    private HistoricalBuildSummariesService buildSummariesSevice;

    private String projectName;

    private File projectDirectory;

    private Mock configurationMock;

    private static final String NONMATCHING_FILE = "nonmatching_file.xml";

    protected void setUp() throws Exception {
        projectName = "listingProject";
        projectDirectory = FilesystemUtils.createDirectory(projectName);
        createLogFiles(projectDirectory);
        configurationMock =
                mock(
                        ConfigurationService.class, new Class[]{EnvironmentService.class,
                        DashboardXmlConfigService.class, BuildLoopQueryService.class}, new Object[]{null,
                        null, null});
        ConfigurationService configuration = (ConfigurationService) configurationMock.proxy();
        buildSummariesSevice =
                new HistoricalBuildSummariesService(configuration, new BuildSummaryService());
    }

    private void setUpConfigurationMock() {
        configurationMock.expects(atLeastOnce()).method("getLogRoot").will(returnValue(projectDirectory));
    }

    public void createLogFiles(File directory) throws Exception {
        long base = 20060704155710L;
        int label = 489;
        final int count = 26;
        for (int i = 0; i < count; i++) {
            String fileName = "log" + (base + i) + "";
            if (i % 7 == 0) {
                fileName += "Lbuild." + (label + i);
            }
            fileName += (i < count / 2) ? ".xml.gz" : ".xml";
            FilesystemUtils.createFile(fileName, projectDirectory);
        }
        FilesystemUtils.createFile(NONMATCHING_FILE, projectDirectory);
    }

    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(projectDirectory);
    }

    public void testShouldBeAbleToReturnLastest25BuildOfSpecificProject() {
        setUpConfigurationMock();
        List lastest25 = buildSummariesSevice.getLastest25(projectName);
        assertEquals(25, lastest25.size());
        assertEquals("log20060704155735.xml", ((Build) lastest25.get(0)).getBuildLogFilename());
        assertEquals("log20060704155711.xml.gz", ((Build) lastest25.get(24)).getBuildLogFilename());
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
        assertEquals(
                "log20060704155717Lbuild.496.xml.gz", ((Build) allSuccessful.get(2))
                .getBuildLogFilename());
        assertEquals(
                "log20060704155710Lbuild.489.xml.gz", ((Build) allSuccessful.get(3))
                .getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnAllBuildOfSpecificProject() {
        setUpConfigurationMock();
        List all = buildSummariesSevice.getAll(projectName);
        assertEquals(26, all.size());
        assertEquals("log20060704155735.xml", ((Build) all.get(0)).getBuildLogFilename());
        assertEquals("log20060704155710Lbuild.489.xml.gz", ((Build) all.get(25)).getBuildLogFilename());
    }

    public void testShouldBeAbleToReturnLatestBuildSpecificProject() {
        setUpConfigurationMock();
        Build summary = buildSummariesSevice.getLatest(projectName);
        assertNotNull(summary);
        assertEquals("log20060704155735.xml", summary.getBuildLogFilename());
    }


    public void testShouldReturnNotApplicableWhenNoSuccessfulBuildOccured() {
        setUpConfigurationMock();
        String lastSuccessfulBuild =
                buildSummariesSevice.getDurationFromLastSuccessfulBuild(
                        projectName, new DateTime(
                        2005, 12,
                        9, 11, 21, 3, 0));
        assertEquals("N/A", lastSuccessfulBuild);
    }

    public void testShouldReturnTimeSinceLastSucceeded() {
        setUpConfigurationMock();
        String lastSuccessfulBuild =
                buildSummariesSevice.getDurationFromLastSuccessfulBuild(
                        projectName, new DateTime(
                        2006, 7, 6,
                        16, 58, 32, 0));
        assertEquals("2 days ago", lastSuccessfulBuild);
    }

    public void testShouldReturnTimeSinceLastSucceededOmittingUnnecessaryParts() {
        setUpConfigurationMock();
        String lastSuccessfulBuild =
                buildSummariesSevice.getDurationFromLastSuccessfulBuild(
                        projectName, new DateTime(
                        2006, 7, 4,
                        15, 58, 31, 0));
        assertEquals("1 minute ago", lastSuccessfulBuild);
    }

    public void testGetLatestShouldReturnInactiveBuildForNewProject() throws Exception {
        configurationMock.expects(atLeastOnce()).method("getLogRoot").will(
                returnValue(new File(projectDirectory, "new_project")));
        BuildSummary latest = buildSummariesSevice.getLatest("new_project");
        assertTrue(latest.isInactive());
    }
}
