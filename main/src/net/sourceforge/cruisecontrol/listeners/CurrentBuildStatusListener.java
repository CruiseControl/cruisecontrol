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

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (event instanceof ProjectStateChangedEvent) {
            final ProjectStateChangedEvent stateChanged = (ProjectStateChangedEvent) event;
            final ProjectState newState = stateChanged.getNewState();
            LOG.debug("updating status to " + newState.getName()  + " for project " + stateChanged.getProjectName());
            final String text = "<span class=\"link\">" + newState.getDescription() + " since<br>";
            CurrentBuildFileWriter.writefile(text, new Date(), fileName);
        } else {
            // ignore other ProjectEvents
            LOG.debug("ignoring event " + event.getClass().getName() + " for project " + event.getProjectName());
        }
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
