package net.sourceforge.cruisecontrol.dashboard.service;

import javax.servlet.ServletContext;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class EnvironmentServiceTest extends MockObjectTestCase {
    private EnvironmentService service;

    private Mock mockContext;

    protected void setUp() throws Exception {
        clearSystemProperties();
        service = new EnvironmentService();
        mockContext = mock(ServletContext.class);
        service.setServletContext((ServletContext) mockContext.proxy());
    }

    protected void tearDown() throws Exception {
        clearSystemProperties();
    }

    private void clearSystemProperties() {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, "");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_EDITABLE, "");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_RMI_PORT, "");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_RMI_PORT, "");
    }

    public void testShouldReturnTrueIfNotCruiseConfigSetup() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_EDITABLE, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_EDITABLE)).will(returnValue(""));
        assertEquals(true, service.isConfigFileEditable());
    }

    public void testShouldReturnJMXPortFromContextParam() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_JMX_PORT)).will(returnValue("9001"));
        assertEquals(9001, service.getJmxPort());
    }

    public void testShouldReturnJMXPortFromSystemProperties() throws Exception {
        mockContext.expects(never()).method("getInitParameter");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "9000");
        assertEquals(9000, service.getJmxPort());
    }

    public void testShouldReturnJMXPort8000AsTheDefaultPort() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_JMX_PORT)).will(returnValue(""));
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_JMX_PORT, "");
        assertEquals(8000, service.getJmxPort());
    }

    public void testShouldReturnRMIPortFromSystemProperties() throws Exception {
        mockContext.expects(never()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_RMI_PORT)).will(returnValue(""));
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_RMI_PORT, "2000");
        assertEquals(2000, service.getRmiPort());
    }

    public void testShouldReturnRMIPort1099AsTheDefaultPort() throws Exception {
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_RMI_PORT)).will(returnValue(""));
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_RMI_PORT, "");
        assertEquals(1099, service.getRmiPort());
    }

    public void testShouldReturnFalseIfForceBuildSetToDisableInContextParam() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED)).will(
                returnValue("disabled"));
        assertEquals(false, service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToEnableInContextParam() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED)).will(
                returnValue("enabled"));
        assertEquals(true, service.isForceBuildEnabled());
    }
    
    public void testShouldReturnFalseIfForceBuildSetToDisableInSystemPropery() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "disabled");
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(false, service.isForceBuildEnabled());
    }

    public void testShouldReturnTrueIfForceBuildSetToenableInSystemPropery() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "enabled");
        mockContext.expects(never()).method("getInitParameter");
        assertEquals(true, service.isForceBuildEnabled());
    }
    
    public void testShouldReturnTrueAsDefault() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "");
        mockContext.expects(once()).method("getInitParameter").with(
                eq(EnvironmentService.CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED)).will(returnValue(""));
        assertEquals(true, service.isForceBuildEnabled());
    }
}
