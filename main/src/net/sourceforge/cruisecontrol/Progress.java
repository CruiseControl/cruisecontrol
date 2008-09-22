package net.sourceforge.cruisecontrol;

import java.io.Serializable;
import java.util.Date;

/**
 * Allow progress updates as projects are processed during various ProjectStates.
 * @author Dan Rollo
 * Date: Jul 24, 2007
 * Time: 5:50:45 PM
 */
public interface Progress extends Serializable {

    /** @param value new progress value. */
    public void setValue(String value);

    /** @return current progress value, prefixed with last updated date. */
    public String getValue();

    /** @return the date when current progress value was set. */
    public Date getLastUpdated();

    /**
     * @return the current progress value (not prefixed by last updated date).
     * Goofy, but don't want to change behavior of {@link #getValue()} to preserve backwards compatibility.
     */
    public String getText();
}
