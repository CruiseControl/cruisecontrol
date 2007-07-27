package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

public class SystemPropertyConfigService implements DashboardConfigService {
    public static final String PROPS_CC_HOME = "cc.home";

    public static final String PROPS_CC_CONFIG_FILE = "cc.config.file";

    public static final String PROPS_CC_CONFIG_EDITABLE = "cc.config.editable";

    public static final String PROPS_CC_CONFIG_JMX_PORT = "cc.jmxport";

    public static final String PROPS_CC_CONFIG_RMI_PORT = "cc.rmiport";

    public static final String PROPS_CC_CONFIG_FORCEBUILD_ENABLED = "cc.config.forcebuild";

    public static final String PROPS_CC_CONFIG_LOG_DIR = "cc.logdir";

    public static final String PROPS_CC_CONFIG_ARTIFACTS_DIR = "cc.artifacts";

    public static final String PROPS_CC_CONFIG_PROJECTS_DIR = "cc.projects";

    private final SystemService service;

    public SystemPropertyConfigService(SystemService service) {
        this.service = service;

    }

    public String getArtifactsDir() {
        return service.getProperty(PROPS_CC_CONFIG_ARTIFACTS_DIR);
    }

    public String getConfigXml() {
        return service.getProperty(PROPS_CC_CONFIG_FILE);
    }

    public String getJMXPort() {
        return service.getProperty(PROPS_CC_CONFIG_JMX_PORT);
    }

    public String getLogsDir() {
        return service.getProperty(PROPS_CC_CONFIG_LOG_DIR);
    }

    public String getProjectsDir() throws ConfigurationException {
        return service.getProperty(PROPS_CC_CONFIG_PROJECTS_DIR);
    }

    public String getRMIPort() {
        return service.getProperty(PROPS_CC_CONFIG_RMI_PORT);
    }

    public String isConfigFileEditable() {
        return service.getProperty(PROPS_CC_CONFIG_EDITABLE);
    }

    public String isForceBuildEnabled() {
        return service.getProperty(PROPS_CC_CONFIG_FORCEBUILD_ENABLED);
    }

    public String getCCHome() {
        return service.getProperty(PROPS_CC_HOME);
    }
}
