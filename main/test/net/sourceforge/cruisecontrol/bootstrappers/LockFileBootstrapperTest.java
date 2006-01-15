package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class LockFileBootstrapperTest extends TestCase {

    private LockFileBootstrapper bootstrapper;
    private List filesToDelete;
    
    protected void setUp() {
        bootstrapper = new LockFileBootstrapper();
        filesToDelete = new ArrayList();
    }

    protected void tearDown() throws Exception {
        for (Iterator iter = filesToDelete.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            file.delete();
        }
        bootstrapper = null;
        filesToDelete = null;
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
