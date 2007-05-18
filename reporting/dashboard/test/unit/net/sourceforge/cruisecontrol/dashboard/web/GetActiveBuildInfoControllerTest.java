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
import java.util.List;
import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jdelegator.JDelegator;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.CruiseControlJMXServiceForcedBuildStub;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.CruiseControlJMXServiceStub;
import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class GetActiveBuildInfoControllerTest extends MockObjectTestCase {

    private CruiseControlJMXServiceStub jmxStub;
    private GetActiveBuildInfoController controller;
    private CruiseControlJMXService jmxService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private BuildSummariesService buildSummariesService;

    protected void setUp() throws Exception {
        List allBuilds = Arrays.asList(new Build[]{new BuildSummary("", "", "", ProjectBuildStatus.PASSED, "")});
        Mock mockBuildSummariesService = mock(BuildSummariesService.class,
                new Class[]{Configuration.class, BuildSummaryService.class}, new Object[]{null, null});
        buildSummariesService = (BuildSummariesService) mockBuildSummariesService.proxy();
        mockBuildSummariesService.expects(once()).method("getLastest25").withAnyArguments()
                .will(returnValue(allBuilds));
        mockBuildSummariesService.expects(once()).method("getDurationFromLastSuccessfulBuild")
                .withAnyArguments().will(returnValue("1 days 3 hours"));
        jmxStub = new CruiseControlJMXServiceStub();
        jmxService = (CruiseControlJMXService) JDelegator.delegate(CruiseControlJMXService.class).to(jmxStub);
        controller = new GetActiveBuildInfoController(jmxService, buildSummariesService,
                new BuildSummaryUIService(buildSummariesService));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.setRequestURI("/project/connectfour");
    }

    protected void tearDown() throws Exception {
        jmxStub.getBuildStatus("");
    }

    public void testShouldReturnCommitMessageInBuildingStatusAndCommitMessagesIsNotEmpty() throws Throwable {
        fakeJMXReturnBuildingAsStatus();
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        List messages = (List) model.get("commitMessages");
        ModificationKey key = (ModificationKey) messages.get(0);
        assertEquals("joe", key.getUser());
        assertEquals("Some random change", key.getComment());
        assertEquals("buildDetail", mov.getViewName());
        assertEquals("connectfour", model.get("projectName"));
        assertEquals(ProjectBuildStatus.BUILDING.getStatus(), model.get("status"));
    }

    public void testShouldReturnDurationFromLastSuccessfulBuildInBuildingStatus() throws Throwable {
        fakeJMXReturnBuildingAsStatus();
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        assertTrue(StringUtils.contains((String) model.get("durationToSuccessfulBuild"), "1 days 3 hours"));
    }

    public void testShouldReturnBuildSince() throws Throwable {
        fakeJMXReturnBuildingAsStatus();
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        assertNotNull(model.get("buildSince"));
        assertFalse(StringUtils.contains((String) model.get("buildSince"), "N/A"));
    }

    public void testShouldReturnLastBuildSummaries() throws Throwable {
        fakeJMXReturnBuildingAsStatus();
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        assertNotNull(model.get("summaries"));
        assertEquals(1, ((Collection) model.get("summaries")).size());
    }

    private void fakeJMXReturnBuildingAsStatus() throws Exception {
        jmxStub.fourceBuild("connectfour");
        jmxStub.getBuildStatus("connectfour");
        jmxStub.getBuildStatus("connectfour");
    }

    public void testShouldReturnCommitMessageInWaitingStatus() throws Exception {
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        assertEquals("Waiting for checking in", model.get("flash_message"));
        assertEquals("Waiting", model.get("status"));
    }

    public void testShouldReturnProjectObject() throws Exception {
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        assertEquals("connectfour", (String) model.get("projectName"));
    }

    public void testShouldReturnTextThatBuildIsForcedBuildingStatusWhenCommitMessagesIsEmpty() throws Throwable {
        CruiseControlJMXService stub = new CruiseControlJMXServiceForcedBuildStub();
        CruiseControlJMXService service = (CruiseControlJMXService) JDelegator.delegate(CruiseControlJMXService.class)
                .to(stub);
        controller = new GetActiveBuildInfoController(service, buildSummariesService,
                new BuildSummaryUIService(buildSummariesService));
        ModelAndView mov = controller.handleRequest(request, response);
        Map model = mov.getModel();
        assertEquals("Build forced, No new code is committed into repository", model
                .get("flash_message"));

    }

}
