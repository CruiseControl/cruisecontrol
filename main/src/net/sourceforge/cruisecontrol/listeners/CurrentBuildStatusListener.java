package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;

import java.util.Date;

/**
 * .
 * User: jfredrick
 * Date: Sep 6, 2004
 * Time: 10:54:33 PM
 */
public class CurrentBuildStatusListener implements Listener {
    private String fileName;

    public void handleEvent(final ProjectStateChangedEvent stateChanged) throws CruiseControlException {
        final ProjectState newState = stateChanged.getNewState();
        final String text = "<span class=\"link\">" + newState.getDescription() + " as of:<br>";
        CurrentBuildFileWriter.writefile(text, new Date(), fileName);
    }

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        // ignore other ProjectEvents
    }

    public void validate() throws CruiseControlException {
        if (fileName == null) {
            throw new CruiseControlException("'filename' is required for CurrentBuildStatusBootstrapper");
        }
        CurrentBuildFileWriter.validate(fileName);
    }

    public void setFile(String fileName) {
        this.fileName = fileName.trim();
    }
}
