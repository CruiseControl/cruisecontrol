package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

import java.util.Date;

/**
 * Writes an HTML snippet in a file (typically in a location where the reporting module can read it), indicating
 * the current build status.
 *
 * Obsoletes both {@link net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper}
 * and {@link net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher}
 *
 * <p>{@link net.sourceforge.cruisecontrol.DateFormatFactory} for the dateformat
 * 
 * @see net.sourceforge.cruisecontrol.DateFormatFactory
 * @author jfredrick
 */
public class CurrentBuildStatusListener implements Listener {
    private static final Logger LOG = Logger.getLogger(CurrentBuildStatusListener.class);
    private String fileName;

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (event instanceof ProjectStateChangedEvent) {
            final ProjectStateChangedEvent stateChanged = (ProjectStateChangedEvent) event;
            final ProjectState newState = stateChanged.getNewState();
            LOG.debug("updating status to " + newState.getName()  + " for project " + stateChanged.getProjectName());
            final String text = newState.getDescription() + " since\n";
            CurrentBuildFileWriter.writefile(text, new Date(), fileName);
        } else {
            // ignore other ProjectEvents
            LOG.debug("ignoring event " + event.getClass().getName() + " for project " + event.getProjectName());
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(fileName, "file", this.getClass());
        CurrentBuildFileWriter.validate(fileName);
    }

    public void setFile(String fileName) {
        this.fileName = fileName.trim();
        LOG.debug("set fileName = " + fileName);
    }
}
