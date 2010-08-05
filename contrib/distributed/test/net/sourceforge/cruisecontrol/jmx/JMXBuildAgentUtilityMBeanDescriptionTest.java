package net.sourceforge.cruisecontrol.jmx;

import junit.framework.TestCase;
import mx4j.MBeanDescriptionAdapter;

import java.lang.reflect.Method;

/**
 * @author Dan Rollo
 *         Date: Aug 4, 2010
 *         Time: 7:20:44 PM
 */
public class JMXBuildAgentUtilityMBeanDescriptionTest extends TestCase {

    private JMXBuildAgentUtilityMBean mbean;
    private MBeanDescriptionAdapter mbeanDescription;

    protected void setUp() throws Exception {
        mbean = new JMXBuildAgentUtility();

        mbeanDescription = new JMXBuildAgentUtilityMBeanDescription();
    }
    

    public void testGetOperationParameterNullMethod() throws Exception {
        assertEquals("Invalid param: method should still call super.",
                ProjectControllerMBeanDescriptionTest.DEFAULT_PREFIX_PARAM_NAME + "0",
                mbeanDescription.getOperationParameterName(null, -1));

        assertEquals("Invalid param: method should still call super.",
                ProjectControllerMBeanDescriptionTest.DEFAULT_PREFIX_PARAM_DESC + "0",
                mbeanDescription.getOperationParameterDescription(null, -1));
    }

    public void testGetOperationParameterDefaults() throws Exception {
        final Method method = mbean.getClass().getMethod("getLUSServiceIds", new Class[]{});

        assertEquals("Invalid param: index should still call super.",
                ProjectControllerMBeanDescriptionTest.DEFAULT_PREFIX_PARAM_NAME + "0",
                mbeanDescription.getOperationParameterName(method, -1));

        assertEquals("Invalid param: index should still call super.",
                ProjectControllerMBeanDescriptionTest.DEFAULT_PREFIX_PARAM_DESC + "0",
                mbeanDescription.getOperationParameterDescription(method, -1));


        assertEquals("Invalid param: index should still call super.",
                ProjectControllerMBeanDescriptionTest.DEFAULT_PREFIX_PARAM_NAME + "1",
                mbeanDescription.getOperationParameterName(method, 0));

        assertEquals("Invalid param: index should still call super.",
                ProjectControllerMBeanDescriptionTest.DEFAULT_PREFIX_PARAM_DESC + "1",
                mbeanDescription.getOperationParameterDescription(method, 0));
    }

    public void testInfoDestroyLUS() throws Exception {
        final String methodName = "destroyLUS";
        final Class[] paramTypes = new Class[]{String.class};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Stop the Lookup Service with the given ServiceId.");

        ProjectControllerMBeanDescriptionTest.checkParameter(mbeanDescription, 
                method, 0, "lusServiceId", "The ServiceID of the Registrar to be destroyed.");
    }

    public void testInfoRefresh() throws Exception {
        final String methodName = "refresh";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Reload information about Build Agents.");
    }

    public void testInfoKill() throws Exception {
        final String methodName = "kill";
        final Class[] paramTypes = new Class[]{String.class};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Kill the Build Agent who's ServiceId is specified.");

        ProjectControllerMBeanDescriptionTest.checkParameter(mbeanDescription,
                method, 0, "agentServiceId", "The ServiceID of the Build Agent to be killed.");
    }

    public void testInfoKillAll() throws Exception {
        final String methodName = "killAll";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Kill all Build Agents.");
    }

    public void testInfoRestart() throws Exception {
        final String methodName = "restart";
        final Class[] paramTypes = new Class[]{String.class};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Restart the (webstart) Build Agent who's ServiceId is specified. NOTE: The "
                        + "agent specified MUST have been launched via webstart, or this call will fail.");

        ProjectControllerMBeanDescriptionTest.checkParameter(mbeanDescription,
                method, 0, "agentServiceId", "The ServiceID of the Build Agent to be restarted.");
    }

    public void testInfoRestartAll() throws Exception {
        final String methodName = "restartAll";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Restart all (webstart) Build Agents.");
    }

    public void testInfoLookupServiceCount() throws Exception {
        ProjectControllerMBeanDescriptionTest.checkAttribute(mbeanDescription, 
                "LookupServiceCount", "The number of Lookup Services (Registrars) found.");
    }

    public void testInfoLUSServiceIds() throws Exception {
        ProjectControllerMBeanDescriptionTest.checkAttribute(mbeanDescription,
                "LUSServiceIds", "The ServiceId of Lookup Services (Registrars) found.");
    }

    public void testInfoBuildAgents() throws Exception {
        ProjectControllerMBeanDescriptionTest.checkAttribute(mbeanDescription,
                "BuildAgents", "A big knarly string representation of all Build Agents found.");
    }

    public void testInfoBuildAgentServiceIds() throws Exception {
        ProjectControllerMBeanDescriptionTest.checkAttribute(mbeanDescription,
                "BuildAgentServiceIds",
                "Use the ServiceId (the part after '<hostname>: ') as the "
                + "parameter value to kill() or restart() calls. The ServiceId uniquely identifies a Build Agent.");
    }

    public void testInfoKillOrRestartAfterBuildFinished() throws Exception {
        ProjectControllerMBeanDescriptionTest.checkAttribute(mbeanDescription,
                "KillOrRestartAfterBuildFinished",
                "If true, any invocation of kill or restart on a busy agent will wait until the currently running "
                + "build finishes. If false, invocation of kill or restart will occur immediately, even if the agent "
                + "is currently busy building a project.");
    }
}
