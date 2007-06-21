package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;

import javax.servlet.ServletContext;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class EnvironmentServiceTest extends MockObjectTestCase {
    private EnvironmentService service;

    private Mock mockContext;

    private Mock mockSystemService;

    protected void setUp() throws Exception {
        mockSystemService = mock(SystemService.class);
        service = new EnvironmentService((SystemService) mockSystemService.proxy());
        mockContext = mock(ServletContext.class);
        service.setServletContext((ServletContext) mockContext.proxy());
    }

    public void testShouldReturnTrueIfNotCruiseConfigSetup() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_EDITABLE, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_EDITABLE)).will(returnValue(""));
        assertEquals(true, service.isConfigFileEditable());
    }

    public void testShouldReturnJMXPortFromContextParam() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_JMX_PORT)).will(returnValue("9001"));
        assertEquals(9001, service.getJmxPort());
    }

    public void testShouldReturnJMXPortFromSystemProperties() throws Exception {
        mockContext.expects(never()).method("getInitParameter");
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "9000");
        assertEquals(9000, service.getJmxPort());
    }

    public void testShouldReturnJMXPort8000AsTheDefaultPort() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_JMX_PORT)).will(returnValue(""));
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "");
        assertEquals(8000, service.getJmxPort());
    }

    public void testShouldReturnRMIPortFromSystemProperties() throws Exception {
        mockContext.expects(never()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_RMI_PORT)).will(returnValue(""));
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_RMI_PORT, "2000");
        assertEquals(2000, service.getRmiPort());
    }

    public void testShouldReturnRMIPort1099AsTheDefaultPort() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_RMI_PORT)).will(returnValue(""));
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_RMI_PORT, "");
        assertEquals(1099, service.getRmiPort());
    }

    public void testShouldReturnFalseIfForceBuildSetToDisableInContextParam() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED)).will(
                returnValue("disabled"));
        assertEquals(false, service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToEnableInContextParam() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED)).will(
                returnValue("enabled"));
        assertEquals(true, service.isForceBuildEnabled());
    }

    public void testShouldReturnFalseIfForceBuildSetToDisableInSystemPropery() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "disabled");
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(false, service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToEnableInSystemPropery() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "enabled");
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(true, service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueAsDefault() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED)).will(returnValue(""));
        assertEquals(true, service.isForceBuildEnabled());
    }

    public void testShouldReturnLogsIfLogDirIsSetupInSystemPropery() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "/home/user/logs");
        mockContext.expects(never()).method("getInitParameter");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/logs"), service.getLogDir());
    }

    public void testShouldReturnLogsFolderFromInitParameters() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_LOG_DIR)).will(
                returnValue("/home/user/logs"));
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/logs"), service.getLogDir());
    }

    public void testShouldThrowExceptionWhenNoLogDirIsDefined() throws Exception {
        try {
            mockContext.expects(once()).method("getInitParameter").with(
                    eq(EnvironmentService.CONTEXT_CC_CONFIG_LOG_DIR)).will(returnValue(""));
            setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "");
            setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "");
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            service.getLogDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnLogDirWithCCHOMEWhenLogDirIsRelativePathAndCCHomeIsSetuped()
            throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "logs");
        setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "/home/khu/");
        mockContext.expects(never()).method("getInitParameter");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        assertEquals(new File("/home/khu/logs"), service.getLogDir());
    }

    public void testShouldReturnLogDirWithCCHOMEWhenLogDirIsNotSetupedAndCCHomeIsSetuped()
            throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_LOG_DIR)).will(returnValue(""));
        setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "/home/khu/");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        assertEquals(new File("/home/khu/logs"), service.getLogDir());
    }

    public void testShouldThrowExceptionWhenLogDirIsRelativeAndNoCCHomeSpecified() throws Exception {
        try {
            setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "logs");
            setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "");
            mockContext.expects(never()).method("getInitParameter");
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            service.getLogDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnAbsoluteLogDirAndIgnoreCCHome() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_LOG_DIR, "/home/khu/logs");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(new File("/home/khu/logs"), service.getLogDir());
    }

    private void setUpSystemProperty(String key, String value) {
        mockSystemService.expects(once()).method("getProperty").with(eq(key)).will(
                returnValue(value));
    }

    public void testShouldReturnArtifactsIfArtifactDirIsSetupInSystemPropery() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR,
                "/home/user/artifacts");
        mockContext.expects(never()).method("getInitParameter");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/artifacts"), service.getArtifactsDir());
    }

    public void testShouldReturnArtifactsFolderFromInitParameters() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_ARTIFACTS_DIR)).will(
                returnValue("/home/user/artifacts"));
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR, "");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/artifacts"), service.getArtifactsDir());
    }

    public void testShouldThrowExceptionIfNoArtifactsDefined() throws Exception {
        try {
            mockContext.expects(once()).method("getInitParameter").with(
                    eq(EnvironmentService.CONTEXT_CC_CONFIG_ARTIFACTS_DIR)).will(returnValue(""));
            setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR, "");
            setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "");
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            service.getArtifactsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnArtifactsDirWithCCHOMEWhenArtifactsDirIsRelativePathAndCCHomeIsSetuped()
            throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR, "artifacts");
        setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "/home/khu/");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(new File("/home/khu/artifacts"), service.getArtifactsDir());
    }

    public void testShouldReturnArtifactsDirWithCCHOMEWhenArtifactsDirIsNotSetupedAndCCHomeIsSetuped()
            throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_ARTIFACTS_DIR)).will(returnValue(""));
        setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "/home/khu/");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        assertEquals(new File("/home/khu/artifacts"), service.getArtifactsDir());
    }

    public void testShouldThrowExceptionWhenArtifactsDirIsRelativeAndNoCCHomeSpecified()
            throws Exception {
        try {
            setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR, "artifacts");
            setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "");
            mockContext.expects(never()).method("getInitParameter");
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            service.getArtifactsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnAbsoluteArtifactsDirAndIgnoreCCHome() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR, "/home/khu/artifacts");
        mockContext.expects(never()).method("getInitParameter");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/khu/artifacts"), service.getArtifactsDir());
    }

    public void testShouldReturnProjectsIfProjectsDirIsSetupInSystemPropery() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR,
                "/home/user/projects");
        mockContext.expects(never()).method("getInitParameter");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/projects"), service.getProjectsDir());
    }

    public void testShouldReturnProjectsFolderFromInitParameters() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_PROJECTS_DIR)).will(
                returnValue("/home/user/projects"));
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR, "");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/user/projects"), service.getProjectsDir());
    }

    public void testShouldThrowExceptionIfNoProjectsDefined() throws Exception {
        try {
            mockContext.expects(once()).method("getInitParameter").with(
                    eq(EnvironmentService.CONTEXT_CC_CONFIG_PROJECTS_DIR)).will(returnValue(""));
            setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR, "");
            setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "");
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            service.getProjectsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnProjectsDirWithCCHOMEWhenProjectsDirIsRelativePathAndCCHomeIsSetuped()
            throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR, "projects");
        setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "/home/khu/");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(new File("/home/khu/projects"), service.getProjectsDir());
    }

    public void testShouldReturnProjectsDirWithCCHOMEWhenProjectsDirIsNotSetupedAndCCHomeIsSetuped()
            throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_PROJECTS_DIR)).will(returnValue(""));
        setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "/home/khu/");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        assertEquals(new File("/home/khu/projects"), service.getProjectsDir());
    }

    public void testShouldThrowExceptionWhenProjectsDirIsRelativeAndNoCCHomeSpecified()
            throws Exception {
        try {
            setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR, "projects");
            setUpSystemProperty(EnvironmentService.PROPS_CC_HOME, "");
            mockContext.expects(never()).method("getInitParameter");
            mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
            service.getProjectsDir();
            fail("Exception expected");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    public void testShouldReturnAbsoluteProjectsDirAndIgnoreCCHome() throws Exception {
        setUpSystemProperty(EnvironmentService.PROPS_CC_CONFIG_PROJECTS_DIR, "/home/khu/projects");
        mockContext.expects(never()).method("getInitParameter");
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        assertEquals(new File("/home/khu/projects"), service.getProjectsDir());
    }
}
