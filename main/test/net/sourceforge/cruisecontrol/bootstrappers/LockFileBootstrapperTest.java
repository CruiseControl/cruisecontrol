package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.Util;

public class LockFileBootstrapperTest extends TestCase {

    private LockFileBootstrapper bootstrapper;
    private final FilesToDelete filesToDelete = new FilesToDelete();
    
    protected void setUp() {
        bootstrapper = new LockFileBootstrapper();
    }

    protected void tearDown() throws Exception {
        bootstrapper = null;
        filesToDelete.delete();
    }

    public void testAttemptToCreateLockFile() throws IOException, CruiseControlException {
        File lock = File.createTempFile("test", ".lck");
        filesToDelete.add(lock);
        lock.delete();
        assertFalse(lock.exists());
        
        bootstrapper.setLockFile(lock.getAbsolutePath());
        bootstrapper.setProjectName("project.name");
        
        bootstrapper.bootstrap();
        assertTrue(lock.exists());
        assertProjectName("project.name", lock);
        
        bootstrapper.bootstrap();

        bootstrapper.setProjectName("different.name");
        
        try {
            bootstrapper.bootstrap();
            fail("should throw exception when lock already exists with different name");
        } catch (CruiseControlException expected) {            
        }
    }
    
    private void assertProjectName(String expected, File lock) throws IOException {
        String actual = Util.readFileToString(lock);
        assertEquals("project name in file didn't match", expected, actual);
    }

    public void testValidateShouldThrowExceptionWhenRequiredAttributesNotSet() throws CruiseControlException {
        try {
            bootstrapper.validate();
            fail("should throw exception when lock file and project name not set");
        } catch (CruiseControlException expected) {
        }
    }
    
    public void testValidateShouldThrowExceptionWhenProjectNameNotSet() {
        bootstrapper.setLockFile("delete.me");
        try {
            bootstrapper.validate();
            fail("should throw exception when project name not set");
        } catch (CruiseControlException expected) {
        }
    }
    
    public void testValidateShouldThrowExceptionWhenLockFileNotSet() {
        bootstrapper.setProjectName("project.name");
        try {
            bootstrapper.validate();
            fail("should throw exception when lock file not set");
        } catch (CruiseControlException e) {
        }
    }
    
    public void testValidateShouldntThrowExceptionWhenRequiredAttributesAreSet() throws CruiseControlException {
        bootstrapper.setLockFile("delete.me");
        bootstrapper.setProjectName("project.name");
        bootstrapper.validate();
    }
    
}
