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
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import org.springframework.web.servlet.ModelAndView;

public class DownloadArtifactControllerTest extends SpringBasedControllerTests {
    private DownloadController controller;

    private Configuration configuration;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setDownloadController(DownloadController controller) {
        this.controller = controller;
    }

    protected void onControllerSetup() throws Exception {
        super.onControllerSetup();
        configuration.setCruiseConfigLocation(DataUtils.getConfigXmlAsFile().getAbsolutePath());
        String logDirPath = DataUtils.getProjectLogDirAsFile().getAbsolutePath();
        configuration.setCruiseLogfileLocation(logDirPath);
    }

    private void prepareRequest(String artifacts) {
        getRequest().setMethod("GET");
        getRequest().setRequestURI(
                "/download/artifacts/project1/log20051209122103Lbuild.489.xml/" + artifacts);
    }

    public void testShouldRenderDownloadViewIfTargetFileExistsAndCanBeRead() throws Exception {
        prepareRequest("artifact1.txt");
        ModelAndView mov = this.controller.handleRequest(getRequest(), getResponse());
        assertEquals("downloadBinView", mov.getViewName());
        File targetFile =
                new File(DataUtils.getProject1ArtifactDirAsFile(), "20051209122103"
                        + File.separator + "artifact1.txt");
        assertEquals(targetFile.getAbsoluteFile(), mov.getModel().get("targetFile"));
    }

    public void testShouldBeAbleToDownloadArtifactsWithSpaceInPath() throws Exception {
        getRequest().setMethod("GET");
        getRequest().setRequestURI(
                "/download/artifacts/project%20space/"
                        + "log20051209122104Lbuild.467.xml/artifact%20with%20space.txt");
        ModelAndView mov = this.controller.handleRequest(getRequest(), getResponse());
        File targetFile =
                new File(DataUtils.getProjectSpaceArtifactDirAsFile(), "20051209122104"
                        + File.separator + "artifact with space.txt");
        assertEquals(targetFile.getAbsoluteFile(), mov.getModel().get("targetFile"));
    }

    public void testShouldRenderErrorPageIfFileNotExist() throws Exception {
        prepareRequest("IDontExist");
        ModelAndView mov = this.controller.handleRequest(getRequest(), getResponse());
        assertEquals("page_error", mov.getViewName());
        assertEquals("File does not exist.", mov.getModel().get("errorMessage"));
    }

    public void testShouldRenderErrorPageIfGivenFileIsDirectory() throws Exception {
        prepareRequest("subdir");
        ModelAndView mov = this.controller.handleRequest(getRequest(), getResponse());
        assertEquals("page_error", mov.getViewName());
        assertEquals("File can not be read.", mov.getModel().get("errorMessage"));
    }

}
