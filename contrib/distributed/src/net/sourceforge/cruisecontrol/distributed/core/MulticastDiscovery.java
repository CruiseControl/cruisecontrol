/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
* 200 E. Randolph, 25th Floor
* Chicago, IL 60601 USA
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*     + Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*
*     + Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*       with the distribution.
*
*     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
*       names of its contributors may be used to endorse or promote
*       products derived from this software without specific prior
*       written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.core;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.lease.LeaseRenewalManager;
import net.jini.discovery.LookupDiscovery;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.ServiceItemFilter;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.PropertyEntry;

import org.apache.log4j.Logger;

/**
 * Synchronizes access to shared ServiceDiscoveryManager to allow multiple threads to
 * safely access discovery features.
 */
public final class MulticastDiscovery {

    private static final Logger LOG = Logger.getLogger(MulticastDiscovery.class);

    /** Service Type array used to find BuildAgent services. */
    private static final Class[] SERVICE_CLASSES_BUILDAGENT = new Class[] {BuildAgentService.class};

    public static final int DEFAULT_FIND_WAIT_DUR_MILLIS = 5000;

    private final ServiceDiscoveryManager clientMgr;


    /**
     * Holds the singleton discovery instance.
     * Instantiate here to avoid need to synchronize instance creation.
     */
    private static MulticastDiscovery discovery = new MulticastDiscovery();


    /**
     * Intended only for use by unit tests.
     * @param multicastDiscovery lookup helper
     */
    static void setDiscovery(final MulticastDiscovery multicastDiscovery) {
        if (discovery != null) {

            // release any existing discovery resources
            discovery.terminate();

            LOG.error("WARNING: Discovery released, acceptable only in Unit Tests.");
        }

        if (multicastDiscovery == null) {
            throw new IllegalStateException("Can't set MulticastDiscovery singleton instance to null");
        }
        discovery = multicastDiscovery;
    }

    /** @return the singleton discovery instance. */
    private static MulticastDiscovery getDiscovery() {
        return discovery;
    }

    /** @return true if the {@link #discovery} variable is set, intended only for unit tests.  */
    static boolean isDiscoverySet() {
        return discovery != null;
    }




    private MulticastDiscovery() {
        this(null);
    }

