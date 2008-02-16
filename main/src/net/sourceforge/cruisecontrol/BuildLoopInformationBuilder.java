package net.sourceforge.cruisecontrol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import net.sourceforge.cruisecontrol.BuildLoopInformation.JmxInfo;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;

public class BuildLoopInformationBuilder {
    private final CruiseControlController controller;

    public BuildLoopInformationBuilder(CruiseControlController controller) {
        this.controller = controller;
    }

    public BuildLoopInformation buildBuildLoopInformation() {
        return new BuildLoopInformation(getProjects(), getJmxInfo(), getServerName(), getTimestamp());
    }

    private BuildLoopInformation.ProjectInfo[] getProjects() {
        List projectConfigs = controller.getProjects();
        BuildLoopInformation.ProjectInfo[] projects =
                new BuildLoopInformation.ProjectInfo[projectConfigs.size()];
        for (int i = 0; i < projectConfigs.size(); i++) {
            ProjectConfig projectConfig = (ProjectConfig) projectConfigs.get(i);
            projects[i] = new ProjectInfo(
                projectConfig.getName(),
                projectConfig.getStatus(),
                getStartTime(projectConfig));
            if (projectConfig.isInState(ProjectState.BUILDING)) {
                projects[i].setModifications(projectConfig.getModifications());
            }
        }
        return projects;
    }

    private String getStartTime(ProjectConfig projectConfig) {
        try {
            String timeOnBuildLoop = projectConfig.getBuildStartTime();
            if (!(timeOnBuildLoop == null || timeOnBuildLoop.trim().length() == 0)) {
                return DateUtil.formatIso8601(DateUtil.parseFormattedTime(timeOnBuildLoop, "BuildStartTime"));
            }
        } catch (CruiseControlException e) {
            Logger.getLogger(BuildLoopInformation.class).error(e);
        }
        return "";
    }

    private String getTimestamp() {
        return DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN);
    }

    private JmxInfo getJmxInfo() {
        return new JmxInfo(getServerName());
    }

    private String getServerName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            Logger.getLogger(BuildLoopInformation.class).error(e);
            return "";
        }
    }
}
