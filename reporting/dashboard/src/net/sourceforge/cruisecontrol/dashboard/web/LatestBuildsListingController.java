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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.BuildSummaryStatistics;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ForceBuildCommand;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class LatestBuildsListingController implements Controller {
    private BuildSummariesService buildSummariesService;

    private final BuildSummaryUIService buildSummaryUIService;

    private static final int CACHE_MILLISECONDS = 5000;

    private long lastScanTime = 0;

    private Map cachedDataMap;

    private final EnvironmentService environmentService;

    public LatestBuildsListingController(BuildSummariesService buildSummaryService,
            BuildSummaryUIService buildSummaryUIService, EnvironmentService environmentService) {
        this.buildSummariesService = buildSummaryService;
        this.buildSummaryUIService = buildSummaryUIService;
        this.environmentService = environmentService;
    }

    public synchronized ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long now = new Date().getTime();
        if (lastScanTime == 0 || (now - lastScanTime) > CACHE_MILLISECONDS) {
            boolean isForceBuildEnabled = environmentService.isForceBuildEnabled();
            lastScanTime = now;
            cachedDataMap = new HashMap();
            List allProjectsBuildSummaries = buildSummariesService.getLatestOfProjects();
            try {
                allProjectsBuildSummaries = buildSummariesService.updateWithLiveStatus(allProjectsBuildSummaries);
            } catch (Exception e) {
                // It's OK for now. Usually means that JMX wasn't available.
            }
            cachedDataMap.put("buildSummaries", buildSummaryUIService.transform(allProjectsBuildSummaries,
                    true));
            cachedDataMap.put("command", new ForceBuildCommand());
            cachedDataMap.put("projectStatistics", new BuildSummaryStatistics(allProjectsBuildSummaries));
            cachedDataMap.put("forceBuildEnabled", Boolean.valueOf(isForceBuildEnabled));
            cachedDataMap.put("projectStatistics", new BuildSummaryStatistics(allProjectsBuildSummaries));

        }
        return new ModelAndView("page_latest_builds", cachedDataMap);
    }
}
