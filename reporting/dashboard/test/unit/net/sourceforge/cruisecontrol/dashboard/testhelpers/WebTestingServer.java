package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigFileFactory;
import net.sourceforge.cruisecontrol.dashboard.service.SystemPropertyConfigService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.BuildLoopQueryServiceStub;

public final class WebTestingServer {
    private WebTestingServer() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(DashboardConfigFileFactory.PROPS_CC_DASHBOARD_CONFIG, DataUtils
                .getDashboardConfigXmlOfWebApp().getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_LOG_DIR, DataUtils
                .getLogRootOfWebapp().getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_ARTIFACTS_DIR, DataUtils
                .getArtifactRootOfWebapp().getAbsolutePath());
        System.setProperty(BuildLoopQueryServiceStub.PROPS_CC_CONFIG_FILE, DataUtils.getConfigXmlOfWebApp()
                .getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "enabled");
        DataUtils.cloneCCHome();
        new CruiseDashboardServer().start();
    }
}
