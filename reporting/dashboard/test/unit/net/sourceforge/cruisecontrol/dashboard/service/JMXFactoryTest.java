package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class JMXFactoryTest extends MockObjectTestCase {
    private Mock envMock;

    private Mock jmxConnectorFactoryMock;

    private JMXFactory defaultFactory;

    private Mock jmxConnector;

    protected void setUp() throws Exception {
        envMock =
                mock(EnvironmentService.class, new Class[] {SystemService.class, DashboardConfigService[].class},
                        new Object[] {new SystemService(), new DashboardConfigService[]{}});
        jmxConnectorFactoryMock = mock(JMXConnectorFactory.class);
        envMock.expects(once()).method("getRmiPort").will(returnValue(1099));
        defaultFactory =
                new JMXFactory((EnvironmentService) envMock.proxy(),
                        (JMXConnectorFactory) jmxConnectorFactoryMock.proxy());
        jmxConnector = mock(JMXConnector.class);
    }

    public void testShouldInitializeTheJMXFactoryBaseOnTheJMXPortFromEnvService() throws Exception {
        JMXServiceURL serviceURL = defaultFactory.getServiceURL();
        assertEquals("service:jmx:rmi://localhost/jndi/jrmp", serviceURL.toString());
    }

    public void testShouldInitConnectorWhenNoConnectorSetYet() throws Exception {
        MBeanServerConnection exptectedConnection =
                (MBeanServerConnection) mock(MBeanServerConnection.class).proxy();
        jmxConnectorFactoryMock.expects(once()).method("connect").withAnyArguments().will(
                returnValue(jmxConnector.proxy()));
        jmxConnector.expects(once()).method("getMBeanServerConnection").withNoArguments().will(
                returnValue(exptectedConnection));
        defaultFactory.getJMXConnection();
        assertNotNull(defaultFactory.getJMXConnector());
    }

    public void testShouldSetConnectorToNullWhenCloseConnector() throws Exception {
        MBeanServerConnection exptectedConnection =
                (MBeanServerConnection) mock(MBeanServerConnection.class).proxy();
        jmxConnectorFactoryMock.expects(once()).method("connect").withAnyArguments().will(
                returnValue(jmxConnector.proxy()));
        jmxConnector.expects(once()).method("getMBeanServerConnection").withNoArguments().will(
                returnValue(exptectedConnection));
        defaultFactory.getJMXConnection();
        assertNotNull(defaultFactory.getJMXConnector());
        jmxConnector.expects(once()).method("close").withNoArguments();
        defaultFactory.closeConnector();
        assertNull(defaultFactory.getJMXConnector());
    }

    public void testShouldSetConnectorToNullWhenFailedToGetConnection() throws Exception {
        jmxConnectorFactoryMock.expects(once()).method("connect").withAnyArguments().will(
                returnValue(jmxConnector.proxy()));
        jmxConnector.expects(once()).method("getMBeanServerConnection").withNoArguments().will(
                throwException(new IOException()));
        jmxConnector.expects(once()).method("close").withNoArguments();
        defaultFactory.getJMXConnection();
        assertNull(defaultFactory.getJMXConnector());
    }

    public void testShouldSetConnectorToNullWhenFailedToCloseConnector() throws Exception {
        jmxConnectorFactoryMock.expects(once()).method("connect").withAnyArguments().will(
                returnValue(jmxConnector.proxy()));
        jmxConnector.expects(once()).method("getMBeanServerConnection").withNoArguments().will(
                throwException(new IOException()));
        jmxConnector.expects(once()).method("close").withNoArguments().will(
                throwException(new IOException()));
        defaultFactory.getJMXConnection();
        assertNull(defaultFactory.getJMXConnector());
    }
}
