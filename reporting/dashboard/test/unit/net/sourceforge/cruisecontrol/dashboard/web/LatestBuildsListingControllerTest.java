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
import net.sourceforge.cruisecontrol.dashboard.BuildSummaryStatistics;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.SystemService;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class LatestBuildsListingControllerTest extends MockObjectTestCase {
    private BuildSummariesService buildSummaryService;

    private LatestBuildsListingController controller;

    private Build ealiestFailed;

    private Build lastSucceed;

    private Mock mockBuildSummaryService;

    private Mock mockDashboardXmlConfigService;

    protected void setUp() throws Exception {
        ealiestFailed =
                new BuildSummary("", "2005-12-07 12:21.03", "build1", ProjectBuildStatus.FAILED, "log1");
        lastSucceed =
                new BuildSummary("", "2005-12-05 12:21.03", "build1", ProjectBuildStatus.PASSED, "log1");
        setUpMock();
        controller =
                new LatestBuildsListingController(buildSummaryService, new BuildSummaryUIService(
                        buildSummaryService, (DashboardXmlConfigService) mockDashboardXmlConfigService
                                .proxy()), new EnvironmentService(new SystemService(),
                        new DashboardConfigService[] {}));
    }

    private void setUpMock() throws Exception {
        mockBuildSummaryService =
                mock(BuildSummariesService.class,
                        new Class[] {Configuration.class, BuildSummaryService.class}, new Object[] {null,
                                new BuildSummaryService()});
        mockDashboardXmlConfigService =
                mock(DashboardXmlConfigService.class, new Class[] {SystemService.class},
                        new Object[] {new SystemService()});
        mockBuildSummaryService.expects(once()).method("getLatestOfProjects").withNoArguments().will(
                returnValue(returnedValue()));
        mockBuildSummaryService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(ealiestFailed));
        mockBuildSummaryService.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(lastSucceed));
        buildSummaryService = (BuildSummariesService) mockBuildSummaryService.proxy();
        mockDashboardXmlConfigService.expects(atLeastOnce()).method("getStoryTrackers").will(
                returnValue(new HashMap()));
    }

    public void testShouldBeAbleToListAllTheProjectInDirectory() throws Exception {
        ModelAndView mov =
                controller.handleRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
        Map model = mov.getModel();
        List projects = (List) model.get("buildSummaries");
        assertEquals(5, projects.size());
        BuildSummaryStatistics projectStatistics = (BuildSummaryStatistics) model.get("projectStatistics");
        assertNotNull(projectStatistics);
        assertEquals(new Integer(5), projectStatistics.total());
        assertEquals(new Integer(2), projectStatistics.inactive());
    }

    public void testShouldPutIsForceBuildEnabledIntoModel() throws Exception {
        ModelAndView mov =
                controller.handleRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
        Map model = mov.getModel();
        assertEquals(Boolean.TRUE, model.get("forceBuildEnabled"));
    }

    public void testShouldUseCachedLatestBuildSummaries() throws Exception {
        controller.handleRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
        mockBuildSummaryService =
                mock(BuildSummariesService.class,
                        new Class[] {Configuration.class, BuildSummaryService.class}, new Object[] {null,
                                new BuildSummaryService()});
        mockBuildSummaryService.expects(never()).method("getLatestOfProjects").withNoArguments().will(
                returnValue(returnedValue()));
        mockBuildSummaryService.expects(never()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(ealiestFailed));
        mockBuildSummaryService.expects(never()).method("getLastSucceed").withAnyArguments().will(
                returnValue(lastSucceed));
        controller.handleRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    private List returnedValue() {
        List list = new ArrayList();
        BuildSummary build1 =
                new BuildSummary("project1", "2005-12-09 12:21.03", "build1", ProjectBuildStatus.PASSED,
                        "log1");
        Build build2 =
                new BuildSummary("project2", "2005-12-09 12:21.03", "", ProjectBuildStatus.FAILED, "log2");
        BuildSummary build3 =
                new BuildSummary("project3", "2005-12-09 12:21.03", "", ProjectBuildStatus.BUILDING, "log3");
        BuildSummary build4 =
                new BuildSummary("project4", "2005-12-09 12:21.03", "", ProjectBuildStatus.INACTIVE, "log4");
        BuildSummary build5 =
                new BuildSummary("project5", "2005-12-09 12:21.03", "", ProjectBuildStatus.INACTIVE, "log5");
        list.add(build1);
        list.add(build2);
        list.add(build3);
        list.add(build4);
        list.add(build5);
        return list;
    }

}
