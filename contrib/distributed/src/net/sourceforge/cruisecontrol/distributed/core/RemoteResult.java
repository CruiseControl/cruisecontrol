package net.sourceforge.cruisecontrol.distributed.core;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import java.io.Serializable;
import java.io.File;

/**
 * @author Dan Rollo
 * Date: Oct 16, 2007
 * Time: 1:03:56 AM
 */
public class RemoteResult implements Serializable {

    private static final long serialVersionUID = -8269577385052199821L;

    private final int idx;
    // @todo Allow agent file to include props and be resolved at runtime?
    private File agentDir;
    private File tempZippedFile;
    private File masterDir;
    static final String MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT = "<distributed> child element <remoteResult>";

    public RemoteResult(int idx) {
        this.idx = idx;
    }

    public int getIdx() {
        return idx;
    }


    private void checkNonResetable(final String attribName, final File oldValue, final File newValue) {
        if (oldValue != null) {
            throw new IllegalStateException(attribName + " already set to: " + oldValue.getAbsolutePath()
            + ", can not be reset to: " + (newValue != null ? newValue.getAbsolutePath() : null));
        }
    }


    public void setAgentDir(final String agentDirectory) {
        final File newAgentDir = new File(agentDirectory);
        checkNonResetable("agentDir", this.agentDir, newAgentDir);
        this.agentDir = newAgentDir;
    }

    public File getAgentDir() {
        return agentDir;
    }

    public void setMasterDir(final String masterDirectory) {
        final File newMasterDir = new File(masterDirectory);
        checkNonResetable("masterDir", this.masterDir, newMasterDir);
        this.masterDir = newMasterDir;
    }
    public File getMasterDir() {
        return masterDir;
    }


    /** Intended only for unit testing. */
    void resetTempZippedFile() { tempZippedFile = null; }

    public void storeTempZippedFile(final File tempZippedFile) {
        checkNonResetable("storeTempZippedFile", this.tempZippedFile, tempZippedFile);
        this.tempZippedFile = tempZippedFile;
    }
    public File fetchTempZippedFile() {
        return tempZippedFile;
    }

    
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(idx >= 0, "Invalid remoteResult index: " + idx);
        ValidationHelper.assertIsSet(agentDir, "agentDir", MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT);
        ValidationHelper.assertIsSet(masterDir, "masterDir", MSG_SUFFIX_BAD_DISTRIBUTED_CHILD_ELEMENT_RESULT);
    }

    public String toString() {
        return "agentDir: " + (agentDir != null ? agentDir.getAbsolutePath() : "")
                + "; masterDir: " + (masterDir != null ? masterDir.getAbsolutePath() : "")
                + "; tempZippedFile: " + (tempZippedFile != null ? tempZippedFile.getAbsolutePath() : "");
    }
}
