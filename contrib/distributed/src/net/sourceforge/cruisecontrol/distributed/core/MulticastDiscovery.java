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
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;
import net.jini.lookup.ServiceItemFilter;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.PropertyEntry;

import org.apache.log4j.Logger;

public class MulticastDiscovery {

    private static final Logger LOG = Logger.getLogger(MulticastDiscovery.class);

    private final ServiceTemplate serviceTemplate;
    private final ServiceDiscoveryManager clientMgr;
    private final ServiceDiscListener serviceDiscListener;
    private final LookupCache lookupCache;

    public MulticastDiscovery(final Entry[] entries) {
        this(LookupDiscovery.ALL_GROUPS, null, BuildAgentService.class, entries);
    }

    public MulticastDiscovery(final String[] lookupGroups, final LookupLocator[] unicastLocaters,
                              final Class klass, final Entry[] entries) {
        LOG.debug("Starting multicast discovery for groups: " + lookupGroups);
        ReggieUtil.setupRMISecurityManager();

        try {

            final LookupDiscoveryManager discoverMgr = new LookupDiscoveryManager(
                lookupGroups, unicastLocaters, null);

            clientMgr = new ServiceDiscoveryManager(discoverMgr, new LeaseRenewalManager());
        } catch (IOException e) {
            final String message = "Error starting discovery";
            LOG.debug(message, e);
            throw new RuntimeException(message, e);
        }

        // create cache of desired _service providers
        final Class[] classes = new Class[] {klass};
        serviceTemplate = new ServiceTemplate(null, classes, entries);
        try {
            serviceDiscListener = new ServiceDiscListener(this);
            lookupCache = getClientManager().createLookupCache(getServiceTemplate(), null, serviceDiscListener);
        } catch (RemoteException e) {
            final String message = "Error creating _service cache";
            LOG.debug(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Only for use by JiniLookUpUtility and InteractiveBuilder.
     * @return an array of discovered LUS's
     */
    public ServiceRegistrar[] getRegistrars() {
        //@todo remove ?
        return getClientManager().getDiscoveryManager().getRegistrars();
    }

    public int getLUSCount() {
         return getClientManager().getDiscoveryManager().getRegistrars().length;
    }

    private ServiceTemplate getServiceTemplate() {
        return serviceTemplate;
    }

    private ServiceDiscoveryManager getClientManager() {
        return clientMgr;
    }

    ServiceDiscListener getServiceDiscListener() {
        return serviceDiscListener;
    }

    /**
     * Intended only for use by util classes.
     * @return the cache of discovered services
     */
    public LookupCache getLookupCache() {
        return lookupCache;
    }

    public ServiceItem findMatchingServiceAndClaim() throws RemoteException {
        final ServiceItem result = getLookupCache().lookup(FLTR_AVAILABLE);
        if (result != null) {
            ((BuildAgentService) result.service).claim();
        }
        return result;
    }

    private static final BuildAgentFilter FLTR_AVAILABLE = new BuildAgentFilter(true);
    public static final BuildAgentFilter FLTR_ANY = new BuildAgentFilter(false);

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

    public void terminate() {
        if (getClientManager() != null) {
            getClientManager().terminate();
        }
    }



    private boolean isDiscovered;
    private synchronized void setDiscovered(final boolean discovered) {
        isDiscovered = discovered;
    }
    public synchronized boolean isDiscovered() {
        return isDiscovered;
    }

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
            discovery.setDiscovered(true);
            LOG.info(buildDiscoveryMsg(event, "Added"));
        }

        public void serviceRemoved(final ServiceDiscoveryEvent event) {
            discovery.setDiscovered(false);
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
