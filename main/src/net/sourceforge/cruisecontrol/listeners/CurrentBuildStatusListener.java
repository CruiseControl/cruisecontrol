package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import org.apache.log4j.Logger;

import java.util.Date;

/**
 * .
 * User: jfredrick
 * Date: Sep 6, 2004
 * Time: 10:54:33 PM
 */
public class CurrentBuildStatusListener implements Listener {
    private static final Logger LOG = Logger.getLogger(CurrentBuildStatusListener.class);
    private String fileName;

    public void handleEvent(final ProjectStateChangedEvent stateChanged) throws CruiseControlException {
        LOG.debug("updating status to " + stateChanged.getNewState().getName()
                + " for project " + stateChanged.getProjectName());
        final ProjectState newState = stateChanged.getNewState();
        final String text = "<span class=\"link\">" + newState.getDescription() + " as of:<br>";
        CurrentBuildFileWriter.writefile(text, new Date(), fileName);
    }

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        // ignore other ProjectEvents
        LOG.debug("ignoring event " + event.getClass().getName() + " for project " + event.getProjectName());
    }

    public void validate() throws CruiseControlException {
        if (fileName == null) {
            throw new CruiseControlException("'filename' is required for CurrentBuildStatusBootstrapper");
        }
        CurrentBuildFileWriter.validate(fileName);
    }

    public void setFile(String fileName) {
        this.fileName = fileName.trim();
        LOG.debug("set fileName = " + fileName);
    }
}
