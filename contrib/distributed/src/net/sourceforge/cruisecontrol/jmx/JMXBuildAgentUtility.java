package net.sourceforge.cruisecontrol.jmx;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.distributed.util.BuildAgentUtility;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.jini.core.lookup.ServiceItem;

import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

/**
 * @author Dan Rollo
 * Date: Sep 25, 2008
 * Time: 12:06:08 AM
 */
public class JMXBuildAgentUtility implements JMXBuildAgentUtilityMBean {

    private static final Logger LOG = Logger.getLogger(JMXBuildAgentUtility.class);

    private final BuildAgentUtility buildAgentUtility;

    private boolean isAfterBuildFinished = true;

    private List<ServiceItem> lstServiceItems = new ArrayList<ServiceItem>();
    private List<String> agentServiceIds = new ArrayList<String>();
    private String agentInfoAll;
    private long lastRefreshTime;
    // @todo make refresh timeout configurable
    private final long refreshTimeout = (BuildAgentUtility.LUS_WAIT_SECONDS + 1) * 1000;
    private void doRefreshAgentList() throws RemoteException {

        // don't refresh until 5 seconds have elapsed since last refresh
        if ((System.currentTimeMillis() - lastRefreshTime) > refreshTimeout) { 

            lstServiceItems = new ArrayList<ServiceItem>();
            agentInfoAll = buildAgentUtility.getAgentInfoAll(lstServiceItems);

            agentServiceIds = new ArrayList<String>();
            for (ServiceItem serviceItem : lstServiceItems) {
                agentServiceIds.add(
                        ((BuildAgentService) serviceItem.service).getMachineName()
                                + ": " + serviceItem.serviceID);
            }
            lastRefreshTime = System.currentTimeMillis();
        } else {
            LOG.debug("Skipping Agent Util refresh, using cached agent info. timeout(millis): " + refreshTimeout);
        }
    }

    public JMXBuildAgentUtility() {
        buildAgentUtility = BuildAgentUtility.createForJMX();
    }

    public int getLookupServiceCount() throws RemoteException {
        doRefreshAgentList();
        return buildAgentUtility.getLastLUSCount();
    }

    public String getBuildAgents() throws RemoteException {
        doRefreshAgentList();
        return agentInfoAll;
    }

    public String[] getBuildAgentServiceIds() throws RemoteException {
        doRefreshAgentList();
        return agentServiceIds.toArray(new String[agentServiceIds.size()]);
    }

    public boolean isKillOrRestartAfterBuildFinished() { return isAfterBuildFinished; }
    public void setKillOrRestartAfterBuildFinished(final boolean afterBuildFinished) {
        isAfterBuildFinished = afterBuildFinished;
    }

    public void kill(String agentServiceId) throws RemoteException, CruiseControlException {

        final BuildAgentService buildAgentService = findAgentViaServiceId(agentServiceId);
        if (buildAgentService != null) {
            try {
                buildAgentService.kill(isAfterBuildFinished);
            } catch (RemoteException e) {
                LOG.error("Error killing Agent via JMX", e);
                throw e;
            } catch (Exception e) {
                LOG.error("Error killing Agent via JMX", e);
                throw new CruiseControlException(e);
            }
        }
    }
    public void killAll() throws  CruiseControlException {

        for (ServiceItem serviceItem : lstServiceItems) {
            try {
                kill(serviceItem.serviceID.toString());
            } catch (RemoteException e) {
                LOG.error("Error killing Agent via JMX", e);
            }
        }
    }

    public void restart(String agentServiceId) throws RemoteException, CruiseControlException {

        final BuildAgentService buildAgentService = findAgentViaServiceId(agentServiceId);
        if (buildAgentService != null) {
            try {
                buildAgentService.restart(isAfterBuildFinished);
            } catch (RemoteException e) {
                final String msg = BuildAgentUtility.checkRestartRequiresWebStart(e);
                if (msg != null) {
                    LOG.error(msg, e);
                } else {
                    LOG.error("Error restarting Agent via JMX", e);
                }
                throw e;
            } catch (Exception e) {
                LOG.error("Error killing Agent via JMX", e);
                throw new CruiseControlException(e);
            }
        }
    }
    public void restartAll() throws  CruiseControlException {
        for (ServiceItem serviceItem : lstServiceItems) {
            try {
                restart(serviceItem.serviceID.toString());
            } catch (RemoteException e) {
                final String msg = BuildAgentUtility.checkRestartRequiresWebStart(e);
                if (msg != null) {
                    LOG.error(msg, e);
                } else {
                    LOG.error("Error restarting Agent via JMX", e);
                }
            }
        }
    }


    static final String MSG_NULL_AGENT_SERVICEID = "agentServiceId must not be null";
    
    private static String validateServiceId(final String agentServiceId) {
        if (agentServiceId == null) {
            throw new IllegalArgumentException(MSG_NULL_AGENT_SERVICEID);
        }
        return agentServiceId.trim(); // JMX page can add spaces to values sent
    }

    private BuildAgentService findAgentViaServiceId(final String serviceIdUnTrimmed) {
        final String serviceId = validateServiceId(serviceIdUnTrimmed);

        for (ServiceItem serviceItem : lstServiceItems) {
            if (serviceItem.serviceID.toString().equals(serviceId)) {
                return (BuildAgentService) serviceItem.service;
            }
        }

        LOG.error("JMXBuildAgentUtility : Could not find Agent via serviceID: " + serviceId);
        return null;
    }
}