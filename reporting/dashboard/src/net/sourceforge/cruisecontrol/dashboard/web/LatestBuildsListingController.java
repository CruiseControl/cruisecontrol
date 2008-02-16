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

import net.sourceforge.cruisecontrol.dashboard.BuildSummaryStatistics;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.LatestBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.tabs.BaseTabController;
import net.sourceforge.cruisecontrol.dashboard.web.command.ForceBuildCommand;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LatestBuildsListingController extends BaseTabController {
    private LatestBuildSummariesService buildSummariesService;

    private BuildSummaryUIService buildSummaryUIService;

    public LatestBuildsListingController(LatestBuildSummariesService buildSummaryService,
                                         BuildSummaryUIService buildSummaryUIService) {
        this.buildSummariesService = buildSummaryService;
        this.buildSummaryUIService = buildSummaryUIService;
    }

    protected synchronized ModelAndView handleTabRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List allProjectsBuildSummaries = buildSummariesService.getLatestOfProjects();
        Map dataMap = new HashMap();
        dataMap.put("buildCmds", buildSummaryUIService.transformWithLevel(allProjectsBuildSummaries));
        dataMap.put("command", new ForceBuildCommand());
        dataMap.put("projectStatistics", new BuildSummaryStatistics(allProjectsBuildSummaries));
        return new ModelAndView(getViewName(), dataMap);
    }

    protected abstract String getViewName();
}
