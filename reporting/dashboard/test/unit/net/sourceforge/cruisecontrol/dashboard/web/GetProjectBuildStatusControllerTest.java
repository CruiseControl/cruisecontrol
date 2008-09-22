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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.HistoricalBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.LatestBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import net.sourceforge.cruisecontrol.dashboard.web.view.JsonView;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class GetProjectBuildStatusControllerTest extends MockObjectTestCase {

    private MockHttpServletResponse response;

    private MockHttpServletRequest request;

    private GetProjectBuildStatusController controller;

    private Mock latestBuildSummariesServiceMock;

    private Mock buildSummaryUIServiceMock;

    protected void setUp() throws Exception {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addParameter("name", "project1");
        createMocks();
    }

    private void setUpControllerWithBuildSummaryService() {
        latestBuildSummariesServiceMock.expects(atLeastOnce()).method("getLatestOfProjects").will(
                returnValue(buidSummaries()));
    }

    private void createMocks() {
        latestBuildSummariesServiceMock =
                mock(
                        LatestBuildSummariesService.class,
                        new Class[]{HistoricalBuildSummariesService.class, BuildLoopQueryService.class},
                        new Object[]{null, null});
        LatestBuildSummariesService buildSummaryService =
                (LatestBuildSummariesService) latestBuildSummariesServiceMock.proxy();
        buildSummaryUIServiceMock = mock(
                BuildSummaryUIService.class, new Class[]{HistoricalBuildSummariesService.class,
                DashboardXmlConfigService.class}, new Object[]{null, null});
        controller =
                new GetProjectBuildStatusController(
                        buildSummaryService, (BuildSummaryUIService) buildSummaryUIServiceMock.proxy());
    }

    public void testShouldReturnViewIncludeMultipleProjects() throws Exception {
        setUpControllerWithBuildSummaryService();

        buildSummaryUIServiceMock.expects(atLeastOnce()).method("transformWithLevel").withAnyArguments()
                .will(returnValue(buildSummaryCommands()));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(output, StringUtils.contains(output, "project1"));
        assertTrue(StringUtils.contains(output, "project2"));
        assertTrue(StringUtils.contains(output, "["));
        assertTrue(StringUtils.contains(output, "]"));

        assertTrue(mov.getView() instanceof JsonView);
        Map model = mov.getModelMap();
        assertEquals(1, model.size());
        List list = (List) model.get(JsonView.RENDER_DIRECT);
        assertEquals(5, list.size());
        Map firstElement = (Map) list.get(0);
        assertEquals(1, firstElement.size());
        Map firstInfo = (Map) firstElement.get("building_info");
        assertEquals("project1", firstInfo.get("project_name"));
        Map secondElement = (Map) list.get(1);
        Map secondInfo = (Map) secondElement.get("building_info");
        assertEquals("project2", secondInfo.get("project_name"));
    }

    public void testShouldPutCacheControlHeaderInResponse() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryUIServiceMock.expects(atLeastOnce()).method("transformWithLevel").withAnyArguments()
                .will(returnValue(buildSummaryCommands()));
        controller.handleRequest(request, response);
        assertEquals(GetProjectBuildStatusController.CACHE_CONTROL, response.getHeader("Cache-Control"));
    }

    public void testShouldReturnJasonViewInWaitingStatus() throws Exception {
        setUpControllerWithBuildSummaryService();

        buildSummaryUIServiceMock.expects(atLeastOnce()).method("transformWithLevel").withAnyArguments()
                .will(returnValue(buildSummaryCommands()));

        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "Failed"));
    }


    public void testShouldReturnPassedJasonViewInWaitingStatus() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryUIServiceMock.expects(atLeastOnce()).method("transformWithLevel").withAnyArguments()
                .will(returnValue(buildSummaryCommands()));

        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "Passed"));
    }

    public void testShouldReturnJasonViewIncludeBuildingStatus() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryUIServiceMock.expects(atLeastOnce()).method("transformWithLevel").withAnyArguments()
                .will(returnValue(buildSummaryCommands()));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, GetProjectBuildStatusController.PROJECT_STATUS_IN_BUILDING));
        assertTrue(output.indexOf("build_time_elapsed") >= 0);
    }

    public void testShouldReturnJsonErrorWhenStatusCallFails() throws Exception {
        Mock failingServiceMock =
                mock(
                        LatestBuildSummariesService.class, new Class[]{HistoricalBuildSummariesService.class,
                        BuildLoopQueryService.class}, new Object[]{null, null});

        failingServiceMock.expects(once()).method("getLatestOfProjects").will(
                throwException(new RuntimeException("xyz")));
        controller =
                new GetProjectBuildStatusController(
                        (LatestBuildSummariesService) failingServiceMock
                                .proxy(), null);

        ModelAndView mov = controller.handleRequest(request, response);

        assertTrue(mov.getView() instanceof JsonView);
        Map model = mov.getModelMap();
        assertEquals(1, model.size());
        assertEquals("xyz", model.get("error"));
    }

    private List buildSummaryCommands() {
        List summaries = buidSummaries();
        List buildCommands = new ArrayList();
        for (int i = 0; i < summaries.size(); i++) {
            BuildSummary buildSummary = (BuildSummary) summaries.get(i);
            buildCommands.add(new BuildCommand(buildSummary, null));
        }
        return buildCommands;
    }

    private List buidSummaries() {
        List list = new ArrayList();
        BuildSummary build1 =
                new BuildSummary("project1", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        BuildSummary build2 =
                new BuildSummary("project2", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        BuildSummary build3 =
                new BuildSummary("project3", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        BuildSummary build4 =
                new BuildSummary("project4", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        BuildSummary build5 =
                new BuildSummary("project5", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        build3.updateStatus(CurrentStatus.BUILDING.getCruiseStatus());
        build3.updateBuildSince(new DateTime());
        build4.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        build5.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        list.add(build1);
        list.add(build2);
        list.add(build3);
        list.add(build4);
        list.add(build5);
        return list;
    }
}
