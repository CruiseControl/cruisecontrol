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

import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.CruiseControlOptions;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

public class ForceBuildControllerTest extends MockObjectTestCase {

    private static final String PROJECT_NAME = "project";

    private Mock buildLoopQueryService;
    private Mock buildInformationRepository;

    private ForceBuildController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Override
    protected void setUp() throws Exception {

        // Initialize the config (will be used to get config by BuildLoopInformationBuilder)
        final CruiseControlOptions conf = CruiseControlOptions.getInstance(PROJECT_NAME);
        conf.setOption(CruiseControlOptions.KEY_RMI_PORT, "9090", PROJECT_NAME);


        BuildLoopInformation.JmxInfo jmxinfo = new BuildLoopInformation.JmxInfo("cruise.example.com");
        BuildLoopInformation buildLoopInfo = new BuildLoopInformation(new BuildLoopInformation.ProjectInfo[0], jmxinfo,
                null, null);

        buildLoopQueryService = mock(BuildLoopQueryService.class);
        buildInformationRepository = mock(BuildInformationRepository.class);
        buildInformationRepository.stubs().method("getBuildLoopInfo").with(eq(PROJECT_NAME))
                .will(returnValue(buildLoopInfo));
        controller = new ForceBuildController((BuildLoopQueryService) buildLoopQueryService.proxy(),
                (BuildInformationRepository) buildInformationRepository.proxy());

        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addParameter("projectName", PROJECT_NAME);

        response = new MockHttpServletResponse();
    }

    @Override
    protected void tearDown() throws Exception {
        CruiseControlOptions.delInstance(PROJECT_NAME);
    }

    public void testShouldForceBuildAndReturnMessage() throws Exception {
        buildLoopQueryService.expects(atLeastOnce()).method("forceBuild").with(eq(PROJECT_NAME));

        assertEquals("Your build is scheduled", render(controller.handleRequest(request, response)));
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    public void testOnFailureShouldReturnMessageWithServerAndPort() throws Exception {
        buildLoopQueryService.expects(atLeastOnce()).method("forceBuild").with(eq(PROJECT_NAME))
                .will(throwException(new IOException()));

        assertEquals("Error communicating with build loop on: rmi://cruise.example.com:9090",
                render(controller.handleRequest(request, response)));
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    private String render(ModelAndView modelAndView) throws Exception {
        View view = modelAndView.getView();
        view.render(null, null, response);

        return response.getContentAsString();
    }
}
