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

import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildLiveDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.HistoricalBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.WidgetPluginService;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.utils.DashboardUtils;
import net.sourceforge.cruisecontrol.dashboard.widgets.Widget;
import net.sourceforge.cruisecontrol.util.DateUtil;
import org.joda.time.DateTime;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;

public class BuildDetailController extends BaseMultiActionController {
    private final BuildService buildService;

    private final HistoricalBuildSummariesService buildSummarySerivce;

    private final WidgetPluginService widgetPluginService;

    private final BuildSummaryUIService buildSummaryUIService;

    private final BuildLoopQueryService buildLoopQueryService;

    public BuildDetailController(BuildService buildService,
                                 HistoricalBuildSummariesService buildSummarySerivce,
                                 WidgetPluginService widgetPluginService,
                                 BuildSummaryUIService buildSummaryUIService,
                                 BuildLoopQueryService buildLoopQueryService) {
        this.buildService = buildService;
        this.buildSummarySerivce = buildSummarySerivce;
        this.widgetPluginService = widgetPluginService;
        this.buildSummaryUIService = buildSummaryUIService;
        this.buildLoopQueryService = buildLoopQueryService;
        this.setSupportedMethods(new String[]{"GET"});
    }

    public ModelAndView latest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = DashboardUtils.decode(url[url.length - 1]);
        if (isBuilding(projectName)) {
            return live(projectName);
        } else {
            BuildSummary latest = this.buildSummarySerivce.getLatest(projectName);
            BuildDetail build = this.buildService.getBuild(projectName, latest.getBuildLogFileDateTime());
            loadAllWidgets(request, build);
            return new ModelAndView("page_build_detail", buildDetail(projectName, build));
        }
    }

    public ModelAndView history(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = DashboardUtils.decode(url[url.length - 2]);
        String yyyyMMddHHmmss = url[url.length - 1];
        BuildDetail build = buildService.getBuild(projectName, yyyyMMddHHmmss);
        loadAllWidgets(request, build);
        return new ModelAndView("page_build_detail", buildDetail(projectName, build));
    }

    public ModelAndView live(String projectName) throws Exception {
        PreviousResult lastBuildStatus = buildSummaryUIService.getLastBuildStatus(projectName);
        BuildLiveDetail build = buildService.getActiveBuild(projectName, lastBuildStatus);
        Map model = buildDetail(projectName, build);
        model.put("buildSince", buildStartTime(projectName));
        model.put("buildDuration", buildSummaryUIService.getLastBuildDuration(projectName));
        return new ModelAndView("page_building", model);
    }

    private String buildStartTime(String projectName) throws ParseException {
        String buildStartTime = buildLoopQueryService.getProjectInfo(projectName).getBuildStartTime();
        DateTime startTime = new DateTime(DateUtil.parseIso8601(buildStartTime));
        return CCDateFormatter.getDateStringInHumanBeingReadingStyle(startTime);
    }

    private boolean isBuilding(String projectName) {
        String buildStatus = getCurrentBuildStatus(projectName);
        return CurrentStatus.BUILDING.equals(CurrentStatus.getProjectBuildStatus(buildStatus));
    }

    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response,
                                    Exception e) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        Map data = new HashMap();
        String projectName = "";
        final String partial;
        if (url.length == 6) {
            partial = "projectlog";
            projectName = DashboardUtils.decode(url[url.length - 2]);
            data.put("log", url[url.length - 1]);
        } else if (url.length == 5) {
            partial = "project";
            projectName = DashboardUtils.decode(url[url.length - 1]);
        } else {
            partial = "noproject";
        }
        data.put("projectName", projectName);
        return new ModelAndView("forward:/exceptions/builddetail/" + partial, data);
    }

    private Map buildDetail(String projectName, BuildDetail build) {
        Map model = new HashMap();
        model.put("historicalBuildCmds", buildSummaryUIService.transform(buildSummarySerivce
                .getLastest25(projectName)));
        model.put("buildCmd", buildSummaryUIService.transformWithLevel(build));
        model.put("logfile", build.getBuildLogFilename());
        model.put("durationToSuccessfulBuild", buildSummarySerivce.getDurationFromLastSuccessfulBuild(
                projectName, build.getBuildDate()));
        return model;
    }


    private String getCurrentBuildStatus(String projectName) {
        return buildLoopQueryService.getAllProjectsStatus().get(projectName);
    }

    private void loadAllWidgets(HttpServletRequest request, BuildDetail build) {
        HashMap contextProperties = new HashMap();
        contextProperties.put(Widget.PARAM_WEB_CONTEXT_PATH, request.getContextPath());
        widgetPluginService.mergePluginOutput(build, contextProperties);
    }
}
