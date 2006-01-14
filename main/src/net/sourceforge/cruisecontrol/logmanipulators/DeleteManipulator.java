package net.sourceforge.cruisecontrol.logmanipulators;

import java.io.File;

public class DeleteManipulator extends BaseManipulator {

    private boolean ignoreSuffix = false;
    
    public void execute(String logDir) {
        File[] deleteFiles = getRelevantFiles(logDir, ignoreSuffix);
        for (int i = 0; i < deleteFiles.length; i++) {
            File file = deleteFiles[i];
            file.delete();
        }
    }

    public void setIgnoreSuffix(boolean ignoreSuffix) {
        this.ignoreSuffix = ignoreSuffix;
    }

    
}
