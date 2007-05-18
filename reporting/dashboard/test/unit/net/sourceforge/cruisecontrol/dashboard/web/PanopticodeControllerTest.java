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
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import org.apache.commons.lang.StringUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class PanopticodeControllerTest extends TestCase {
    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private PanopticodeController panopticode = new PanopticodeController(new ConfigurationStub());

    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    public void testShouldReturnSvgfileIfcanFindPicture() throws Exception {
        request.setRequestURI("/project1/coverage");
        panopticode.handleRequest(request, response);
        assertEquals("image/svg+xml", response.getContentType());
        assertTrue(StringUtils.contains(response.getContentAsString(), "<svg"));
    }

    public void testCouldGetBothCodeCoverageAndCodeComplexityPicture() throws Exception {
        request.setRequestURI("/project1/coverage");
        panopticode.handleRequest(request, response);
        assertTrue(StringUtils.contains(response.getContentAsString(), "Coverage"));

        request.setRequestURI("/project1/complexity");
        panopticode.handleRequest(request, response);
        assertTrue(StringUtils.contains(response.getContentAsString(), "Complexity"));
    }

    public void testShouldReturnErrorMessageInPlainTextIfProjectNotExist() throws Exception {
        request.setRequestURI("/not.exist/coverage");
        panopticode.handleRequest(request, response);
        assertEquals("text/plain", response.getContentType());
        assertTrue(StringUtils.contains(response.getContentAsString(), "No panopticode output"));
    }

    public void testShouldReturnErrorMessageInPlainTextIfNoProperCategory() throws Exception {
        request.setRequestURI("/project1/not.supported.category");
        panopticode.handleRequest(request, response);
        assertEquals("text/plain", response.getContentType());
        assertTrue(StringUtils.contains(response.getContentAsString(), "No panopticode output"));
    }

    public void testErrorMessageMustBePresentIfPictureNotFound() throws Exception {
        PanopticodeController controller = new PanopticodeController(new DifferentConfigLocationConfigurationStub());
        request.setRequestURI("/project1/coverage");
        controller.handleRequest(request, response);
        assertEquals("text/plain", response.getContentType());
        assertTrue(StringUtils.contains(response.getContentAsString(), "No panopticode output"));
    }

    private static class ConfigurationStub extends Configuration {

        public ConfigurationStub() {
            super(new ConfigXmlFileService(new OSEnvironment()));
        }

        public String getCruiseConfigDirLocation() {
            try {
                return DataUtils.getConfigXmlAsFile().getParent();
            } catch (Exception e) {
                return "";
            }
        }

        public boolean hasProject(String projectName) {
            return "project1".equals(projectName);
        }
    }

    private static class DifferentConfigLocationConfigurationStub extends ConfigurationStub {
        public String getCruiseConfigDirLocation() {
            return new File("any").getAbsolutePath();
        }
    }
}
