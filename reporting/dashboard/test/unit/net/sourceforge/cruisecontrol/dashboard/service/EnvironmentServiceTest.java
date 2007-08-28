package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

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
                mock(SystemPropertyConfigService.class, new Class[] {SystemService.class},
                        new Object[] {(SystemService) mockSystemService.proxy()});
        mockDashboardConfigService =
                mock(DashboardXmlConfigService.class, new Class[] {SystemService.class},
                        new Object[] {(SystemService) mockSystemService.proxy()});
        mockServletContextConfigService = mock(ServletContextConfigService.class);
        mockDefaultConfigService = mock(DefaultDashboardConfigService.class);
        service =
                new EnvironmentService((SystemService) mockSystemService.proxy(),
                        new DashboardConfigService[] {
                                (DashboardConfigService) mockSystemPropertyConfigService.proxy(),
                                (DashboardConfigService) mockDashboardConfigService.proxy(),
                                (DashboardConfigService) mockServletContextConfigService.proxy(),
                                (DashboardConfigService) mockDefaultConfigService.proxy()});
    }

    public void testShouldReturnTrueIfNoConfigEditableSetup() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("isConfigFileEditable").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("isConfigFileEditable").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("isConfigFileEditable").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("isConfigFileEditable").will(returnValue("true"));
        assertEquals(true, service.isConfigFileEditable());
    }

    //JMX
    public void testShouldReturnJMXPortFromSystemProperties() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getJMXPort").will(returnValue("9000"));
        assertEquals(9000, service.getJmxPort());
    }

    public void testShouldreturnJMXPortFromDashboardConfig() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getJMXPort").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getJMXPort").will(returnValue("9001"));
        assertEquals(9001, service.getJmxPort());
    }

    public void testShouldReturnJMXPortFromContextParam() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getJMXPort").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getJMXPort").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getJMXPort").will(returnValue("9002"));
        assertEquals(9002, service.getJmxPort());
    }

    public void testShouldReturnJMXPort8000AsDefault() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getJMXPort").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getJMXPort").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getJMXPort").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("getJMXPort").will(returnValue("8000"));
        assertEquals(8000, service.getJmxPort());
    }

    //RMI
    public void testShouldReturnRMIPortFromSystemProperties() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getRMIPort").will(returnValue("2000"));
        assertEquals(2000, service.getRmiPort());
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
        mockSystemPropertyConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getLogsDir").will(
                returnValue("/home/user/logs"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/logs"), service.getLogDir());
    }

    public void testShouldThrowExceptionWhenNoLogDirIsDefined() throws Exception {
        try {
            mockSystemPropertyConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
            mockDashboardConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
            mockServletContextConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
            mockDefaultConfigService.expects(once()).method("getLogsDir").will(returnValue("logs"));
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue(""));
            service.getLogDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnLogDirWithCCHOMEWhenLogDirIsRelativePathAndCCHomeIsSetuped() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("getLogsDir").will(returnValue("logs"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue("/home/khu/"));
        assertEquals(new File("/home/khu/logs"), service.getLogDir());
    }

    public void testShouldThrowExceptionWhenLogDirIsRelativeAndNoCCHomeSpecified() throws Exception {
        try {
            mockSystemPropertyConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
            mockDashboardConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
            mockServletContextConfigService.expects(once()).method("getLogsDir").will(returnValue(""));
            mockDefaultConfigService.expects(once()).method("getLogsDir").will(returnValue("logs"));
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue(""));
            service.getLogDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnAbsoluteLogDirAndIgnoreCCHome() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getLogsDir").will(
                returnValue("/home/khu/logs"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/khu/logs"), service.getLogDir());
    }

    //artifacts
    public void testShouldThrowExceptionWhenNoArtifactsDirIsDefined() throws Exception {
        try {
            mockSystemPropertyConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
            mockDashboardConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
            mockServletContextConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
            mockDefaultConfigService.expects(once()).method("getArtifactsDir").will(returnValue("artifacts"));
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue(""));
            service.getArtifactsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnLogDirWithCCHOMEWhenArtifactsDirIsRelativePathAndCCHomeIsSetuped()
            throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("getArtifactsDir").will(returnValue("artifacts"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue("/home/khu/"));
        assertEquals(new File("/home/khu/artifacts"), service.getArtifactsDir());
    }

    public void testShouldThrowExceptionWhenArtifactsDirIsRelativeAndNoCCHomeSpecified() throws Exception {
        try {
            mockSystemPropertyConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
            mockDashboardConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
            mockServletContextConfigService.expects(once()).method("getArtifactsDir").will(returnValue(""));
            mockDefaultConfigService.expects(once()).method("getArtifactsDir").will(returnValue("artifacts"));
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue(""));
            service.getArtifactsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnAbsoluteArtifactsDirAndIgnoreCCHome() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getArtifactsDir").will(
                returnValue("/home/khu/artifacts"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/khu/artifacts"), service.getArtifactsDir());
    }

    //projects
    public void testShouldThrowExceptionWhenNoProjectsDirIsDefined() throws Exception {
        try {
            mockSystemPropertyConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
            mockDashboardConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
            mockServletContextConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
            mockDefaultConfigService.expects(once()).method("getProjectsDir").will(returnValue("projects"));
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue(""));
            service.getProjectsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnLogDirWithCCHOMEWhenProjectsDirIsRelativePathAndCCHomeIsSetuped()
            throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("getProjectsDir").will(returnValue("projects"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue("/home/khu/"));
        assertEquals(new File("/home/khu/projects"), service.getProjectsDir());
    }

    public void testShouldThrowExceptionWhenProjectsDirIsRelativeAndNoCCHomeSpecified() throws Exception {
        try {
            mockSystemPropertyConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
            mockDashboardConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
            mockServletContextConfigService.expects(once()).method("getProjectsDir").will(returnValue(""));
            mockDefaultConfigService.expects(once()).method("getProjectsDir").will(returnValue("projects"));
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            mockSystemPropertyConfigService.expects(once()).method("getCCHome").will(returnValue(""));
            service.getProjectsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnAbsoluteProjectsDirAndIgnoreCCHome() throws Exception {
        mockSystemPropertyConfigService.expects(once()).method("getProjectsDir").will(
                returnValue("/home/khu/projects"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/khu/projects"), service.getProjectsDir());
    }

    private void setReturnValueOfForceBuild(String value) {
        mockSystemPropertyConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(""));
        mockDashboardConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(""));
        mockServletContextConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(""));
        mockDefaultConfigService.expects(once()).method("isForceBuildEnabled").will(returnValue(value));
    }
}
