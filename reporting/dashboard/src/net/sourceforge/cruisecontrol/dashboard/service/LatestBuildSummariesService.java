package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.BuildLoopInformation;

import java.util.ArrayList;
import java.util.Collections;
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

    public BuildSummary getLatestProject(final String projectName) {
        final BuildSummary buildSummary = this.historicalBuildSummariesService.getLatest(projectName);
        final BuildLoopInformation.ProjectInfo projectInfo = buildLoopQueryService.getProjectInfo(projectName);
        final String status = projectInfo.getStatus();
        buildSummary.updateStatus(status);
        if (CurrentStatus.BUILDING.equals(buildSummary.getCurrentStatus())) {
            buildSummary.updateBuildSince(CCDateFormatter.iso8601(projectInfo.getBuildStartTime()));
        }
        buildSummary.setServerName(buildLoopQueryService.getServerName(buildSummary.getProjectName()));
        return buildSummary;
    }

    public List getLatestOfProjects() {
        final List<BuildSummary> allSummaries = new ArrayList<BuildSummary>();
        final Map<String, String> buildLiveStatuses = buildLoopQueryService.getAllProjectsStatus();

        allSummaries.addAll(historicalBuildSummariesService.createInactiveProjects());
        allSummaries.addAll(historicalBuildSummariesService.createActiveProjects());
        allSummaries.addAll(historicalBuildSummariesService.createDiscontinuedProjects());
        for (final BuildSummary buildSummary : allSummaries) {
            if (!buildLiveStatuses.containsKey(buildSummary.getProjectName())) {
                continue;
            } else {
                buildSummary.updateStatus(buildLiveStatuses.get(buildSummary.getProjectName()));
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
