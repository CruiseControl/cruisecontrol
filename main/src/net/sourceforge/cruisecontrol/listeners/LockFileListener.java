package net.sourceforge.cruisecontrol.listeners;

import java.io.File;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

public class LockFileListener implements Listener {

    private String path;
    private boolean needToDeleteLock = false;

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (!(event instanceof ProjectStateChangedEvent)) {
            return;
        }

        ProjectState newState = ((ProjectStateChangedEvent) event).getNewState();
        
        if (projectGotPastBootstrapping(newState)) {
            needToDeleteLock = true;
        }
        
        if (newState.equals(ProjectState.IDLE) && needToDeleteLock) {
            needToDeleteLock = false;
            boolean deletedFile = attemptToDeleteLock();
            if (!deletedFile) {
                throw new CruiseControlException("project " + event.getProjectName()
                                + " failed to delete lock file " + path);
            }
        }
    }

    boolean attemptToDeleteLock() {
        return new File(path).delete();
    }

    private boolean projectGotPastBootstrapping(ProjectState newState) {
        return newState.equals(ProjectState.MODIFICATIONSET);
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(path, "lockfile", LockFileListener.class);
    }

    public void setLockFile(String path) {
        this.path = path;
    }

}
