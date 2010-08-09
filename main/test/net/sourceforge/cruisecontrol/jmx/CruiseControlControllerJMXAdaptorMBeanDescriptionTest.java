package net.sourceforge.cruisecontrol.jmx;

import junit.framework.TestCase;

import mx4j.MBeanDescriptionAdapter;
import net.sourceforge.cruisecontrol.CruiseControlController;

import java.lang.reflect.Method;

/**
 * @author Dan Rollo
 *         Date: Aug 9, 2010
 *         Time: 12:46:54 AM
 *         To change this template use File | Settings | File Templates.
 */
public class CruiseControlControllerJMXAdaptorMBeanDescriptionTest extends TestCase {

    private CruiseControlControllerJMXAdaptorMBean mbean;
    private MBeanDescriptionAdapter mbeanDescription;

    protected void setUp() throws Exception {
        mbean = new CruiseControlControllerJMXAdaptor(new CruiseControlController());

        mbeanDescription = new CruiseControlControllerJMXAdaptorMBeanDescription();
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

    public void testInfoPause() throws Exception {
        final String methodName = "pause";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Pauses the server.");
    }

    public void testInfoReloadConfigFile() throws Exception {
        final String methodName = "reloadConfigFile";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Re-read the server configuration file.");
    }

    public void testInfoResume() throws Exception {
        final String methodName = "resume";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Resumes the server when it is paused.");
    }

    public void testInfoHalt() throws Exception {
        final String methodName = "halt";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "Shutdown this server.");
    }

    public void testInfogGtPluginInfo() throws Exception {
        final String methodName = "getPluginInfo";
        final Class[] paramTypes = new Class[]{String.class};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "The PluginInfo tree for the give project, or whole server if projectName parameter is null.");

        ProjectControllerMBeanDescriptionTest.checkParameter(mbeanDescription,
                method, 0, "projectName", "Null to fetch entire tree, or a single project name.");
    }

    public void testInfogGtPluginHTML() throws Exception {
        final String methodName = "getPluginHTML";
        final Class[] paramTypes = new Class[]{String.class};
        final Method method = mbean.getClass().getMethod(methodName, paramTypes);

        ProjectControllerMBeanDescriptionTest.checkMethod(mbeanDescription, method,
                "The HTML plugin content for the give project, or whole server if projectName parameter is null.");

        ProjectControllerMBeanDescriptionTest.checkParameter(mbeanDescription,
                method, 0, "projectName", "Null to fetch entire tree, or a single project name.");
    }

    public void testInfoGetOutputLoggerID() throws Exception {
        ProjectControllerMBeanDescriptionTest.checkAttribute(mbeanDescription,
                "ConfigFileName", "The name of the config file this server reads its settings from.");
    }

}
