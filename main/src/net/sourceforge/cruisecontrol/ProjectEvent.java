package net.sourceforge.cruisecontrol;

/**
 * .
 * User: jfredrick
 * Date: Sep 6, 2004
 * Time: 10:51:23 PM
 */
public abstract class ProjectEvent {
    private final String projectName;

    public ProjectEvent(String name) {
        projectName = name;
    }

    public String getProjectName() {
        return projectName;
    }
}
