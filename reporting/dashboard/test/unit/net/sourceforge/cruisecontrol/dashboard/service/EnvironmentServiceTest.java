package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import java.io.File;

public class EnvironmentServiceTest extends MockObjectTestCase {
    private EnvironmentService service;

    private Mock mockSystemService;

    private Mock mockSystemPropertyConfigService;

    private Mock mockDashboardConfigService;

    private Mock mockServletContextConfigService;

    private Mock mockDefaultConfigService;

    protected void setUp() throws Exception {
        mockSystemService = mock(SystemService.class);
        mockSystemPropertyConfigService =
                mock(
                        SystemPropertyConfigService.class, new Class[]{SystemService.class},
                        new Object[]{(SystemService) mockSystemService.proxy()});
        mockDashboardConfigService =
                mock(
                        DashboardXmlConfigService.class, new Class[]{DashboardConfigFileFactory.class},
                        new Object[]{new DashboardConfigFileFactory(
                                (SystemService) mockSystemService
                                        .proxy())});
        mockServletContextConfigService = mock(ServletContextConfigService.class);
        mockDefaultConfigService = mock(DefaultDashboardConfigService.class);
        service =
                new EnvironmentService(
                        new DashboardConfigService[]{
                                (DashboardConfigService) mockSystemPropertyConfigService.proxy(),
                                (DashboardConfigService) mockDashboardConfigService.proxy(),
                                (DashboardConfigService) mockServletContextConfigService.proxy(),
                                (DashboardConfigService) mockDefaultConfigService.proxy()});
    }

    //force build enabled
    public void testShouldReturnFalseIfForceBuildSetToDisable() throws Exception {
        setReturnValueOfForceBuild("disabled");
        assertFalse(service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToEnable() throws Exception {
        setReturnValueOfForceBuild("Enabled");
        assertTrue(service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToTrue() throws Exception {
        setReturnValueOfForceBuild("True");
        assertTrue(service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToYes() throws Exception {
        setReturnValueOfForceBuild("Yes");
        assertTrue(service.isForceBuildEnabled());
    }

    //Log dir
    public void testShouldReturnLogsFolderFromInitParameters() throws Exception {
        String existingLogDir = DataUtils.getLogDirAsFile().getAbsolutePath();
        mockSystemPropertyConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getLogsDir")
                .will(returnValue(existingLogDir));
        assertEquals(new File(existingLogDir), service.getLogDir());
    }

    private void setReturnValueOfForceBuild(String value) {
        mockSystemPropertyConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(value));
    }
}
