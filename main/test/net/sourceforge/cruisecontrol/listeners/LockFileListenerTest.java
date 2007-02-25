package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;

public class LockFileListenerTest extends TestCase {

    private LockFileListener listener;
    private final FilesToDelete filesToDelete = new FilesToDelete();
    
    private static final ProjectStateChangedEvent BOOTSTRAPPING = new ProjectStateChangedEvent("test",
                    ProjectState.BOOTSTRAPPING);
    private static final ProjectStateChangedEvent MODIFICATIONSET = new ProjectStateChangedEvent("test",
                    ProjectState.MODIFICATIONSET);
    private static final ProjectStateChangedEvent BUILDING = new ProjectStateChangedEvent("test",
                    ProjectState.BUILDING);
    private static final ProjectStateChangedEvent MERGING_LOGS = new ProjectStateChangedEvent("test",
                    ProjectState.MERGING_LOGS);
    private static final ProjectStateChangedEvent PUBLISHING = new ProjectStateChangedEvent("test",
                    ProjectState.PUBLISHING);

    private static final ProjectStateChangedEvent IDLE = new ProjectStateChangedEvent("test", ProjectState.IDLE);
    private static final ProjectStateChangedEvent WAITING = new ProjectStateChangedEvent("test", ProjectState.WAITING);
    private static final ProjectStateChangedEvent QUEUED = new ProjectStateChangedEvent("test", ProjectState.QUEUED);
    private static final ProjectStateChangedEvent PAUSED = new ProjectStateChangedEvent("test", ProjectState.PAUSED);
    private static final ProjectStateChangedEvent STOPPED = new ProjectStateChangedEvent("test", ProjectState.STOPPED);
    
    protected void setUp() {
        listener = new LockFileListener();
    }
    
    protected void tearDown() {
        filesToDelete.delete();
        listener = null;
    }
    
    public void testShouldNotDeleteLockIfDidntGetPastBootstrapping() throws IOException, CruiseControlException {
        File lock = File.createTempFile("test", ".lck");
        filesToDelete.add(lock);
        listener.setLockFile(lock.getAbsolutePath());
        assertTrue(lock.exists());

        listener.handleEvent(BOOTSTRAPPING);
        listener.handleEvent(IDLE);
        listener.handleEvent(WAITING);
        listener.handleEvent(QUEUED);
        listener.handleEvent(PAUSED);
        listener.handleEvent(STOPPED);
        
        assertTrue(lock.exists());
    }
    
    public void testShouldDeleteLockIfGotPastBootstrapping() throws IOException, CruiseControlException {
        File lock = File.createTempFile("test", ".lck");
        filesToDelete.add(lock);
        listener.setLockFile(lock.getAbsolutePath());
        assertTrue(lock.exists());
        
        listener.handleEvent(BOOTSTRAPPING);
        listener.handleEvent(MODIFICATIONSET);
        listener.handleEvent(BUILDING);
        listener.handleEvent(MERGING_LOGS);
        listener.handleEvent(PUBLISHING);
        assertTrue(lock.exists());
        
        listener.handleEvent(IDLE);
        assertFalse(lock.exists());
    }
    
    public void testShouldThrowExceptionWhenFailsToDeleteLock() throws CruiseControlException {
        listener = new LockFileListener() {
            boolean attemptToDeleteLock() {
                return false;
            }
        };
        
        listener.handleEvent(MODIFICATIONSET);
        try {
            listener.handleEvent(IDLE);
            fail("should have thrown exception when lock wasn't deleted");
        } catch (CruiseControlException expected) {
        }
    }
    
    public void testValidate() throws CruiseControlException {
        try {
            listener.validate();
            fail("should throw exception when lock file not set");
        } catch (CruiseControlException expected) {
        }
        
        listener.setLockFile("delete.me");
        listener.validate();
    }

}
