package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;

/**
 * .
 * User: jfredrick
 * Date: Sep 6, 2004
 * Time: 11:09:50 PM
 */
public class ProjectStateChangedEvent extends ProjectEvent {
    private final ProjectState newState;

    public ProjectStateChangedEvent(String projectName, ProjectState state) {
        super(projectName);
        newState = state;
    }

    public ProjectState getNewState() {
        return newState;
    }
}
