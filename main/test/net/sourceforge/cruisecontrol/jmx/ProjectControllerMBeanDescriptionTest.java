package net.sourceforge.cruisecontrol.jmx;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Project;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Dan Rollo
 *         Date: Aug 4, 2010
 *         Time: 12:58:42 PM
 */
public class ProjectControllerMBeanDescriptionTest extends TestCase {

    /** The default parameter name prefix. */
    private static final String DEFAULT_PREFIX_PARAM_NAME = "param";

    /** The default parameter description prefix. */
    private static final String DEFAULT_PREFIX_PARAM_DESC = "Operation's parameter n. ";

    private ProjectControllerMBean mbeanProjectController;
    private ProjectControllerMBeanDescription mbeanDescription;

    protected void setUp() throws Exception {
        mbeanProjectController = new ProjectController(new Project());

        mbeanDescription = new ProjectControllerMBeanDescription();
    }


    public void testGetOperationParameterNullMethod() throws Exception {
        assertEquals("Invalid param: method should still call super.",
                DEFAULT_PREFIX_PARAM_NAME + "0", mbeanDescription.getOperationParameterName(null, -1));

        assertEquals("Invalid param: method should still call super.",
                DEFAULT_PREFIX_PARAM_DESC + "0", mbeanDescription.getOperationParameterDescription(null, -1));
    }

    public void testGetOperationParameterDefaults() throws Exception {
        final Method method = mbeanProjectController.getClass().getMethod("getLastBuild", new Class[]{});

        assertEquals("Invalid param: index should still call super.",
                DEFAULT_PREFIX_PARAM_NAME + "0", mbeanDescription.getOperationParameterName(method, -1));

        assertEquals("Invalid param: index should still call super.",
                DEFAULT_PREFIX_PARAM_DESC + "0", mbeanDescription.getOperationParameterDescription(method, -1));


        assertEquals("Invalid param: index should still call super.",
                DEFAULT_PREFIX_PARAM_NAME + "1", mbeanDescription.getOperationParameterName(method, 0));

        assertEquals("Invalid param: index should still call super.",
                DEFAULT_PREFIX_PARAM_DESC + "1", mbeanDescription.getOperationParameterDescription(method, 0));
    }


    private void checkAttribute(final String attributeName, final String expectedMethodDescription) {
        assertEquals(expectedMethodDescription, mbeanDescription.getAttributeDescription(attributeName));
    }

    private void checkMethod(final Method method, final String expectedMethodDescription) {
        assertEquals(expectedMethodDescription, mbeanDescription.getOperationDescription(method));
    }

    private void checkParameter(final Method method,
                                           final int parameterIndex,
                                           final String expectedParameterName, final String expectedParameterDesc)
            {

        assertEquals(expectedParameterName, mbeanDescription.getOperationParameterName(method, parameterIndex));
        assertEquals(expectedParameterDesc, mbeanDescription.getOperationParameterDescription(method, parameterIndex));
    }

    public void testInfoPause() throws Exception {
        final String methodName = "pause";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Pauses the project");
    }

    public void testInfoResume() throws Exception {
        final String methodName = "resume";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Resumes the project when it's paused");
    }

    public void testInfoBuild() throws Exception {
        final String methodName = "build";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Forces a build of the project");
    }

    public void testInfoBuildWithTarget() throws Exception {
        final String methodName = "buildWithTarget";
        final Class[] paramTypes = new Class[]{String.class};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Forces a build of the project using the given target");

        checkParameter(method, 0, "target", "The target to invoke");
    }

    public void testInfoBuildWithTargetAdditionalProps() throws Exception {
        final String methodName = "buildWithTarget";
        final Class[] paramTypes = new Class[]{String.class, Map.class};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Forces a build of the project using the given target");

        checkParameter(method, 0, "target", "The target to invoke");
        checkParameter(method, 1, "addedProperties", "The additional properties that will be passed to the build");
    }

    public void testInfoSerialize() throws Exception {
        final String methodName = "serialize";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Persists the state of the project to disk");
    }

    public void testInfoCommitMessages() throws Exception {
        final String methodName = "commitMessages";
        final Class[] paramTypes = new Class[]{};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Gets the commit messages which include the user name and the message.");
    }

    public void testInfoGetOutputLoggerID() throws Exception {
        checkAttribute("OutputLoggerID", "A unique (for this VM) identifying string for a logger instance. Intended "
                + "to allow reporting apps (eg: Dashboard) to check if the live output log file has been reset and to "
                + "start asking for output from the first line of the current output file if the logger has changed.");
    }

    public void testInfoGetLogLabels() throws Exception {
        checkAttribute("LogLabels", "A list with the names of the available log files.");
    }

    public void testInfoGetLogLabelLines() throws Exception {

        final String methodName = "getLogLabelLines";
        final Class[] paramTypes = new Class[]{String.class, int.class};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Lines from the given firstLine up to max lines, or an empty array if no more lines exist");

        checkParameter(method, 0, "logLabel", "A valid build label, must exist in the list returned by getLogLabels()");

        checkParameter(method, 1, "firstLine", "The starting line to skip to in the log for the given build label");
    }

    public void testInfoGetBuildOutput() throws Exception {

        final String methodName = "getBuildOutput";
        final Class[] paramTypes = new Class[]{Integer.class};
        final Method method = mbeanProjectController.getClass().getMethod(methodName, paramTypes);

        checkMethod(method, "Output from the live output buffer, after line specified (inclusive)");

        checkParameter(method, 0, "firstLine", "The starting line to skip to");
    }

}
