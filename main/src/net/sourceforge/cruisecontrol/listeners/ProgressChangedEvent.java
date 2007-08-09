package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.ProjectEvent;

/**
 * Notification object with new progress value.
 * @author Dan Rollo
 * Date: Jul 20, 2007
 * Time: 3:49:36 PM
 */
public class ProgressChangedEvent extends ProjectEvent {

    private final Progress progress;

    public ProgressChangedEvent(final String projectName, final Progress progress) {
        super(projectName);
        this.progress = progress;
    }

    public Progress getProgress() {
        return progress;
    }
}
