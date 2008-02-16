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
package net.sourceforge.cruisecontrol.dashboard.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.LogFile;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.BuildLiveDetail;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import net.sourceforge.cruisecontrol.dashboard.service.HistoricalBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigFileFactory;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.WidgetPluginService;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;
import org.springframework.web.servlet.ModelAndView;

public class GetActiveBuildInfoControllerTest extends MockObjectTestCase {

    private BuildDetailController controller;

    private Mock buildLoopService;

    private Mock mockBuildSummariesService;

    private Mock mockBuildService;
    private static final String PROJECT_NAME = "connectfour";

    protected void setUp() throws Exception {
        List allBuilds = Arrays.asList(new Build[] {
            new BuildSummary("", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML)
        });
        mockBuildSummariesService =
                mock(HistoricalBuildSummariesService.class, new Class[] {ConfigurationService.class,
                        BuildSummaryService.class}, new Object[] {null, null});
        mockBuildSummariesService.expects(once()).method("getLastest25").withAnyArguments().will(
                returnValue(allBuilds));
        mockBuildSummariesService.expects(once()).method("getDurationFromLastSuccessfulBuild")
                .withAnyArguments().will(returnValue("1 days 3 hours"));
        buildLoopService = mock(BuildLoopQueryService.class,
                new Class[] {EnvironmentService.class, BuildInformationRepository.class},
                new Object[] {null, null});
        mockBuildService = mock(BuildService.class,
                new Class[] {ConfigurationService.class, BuildLoopQueryService.class},
                new Object[] {null, null});

        Mock mockConfigService = mock(DashboardXmlConfigService.class,
                new Class[]{DashboardConfigFileFactory.class},
                new Object[]{null});
        mockConfigService.expects(atLeastOnce()).method("getStoryTrackers").will(returnValue(new HashMap()));
        HistoricalBuildSummariesService buildSummariesService =
                (HistoricalBuildSummariesService) mockBuildSummariesService.proxy();
        controller =
                new BuildDetailController((BuildService) mockBuildService.proxy(), buildSummariesService,
                        new WidgetPluginService((DashboardXmlConfigService) mockConfigService.proxy()),
                        new BuildSummaryUIService(buildSummariesService,
                                (DashboardXmlConfigService) mockConfigService.proxy()),
                        (BuildLoopQueryService) buildLoopService.proxy());
    }

    public void testShouldReturnDurationFromLastSuccessfulBuildInBuildingStatus() throws Throwable {
        mockBuildSummariesService.expects(atLeastOnce()).method("getLatest").withAnyArguments().will(
                returnValue(null));
        mockBuildService.expects(once()).method("getActiveBuild").will(returnValue(getActiveBuild()));
        buildLoopService.expects(once()).method("getProjectInfo").will(returnValue(connectfourInfo()));
        ModelAndView mov = controller.live(PROJECT_NAME);
        Map model = mov.getModel();
        assertTrue(StringUtils.contains((String) model.get("durationToSuccessfulBuild"), "1 days 3 hours"));
    }

    private ProjectInfo connectfourInfo() {
        return new ProjectInfo(PROJECT_NAME, "now building", "2005-12-05T13:09:56");
    }

    public void testShouldReturnBuildSince() throws Throwable {
        mockBuildSummariesService.expects(atLeastOnce()).method("getLatest").withAnyArguments().will(
                returnValue(null));
        mockBuildService.expects(once()).method("getActiveBuild").will(returnValue(getActiveBuild()));
        buildLoopService.expects(once()).method("getProjectInfo").will(returnValue(connectfourInfo()));
        ModelAndView mov = controller.live(PROJECT_NAME);
        Map model = mov.getModel();
        assertNotNull(model.get("buildSince"));
        assertFalse(StringUtils.contains((String) model.get("buildSince"), "N/A"));
    }

    public void testShouldReturnLastBuildSummaries() throws Throwable {
        mockBuildSummariesService.expects(atLeastOnce()).method("getLatest").withAnyArguments().will(
                returnValue(null));
        mockBuildService.expects(once()).method("getActiveBuild").will(returnValue(getActiveBuild()));
        buildLoopService.expects(once()).method("getProjectInfo").will(returnValue(connectfourInfo()));
        ModelAndView mov = controller.live(PROJECT_NAME);
        Map model = mov.getModel();
        assertNotNull(model.get("historicalBuildCmds"));
        assertEquals(1, ((Collection) model.get("historicalBuildCmds")).size());
    }

    public BuildLiveDetail getActiveBuild() {
        return new BuildLiveDetail(PROJECT_NAME, PreviousResult.FAILED);
    }

    public Build getFailedBuild() {
        final String logFile = "log20060101121212.xml";
        return new FailedBuildDetailStub(logFile);
    }

    private static class FailedBuildDetailStub extends BuildDetail {
        private final String logFile;

        public FailedBuildDetailStub(String logFile) {
            super(new LogFile(logFile));
            this.logFile = logFile;
        }


        public CurrentStatus getCurrentStatus() {
            return CurrentStatus.WAITING;
        }

        public PreviousResult getPreviousBuildResult() {
            return PreviousResult.FAILED;
        }

        public String getProjectName() {
            return PROJECT_NAME;
        }

        public String getBuildLogFilename() {
            return logFile;
        }

        public DateTime getBuildDate() {
            return new DateTime();
        }
    }
}
