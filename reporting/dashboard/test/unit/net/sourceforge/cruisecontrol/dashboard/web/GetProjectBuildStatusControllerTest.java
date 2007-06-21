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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.JMXFactory;
import net.sourceforge.cruisecontrol.dashboard.service.SystemService;
import net.sourceforge.cruisecontrol.dashboard.web.view.JsonView;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class GetProjectBuildStatusControllerTest extends MockObjectTestCase {

    private MockHttpServletResponse response;

    private MockHttpServletRequest request;

    private GetProjectBuildStatusController controller;

    private Build earliestFailed;

    private Build lastPassed;

    private BuildSummariesService buildSummaryService;

    private Mock buildSummaryServiceMock;

    private Mock jmxServiceMock;

    protected void setUp() throws Exception {
        earliestFailed =
                new BuildSummary("project1", "2004-04-20 17:47.44", "", ProjectBuildStatus.FAILED, "");
        lastPassed = new BuildSummary("project1", "2004-04-20 17:47.44", "", ProjectBuildStatus.PASSED, "");
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addParameter("name", "project1");
    }

    private void setUpControllerWithBuildSummaryService() {
        buildSummaryServiceMock =
                mock(BuildSummariesService.class,
                        new Class[] {Configuration.class, BuildSummaryService.class}, new Object[] {null,
                                null});
        buildSummaryServiceMock.expects(atLeastOnce()).method("getLatestOfProjects").withAnyArguments().will(
                returnValue(buidSummaries()));
        buildSummaryService = (BuildSummariesService) buildSummaryServiceMock.proxy();
        jmxServiceMock =
                mock(CruiseControlJMXService.class, new Class[] {JMXFactory.class, EnvironmentService.class},
                        new Object[] {null, new EnvironmentService(new SystemService())});
        jmxServiceMock.expects(once()).method("isCruiseAlive").will(returnValue(true));
        jmxServiceMock.expects(once()).method("getAllProjectsStatus").withNoArguments().will(
                returnValue(returnedMap()));
        controller =
                new GetProjectBuildStatusController(buildSummaryService,
                        (CruiseControlJMXService) jmxServiceMock.proxy(), new BuildSummaryUIService(
                                buildSummaryService));
    }

    public void testShouldReturnViewIncludeMultipleProjects() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "project1"));
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

    public void testShouldReturnViewIncludeBuildInfomation() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "project1"));
    }

    public void testShouldReturnJasonViewInWaitingStatus() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "Failed"));
    }

    public void testShouldReturnJasonViewWithClassNameAsLongFailedStatus() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "failed_level_8"));
    }

    public void testShouldReturnPassedJasonViewInWaitingStatus() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, "Passed"));
    }

    public void testShouldReturnJasonViewIncludeBuildingStatus() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        ModelAndView mov = controller.handleRequest(request, response);
        mov.getView().render(mov.getModelMap(), request, response);
        String output = response.getContentAsString();
        assertTrue(StringUtils.contains(output, GetProjectBuildStatusController.PROJECT_STATUS_IN_BUILDING));
        assertTrue(output.indexOf("build_time_elapsed") >= 0);
    }

    public void testShouldReturnJsonErrorWhenStatusCallFails() throws Exception {
        Mock failingServiceMock =
                mock(BuildSummariesService.class,
                        new Class[] {Configuration.class, BuildSummaryService.class}, new Object[] {null,
                                null});
        jmxServiceMock =
            mock(CruiseControlJMXService.class, new Class[] {JMXFactory.class, EnvironmentService.class},
                    new Object[] {null, new EnvironmentService(new SystemService())});
        jmxServiceMock.expects(once()).method("isCruiseAlive").will(returnValue(true));
        failingServiceMock.expects(once()).method("getLatestOfProjects").will(
                throwException(new RuntimeException("xyz")));
        controller =
                new GetProjectBuildStatusController((BuildSummariesService) failingServiceMock.proxy(),
                        (CruiseControlJMXService) jmxServiceMock.proxy(), null);

        ModelAndView mov = controller.handleRequest(request, response);

        assertTrue(mov.getView() instanceof JsonView);
        Map model = mov.getModelMap();
        assertEquals(1, model.size());
        assertEquals("xyz", model.get("error"));
    }

    public void testShouldUseCachedDataModelWithinTheCachePeriod() throws Exception {
        setUpControllerWithBuildSummaryService();
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastPassed));
        controller.handleRequest(request, response);
        buildSummaryServiceMock.expects(never()).method("getLatestOfProjects").withAnyArguments().will(
                returnValue(buidSummaries()));
        jmxServiceMock.expects(never()).method("getAllProjectsStatus").withNoArguments().will(
                returnValue(returnedMap()));
        buildSummaryServiceMock.expects(never()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earliestFailed));
        buildSummaryServiceMock.expects(never()).method("getLastSucceed").withAnyArguments().will(
                returnValue(lastPassed));
        controller.handleRequest(request, response);
    }

    private Map returnedMap() {
        Map map = new HashMap();
        map.put("project1", "Passed");
        map.put("project2", "Failed");
        map.put("project3", "now building since 20070420170000");
        map.put("project4", "Inactive");
        map.put("project5", "Inactive");
        return map;
    }

    private List buidSummaries() {
        List list = new ArrayList();
        BuildSummary build1 =
                new BuildSummary("project1", "2005-12-09 12:21.03", "build.1", ProjectBuildStatus.PASSED,
                        "log1");
        BuildSummary build2 =
                new BuildSummary("project2", "2005-12-09 12:21.03", "", ProjectBuildStatus.FAILED, "log2");
        BuildSummary build3 =
                new BuildSummary("project3", "2005-12-09 12:21.03", "", ProjectBuildStatus.BUILDING, "log2");
        BuildSummary build4 =
                new BuildSummary("project4", "2005-12-09 12:21.03", "", ProjectBuildStatus.INACTIVE, "log2");
        BuildSummary build5 =
                new BuildSummary("project5", "2005-12-09 12:21.03", "", ProjectBuildStatus.INACTIVE, "log2");
        list.add(build1);
        list.add(build2);
        list.add(build3);
        list.add(build4);
        list.add(build5);
        return list;
    }
}
