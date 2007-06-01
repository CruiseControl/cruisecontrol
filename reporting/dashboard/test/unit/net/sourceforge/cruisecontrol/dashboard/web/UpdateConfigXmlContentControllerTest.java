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

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.apache.commons.io.FileUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class UpdateConfigXmlContentControllerTest extends TestCase {
    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private final String configFileContent = "<cruisecontrol><project name=\"project2\"/></cruisecontrol>\n";

    private Configuration configuration;

    private File configFile;

    protected void setUp() throws Exception {
        configuration = new Configuration(new ConfigXmlFileService(new EnvironmentService()));
        configFile = DataUtils.createDefaultCCConfigFile();
        configuration.setCruiseConfigLocation(configFile.getPath());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("POST");
    }

    public void testShouldShowFormViewWithSuccessMessageAfterUpdatingConfig() throws Exception {
        request.addParameter("configFileContent", configFileContent);
        assertFalse(configFileContent.equals(FileUtils.readFileToString(configFile, null)));
        UpdateConfigXmlContentController controller =
                new UpdateConfigXmlContentController(configuration, new EnvironmentService());
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals("redirect:/admin/config", mov.getViewName());
        Assert.assertEquals(UpdateConfigXmlContentController.CONFIGURATION_HAS_BEEN_UPDATED_SUCCESSFULLY, mov
                .getModel().get("edit_flash_message"));
        assertTrue(configFileContent.equals(FileUtils.readFileToString(configFile, null)));
    }

    public void testShouldShowFormViewWithErrorMessageIfConfigFileIsNotValidXML() throws Exception {
        request.addParameter("configFileContent", "some thing wrong");
        assertFalse("some thing wrong".equals(FileUtils.readFileToString(configFile, null)));
        UpdateConfigXmlContentController controller =
                new UpdateConfigXmlContentController(configuration, new EnvironmentService());
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals("page_admin", mov.getViewName());
        // TODO
        // assertEquals(AdminController.CONFIGURATION_HAS_BEEN_UPDATED_SUCCESSFULLY,
        // mov.getModel().get("message"));
        assertFalse("some thing wrong".equals(FileUtils.readFileToString(configFile, null)));
    }
}
