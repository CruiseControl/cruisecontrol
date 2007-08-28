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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.service.WidgetPluginService;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.utils.DashboardUtils;
import net.sourceforge.cruisecontrol.dashboard.widgets.Widget;

import org.springframework.web.servlet.ModelAndView;

public class BuildDetailController extends BaseMultiActionController {
    private BuildService buildService;

    private final BuildSummariesService buildSummarySerivce;

    private final WidgetPluginService widgetPluginService;

    private BuildSummaryUIService buildSummaryUIService;

    private CruiseControlJMXService jmxService;

    public BuildDetailController(BuildService buildService, BuildSummariesService buildSummarySerivce,
            WidgetPluginService widgetPluginService, BuildSummaryUIService buildSummaryUIService,
            CruiseControlJMXService jmxService) {
        this.buildService = buildService;
        this.buildSummarySerivce = buildSummarySerivce;
        this.widgetPluginService = widgetPluginService;
        this.buildSummaryUIService = buildSummaryUIService;
        this.jmxService = jmxService;
        this.setSupportedMethods(new String[] {"GET"});
    }

    public ModelAndView latest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = DashboardUtils.decode(url[url.length - 1]);
        Build latest = this.buildSummarySerivce.getLatest(projectName);
        Build build = this.buildService.getBuild(projectName, latest.getBuildLogFilename());
        return buildDetail(request, projectName, build);
    }

    public ModelAndView history(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = DashboardUtils.decode(url[url.length - 2]);
        String logfileName = url[url.length - 1];
        return buildDetail(request, projectName, buildService.getBuild(projectName, logfileName));
    }

    public ModelAndView live(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = DashboardUtils.decode(url[url.length - 1]);
        String buildStatusStr = (String) jmxService.getAllProjectsStatus().get(projectName);
        ProjectBuildStatus buildStatus = ProjectBuildStatus.getProjectBuildStatus(buildStatusStr);
        if (!ProjectBuildStatus.BUILDING.equals(buildStatus)) {
            return latest(request, response);
        } else {
            return liveWithInformation(request, projectName, buildStatusStr);
        }
    }

    public ModelAndView liveWithInformation(HttpServletRequest request, String projectName,
            String buildStatusStr) {
        ModelAndView mov = buildDetail(request, projectName, buildService.getActiveBuild(projectName));
        mov.getModel().put("lastStatus", buildSummaryUIService.getLastBuildStatus(projectName).getStatus());
        mov.getModel().put(
                "buildSince",
                CCDateFormatter.getDateStringInHumanBeingReadingStyle(ProjectBuildStatus
                        .getTimestamp(buildStatusStr)));
        return mov;
    }

    private ModelAndView buildDetail(HttpServletRequest request, String projectName, Build build) {
        HashMap contextProperties = new HashMap();
        contextProperties.put(Widget.PARAM_WEB_CONTEXT_PATH, request.getContextPath());
        widgetPluginService.mergePluginOutput((BuildDetail) build, contextProperties);
        Map model = new HashMap();
        model.put("summaries", buildSummaryUIService.transform(buildSummarySerivce.getLastest25(projectName),
                false));
        model.put("build", buildSummaryUIService.transform(build, true));
        model.put("status", build.getStatus().getStatus());
        model.put("logfile", build.getBuildLogFilename());
        model.put("durationToSuccessfulBuild", buildSummarySerivce.getDurationFromLastSuccessfulBuild(
                projectName, build.getBuildDate()));
        return new ModelAndView("page_build_detail", model);
    }
}
