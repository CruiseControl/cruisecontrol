package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

public interface DashboardConfigService {

    public String isForceBuildEnabled();

    public String getLogsDir() throws ConfigurationException;

    public String getArtifactsDir() throws ConfigurationException;
}
