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

import java.io.File;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class DownloadLogControllerTest extends MockObjectTestCase {
    private DownloadController controller;

    private static final String LOG_FILE = "log20051209122103Lbuild.489.xml";

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private Mock configurationMock;

    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.setRequestURI("/download/log/project1/" + LOG_FILE);
        configurationMock =
            mock(ConfigurationService.class, new Class[] {EnvironmentService.class,
                DashboardXmlConfigService.class, BuildLoopQueryService.class}, new Object[] {null,
                null, null});
        controller = new DownloadController((ConfigurationService) configurationMock.proxy());
    }

    private MockHttpServletRequest getRequest() {
        return request;
    }

    private HttpServletResponse getResponse() {
        return response;
    }

    public void testShouldRenderFileViewIfTargetFileExistsAndCanBeRead() throws Exception {
        File projectLogRoot = DataUtils.getProjectLogDirAsFile("project1");
        configurationMock.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(projectLogRoot));
        configurationMock.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(projectLogRoot));
        ModelAndView mov = this.controller.log(getRequest(), getResponse());
        assertEquals("fileView", mov.getViewName());
        assertEquals(new File(projectLogRoot, LOG_FILE), mov.getModel().get("targetFile"));
    }

    public void testShouldRenderDownloadViewIfPathContainsWhiteSpace() throws Exception {
        File projectLogRoot = DataUtils.getProjectLogDirAsFile("project space");
        getRequest().setRequestURI("/download/log/project%20space/log20051209122104Lbuild.467.xml");
        configurationMock.expects(once()).method("getLogRoot").with(eq("project space")).will(
                returnValue(projectLogRoot));
        configurationMock.expects(once()).method("getLogRoot").with(eq("project space")).will(
                returnValue(projectLogRoot));
        ModelAndView mov = this.controller.log(getRequest(), getResponse());
        File logfile = new File(projectLogRoot, "log20051209122104Lbuild.467.xml");
        assertEquals(logfile, mov.getModel().get("targetFile"));
    }

    public void testShouldRenderErrorPageIfFileNotExist() throws Exception {
        getRequest().setRequestURI("/download/log/project1/IDontExist");
        configurationMock.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(new File("IDontExist")));
        ModelAndView mov = this.controller.log(getRequest(), getResponse());
        assertEquals("page_error", mov.getViewName());
        assertEquals("File does not exist.", mov.getModel().get("errorMessage"));
    }
}
