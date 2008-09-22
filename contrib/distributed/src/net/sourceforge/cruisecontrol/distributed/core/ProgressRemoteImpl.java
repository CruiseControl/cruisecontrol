package net.sourceforge.cruisecontrol.distributed.core;

import net.sourceforge.cruisecontrol.Progress;

import java.rmi.RemoteException;
import java.util.Date;

/**
 * Allow progress updates from distributed agents.
 * @author Dan Rollo
 * Date: Jul 25, 2007
 * Time: 5:55:29 PM
 */
public class ProgressRemoteImpl implements ProgressRemote {

    private final Progress progress;
    private final String agentName;

    public ProgressRemoteImpl(final Progress progress, final String agentName) {
        this.progress = progress;
        this.agentName = agentName;
    }

    public void setValueRemote(String value) throws RemoteException {
        progress.setValue(agentName + " " + value);
    }

    public String getValueRemote() throws RemoteException {
        return  progress.getValue();
    }

    public Date getLastUpdatedRemote() throws RemoteException {
        return progress.getLastUpdated();
    }

    public String getTextRemote() throws RemoteException {
        return progress.getText();
    }
}
