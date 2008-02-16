package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.BuildLoopInformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LatestBuildSummariesService {
    private HistoricalBuildSummariesService historicalBuildSummariesService;
    private BuildLoopQueryService buildLoopQueryService;

    public LatestBuildSummariesService(HistoricalBuildSummariesService historicalBuildSummariesService,
                                       BuildLoopQueryService buildLoopQueryService) {
        this.historicalBuildSummariesService = historicalBuildSummariesService;
        this.buildLoopQueryService = buildLoopQueryService;
    }

    public BuildSummary getLatestProject(String projectName) {
        BuildSummary buildSummary = (BuildSummary) this.historicalBuildSummariesService.getLatest(projectName);
        BuildLoopInformation.ProjectInfo projectInfo = buildLoopQueryService.getProjectInfo(projectName);
        String status = projectInfo.getStatus();
        buildSummary.updateStatus(status);
        if (CurrentStatus.BUILDING.equals(buildSummary.getCurrentStatus())) {
            buildSummary.updateBuildSince(CCDateFormatter.iso8601(projectInfo.getBuildStartTime()));
        }
        buildSummary.setServerName(buildLoopQueryService.getServerName(buildSummary.getProjectName()));
        return buildSummary;
    }

    public List getLatestOfProjects() {
        List allSummaries = new ArrayList();
        Map buildLiveStatuses = buildLoopQueryService.getAllProjectsStatus();

        allSummaries.addAll(historicalBuildSummariesService.createInactiveProjects());
        allSummaries.addAll(historicalBuildSummariesService.createActiveProjects());
        allSummaries.addAll(historicalBuildSummariesService.createDiscontinuedProjects());
        for (Iterator iter = allSummaries.iterator(); iter.hasNext();) {
            BuildSummary buildSummary = (BuildSummary) iter.next();
            if (!buildLiveStatuses.containsKey(buildSummary.getProjectName())) {
                continue;
            } else {
                buildSummary.updateStatus((String) buildLiveStatuses.get(buildSummary.getProjectName()));
                if (CurrentStatus.BUILDING.equals(buildSummary.getCurrentStatus())) {
                    String time = buildLoopQueryService.getProjectInfo(
                            buildSummary.getProjectName()).getBuildStartTime();
                    buildSummary.updateBuildSince(CCDateFormatter.iso8601(time));
                }
                buildSummary.setServerName(buildLoopQueryService.getServerName(buildSummary.getProjectName()));
            }
        }
        Collections.sort(allSummaries);
        return allSummaries;
    }
}
