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

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class GetActiveBuildInfoController implements Controller {

    private final CruiseControlJMXService jmxService;

    private final BuildSummariesService buildSummaryService;

    private BuildSummaryUIService uiService;

    public GetActiveBuildInfoController(CruiseControlJMXService service, BuildSummariesService buildSummaryService,
                                        BuildSummaryUIService uiService) {
        this.jmxService = service;
        this.buildSummaryService = buildSummaryService;
        this.uiService = uiService;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = StringUtils.split(request.getRequestURI(), '/');
        String projectName = url[url.length - 1];
        ModelAndView mv = new ModelAndView("buildDetail");
        List commitMessages = jmxService.getCommitMessages(projectName);
        mv.getModel().put("commitMessages", commitMessages);
        mv.getModel().put("projectName", projectName);
        mv.getModel().put("buildSince", getBuildSince(projectName));
        String durationFromLastSuccessfulBuild = buildSummaryService
                .getDurationFromLastSuccessfulBuild(projectName, new DateTime());
        mv.getModel().put("durationToSuccessfulBuild", durationFromLastSuccessfulBuild);
        List top25Summaries = buildSummaryService.getLastest25(projectName);
        mv.getModel().put("summaries", uiService.toCommands(top25Summaries));
        String buildStatus = jmxService.getBuildStatus(projectName);
        ProjectBuildStatus status = ProjectBuildStatus.getProjectBuildStatus(buildStatus);
        mv.getModel().put("status", status.getStatus());
        if (status.isBuilding() && CollectionUtils.isEmpty(commitMessages)) {
            mv.getModel().put("flash_message", "Build forced, No new code is committed into repository");
        } else {
            mv.getModel().put("flash_message", "Waiting for checking in");
        }
        return mv;
    }

    private String getBuildSince(String projectName) {
        try {
            String buildStatus = (String) jmxService.getAllProjectsStatus().get(projectName);
            DateTime buildSince = ProjectBuildStatus.getTimestamp(buildStatus);
            return CCDateFormatter.getDateStringInHumanBeingReadingStyle(buildSince);
        } catch (Exception e) {
            return "N/A";
        }
    }
}
