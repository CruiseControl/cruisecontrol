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
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class BuildListingControllerTest extends MockObjectTestCase {
    private BuildListingController controller;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private Mock mockBuildSummaries;

    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.setRequestURI("/list/passed/project1");
        mockBuildSummaries = mock(BuildSummariesService.class,
                new Class[]{Configuration.class, BuildSummaryService.class}, new Object[]{null, null});
        BuildSummariesService serivce = (BuildSummariesService) mockBuildSummaries.proxy();
        controller = new BuildListingController(serivce);
    }

    public void testShouldReturnAllSucceedBuilds() throws Exception {
        mockBuildSummaries.expects(once()).method("getAll").with(eq("project1")).will(returnValue(new ArrayList()));
        ModelAndView mv = controller.all(request, response);
        Map dataModel = mv.getModel();
        assertEquals(0, ((List) dataModel.get("buildSummaries")).size());
        assertEquals("project1", (String) dataModel.get("projectName"));
    }

    public void testShouldReturnAllBuilds() throws Exception {
        mockBuildSummaries.expects(once()).method("getAllSucceed").with(eq("project1"))
                .will(returnValue(new ArrayList()));
        ModelAndView mv = controller.passed(request, response);
        Map dataModel = mv.getModel();
        assertEquals(0, ((List) dataModel.get("buildSummaries")).size());
        assertEquals("project1", (String) dataModel.get("projectName"));
    }
}
