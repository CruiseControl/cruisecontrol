package net.sourceforge.cruisecontrol.builders;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.MockCommandline;

import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dan Rollo
 * Date: Jul 23, 2008
 * Time: 4:33:27 PM
 */
public class ScriptRunnerTest extends TestCase {

    /* @todo Update junit to 4.7+
    requires junit 4.7+ to read name of currently running test
    @Rule public TestName name = new TestName();

    @Test public void testA() {
            assertEquals("testA", name.getMethodName());
        }
    */

    @Before
    public void setUp() {
        final File expectedAntBuilderLog = new File(TestUtil.getTargetDir(), AntOutputLogger.DEFAULT_OUTFILE_NAME);
        if (expectedAntBuilderLog.exists()) {
            assertTrue(expectedAntBuilderLog.delete());
        }
    }

    @Test
    public void testDoNotCreateAntOutputLoggerFile() throws Exception {

        final MockCommandline mockCommandline = new MockCommandline();
        mockCommandline.setAssertCorrectCommandline(false);

        mockCommandline.setProcessInputStream(
                // need to provide some output to ensure StreamPumper threads have some work to do
                new ByteArrayInputStream("some stuff\nmore stuff\n".getBytes()));

        mockCommandline.setProcessErrorStream(new ByteArrayInputStream("".getBytes()));

        final Script dummyScript = new Script() {

            public Commandline buildCommandline() throws CruiseControlException {
                return mockCommandline;
            }

            public void setExitCode(int result) {
            }

            public int getExitCode() {
                return 0;
            }
        };

        final ScriptRunner sr = new ScriptRunner();

        sr.runScript(TestUtil.getTargetDir(), dummyScript, ScriptRunner.NO_TIMEOUT);

        final File expectedAntBuilderLog = new File(TestUtil.getTargetDir(), AntOutputLogger.DEFAULT_OUTFILE_NAME);
        assertFalse("Generic ScriptRunner should not have created AntOuputLogger file: "
                + expectedAntBuilderLog.getAbsolutePath(),
                expectedAntBuilderLog.exists());
    }

    @Test
    public void testClearOutputLoggerFileAtEndOfScript() throws Exception {

        final String logFilename = "logFilename-" + getName();
        final File outputLog = new File(TestUtil.getTargetDir(), logFilename);
        assertFalse(outputLog.exists());

        final MockCommandline mockCommandline = new MockCommandline();
        mockCommandline.setAssertCorrectCommandline(false);

        mockCommandline.setProcessInputStream(
                // need to provide some output to ensure StreamPumper threads have some work to do
                new ByteArrayInputStream("some stuff\nmore stuff\n".getBytes()));

        mockCommandline.setProcessErrorStream(new ByteArrayInputStream("".getBytes()));

        final Script dummyScript = new Script() {

            public Commandline buildCommandline() throws CruiseControlException {
                return mockCommandline;
            }

            public void setExitCode(int result) {
            }

            public int getExitCode() {
                return 0;
            }
        };

        final ScriptRunner sr = new ScriptRunner();

        final TestBuilder builder = new TestBuilder();

        final String projectName = "testProjectName";

        final BuildOutputLogger buildOutputLogger = builder.getBuildOutputConsumer(projectName,
                outputLog.getParentFile(), outputLog.getName());
        final String origId = buildOutputLogger.getID();

        sr.runScript(TestUtil.getTargetDir(), dummyScript, ScriptRunner.NO_TIMEOUT, buildOutputLogger);

        assertFalse("LiveOutput log should be cleared at end of script run: " + outputLog.getAbsolutePath(),
                outputLog.exists());

        final String currentId = buildOutputLogger.getID();
        assertTrue("buildOutputLogger id should change due to clear() at end of script run. origId: " + origId
                + "; current Id: " + currentId,
                currentId.startsWith(origId) && (currentId.length() > origId.length()));
    }

    private static final class TestBuilder extends Builder {
        private static final long serialVersionUID = -4055176461374960419L;

        public Element build(Map properties, Progress progress) throws CruiseControlException {
            return null;
        }

        public Element buildWithTarget(Map properties, String target, Progress progress) throws CruiseControlException {
            return null;
        }

        public BuildOutputLogger getBuildOutputConsumer(final String projectName,
                                                        final File workingDir, final String logFilename) {
            return super.getBuildOutputConsumer(projectName, workingDir, logFilename);
        }
    }

}
