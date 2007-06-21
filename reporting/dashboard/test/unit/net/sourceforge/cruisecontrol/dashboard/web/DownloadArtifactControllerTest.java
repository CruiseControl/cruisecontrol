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

import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class DownloadArtifactControllerTest extends MockObjectTestCase {
    private DownloadController controller;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private Mock configurationMock;

    private File artifactsRoot;

    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        configurationMock =
                mock(Configuration.class, new Class[] {ConfigXmlFileService.class},
                        new Object[] {null});
        controller = new DownloadController((Configuration) configurationMock.proxy());
        artifactsRoot = DataUtils.getProject1ArtifactDirAsFile();
    }

    private void prepareRequest(String artifacts) {
        getRequest().setRequestURI(
                "/download/artifacts/project1/log20051209122103Lbuild.489.xml/" + artifacts);
    }

    private MockHttpServletRequest getRequest() {
        return request;
    }

    private HttpServletResponse getResponse() {
        return response;
    }

    public void testShouldRenderDownloadViewIfTargetFileExistsAndCanBeRead() throws Exception {
        prepareRequest("artifact1.txt");
        configurationMock.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(artifactsRoot));
        configurationMock.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(artifactsRoot));
        ModelAndView mov = this.controller.artifacts(getRequest(), getResponse());
        assertEquals("fileView", mov.getViewName());
        File targetFile =
                new File(artifactsRoot, "20051209122103" + File.separator + "artifact1.txt");
        assertEquals(targetFile.getAbsolutePath(), ((File) mov.getModel().get("targetFile"))
                .getAbsolutePath());
    }

    public void testShouldRenderDownloadViewIfDirectoryExists() throws Exception {
        prepareRequest("subdir");
        configurationMock.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(artifactsRoot));
        configurationMock.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(artifactsRoot));
        ModelAndView mov = this.controller.artifacts(getRequest(), getResponse());
        assertEquals("directoryView", mov.getViewName());
    }

    public void testShouldRenderErrorPageIfFileNotExist() throws Exception {
        prepareRequest("IDontExist");
        configurationMock.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(artifactsRoot));
        ModelAndView mov = this.controller.artifacts(getRequest(), getResponse());
        assertEquals("page_error", mov.getViewName());
        assertEquals("File does not exist.", mov.getModel().get("errorMessage"));
    }

}
