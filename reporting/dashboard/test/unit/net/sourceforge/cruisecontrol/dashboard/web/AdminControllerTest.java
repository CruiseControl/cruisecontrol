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

import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.SystemService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ConfigurationCommand;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class AdminControllerTest extends MockObjectTestCase {
    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private Mock configurationMock;

    private ConfigurationService configuration;

    private AdminController controller;

    private File logsRoot = new File("logs_root");

    private File artifactsRoot = new File("artifacts_root");

    private String expectedPath = "test/data/config.xml";

    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        configurationMock = mock(ConfigurationService.class, new Class[] {
                EnvironmentService.class, DashboardXmlConfigService.class,
                BuildLoopQueryService.class },
                new Object[] { null, null, null });
        configuration = (ConfigurationService) configurationMock.proxy();
        controller = new AdminController(configuration, new SystemService());

        configurationMock.expects(once()).method("getLogsRoot").will(
                returnValue(logsRoot));
        configurationMock.expects(once()).method("getArtifactsRoot").will(
                returnValue(artifactsRoot));
        configurationMock.expects(once()).method("isForceBuildEnabled").will(
                returnValue(true));
    }

    public void testViewNameShouldBeAdminWhenGetIt() throws Exception {
        configurationMock.expects(once()).method("getDashboardConfigLocation")
        .will(returnValue(expectedPath));
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals("page_admin", mov.getViewName());
    }

    public void testShouldShowExistingConfigFileLocationAndContentIfItHasBeenSet()
            throws Exception {
        configurationMock.expects(once()).method("getDashboardConfigLocation")
        .will(returnValue(expectedPath));
        ModelAndView mov = controller.handleRequest(request, response);
        ConfigurationCommand command = (ConfigurationCommand) mov.getModel()
                .get("command");
        assertEquals(expectedPath, command.getConfigFileLocation());
    }

    public void testShouldHasDiagnosticsInformation() throws Exception {
        configurationMock.expects(once()).method("getDashboardConfigLocation")
        .will(returnValue(expectedPath));
        Mock systemServiceMock = mock(SystemService.class);
        SystemService systemService = (SystemService) systemServiceMock.proxy();
        controller = new AdminController(configuration, systemService);
        systemServiceMock.expects(once()).method("getJvmVersion").will(
                returnValue("1.5"));
        systemServiceMock.expects(once()).method("getOsInfo").will(
                returnValue("Linux"));
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals("1.5", mov.getModel().get("jvm_version"));
        assertEquals("Linux", mov.getModel().get("os_info"));
        assertEquals("Yes", mov.getModel().get("forcebuild_enabled"));

        // lower case the next two comparison because of cygwin
        // otherwise you get inconsistent casing of the drive letters.
        assertEquals(logsRoot.getAbsolutePath().toLowerCase(), mov.getModel().get("logs_root").toString().toLowerCase());
        assertEquals(artifactsRoot.getAbsolutePath().toLowerCase(), mov.getModel().get("artifacts_root").toString().toLowerCase());
    }
    
    public void testShouldShowErrorMessageWhenConfigFileIsNotSpecified() throws Exception {
        configurationMock.expects(once()).method("getDashboardConfigLocation")
        .will(returnValue(null));
        Mock systemServiceMock = mock(SystemService.class);
        SystemService systemService = (SystemService) systemServiceMock.proxy();
        controller = new AdminController(configuration, systemService);
        systemServiceMock.expects(once()).method("getJvmVersion").will(
                returnValue("1.5"));
        systemServiceMock.expects(once()).method("getOsInfo").will(
                returnValue("Linux"));
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals("Configuration file is not specified!", mov.getModel().get("error_message"));
    }
    
    public void testShouldShowErrorMessageWhenConfigFileDoesNotExist() throws Exception {
        configurationMock.expects(once()).method("getDashboardConfigLocation")
        .will(returnValue("not exist"));
        Mock systemServiceMock = mock(SystemService.class);
        SystemService systemService = (SystemService) systemServiceMock.proxy();
        controller = new AdminController(configuration, systemService);
        systemServiceMock.expects(once()).method("getJvmVersion").will(
                returnValue("1.5"));
        systemServiceMock.expects(once()).method("getOsInfo").will(
                returnValue("Linux"));
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals(AdminController.ERROR_MESSAGE_NOT_EXIST, mov.getModel().get("error_message"));
    }
}
