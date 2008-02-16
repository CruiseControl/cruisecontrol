package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.BuildLoopInformation.JmxInfo;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import javax.management.MBeanServerConnection;

public class JmxConnectionCacheServiceTest extends MockObjectTestCase {
    private JmxConnectionCacheService cachedConnections;

    private Mock buildInfoRepoMock;

    private Mock proceedingJoinPoint;

    private Mock jmxInfoMock;

    private Mock buildloopinfoMock;

    protected void setUp() throws Exception {
        jmxInfoMock = mock(JmxInfo.class, new Class[]{String.class}, new Object[]{"localhost"});
        buildloopinfoMock =
                mock(
                        BuildLoopInformation.class, new Class[]{ProjectInfo[].class, JmxInfo.class,
                        String.class, String.class}, new Object[]{null, null, null, null});
        buildInfoRepoMock = mock(BuildInformationRepository.class);
        proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        cachedConnections =
                new JmxConnectionCacheService((BuildInformationRepository) buildInfoRepoMock.proxy());
    }

    public void testShouldInitConnectionWhenNoConnectionSetYet() throws Throwable {
        MBeanServerConnection expectedConnection =
                (MBeanServerConnection) mock(MBeanServerConnection.class).proxy();
        buildInfoRepoMock.expects(once()).method("getBuildLoopInfo").with(eq("project1")).will(
                returnValue(buildloopinfoMock.proxy()));
        buildloopinfoMock.expects(once()).method("getJmxInfo").withNoArguments().will(
                returnValue(jmxInfoMock.proxy()));

        proceedingJoinPoint.expects(once()).method("proceed").will(returnValue(expectedConnection));
        assertEquals(
                expectedConnection, cachedConnections.getJMXConnection(
                (ProceedingJoinPoint) proceedingJoinPoint.proxy(), "project1"));
    }

    public void testShouldReturnExistingConnectionIfConnectionHasBeenSetAlready() throws Throwable {
        MBeanServerConnection expectedConnection =
                (MBeanServerConnection) mock(MBeanServerConnection.class).proxy();
        String projectName = "project1";
        buildInfoRepoMock.expects(atLeastOnce()).method("getBuildLoopInfo").with(eq(projectName)).will(
                returnValue(buildloopinfoMock.proxy()));
        buildloopinfoMock.expects(atLeastOnce()).method("getJmxInfo").withNoArguments().will(
                returnValue(jmxInfoMock.proxy()));

        proceedingJoinPoint.expects(once()).method("proceed").will(returnValue(expectedConnection));
        assertEquals(
                expectedConnection, cachedConnections.getJMXConnection(
                (ProceedingJoinPoint) proceedingJoinPoint.proxy(), projectName));
        assertEquals(
                expectedConnection, cachedConnections.getJMXConnection(
                (ProceedingJoinPoint) proceedingJoinPoint.proxy(), projectName));
    }
}
