package net.sourceforge.cruisecontrol.jmx;

import net.sourceforge.cruisecontrol.CruiseControlException;

import java.rmi.RemoteException;

/**
 * @author Dan Rollo
 * Date: Sep 24, 2008
 * Time: 12:32:17 AM
 */
public interface JMXBuildAgentUtilityMBean {

    int getLookupServiceCount() throws RemoteException;

    String getBuildAgents() throws RemoteException;

    String[] getBuildAgentServiceIds() throws RemoteException;

    public boolean isKillOrRestartAfterBuildFinished();
    public void setKillOrRestartAfterBuildFinished(final boolean afterBuildFinished);
    public void kill(final String agentServiceId) throws CruiseControlException, RemoteException;
    public void killAll() throws CruiseControlException;
    public void restart(final String agentServiceId) throws CruiseControlException, RemoteException;
    public void restartAll() throws CruiseControlException;

}


