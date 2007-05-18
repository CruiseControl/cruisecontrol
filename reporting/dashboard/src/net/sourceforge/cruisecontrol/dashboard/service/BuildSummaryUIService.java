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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import net.sourceforge.cruisecontrol.dashboard.web.command.CCTrayBuildSummaryAdapter;
import net.sourceforge.cruisecontrol.dashboard.web.command.RSSBuildSummaryAdapter;
import net.sourceforge.cruisecontrol.dashboard.web.command.XmlAdapter;
import org.joda.time.DateTime;

public class BuildSummaryUIService {
    private final BuildSummariesService buildSummariesService;

    public BuildSummaryUIService(BuildSummariesService buildSummariesService) {
        this.buildSummariesService = buildSummariesService;
    }

    public List toCommands(List buildSummaries) {
        List commands = new ArrayList();
        for (int i = 0; i < buildSummaries.size(); i++) {
            commands.add(new BuildCommand(((Build) buildSummaries.get(i))));
        }
        return commands;
    }

    public List transform(List buildSummaries) {
        List buildSummaryCommands = new ArrayList();
        for (Iterator iter = buildSummaries.iterator(); iter.hasNext();) {
            buildSummaryCommands.add(transform((Build) iter.next()));
        }
        return buildSummaryCommands;
    }

    public BuildCommand transform(Build build) {
        BuildCommand command = new BuildCommand(build);
        String status = command.getBuild().getStatus();
        String projectName = command.getBuild().getProjectName();
        if (ProjectBuildStatus.FAILED.getStatus().equals(status)) {
            Build earliesFailedBuild = buildSummariesService.getEaliestFailed(projectName, command.getBuild()
                    .getBuildDate());
            command.updateFailedCSS(earliesFailedBuild);
        } else if (ProjectBuildStatus.PASSED.getStatus().equals(status)) {
            Build lastSucceed = buildSummariesService.getLastSucceed(projectName, new DateTime());
            command.updatePassedCss(lastSucceed);
        } else {
            command.updateDefaultCss();
        }
        return command;
    }

    public String toXml(List buildSummaries, Map buildStatuses, String baseUrl, String type) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buildSummaries.size(); i++) {
            Build buildSummary = (Build) buildSummaries.get(i);
            if (!ProjectBuildStatus.INACTIVE.getStatus().equals(buildSummary.getStatus())) {
                XmlAdapter adapter = null;
                if ("rss".endsWith(type)) {
                    adapter = new RSSBuildSummaryAdapter(baseUrl, buildSummary);
                } else {
                    adapter = new CCTrayBuildSummaryAdapter(baseUrl, buildSummary);
                    buildSummary.updateStatus((String) buildStatuses.get(buildSummary
                            .getProjectName()));
                }
                sb.append(adapter.toXml());
            }
        }
        return sb.toString();
    }

}
