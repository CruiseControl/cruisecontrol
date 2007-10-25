package net.sourceforge.cruisecontrol.distributed.core;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;

/**
 * @author Dan Rollo
 * Date: Oct 24, 2007
 * Time: 12:15:23 AM
 */
public class RemoteResultTest extends TestCase {

    /** expose msg text for use by other unit tests. */
    public static final String MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT
            = RemoteResult.MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT;

    public static void resetTempZippedFile(final RemoteResult remoteResult) { remoteResult.resetTempZippedFile(); }

    public void testValidate() throws Exception {
        final int badIdx = -1;
        try {
            new RemoteResult(badIdx).validate();
            fail("Invalid index should have failed.");
        } catch (CruiseControlException e) {
            assertEquals("Invalid remoteResult index: " + badIdx, e.getMessage());
        }

        final RemoteResult remoteResult = new RemoteResult(0);
        try {
            remoteResult.validate();
            fail("Invalid index should have failed.");
        } catch (CruiseControlException e) {
            assertEquals("'agentDir' is required for "
                    + RemoteResultTest.MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT, e.getMessage());
        }
        remoteResult.setAgentDir("agent");

        try {
            remoteResult.validate();
            fail("Invalid index should have failed.");
        } catch (CruiseControlException e) {
            assertEquals("'masterDir' is required for "
                    + RemoteResultTest.MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT, e.getMessage());
        }
        remoteResult.setMasterDir("master");

        remoteResult.validate();
    }

    public void testImutable() throws Exception {
        final RemoteResult remoteResult = new RemoteResult(0);
        remoteResult.setAgentDir("agent");
        try {
            remoteResult.setAgentDir("reset");
            fail("Reset should fail");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("agentDir already set to: "));
        }

        remoteResult.setMasterDir("master");
        try {
            remoteResult.setMasterDir("reset");
            fail("Reset should fail");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("masterDir already set to: "));
        }

        remoteResult.storeTempZippedFile(new File("tempZip"));
        try {
            remoteResult.storeTempZippedFile(new File("dummy"));
            fail("Reset should fail");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("storeTempZippedFile already set to: "));
        }
        remoteResult.resetTempZippedFile();
        remoteResult.storeTempZippedFile(new File("tempZip"));
    }
}
