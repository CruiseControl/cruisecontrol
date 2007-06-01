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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class UpdateConfigXmlLocationControllerTest extends TestCase {

    private String newConfigFilePath;

    private File configFile;

    private Configuration configuration;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    protected void setUp() throws Exception {
        configuration = new Configuration(new ConfigXmlFileService(new EnvironmentService()));
        configFile = DataUtils.createDefaultCCConfigFile();
        configuration.setCruiseConfigLocation(configFile.getPath());
        newConfigFilePath = DataUtils.createTempFile("config", ".xml").toString();
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        response = new MockHttpServletResponse();
    }

    protected void tearDown() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_EDITABLE, "");
    }

    public void testShouldShowFormViewWithSuccessMessageAfterSetLocation() throws Exception {
        request.setParameter("configFileLocation", newConfigFilePath);
        UpdateConfigXmlLocationController controller =
                new UpdateConfigXmlLocationController(configuration, new EnvironmentService());
        assertEquals(configFile.getAbsolutePath(), configuration.getCruiseConfigLocation());
        ModelAndView mov = controller.handleRequest(request, response);
        assertEquals("redirect:/admin/config", mov.getViewName());
        assertTrue(StringUtils.contains((String) mov.getModel().get("location_flash_message"),
                "Configuration file has been set successfully."));
        assertEquals(newConfigFilePath, configuration.getCruiseConfigLocation());
    }

    public void testShouldNotUpdateConfigXmlIfConfigXmlEditableIsFalse() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_EDITABLE, "false");
        request.setParameter("configFileLocation", newConfigFilePath);
        UpdateConfigXmlLocationController controller =
                new UpdateConfigXmlLocationController(configuration, new EnvironmentService());
        controller.handleRequest(request, response);
        assertFalse(newConfigFilePath.equals(configuration.getCruiseConfigLocation()));
    }

    public void testShouldShowFormViewWithErrorMessageIfConfigFilePathIsBlank() throws Exception {
        request.setParameter("configFileLocation", "");
        UpdateConfigXmlLocationController controller =
                new UpdateConfigXmlLocationController(configuration, new EnvironmentService());
        assertEquals(configFile.getAbsolutePath(), configuration.getCruiseConfigLocation());
        ModelAndView view = controller.handleRequest(request, response);
        assertEquals("page_admin", view.getViewName());
        assertEquals(configFile.getAbsolutePath(), configuration.getCruiseConfigLocation());
    }
}
