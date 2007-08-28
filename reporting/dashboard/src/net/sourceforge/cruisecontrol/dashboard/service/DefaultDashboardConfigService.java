package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

public class DefaultDashboardConfigService implements DashboardConfigService {

    public String getArtifactsDir() throws ConfigurationException {
        return "artifacts";
    }

    public String getLogsDir() throws ConfigurationException {
        return "logs";
    }

    public String getProjectsDir() throws ConfigurationException {
        return "projects";
    }

    public String getConfigXml() {
        return null;
    }

    public String getJMXPort() {
        return "8000";
    }

    public String getRMIPort() {
        return "1099";
    }

    public String isConfigFileEditable() {
        return "true";
    }

    public String isForceBuildEnabled() {
        return "true";
    }

    public String getCCHome() {
        return "";
    }

}
