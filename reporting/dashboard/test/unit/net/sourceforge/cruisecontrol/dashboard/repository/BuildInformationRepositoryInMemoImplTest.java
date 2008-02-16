package net.sourceforge.cruisecontrol.dashboard.repository;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.dashboard.service.JMXConnectorFactory;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;

import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;

import java.util.Map;
import java.util.HashMap;
import java.net.MalformedURLException;

public class BuildInformationRepositoryInMemoImplTest extends MockObjectTestCase {

    private BuildInformationRepository repository;

    private Mock connectorFactoryMock;

    private Mock jmxConnectorMock;

    private static final String SERVER_NAME = "server1";

    private Mock jmxInfoMock;

    private static final String RMI_URL = "http://server1:1099";

    protected void setUp() throws Exception {
        connectorFactoryMock = mock(JMXConnectorFactory.class);
        jmxConnectorMock = mock(JMXConnector.class);
        repository =
                new BuildInformationRepositoryInMemoImpl((JMXConnectorFactory) connectorFactoryMock.proxy());
        jmxInfoMock =
                mock(BuildLoopInformation.JmxInfo.class, new Class[] {String.class}, new Object[] {SERVER_NAME});
    }

    protected void tearDown() throws Exception {
        repository.removeAll();
    }

    public void testShouldSaveBuildInformationWhenNoCorrespondingBuildInformationExist() throws Exception {
        assertEquals(0, repository.size());
        save(new String[] {"project1"});
        save(new String[] {"project1", "project2"});
        assertEquals(2, repository.size());
    }

    public void testShouldSaveBuildInformationWhenBuildInformationExist() throws Exception {
        assertEquals(0, repository.size());
        save(new String[] {"project1"});
        save(new String[] {"project1"});
        assertEquals(1, repository.size());
    }

    public void testShouldRemoveMissingProjects() throws Exception {
        assertEquals(0, repository.size());
        save(new String[] {"project1", "project2"});
        assertEquals(2, repository.size());
        save(new String[] {"project1"});
        assertEquals(1, repository.size());
    }

    public void testShouldReturnAllTheBuildLoopInformations() throws Exception {
        save(new String[] {"project1"});
        assertEquals(1, repository.getProjectInfos().size());
    }

    public void testShouldReturnSpecificBuildLoop() throws Exception {
        save(new String[] {"project1"});
        assertEquals("project1", repository.getProjectInfo("project1").getName());
    }

    public void testShouldReturnJMXConnection() throws Exception {
        save(new String[] {"project1"});
        Mock connection = mock(MBeanServerConnection.class);
        jmxInfoMock.expects(atLeastOnce())
            .method("getRmiUrl")
            .will(returnValue(RMI_URL));
        connectorFactoryMock.expects(atLeastOnce())
            .method("connect")
            .with(eq(jmxServiceUrl(RMI_URL)), eq(environment(RMI_URL)))
            .will(returnValue(jmxConnectorMock.proxy()));
        jmxConnectorMock.expects(once()).method("getMBeanServerConnection")
            .will(returnValue(connection.proxy()));
        assertEquals(connection.proxy(), repository.getJmxConnection("project1"));
    }

    public void testShouldReturnNullIfProjectNotFound() throws Exception {
        save(new String[] {"differentProject"});
        assertNull(repository.getJmxConnection("not existing project"));
    }

    public void testShouldReturnIfRepositoryKnownAboutAProject() throws Exception {
        save(new String[] {"project1"});
        assertTrue("Project should be known", repository.hasBuildLoopInfoFor("project1"));
        assertFalse("Project should be unknown", repository.hasBuildLoopInfoFor("unknown_project"));
    }

    private JMXServiceURL jmxServiceUrl(String rmiUrl) throws MalformedURLException {
        return new JMXServiceURL("service:jmx:" + rmiUrl + "/jndi/jrmp");
    }

    private Map environment(String rmiUrl) {
        Map environmentMap = new HashMap();
        environmentMap.put("java.naming.factory.initial", "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environmentMap.put("java.naming.provider.url", rmiUrl);
        return environmentMap;
    }

    private void save(String[] projectNames) {
        ProjectInfo[] projectInfos = new ProjectInfo[projectNames.length];
        for (int i = 0; i < projectNames.length; i++) {
            projectInfos[i] = new ProjectInfo(projectNames[i], null, null);
        }
        BuildLoopInformation.JmxInfo jmxinfo = (BuildLoopInformation.JmxInfo) jmxInfoMock.proxy();
        repository.saveOrUpdate(new BuildLoopInformation(projectInfos, jmxinfo, SERVER_NAME, null));
    }
}
