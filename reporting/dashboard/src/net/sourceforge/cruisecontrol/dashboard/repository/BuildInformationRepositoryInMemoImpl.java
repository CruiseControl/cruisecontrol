package net.sourceforge.cruisecontrol.dashboard.repository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.dashboard.service.JMXConnectorFactory;

public class BuildInformationRepositoryInMemoImpl implements BuildInformationRepository {
    private final Map<String, ProjectInfo> data = new HashMap<String, ProjectInfo>();

    private final JMXConnectorFactory jmxConnectorFactory;

    private final Map<String, BuildLoopInformation> info = new HashMap<String, BuildLoopInformation>();

    public BuildInformationRepositoryInMemoImpl() {
        this(new JMXConnectorFactory());
    }

    BuildInformationRepositoryInMemoImpl(JMXConnectorFactory jmxConnectorFactory) {
        this.jmxConnectorFactory = jmxConnectorFactory;
    }

    public ProjectInfo getProjectInfo(String projectName) {
        return data.get(projectName);
    }

    public synchronized ClosableProjectMBeanConnection getJmxConnection(final String projectName) throws IOException {
        if (!knowAboutProject(projectName)) {
            return null;
        }
        final JMXConnector jmxConnector
                = jmxConnectorFactory.connect(jmxServiceUrl(projectName), environment(projectName));
        return new ClosableProjectMBeanConnectionImpl(jmxConnector);
    }

    public List<ProjectInfo> getProjectInfos() {
        return new ArrayList<ProjectInfo>(data.values());
    }

    public BuildLoopInformation getBuildLoopInfo(final String projectName) {
        return buildInfo(projectName);
    }

    public void saveOrUpdate(final BuildLoopInformation buildLoopInfo) {
        final ProjectInfo[] projects = buildLoopInfo.getProjects();
        filterDiscontinuedProjects(buildLoopInfo, projects);
        for (final ProjectInfo projectInfo : projects) {
            data.put(projectInfo.getName(), projectInfo);
            info.put(projectInfo.getName(), buildLoopInfo);
        }
    }

    private void filterDiscontinuedProjects(final BuildLoopInformation updatedInfo, final ProjectInfo[] projects) {
        final Set<String> currentProjectNames = new HashSet<String>(data.keySet());
        for (final String name : currentProjectNames) {
            final BuildLoopInformation currentInfo = info.get(name);
            if (isSameBuildLoop(updatedInfo, currentInfo) && isMissing(name, projects)) {
                info.remove(name);
                data.remove(name);
            }
        }
    }

    private boolean isSameBuildLoop(final BuildLoopInformation buildLoopInfo,
                                    final BuildLoopInformation currentBuildLoop) {
        return currentBuildLoop.getUuid().equals(buildLoopInfo.getUuid());
    }

    private boolean isMissing(final String projectName, final ProjectInfo[] projects) {
        for (final ProjectInfo project : projects) {
            if (project.getName().equals(projectName)) {
                return false;
            }
        }
        return true;
    }

    public void removeAll() {
        data.clear();
    }

    public int size() {
        return data.size();
    }

    private Map<String, String> environment(final String projectName) {
        Map<String, String> environment = new HashMap<String, String>();
        environment.put("java.naming.factory.initial", "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put("java.naming.provider.url", getBuildLoopInfo(projectName).getJmxInfo().getRmiUrl());
        return environment;
    }

    private JMXServiceURL jmxServiceUrl(final String projectName) throws MalformedURLException {
        final String serviceUrl =
                "service:jmx:" + getBuildLoopInfo(projectName).getJmxInfo().getRmiUrl() + "/jndi/jrmp";
        return new JMXServiceURL(serviceUrl);
    }

    private boolean knowAboutProject(final String projectName) {
        return info.containsKey(projectName);
    }

    private BuildLoopInformation buildInfo(final String projectName) {
        final BuildLoopInformation buildInfo = info.get(projectName);
        if (buildInfo == null) {
            throw new RuntimeException("Cannot find build info for project " + projectName);
        }
        return buildInfo;
    }

    public boolean hasBuildLoopInfoFor(final String projectName) {
        return data.containsKey(projectName);
    }
}
