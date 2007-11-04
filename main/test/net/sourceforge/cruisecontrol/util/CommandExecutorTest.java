package net.sourceforge.cruisecontrol.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class CommandExecutorTest extends TestCase {

    public void testExecuteAndWaitThrowsExceptionOnNonZeroExitCode() throws Exception {
        CommandExecutor executor = new CommandExecutor(new ReturnFailedExitCodeCommandLine());
        try {
            executor.executeAndWait();
            fail("Should have thrown exception for non zero exit code");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    private class ReturnFailedExitCodeCommandLine extends Commandline {
        public Process execute() throws IOException {
            MockProcess returnNonZeroProcess = new MockProcess(new ByteArrayOutputStream());
            returnNonZeroProcess.setExitValue(-1);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
            returnNonZeroProcess.setErrorStream(inputStream);
            returnNonZeroProcess.setInputStream(inputStream);

            return returnNonZeroProcess;
        }
    }
}
