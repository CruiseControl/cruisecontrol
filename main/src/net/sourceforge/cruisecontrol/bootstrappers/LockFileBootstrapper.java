package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

public class LockFileBootstrapper implements Bootstrapper {

    private String path;

    public void bootstrap() throws CruiseControlException {
        File lock = new File(path);
        try {
            if (!lock.createNewFile()) {
                throw new CruiseControlException("lock file already exists, aborting build attempt");
            } else {
                lock.deleteOnExit();
            }
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(path, "lockfile", LockFileBootstrapper.class);
    }

    public void setLockFile(String path) {
        this.path = path;
    }
}