    MulticastDiscovery(final LookupLocator[] unicastLocaters) {
        final String[] lookupGroups = LookupDiscovery.ALL_GROUPS;

        LOG.debug("Starting multicast discovery for groups: " + lookupGroups);
        ReggieUtil.setupRMISecurityManager();

        try {

            final LookupDiscoveryManager discoverMgr = new LookupDiscoveryManager(lookupGroups, unicastLocaters,
                    new DiscoveryListener() {
                        public void discovered(DiscoveryEvent e) {
                            setDiscoveredImpl();
                            logDiscoveryEvent(DiscEventType.DISCOVERED, e);
                        }

                        public void discarded(DiscoveryEvent e) {
                            logDiscoveryEvent(DiscEventType.DISCARDED, e);
                        }
                    });

            clientMgr = new ServiceDiscoveryManager(discoverMgr, new LeaseRenewalManager());

        } catch (IOException e) {
            final String message = "Error starting discovery";
            LOG.debug(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Start discovery of LUS's. Does NOT always need to be called, as calls to other methods
     * will automatically start discovery.
     * Only needed by short-lived classes, like JiniLookUpUtility and InteractiveBuildUtility.
     */
    public static void begin() {
        getDiscovery();
    }

    /**
     * Only for use by JiniLookUpUtility and InteractiveBuilder.
     * @return an array of discovered LUS's
     */
    private ServiceRegistrar[] getRegistrarsImpl() {
        //@todo remove ?
        return clientMgr.getDiscoveryManager().getRegistrars();
    }
    /**
     * Only for use by JiniLookUpUtility and InteractiveBuilder.
     * @return an array of discovered LUS's
     */
    public static ServiceRegistrar[] getRegistrars() {
        //@todo remove ?
        return getDiscovery().getRegistrarsImpl();
    }

    private int getLUSCountImpl() {
        return clientMgr.getDiscoveryManager().getRegistrars().length;
    }
    public static int getLUSCount() {
        return getDiscovery().getLUSCountImpl();
    }


    private ServiceItem[] findBuildAgentServicesImpl(final Entry[] entries, final long waitDurMillis)
            throws RemoteException {

        final ServiceTemplate tmpl = new ServiceTemplate(null, SERVICE_CLASSES_BUILDAGENT, entries);

        try {                                 // minMatches must be > 0
            return clientMgr.lookup(tmpl, 1, Integer.MAX_VALUE, MulticastDiscovery.FLTR_ANY, waitDurMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error finding BuildAgent services.", e);
        }
    }
    public static ServiceItem[] findBuildAgentServices(final Entry[] entries, final long waitDurMillis)
            throws RemoteException {
        return getDiscovery().findBuildAgentServicesImpl(entries, waitDurMillis);
    }


    private ServiceItem findAvailableBuildAgentService(final Entry[] entries, final long waitDurMillis)
            throws RemoteException {

        final ServiceTemplate tmpl = new ServiceTemplate(null, SERVICE_CLASSES_BUILDAGENT, entries);

        try {
            return clientMgr.lookup(tmpl, FLTR_AVAILABLE, waitDurMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error finding BuildAgent service.", e);
        }
    }
    private ServiceItem findMatchingServiceAndClaimImpl(final Entry[] entries, final long waitDurMillis)
            throws RemoteException {
        
        final ServiceItem result = findAvailableBuildAgentService(entries, waitDurMillis);
        if (result != null) {
            ((BuildAgentService) result.service).claim();
        }
        return result;
    }
    public static ServiceItem findMatchingServiceAndClaim(final Entry[] entries, final long waitDurMillis)
            throws RemoteException {

        return getDiscovery().findMatchingServiceAndClaimImpl(entries, waitDurMillis);
    }

    private static final BuildAgentFilter FLTR_AVAILABLE = new BuildAgentFilter(true);
    private static final BuildAgentFilter FLTR_ANY = new BuildAgentFilter(false);

    static final class BuildAgentFilter implements ServiceItemFilter {
        private final boolean findOnlyNonBusy;

        private BuildAgentFilter(final boolean onlyNonBusy) {
            findOnlyNonBusy = onlyNonBusy;
        }

        public boolean check(final ServiceItem item) {

            LOG.debug("Service Filter: item.service: " + item.service);
            if (!(item.service instanceof BuildAgentService)) {
                return false;
            }

            final BuildAgentService agent = (BuildAgentService) item.service;
            // read agent machine name to make sure agent is still valid
            final String agentMachine;
            try {
                agentMachine = agent.getMachineName();
            } catch (RemoteException e) {
                final String msg = "Error reading agent machine name. Filtering out agent.";
                LOG.debug(msg, e);
                return false; // filter out this agent by returning false
            }

            if (!findOnlyNonBusy) {
                return true; // we don't care if agent is busy or not
            }

            try {
                return !agent.isBusy();
            } catch (RemoteException e) {
                final String msg = "Error checking agent busy status. Filtering out agent on machine: "
                        + agentMachine;
                LOG.debug(msg, e);
                return false; // filter out this agent by returning false
            }
        }
    }

    private void terminate() {
        if (clientMgr != null) {
            clientMgr.terminate();
        }
    }

    private static final class DiscEventType {
        static final DiscEventType DISCOVERED = new DiscEventType("Discovered");
        static final DiscEventType DISCARDED = new DiscEventType("Discarded");

        private final String name;
        private DiscEventType(final String name) { this.name = name; }
        public String toString() { return name; }
    }

    private static void logDiscoveryEvent(final DiscEventType type, final DiscoveryEvent e) {
        final ServiceRegistrar[] regs = e.getRegistrars();
        String regMsg = ", " + regs.length + " LUS's: [";
        for (int i = 0; i < regs.length; i++) {
            regMsg += regs[i].getServiceID() + ", ";
        }
        regMsg = regMsg.substring(0, regMsg.lastIndexOf(", ")) + "]";
        LOG.info("LUS " + type + regMsg);
    }

    // For unit tests only - begin
    private void addDiscoveryListenerImpl(final DiscoveryListener discoveryListener) {
        clientMgr.getDiscoveryManager().addDiscoveryListener(discoveryListener);
    }
    static void addDiscoveryListener(final DiscoveryListener discoveryListener) {
        getDiscovery().addDiscoveryListenerImpl(discoveryListener);
    }
    private void removeDiscoveryListenerImpl(final DiscoveryListener discoveryListener) {
        clientMgr.getDiscoveryManager().removeDiscoveryListener(discoveryListener);
    }
    static void removeDiscoveryListener(final DiscoveryListener discoveryListener) {
        getDiscovery().removeDiscoveryListenerImpl(discoveryListener);
    }
    private boolean isDiscovered;
    private void setDiscoveredImpl() {
        isDiscovered = true;
    }
    private boolean isDiscoveredImpl() {
        return isDiscovered;
    }
    static boolean isDiscovered() {
        return getDiscovery().isDiscoveredImpl();
    }
    // For unit tests only - end

    /*
    public final class ServiceDiscListener implements ServiceDiscoveryListener {
        private final MulticastDiscovery discovery;

        private ServiceDiscListener(final MulticastDiscovery discovery) {
            this.discovery = discovery;
        }

        String buildDiscoveryMsg(final ServiceDiscoveryEvent event, final String actionName) {

            final StringBuffer msg = new StringBuffer("\nService ");
            msg.append(actionName).append(": ");

            final ServiceItem postItem = event.getPostEventServiceItem();
            if (postItem != null) {
                appendEvent(msg, postItem, "PostEvent: ");
            } else {
                final ServiceItem preItem = event.getPreEventServiceItem();
                if (preItem != null) {
                     appendEvent(msg, preItem, "PreEvent: ");
                } else {
                    msg.append("NOT SURE WHAT THIS EVENT IS!!!");
                }
            }
            return msg.toString();
        }

        public void serviceAdded(final ServiceDiscoveryEvent event) {
            discovery.setDiscoveredImpl(true);
            LOG.info(buildDiscoveryMsg(event, "Added"));
        }

        public void serviceRemoved(final ServiceDiscoveryEvent event) {
            discovery.setDiscoveredImpl(false);
            LOG.info(buildDiscoveryMsg(event, "Removed"));
        }

        public void serviceChanged(final ServiceDiscoveryEvent event) {
            LOG.info(buildDiscoveryMsg(event, "Changed"));
        }
    }

    private static void appendEvent(final StringBuffer msg, final ServiceItem serviceItem, String eventType) {
        msg.append(eventType);
        msg.append(serviceItem.service.getClass().toString());
        msg.append("; ID:").append(serviceItem.serviceID);
        appendEntries(msg, serviceItem.attributeSets);
    }
    */

    private static String appendEntries(final StringBuffer sb, final Entry[] entries) {
        sb.append("\n\tEntries:\n\t");
        sb.append(Arrays.asList(entries).toString().replaceAll("\\), ", "\\), \n\t")
                    .replaceAll(PropertyEntry.class.getName(), ""));
        sb.append("\n");
        return sb.toString();
    }
    public static String toStringEntries(final Entry[] entries) {
        return appendEntries(new StringBuffer(), entries);
    }
}
