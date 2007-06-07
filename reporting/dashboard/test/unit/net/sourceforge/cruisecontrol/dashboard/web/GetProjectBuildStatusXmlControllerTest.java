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

import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;

public class GetProjectBuildStatusXmlControllerTest extends MockObjectTestCase {
    private BuildSummariesService buildSummariesService;

    private GetProjectBuildStatusXmlController controller;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private Mock mockBuildSummaryService;

    private BuildSummary oneBuild;

    protected void setUp() throws Exception {
        oneBuild =
                new BuildSummary("project1", "2005-12-09 12:21.03", "build1",
                        ProjectBuildStatus.PASSED, "log1");
        mockBuildSummaryService =
                mock(BuildSummariesService.class, new Class[] {Configuration.class,
                        BuildSummaryService.class}, new Object[] {null, null});
        buildSummariesService = (BuildSummariesService) mockBuildSummaryService.proxy();
        CruiseControlJMXService cruisecontrolJMXService =
                new CruiseControlJMXService(null, new EnvironmentService()) {
                    public Map getAllProjectsStatus() {
                        Map map = new HashMap();
                        map.put("project1", "now building since");
                        return map;
                    }
                };
        controller =
                new GetProjectBuildStatusXmlController(buildSummariesService,
                        cruisecontrolJMXService, new BuildSummaryUIService(buildSummariesService));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
    }

    private List returnedValue() {
        List list = new ArrayList();
        BuildSummary build2 =
                new BuildSummary("project2", "2005-12-09 12:21.03", "build2",
                        ProjectBuildStatus.INACTIVE, "log2");
        list.add(oneBuild);
        list.add(build2);
        return list;
    }

    public void testShouldReturnAllFieldsInCCTrayFormat() throws Exception {
        mockBuildSummaryService.expects(once()).method("getLatestOfProjects").withNoArguments()
                .will(returnValue(returnedValue()));
        request.setRequestURI("/dashboard/cctray.xml");
        controller.handleRequest(request, response);
        String xml = response.getContentAsString();
        assertFalse(StringUtils.contains(xml, "name=\"project2\""));
        assertFalse(StringUtils.contains(xml, "lastBuildLabel=\"build2\""));
    }

    public void testShouldReturnAllFieldsInRssFormat() throws Exception {
        mockBuildSummaryService.expects(once()).method("getLatestOfProjects").withNoArguments()
                .will(returnValue(returnedValue()));
        request.setRequestURI("/dashboard/rss.xml");
        controller.handleRequest(request, response);
        String xml = response.getContentAsString();
        assertTrue(StringUtils.contains(xml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(StringUtils.contains(xml, "<rss version=\"2.0\">"));
        assertTrue(StringUtils.contains(xml, "<channel>"));
        assertTrue(StringUtils.contains(xml, "<title>CruiseControl Results</title>"));
        assertTrue(StringUtils.contains(xml, "<link>http://localhost:80/dashboard/</link>"));
        assertTrue(StringUtils.contains(xml, "<language>en-us</language>"));
        assertTrue(StringUtils.contains(xml, "</channel>"));
        assertTrue(StringUtils.contains(xml, "<channel>"));
        assertTrue(StringUtils.contains(xml, "</rss>"));
    }

    public void testShouldReturnSpecificRssFormat() throws Exception {
        mockBuildSummaryService.expects(once()).method("getLatest").with(eq("project1")).will(
                returnValue(oneBuild));
        request.setRequestURI("/dashboard/rss.xml");
        request.addParameter("projectName", "project1");
        controller.handleRequest(request, response);
        String xml = response.getContentAsString();
        assertTrue(StringUtils.contains(xml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(StringUtils.contains(xml, "<rss version=\"2.0\">"));
        assertTrue(StringUtils.contains(xml, "<channel>"));
        assertTrue(StringUtils.contains(xml, "<title>CruiseControl Results</title>"));
        assertTrue(StringUtils.contains(xml, "<link>http://localhost:80/dashboard/</link>"));
        assertTrue(StringUtils.contains(xml, "<language>en-us</language>"));
        assertTrue(StringUtils.contains(xml, "</channel>"));
        assertTrue(StringUtils.contains(xml, "<channel>"));
        assertTrue(StringUtils.contains(xml, "</rss>"));

        assertTrue(StringUtils.contains(xml, "project1"));
        assertFalse(StringUtils.contains(xml, "project2"));
    }

    public void testPostMethodIsNotAllowed() throws Exception {
        request.setMethod("POST");
        try {
            controller.handleRequest(request, response);
            fail("exception exptected");
        } catch (HttpRequestMethodNotSupportedException e) {
            // Pass
        }
    }
}
