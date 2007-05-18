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
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.PluginOutputService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

public class BuildDetailController extends MultiActionController {
    private BuildService buildService;

    private final BuildSummariesService buildSummarySerivce;

    private final PluginOutputService pluginOutputService;

    private BuildSummaryUIService buildSummaryUIService;

    public BuildDetailController(BuildService buildService, BuildSummariesService buildSummarySerivce,
                                 PluginOutputService pluginOutputService, BuildSummaryUIService buildSummaryUIService) {
        this.buildService = buildService;
        this.buildSummarySerivce = buildSummarySerivce;
        this.pluginOutputService = pluginOutputService;
        this.buildSummaryUIService = buildSummaryUIService;
        this.setSupportedMethods(new String[]{"GET"});
    }

    public ModelAndView latest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = StringUtils.split(request.getRequestURI(), '/');
        String projectName = url[url.length - 1];
        Build latest = this.buildSummarySerivce.getLatest(projectName);
        Build build = this.buildService.getBuild(projectName, latest.getBuildLogFilename());
        return buildDetail(projectName, build);
    }

    public ModelAndView history(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = StringUtils.split(request.getRequestURI(), '/');
        String projectName = url[url.length - 2];
        String logfileName = url[url.length - 1];
        return buildDetail(projectName, buildService.getBuild(projectName, logfileName));
    }

    private ModelAndView buildDetail(String projectName, Build build) {
        Map model = new HashMap();
        pluginOutputService.mergePluginOutput((BuildDetail) build, new HashMap());
        model.put("summaries", buildSummaryUIService.toCommands(buildSummarySerivce
                .getLastest25(projectName)));
        model.put("build", buildSummaryUIService.transform(build));
        model.put("status", build.getStatus());
        model.put("logfile", build.getBuildLogFilename());
        model.put("durationToSuccessfulBuild", buildSummarySerivce
                .getDurationFromLastSuccessfulBuild(projectName, build.getBuildDate()));
        return new ModelAndView("buildDetail", model);
    }
}
