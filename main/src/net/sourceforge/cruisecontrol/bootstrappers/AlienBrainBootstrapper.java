package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.IOException;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.AlienBrainCore;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

public class AlienBrainBootstrapper extends AlienBrainCore implements Bootstrapper {

    /** Configuration parameters */
    private String localPath;
    private boolean forceFileUpdate;
    private String overwriteWritable = "skip";

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setForceFileUpdate(boolean forceFileUpdate) {
        this.forceFileUpdate = forceFileUpdate;
    }

    public void setOverwriteWritable(String policy) {
        this.overwriteWritable = policy.toLowerCase();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getPath(), "path", this.getClass());
        ValidationHelper.assertTrue("skip".equals(overwriteWritable) || "replace".equals(overwriteWritable),
                             "overwritewritable must be one of 'skip' or 'replace' in AlienBrainBootstrapper");
    }

    public void bootstrap() {
        try {
            if (getBranch() != null) {
                setActiveBranch(getBranch());
            }

            ManagedCommandline cmdLine = buildBootstrapCommand();
            cmdLine.execute();
            LOG.debug(cmdLine.getStdoutAsString());
            LOG.debug(cmdLine.getStderrAsString());
        } catch (IOException e) {
            LOG.error("Error executing AlienBrain bootstrap." + e);
        } catch (CruiseControlException e) {
            LOG.error("Error executing AlienBrain bootstrap." + e);
        }

    }

    public ManagedCommandline buildBootstrapCommand() {

        ManagedCommandline cmdLine = buildCommonCommand();

        cmdLine.createArgument().setValue("getlatest");
        cmdLine.createArgument().setValue(getPath());
        addArgumentIfSet(cmdLine, localPath, "-localpath");
        addFlagIfSet(cmdLine, forceFileUpdate, "-forcefileupdate");
        addArgumentIfSet(cmdLine, overwriteWritable, "-overwritewritable");

        return cmdLine;
    }
}
