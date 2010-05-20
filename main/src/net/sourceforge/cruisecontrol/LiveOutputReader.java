package net.sourceforge.cruisecontrol;

import java.io.Serializable;

/**
 * Allow reading of live output.
 * @author Dan Rollo
 *         Date: May 19, 2010
 *         Time: 9:08:33 PM
 */
public interface LiveOutputReader extends Serializable {

    /**
     * @return A unique (for this VM) identifying string for this logger instance.
     * This is intended to allow reporting apps (eg: Dashboard) to check if the logger instance changes mid-build,
     * causing the "live output" log file to reset (possibly due to a CompositeBuilder moving to a new Builder, etc.).
     * If the logger instance changes (indicated by a new ID value), the client should  start asking for output from
     * the first line of the current output file.
     * @see #retrieveLines(int)
     * @see net.sourceforge.cruisecontrol.util.BuildOutputLogger#getID()
     */
    String getID();

    /**
     * @param firstLine line to skip to.
     * @return All lines available from firstLine (inclusive) up to MAX_LINES.
     * Before the first call to retrieveLines(), the client should call {@link #getID()}, and hold that id value.
     * If a client later calls retrieveLines() with a non-zero 'firstLine' parameter, and receives an empty array
     * as a result, that client should also call {@link #getID()}. If {@link #getID ()} returns a different value
     * from the prior call to {@link #getID ()}, the client should make another call to retrieveLines() with the
     * firstLine parameter set back to zero. This will allow the client to live output when the output logger is
     * changed during a build.
     * @see net.sourceforge.cruisecontrol.util.BuildOutputLogger#retrieveLines(int)
     */
    String[] retrieveLines(final int firstLine);
}
