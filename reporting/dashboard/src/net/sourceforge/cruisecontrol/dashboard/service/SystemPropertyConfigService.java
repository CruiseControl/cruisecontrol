package net.sourceforge.cruisecontrol.dashboard.service;


public class SystemPropertyConfigService implements DashboardConfigService {

    public static final String PROPS_CC_CONFIG_FORCEBUILD_ENABLED = "cc.config.forcebuild";

    public static final String PROPS_CC_CONFIG_LOG_DIR = "cc.logdir";

    public static final String PROPS_CC_CONFIG_ARTIFACTS_DIR = "cc.artifacts";

    private final SystemService service;

    public SystemPropertyConfigService(SystemService service) {
        this.service = service;
    }

    public String getArtifactsDir() {
        return service.getProperty(PROPS_CC_CONFIG_ARTIFACTS_DIR);
    }

    public String getLogsDir() {
        return service.getProperty(PROPS_CC_CONFIG_LOG_DIR);
    }

    public String isForceBuildEnabled() {
        return service.getProperty(PROPS_CC_CONFIG_FORCEBUILD_ENABLED);
    }

}
