package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class FileSystemTest extends TestCase {

    public FileSystemTest(String name) {
        super(name);
    }

    public void testValidate() {
        FileSystem fs = new FileSystem();

        try {
            fs.validate();
            fail("FileSystem should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'folder' is a required attribute for FileSystem", e.getMessage());
        }

        fs.setFolder("folder");

        try {
            fs.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("FileSystem should not throw exceptions when required attributes are set.");
        }
    }
}
