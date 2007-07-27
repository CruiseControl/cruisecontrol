package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

public interface DashboardConfigService {
    public String getCCHome();

    public String getConfigXml();

    public String getJMXPort();

    public String getRMIPort();

    public String isForceBuildEnabled();
    
    public String isConfigFileEditable();

    public String getLogsDir() throws ConfigurationException;

    public String getArtifactsDir() throws ConfigurationException;

    public String getProjectsDir() throws ConfigurationException;
}
