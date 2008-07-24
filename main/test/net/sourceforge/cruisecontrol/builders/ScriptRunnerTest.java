package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.MockCommandline;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;
import java.io.ByteArrayInputStream;

/**
 * @author Dan Rollo
 * Date: Jul 23, 2008
 * Time: 4:33:27 PM
 */
public class ScriptRunnerTest extends TestCase {

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
}
