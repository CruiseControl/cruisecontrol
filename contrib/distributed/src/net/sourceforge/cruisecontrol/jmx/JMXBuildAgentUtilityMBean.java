package net.sourceforge.cruisecontrol.jmx;

import net.sourceforge.cruisecontrol.CruiseControlException;

import java.rmi.RemoteException;

/**
 * @author Dan Rollo
 * Date: Sep 24, 2008
 * Time: 12:32:17 AM
 */
public interface JMXBuildAgentUtilityMBean {

    void refresh() throws RemoteException;
    
    int getLookupServiceCount() throws RemoteException;
    String[] getLUSServiceIds() throws RemoteException;
    void destroyLUS(final String lusServiceId) throws RemoteException, CruiseControlException;

    String getBuildAgents() throws RemoteException;

    String[] getBuildAgentServiceIds() throws RemoteException;

    boolean isKillOrRestartAfterBuildFinished();
    void setKillOrRestartAfterBuildFinished(final boolean afterBuildFinished);
    void kill(final String agentServiceId) throws CruiseControlException, RemoteException;
    void killAll() throws CruiseControlException;
    void restart(final String agentServiceId) throws CruiseControlException, RemoteException;
    void restartAll() throws CruiseControlException;

}


