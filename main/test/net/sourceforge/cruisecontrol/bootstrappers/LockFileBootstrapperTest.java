package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;

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
        
        bootstrapper.bootstrap();
        assertTrue(lock.exists());
        
        try {
            bootstrapper.bootstrap();
            fail("should throw exception when lock already exists");
        } catch (CruiseControlException expected) {            
        }
    }
    
    public void testValidate() throws CruiseControlException {
        try {
            bootstrapper.validate();
            fail("should throw exception when lock file not set");
        } catch (CruiseControlException expected) {
        }
        
        bootstrapper.setLockFile("delete.me");
        bootstrapper.validate();
    }
    
}
