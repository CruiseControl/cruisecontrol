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

import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetProjectBuildOutputControllerTest extends MockObjectTestCase {

    private Mock serviceMock;

    private GetProjectBuildOutputController controller;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    protected void setUp() throws Exception {
        serviceMock =
                mock(BuildLoopQueryService.class,
                        new Class[] {EnvironmentService.class, BuildInformationRepository.class},
                        new Object[] {null, null});
        controller = new GetProjectBuildOutputController((BuildLoopQueryService) serviceMock.proxy());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    public void testShouldReturnBuildOutputAsPlainText() throws Exception {

        final String[] output = new String[] {"Build succeeded.\n"};

        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(2))).will(
                returnValue(output));

        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue("LiveOutputIDValue"));

        request.setParameter("project", "project1");
        request.setParameter("start", "2");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID,
                GetProjectBuildOutputController.DEFAULT_OUTPUT_ID);
        controller.handleRequest(request, response);

        assertEquals("text/plain", response.getContentType());
        assertEquals(StringUtils.join(output, "\n") + "\n", response.getContentAsString());
    }

    public void testShouldStartAtBeginningWhenNoStartParameterIsGiven() throws Exception {
        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(0))).will(
                returnValue(new String[] {"Doesn't matter"}));

        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue("LiveOutputIDValue"));

        request.setParameter("project", "project1");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID,
                GetProjectBuildOutputController.DEFAULT_OUTPUT_ID);
        controller.handleRequest(request, response);
    }

    public void testShouldReturnNextStartLine() throws Exception {
        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(500))).will(
                returnValue(new String[] {"1", "2", "3"}));

        final String liveOutputID = "LiveOutputIDValue";
        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue(liveOutputID));

        request.setParameter("project", "project1");
        request.setParameter("start", "500");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID,
                GetProjectBuildOutputController.DEFAULT_OUTPUT_ID);
        controller.handleRequest(request, response);

        String nextStartLine = (String) response.getHeader("X-JSON");
        assertEquals("[503, \"" + liveOutputID + "\"]", nextStartLine);
    }

    public void testNextLineShouldEqualsStartLineWhenNoOutputReturnsSameOutputID() throws Exception {
        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(500))).will(
                returnValue(new String[] {}));

        final String liveOutputID = "LiveOutputIDValue";
        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue(liveOutputID));

        request.setParameter("project", "project1");
        request.setParameter("start", "500");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID, liveOutputID);
        controller.handleRequest(request, response);

        String nextStartLine = (String) response.getHeader("X-JSON");
        assertEquals("[500, \"" + liveOutputID + "\"]", nextStartLine);
    }

    public void testNextLineShouldEqualsStartLineWhenNoOutputReturnsNewOutputID() throws Exception {
        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(500))).will(
                returnValue(new String[] {}));

        final String newID = "NewID";
        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue(newID));

        request.setParameter("project", "project1");
        request.setParameter("start", "500");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID, "OldID");
        controller.handleRequest(request, response);

        String nextStartLine = (String) response.getHeader("X-JSON");
        assertEquals("[0, \"" + newID + "\"]", nextStartLine);
    }

    public void testShouldGetNewOutputIDWhenNoOldOutputIDParameterAndSomeOutputLines() throws Exception {
        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(500))).will(
                returnValue(new String[] {"1", "2", "3"}));

        final String newID = "NewID";
        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue(newID));

        request.setParameter("project", "project1");
        request.setParameter("start", "500");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID,
                GetProjectBuildOutputController.DEFAULT_OUTPUT_ID);
        controller.handleRequest(request, response);

        String nextStartLine = (String) response.getHeader("X-JSON");
        assertEquals("[503, \"" + newID + "\"]", nextStartLine);
    }

    public void testShouldReturnZeroStartWhenOutputIsEmptyAndIDChanged() throws Exception {
        assertEquals(0, controller.calculateNextStart("id", "id2", 10, null));
        assertEquals(0, controller.calculateNextStart("id", "id2", 10, new String[0]));
    }

    public void testShouldReturnSameStartWhenOutputIsEmpty() throws Exception {
        assertEquals(10, controller.calculateNextStart(null, null, 10, null));
        assertEquals(10, controller.calculateNextStart(null, null, 10, new String[0]));
    }

    public void testShouldReturnNextStartLineEvenSkipSomeLines() throws Exception {
        serviceMock.expects(once()).method("getBuildOutput").with(eq("project1"), eq(new Integer(500))).will(
                returnValue(new String[] {"Skipped 2 lines", "1", "2", "3"}));

        final String liveOutputID = "LiveOutputIDValue";
        serviceMock.expects(once()).method("getLiveOutputID").with(eq("project1")).will(
                returnValue(liveOutputID));

        request.setParameter("project", "project1");
        request.setParameter("start", "500");
        request.setParameter(GetProjectBuildOutputController.PARAM_OUTPUT_ID,
                GetProjectBuildOutputController.DEFAULT_OUTPUT_ID);
        controller.handleRequest(request, response);

        String nextStartLine = (String) response.getHeader("X-JSON");
        assertEquals("[505, \"" + liveOutputID + "\"]", nextStartLine);
    }
}
