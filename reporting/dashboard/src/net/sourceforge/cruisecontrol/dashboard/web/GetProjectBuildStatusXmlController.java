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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

public class GetProjectBuildStatusXmlController extends MultiActionController {
    private BuildSummariesService buildSummariesService;

    private CruiseControlJMXService cruiseControlJMXService;

    private final BuildSummaryUIService uiService;

    public GetProjectBuildStatusXmlController(BuildSummariesService buildSummarySerivce,
            CruiseControlJMXService cruiseControlJMXService, BuildSummaryUIService uiService) {
        super();
        this.buildSummariesService = buildSummarySerivce;
        this.cruiseControlJMXService = cruiseControlJMXService;
        this.uiService = uiService;
        this.setSupportedMethods(new String[] {"GET"});
    }

    public ModelAndView cctray(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType("text/xml");
        PrintWriter writer = resp.getWriter();
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<Projects>");
        writer.println(uiService.toXml(buildSummariesService.getLatestOfProjects(), cruiseControlJMXService
                .getAllProjectsStatus(), getBaseURL(req), "cctray"));
        writer.println("</Projects>");
        return null;
    }

    public ModelAndView rss(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        PrintWriter writer = resp.getWriter();
        resp.setContentType("text/xml");
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<rss version=\"2.0\">");
        writer.println("<channel>");
        writer.println("  <title>CruiseControl Results</title>");
        writer.print("  <link>");
        writer.print(getBaseURL(req));
        writer.println("</link>");
        writer.println("  <description>Summary of the project build results.</description>");
        writer.println("  <language>en-us</language>");
        writer.println(uiService.toXml(getLatestSummariesForRSS(req), cruiseControlJMXService
                .getAllProjectsStatus(), getBaseURL(req), "rss"));
        writer.println("</channel>");
        writer.println("</rss>");
        return null;
    }

    private List getLatestSummariesForRSS(HttpServletRequest req) throws Exception {
        String projectName = req.getParameter("projectName");
        List result = new ArrayList();
        if (StringUtils.isNotEmpty(projectName)) {
            result.add(buildSummariesService.getLatest(projectName));
        } else {
            result.addAll(buildSummariesService.getLatestOfProjects());
        }
        return result;
    }

    private String getBaseURL(HttpServletRequest req) {
        String uri = req.getRequestURL().toString();
        String baseUrl = uri.substring(0, uri.lastIndexOf("/") + 1);
        return baseUrl;
    }
}
