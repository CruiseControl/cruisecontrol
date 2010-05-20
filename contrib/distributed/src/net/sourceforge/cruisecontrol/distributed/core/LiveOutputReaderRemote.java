package net.sourceforge.cruisecontrol.distributed.core;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Allow reading of live output from distributed agents.
 * @author Dan Rollo
 *         Date: May 19, 2010
 *         Time: 8:49:07 PM
 */
public interface LiveOutputReaderRemote extends Remote, Serializable {

    /**
     * @return A unique (for this VM) identifying string for this logger instance.
     * @throws RemoteException if a remote call fails
     * @see #retrieveLinesRemote(int)
     * @see net.sourceforge.cruisecontrol.util.BuildOutputLogger#getID()
     */
    String getIDRemote() throws RemoteException;

    /**
     * @param firstLine line to skip to.
     * @return All lines available from firstLine (inclusive) up to MAX_LINES.
     * @throws RemoteException if a remote call fails
     * @see net.sourceforge.cruisecontrol.util.BuildOutputLogger#retrieveLines(int)
     * @see #getIDRemote()
     */
    String[] retrieveLinesRemote(final int firstLine) throws RemoteException;
}
