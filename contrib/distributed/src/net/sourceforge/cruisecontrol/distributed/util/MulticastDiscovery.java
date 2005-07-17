/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
* 651 W Washington Ave. Suite 600
* Chicago, IL 60661 USA
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

package net.sourceforge.cruisecontrol.distributed.util;

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

import org.apache.log4j.Logger;

public class MulticastDiscovery {

    private static final Logger LOG = Logger.getLogger(MulticastDiscovery.class);

    private final ServiceTemplate serviceTemplate;
    private final ServiceDiscoveryManager clientMgr;
    private final LookupCache lookupCache;

    public MulticastDiscovery(final Entry[] entries) {
        this(LookupDiscovery.ALL_GROUPS, null, BuildAgentService.class, entries);
    }

    public MulticastDiscovery(final String[] lookupGroups, final LookupLocator[] unicastLocaters,
                              final Class klass, final Entry[] entries) {
        LOG.debug("Starting multicast discovery for groups: " + lookupGroups);
        ReggieUtil.setupRMISecurityManager();

        try {

            LookupDiscoveryManager  discoverMgr = new LookupDiscoveryManager(
                lookupGroups, unicastLocaters, null);

            clientMgr = new ServiceDiscoveryManager(discoverMgr, new LeaseRenewalManager());
        } catch (IOException e) {
            String message = "Error starting discovery";
            LOG.debug(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }

        // create cache of desired _service providers
        Class[] classes = new Class[] {klass};
        serviceTemplate = new ServiceTemplate(null, classes, entries);
        try {
            lookupCache = getClientManager().createLookupCache(
                    getServiceTemplate(), null, new ServiceDiscListener(this));
        } catch (RemoteException e) {
            String message = "Error creating _service cache";
            LOG.debug(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }

// elsewhere, do lookup for service if we don't use a cache
//
//        ServiceItem item = null;
//        // Try to find the _service, blocking till timeout if necessary
//        item = _clientMgr.lookup(template,
//            null, /* no filter */
//            WAITFOR /* timeout */);

    }

    /** Only for use by JiniLookUpUtility and InteractiveBuilder.  **/
    public ServiceRegistrar[] getRegistrars() {
        //@todo remove ?
        return getClientManager().getDiscoveryManager().getRegistrars();
    }


    private ServiceTemplate getServiceTemplate() {
        return serviceTemplate;
    }

    private ServiceDiscoveryManager getClientManager() {
        return clientMgr;
    }

    /** Intended only for use by util classes. */
    LookupCache getLookupCache() {
        return lookupCache;
    }

    public ServiceItem findMatchingService() throws RemoteException {
        return findMatchingService(true);
    }
    public ServiceItem findMatchingService(final boolean doClaim) throws RemoteException {
        final ServiceItem result = getLookupCache().lookup(FLTR_AVAILABLE);
        if (doClaim && result != null) {
            ((BuildAgentService) result.service).claim();
        }
        return result;
    }

    static final BuildAgentFilter FLTR_AVAILABLE = new BuildAgentFilter(true);
    static final BuildAgentFilter FLTR_ANY = new BuildAgentFilter(false);

    static final class BuildAgentFilter implements ServiceItemFilter {
        private final boolean findOnlyNonBusy;

        private BuildAgentFilter(final boolean onlyNonBusy) {
            findOnlyNonBusy = onlyNonBusy;
        }

        public boolean check(ServiceItem item) {

            LOG.debug("Service Filter: item.service: " + item.service);
            if (!(item.service instanceof BuildAgentService)) {
                return false;
            } else if (!findOnlyNonBusy) {
                return true; // we don't care if agent is busy or not
            }

            BuildAgentService agent = (BuildAgentService) item.service;
            try {
                return (!agent.isBusy());
            } catch (RemoteException e) {
                final String msg = "Error checking agent busy status.";
                LOG.error(msg, e);
                throw new RuntimeException(msg, e);
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

        private String buildDiscoveryMsg(ServiceDiscoveryEvent event, final String actionName) {
            String msg = "\nService " + actionName + ": ";

            final ServiceItem postItem = event.getPostEventServiceItem();
            if (postItem != null) {
                final Entry[] entries = postItem.attributeSets;
                msg += "PostEvent: " + postItem.service.getClass().toString() + "; ID:" + postItem.serviceID
                        + "\n\tEntries:\n\t"
                        + Arrays.asList(entries).toString().replaceAll("\\), ", "\\), \n\t")
                        + "\n";
            } else {
                final ServiceItem preItem = event.getPreEventServiceItem();
                if (preItem != null) {
                    final Entry[] entries = preItem.attributeSets;
                    msg += "PreEvent: " + preItem.service.getClass().toString() + "; ID:" + preItem.serviceID
                            + "\n\tEntries:\n\t"
                            + Arrays.asList(entries).toString().replaceAll("\\), ", "\\), \n\t")
                            + "\n";
                } else {
                    msg += "NOT SURE WHAT THIS EVENT IS!!!";
                }
            }
            return msg;
        }

        public void serviceAdded(ServiceDiscoveryEvent event) {
            discovery.setDiscovered(true);
            LOG.info(buildDiscoveryMsg(event, "Added"));
        }

        public void serviceRemoved(ServiceDiscoveryEvent event) {
            discovery.setDiscovered(false);
            LOG.info(buildDiscoveryMsg(event, "Removed"));
        }

        public void serviceChanged(ServiceDiscoveryEvent event) {
            LOG.info(buildDiscoveryMsg(event, "Changed"));
        }
    }

}
