package net.sourceforge.cruisecontrol.dashboard.repository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.dashboard.service.JMXConnectorFactory;

public class BuildInformationRepositoryInMemoImpl implements BuildInformationRepository {
    private Map data = new HashMap();

    private JMXConnectorFactory jmxConnectorFactory;

    private Map info = new HashMap();

    public BuildInformationRepositoryInMemoImpl() {
        this(new JMXConnectorFactory());
    }

    BuildInformationRepositoryInMemoImpl(JMXConnectorFactory jmxConnectorFactory) {
        this.jmxConnectorFactory = jmxConnectorFactory;
    }

    public ProjectInfo getProjectInfo(String projectName) {
        return (ProjectInfo) data.get(projectName);
    }

    public synchronized MBeanServerConnection getJmxConnection(String projectName) throws IOException {
        if (!knowAboutProject(projectName)) {
            return null;
        }
        JMXConnector jmxConnector = jmxConnectorFactory.connect(jmxServiceUrl(projectName), environment(projectName));
        return jmxConnector.getMBeanServerConnection();
    }

    public List getProjectInfos() {
        return new ArrayList(data.values());
    }

    public BuildLoopInformation getBuildLoopInfo(String projectName) {
        return buildInfo(projectName);
    }

    public void saveOrUpdate(BuildLoopInformation buildLoopInfo) {
        ProjectInfo[] projects = buildLoopInfo.getProjects();
        filterDiscontinuedProjects(buildLoopInfo, projects);
        for (int i = 0; i < projects.length; i++) {
            ProjectInfo projectInfo = projects[i];
            data.put(projectInfo.getName(), projectInfo);
            info.put(projectInfo.getName(), buildLoopInfo);
        }
    }

    private void filterDiscontinuedProjects(BuildLoopInformation updatedInfo, ProjectInfo[] projects) {
        Set currentProjectNames = new HashSet(data.keySet());
        for (Iterator iterator = currentProjectNames.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            BuildLoopInformation currentInfo = (BuildLoopInformation) info.get(name);
            if (isSameBuildLoop(updatedInfo, currentInfo) && isMissing(name, projects)) {
                info.remove(name);
                data.remove(name);
            }
        }
    }

    private boolean isSameBuildLoop(BuildLoopInformation buildLoopInfo, BuildLoopInformation currentBuildLoop) {
        return currentBuildLoop.getUuid().equals(buildLoopInfo.getUuid());
    }

    private boolean isMissing(String projectName, ProjectInfo[] projects) {
        for (int i = 0; i < projects.length; i++) {
            if (projects[i].getName().equals(projectName)) {
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

    private Map environment(String projectName) {
        Map environment = new HashMap();
        environment.put("java.naming.factory.initial", "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put("java.naming.provider.url", getBuildLoopInfo(projectName).getJmxInfo().getRmiUrl());
        return environment;
    }

    private JMXServiceURL jmxServiceUrl(String projectName) throws MalformedURLException {
        String serviceUrl =
                "service:jmx:" + getBuildLoopInfo(projectName).getJmxInfo().getRmiUrl() + "/jndi/jrmp";
        return new JMXServiceURL(serviceUrl);
    }

    private boolean knowAboutProject(String projectName) {
        return info.containsKey(projectName);
    }

    private BuildLoopInformation buildInfo(String projectName) {
        BuildLoopInformation buildInfo = (BuildLoopInformation) info.get(projectName);
        if (buildInfo == null) {
            throw new RuntimeException("Cannot find build info for project " + projectName);
        }
        return buildInfo;
    }

    public boolean hasBuildLoopInfoFor(String projectName) {
        return data.containsKey(projectName);
    }
}
