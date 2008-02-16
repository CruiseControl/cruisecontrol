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

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigFileFactory;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.web.view.JsonView;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GetCommitMessageControllerTest extends MockObjectTestCase {
    private MockHttpServletRequest request = new MockHttpServletRequest();

    private MockHttpServletResponse response = new MockHttpServletResponse();

    private Mock buildLoopQueryServiceMock =
            mock(BuildLoopQueryService.class, 
                 new Class[] {EnvironmentService.class, BuildInformationRepository.class},
                 new Object[] {new EnvironmentService(new DashboardConfigService[] {}), null});

    private Mock dashboardConfigMock =
            mock(DashboardXmlConfigService.class, new Class[] {DashboardConfigFileFactory.class},
                    new Object[] {null});

    private GetCommitMessageController controller =
            new GetCommitMessageController((BuildLoopQueryService) buildLoopQueryServiceMock.proxy(),
                    (DashboardXmlConfigService) dashboardConfigMock.proxy());

    public void testJSONObjectTypeShouldBeArray() throws Exception {
        buildLoopQueryServiceMock.expects(once()).method("getCommitMessages").with(eq("project1")).will(
                returnValue(Arrays.asList(new ModificationKey[] {})));
        dashboardConfigMock.expects(never()).method("getStoryTrackers").will(returnValue(new HashMap()));
        request.setParameter("project", "project1");

        controller.handleRequest(request, response);

        String json = response.getContentAsString();
        assertEquals("", json);
    }

    public void testShouldReturnEmptyArrayIfThereIsNoCommitMessages() throws Exception {
        buildLoopQueryServiceMock.expects(once()).method("getCommitMessages").with(eq("project1")).will(
                returnValue(Arrays.asList(new ModificationKey[] {})));
        dashboardConfigMock.expects(never()).method("getStoryTrackers").will(returnValue(new HashMap()));
        request.setParameter("project", "project1");

        controller.handleRequest(request, response);

        String json = response.getContentAsString();
        assertEquals("", json);
    }

    public void testJSONObjectShouldHasUserAndMessageProperty() throws Exception {
        buildLoopQueryServiceMock.expects(once()).method("getCommitMessages").with(eq("project1")).will(
                returnValue(Arrays.asList(new Modification[] {createModification("joe", "add new feature"),
                        createModification("joe", "update build")})));
        dashboardConfigMock.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        request.setParameter("project", "project1");

        String json  = getResponse(controller.handleRequest(request, response));
        assertTrue(StringUtils.contains(json, "{"));
        assertTrue(StringUtils.contains(json, "user"));
        assertTrue(StringUtils.contains(json, "joe"));
        assertTrue(StringUtils.contains(json, "build"));
    }

    public Modification  createModification(String username, String comment) {
        Modification m1 = new Modification();
        m1.userName = username;
        m1.comment = comment;
        m1.modifiedTime = new Date();
        return m1;
    }

    public void testShouldContainHyperlinkIfConfiguredStoryTracker() throws Exception {
        buildLoopQueryServiceMock.expects(once()).method("getCommitMessages").with(eq("project_with_story_tracker"))
                .will(
                        returnValue(Arrays.asList(new Modification[] {
                                createModification("joe", "add new feature"),
                                createModification("joe", "update build456")})));
        Map expectedMap = new HashMap();
        StoryTracker expectedStoryTracker =
                new StoryTracker("project_with_story_tracker", "http://abc/", "build,bug");
        expectedMap.put("project_with_story_tracker", expectedStoryTracker);
        dashboardConfigMock.expects(once()).method("getStoryTrackers").will(returnValue(expectedMap));
        request.setParameter("project", "project_with_story_tracker");

        String json = getResponse(controller.handleRequest(request, response));
        assertTrue(StringUtils.contains(json, "{"));
        String escaped = StringEscapeUtils.escapeJavaScript("<a href=\"http://abc/456\">");
        assertTrue(StringUtils.contains(json, escaped));
        assertTrue(StringUtils.contains(json, "user"));
        assertTrue(StringUtils.contains(json, "}"));
    }


    private String getResponse(ModelAndView mov) throws Exception {
        JsonView jsonView = (JsonView) mov.getView();
        jsonView.render(mov.getModel(), request, response);
        return response.getContentAsString();
    }
}
