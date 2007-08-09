package net.sourceforge.cruisecontrol;

import java.io.Serializable;

/**
 * Allow progress updates as projects are processed during various ProjectStates.
 * @author Dan Rollo
 * Date: Jul 24, 2007
 * Time: 5:50:45 PM
 */
public interface Progress extends Serializable {

    /** @param value new progress value. */
    public void setValue(String value);

    /** @return current progress value. */
    public String getValue();
}
