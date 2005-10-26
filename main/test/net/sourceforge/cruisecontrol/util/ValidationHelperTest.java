package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.UnreadableMockFile;
import java.io.File;
import java.io.IOException;

public class ValidationHelperTest extends TestCase {
    public void testExists() throws IOException, CruiseControlException {
        try {
            ValidationHelper.assertExists(new File("THISFILEDOESNTEXIST" + System.currentTimeMillis()), "file",
                    this.getClass());
            fail("Exception expected");
        } catch (CruiseControlException expected) {
        }

        final File tempFile = File.createTempFile(ValidationHelperTest.class.getName(), "temp");
        tempFile.deleteOnExit();
        ValidationHelper.assertExists(tempFile, "file", this.getClass());
    }

    public void testExistsWithInvalidArguments() throws CruiseControlException {
        try {
            ValidationHelper
                    .assertExists(new File("THISFILEDOESNTEXIST" + System.currentTimeMillis()), null, this.getClass());
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
        }


        try {
            ValidationHelper.assertExists(new File("THISFILEDOESNTEXIST" + System.currentTimeMillis()), "foo", null);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ValidationHelper.assertExists(new File("THISFILEDOESNTEXIST" + System.currentTimeMillis()), null, null);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ValidationHelper.assertExists(null, "foo", this.getClass());
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ValidationHelper.assertExists(null, null, null);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testIsNotDirectory() throws CruiseControlException {
        try {
            ValidationHelper
                    .assertIsNotDirectory(new File(System.getProperty("java.io.tmpdir")), "foo", this.getClass());
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        ValidationHelper.assertIsNotDirectory(new File("foo"), "bar", this.getClass());
    }

    public void testIsReadable() throws CruiseControlException, IOException {
        try {
            //A file that exists, but isn't readable
            UnreadableMockFile unreadableFile = new UnreadableMockFile();
            ValidationHelper.assertIsReadable(unreadableFile, "foo", this.getClass());
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        final File tempFile = File.createTempFile(ValidationHelperTest.class.getName(), "temp");
        tempFile.deleteOnExit();
        ValidationHelper.assertIsReadable(tempFile, "bar", this.getClass());
    }
}
