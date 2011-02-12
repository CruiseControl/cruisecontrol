package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.IOException;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.sourcecontrols.AlienBrainCore;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

@Description(
        "Syncs a single path from AlienBrain before the build begins. Useful if you want "
        + "to leave all SCM up to CruiseControl. Allowing the bootstrapper to update the "
        + "project makes for a simpler build.xml but allows a window where a file can be "
        + "committed after the update and before the modification check.")
public class AlienBrainBootstrapper extends AlienBrainCore implements Bootstrapper {

    /** Configuration parameters */
    private String localPath;
    private boolean forceFileUpdate;
    private String overwriteWritable = "skip";

    @Description("If localpath is specified the item is copied to the specified local path.")
    @Optional
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Description(
            "If set to true, the local file is always updated with the file on the server. "
            + "This is not the same as overwritewritable=\"replace\". It means that the "
            + "file will be retrieved even if it has not been modified in the repository.")
    @Optional
    @Default("false")
    public void setForceFileUpdate(boolean forceFileUpdate) {
        this.forceFileUpdate = forceFileUpdate;
    }

    @Description(
            "<dl>"
            + "<dt>skip:</dt>"
            + "<dd>do not touch the file</dd>"
            + "<dt>replace:</dt>"
            + "<dd>replace the file with the version on the server</dd>"
            + "</dl>")
    @Optional
    @Default("skip")
    public void setOverwriteWritable(String policy) {
        this.overwriteWritable = policy.toLowerCase();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getPath(), "path", this.getClass());
        ValidationHelper.assertTrue("skip".equals(overwriteWritable) || "replace".equals(overwriteWritable),
                "overwritewritable must be one of 'skip' or 'replace' in AlienBrainBootstrapper");
    }

    public void bootstrap() throws CruiseControlException {
        try {
            if (getBranch() != null) {
                setActiveBranch(getBranch());
            }

            ManagedCommandline cmdLine = buildBootstrapCommand();
            cmdLine.execute();
        } catch (IOException e) {
            throw new CruiseControlException("Error executing AlienBrain bootstrap", e);
        }
    }

    public ManagedCommandline buildBootstrapCommand() {
        ManagedCommandline cmdLine = buildCommonCommand();

        cmdLine.createArguments("getlatest", getPath());
        addArgumentIfSet(cmdLine, localPath, "-localpath");
        addFlagIfSet(cmdLine, forceFileUpdate, "-forcefileupdate");
        addArgumentIfSet(cmdLine, overwriteWritable, "-overwritewritable");

        return cmdLine;
    }
}
