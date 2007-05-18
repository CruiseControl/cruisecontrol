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
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class DownloadLogController implements Controller {
    private Configuration configuration;

    private BuildService buildFactory;

    public DownloadLogController(Configuration configuration, BuildService buildFactory) {
        this.configuration = configuration;
        this.buildFactory = buildFactory;
    }

    public ModelAndView handleRequest(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        String[] url = StringUtils.split(httpServletRequest.getRequestURI(), '/');
        String projectName = url[url.length - 2];
        String fileToBeDownloaded = url[url.length - 1];
        File file = buildFactory.getBuildFile(projectName, fileToBeDownloaded);
        ModelAndView mov;

        String absolutePath;
        try {
            absolutePath = file.getCanonicalPath();
        } catch (IOException e) {
            absolutePath = file.getAbsolutePath();
        }
        if (!(absolutePath.toLowerCase().indexOf(
                configuration.getCruiseLogfileLocation().toLowerCase()) == 0)) {
            mov = new ModelAndView("error");
            mov.getModel().put("errorMessage",
                    "Permission Denied: you don't have permission to download this file.");
        } else if (!file.exists()) {
            mov = new ModelAndView("error");
            mov.getModel().put("errorMessage", "File does not exist.");
        } else if (!file.isFile()) {
            mov = new ModelAndView("error");
            mov.getModel().put("errorMessage", "File can not be read.");
        } else {
            mov = new ModelAndView("downloadXmlView");
            mov.getModel().put("targetFile", new File(absolutePath));
        }

        return mov;
    }

}
