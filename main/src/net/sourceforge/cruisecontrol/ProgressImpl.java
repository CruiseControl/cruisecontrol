package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.listeners.ProgressChangedEvent;
import net.sourceforge.cruisecontrol.util.DateUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * Allow progress updates as projects are processed during various ProjectStates.
 * @author Dan Rollo
 * Date: Jul 20, 2007
 * Time: 1:46:54 PM
 */
public class ProgressImpl implements Progress {

    private static final long serialVersionUID = -660370539956160650L;

    /** The parent Project. */
    private final Project project;

    /** current progress value. */
    private Serializable val;

    private Date lastUpdated = new Date();

    ProgressImpl(final Project project) {
        this.project = project;
    }

    /** @param value new progress value. */
    public void setValue(String value) {
        val = value;
        lastUpdated = new Date();
        project.notifyListeners(new ProgressChangedEvent(project.getName(), this));
    }

    /** @return current progress value represented as a String, prefixed with last update date. */
    public String getValue() {
        return DateUtil.getFormattedTime(lastUpdated) + " " + val;
    }
}


