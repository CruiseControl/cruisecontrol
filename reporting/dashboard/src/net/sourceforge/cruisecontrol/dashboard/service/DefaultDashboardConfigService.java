package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

public class DefaultDashboardConfigService implements DashboardConfigService {

    public String getArtifactsDir() throws ConfigurationException {
        return "artifacts";
    }

    public String getLogsDir() throws ConfigurationException {
        return "logs";
    }

    public String isForceBuildEnabled() {
        return "true";
    }

}
