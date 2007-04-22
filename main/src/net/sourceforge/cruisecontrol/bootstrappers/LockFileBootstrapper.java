package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

public class LockFileBootstrapper implements Bootstrapper {

    private String path;
    private String projectName;
    private static final Logger LOG = Logger.getLogger(LockFileBootstrapper.class);

    public void bootstrap() throws CruiseControlException {
        File lock = new File(path);
        try {
            if (!lock.createNewFile()) {
                String projectNameInFile = Util.readFileToString(lock);
                if (!projectName.equalsIgnoreCase(projectNameInFile)) {
                    String message = "Lock file [" + path + "] already exists from project " + projectNameInFile
                            + " , aborting build attempt.";
                    throw new CruiseControlException(message);
                } else {
                    LOG.debug("Lock file [" + path + "] already exists but project names match");
                }
            } else {
                IO.write(lock, projectName);
                LOG.debug("Created lock file [" + path + "]");
                lock.deleteOnExit();
            }
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(path, "lockfile", LockFileBootstrapper.class);
        ValidationHelper.assertIsSet(projectName, "projectName", LockFileBootstrapper.class);
    }

    public void setLockFile(String path) {
        this.path = path;
    }

    public void setProjectName(String name) {
        projectName = name;
    }
}
