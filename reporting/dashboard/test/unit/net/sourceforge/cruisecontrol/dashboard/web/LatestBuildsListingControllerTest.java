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


import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.BuildSummaryStatistics;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigFileFactory;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.HistoricalBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.LatestBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LatestBuildsListingControllerTest extends MockObjectTestCase {
    private Mock latestBuildSummaryServiceMock;

    private Mock buildSummaryUIServiceMock;

    private HistoricalBuildSummariesService buildSummaryService;

    private LatestBuildsListingController controller;

    private Build ealiestFailed;

    private Build lastSucceed;

    private Mock mockHistoricalBuildSummaryService;

    private Mock mockDashboardXmlConfigService;

    protected void setUp() throws Exception {
        ealiestFailed = new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        lastSucceed = new BuildSummary("", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        setUpMock();
        controller = new LatestBuildsListingController(
                (LatestBuildSummariesService) latestBuildSummaryServiceMock.proxy(),
                (BuildSummaryUIService) buildSummaryUIServiceMock.proxy()) {

            protected String[] getCssFiles() {
                return new String[0];
            }

            protected String getViewName() {
                return "dashboard";
            }
        };
    }

    private void setUpMock() throws Exception {
        latestBuildSummaryServiceMock = mock(
                LatestBuildSummariesService.class,
                new Class[]{HistoricalBuildSummariesService.class, BuildLoopQueryService.class},
                new Object[]{null, null});
        buildSummaryUIServiceMock = mock(
                BuildSummaryUIService.class,
                new Class[]{HistoricalBuildSummariesService.class, DashboardXmlConfigService.class},
                new Object[]{null, null});
        mockDashboardXmlConfigService =
                mock(
                        DashboardXmlConfigService.class, new Class[]{DashboardConfigFileFactory.class},
                        new Object[]{null});
        latestBuildSummaryServiceMock.expects(once()).method("getLatestOfProjects").will(
                returnValue(updatedBuildSummaries()));
        buildSummaryUIServiceMock.expects(once()).method("transformWithLevel").will(returnValue(getBuildCommands()));
    }

    public void testShouldBeAbleToListAllTheProjectInDirectory() throws Exception {
        ModelAndView mov =
                controller.handleRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
        Map model = mov.getModel();
        List projects = (List) model.get("buildCmds");
        assertEquals(5, projects.size());
        BuildSummaryStatistics projectStatistics = (BuildSummaryStatistics) model.get("projectStatistics");
        assertNotNull(projectStatistics);
    }

    public void testShouldUpdateLiveStatus() throws Exception {
        ModelAndView mov =
                controller.handleRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
        Map model = mov.getModel();
        List projects = (List) model.get("buildCmds");
        for (Iterator iterator = projects.iterator(); iterator.hasNext();) {
            BuildCommand build = (BuildCommand) iterator.next();
            if ("project2".equals(build.getBuild().getProjectName())) {
                assertEquals(CurrentStatus.BUILDING, build.getBuild().getCurrentStatus());
                return;
            }
        }
        fail("There should be at least one project is building");
    }

    private List getBuildCommands() {
        List buildSummaries = updatedBuildSummaries();
        List allBuildCommands = new ArrayList();
        for (int i = 0; i < buildSummaries.size(); i++) {
            BuildSummary buildSummary = (BuildSummary) buildSummaries.get(i);
            allBuildCommands.add(new BuildCommand(buildSummary, null));
        }
        return allBuildCommands;
    }

    private List updatedBuildSummaries() {
        BuildSummary summary4 =
                new BuildSummary("project4", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        summary4.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        BuildSummary summary5 =
                new BuildSummary("project5", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        summary5.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        BuildSummary[] buildSummaries = new BuildSummary[]{
                new BuildSummary("project1", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML),
                new BuildSummary("project2", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML),
                new BuildSummary("project3", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML),
                summary4, summary5};
        buildSummaries[1].updateStatus("now building since 20070420170000");
        return Arrays.asList(buildSummaries);
    }


    private List originalBuilds() {
        BuildSummary building =
                new BuildSummary("project3", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        BuildSummary summary4 =
                new BuildSummary("project4", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        summary4.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        BuildSummary summary5 =
                new BuildSummary("project5", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        summary5.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        building.updateStatus(CurrentStatus.BUILDING.getCruiseStatus());
        Build[] builds =
                new Build[]{
                        new BuildSummary("project1", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML),
                        new BuildSummary("project2", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML),
                        building, summary4, summary5};
        return Arrays.asList(builds);
    }
}
