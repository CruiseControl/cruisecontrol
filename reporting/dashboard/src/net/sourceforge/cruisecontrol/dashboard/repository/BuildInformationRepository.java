package net.sourceforge.cruisecontrol.dashboard.repository;

import java.util.List;
import java.io.IOException;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;

public interface BuildInformationRepository {
    void saveOrUpdate(BuildLoopInformation information);

    ProjectInfo getProjectInfo(String projectName);

    ClosableProjectMBeanConnection getJmxConnection(String projectName) throws IOException;

    List<ProjectInfo> getProjectInfos();

    BuildLoopInformation getBuildLoopInfo(String projectName);

    boolean hasBuildLoopInfoFor(String projectName);

    int size();

    void removeAll();
}
