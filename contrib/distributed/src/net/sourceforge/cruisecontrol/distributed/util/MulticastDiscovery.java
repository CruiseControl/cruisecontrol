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
import java.net.UnknownServiceException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;

import org.apache.log4j.Logger;

public class MulticastDiscovery implements DiscoveryListener {

    private static final Logger LOG = Logger.getLogger(MulticastDiscovery.class);

    private List registrars = new ArrayList();

    public MulticastDiscovery() {
        this(LookupDiscovery.ALL_GROUPS);
    }

    public MulticastDiscovery(String[] lookupGroups) {
        LOG.debug("Starting multicast discovery for groups: " + lookupGroups);
        System.setSecurityManager(new java.rmi.RMISecurityManager());
        try {
            LookupDiscovery discover = new LookupDiscovery(lookupGroups);
            discover.addDiscoveryListener(this);
        } catch (IOException e) {
            String message = "Error starting discovery";
            LOG.debug(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    public void discovered(DiscoveryEvent evt) {

        ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar = null;
        for (int n = 0; n < registrarsArray.length; n++) {
            registrar = registrarsArray[n];
            registrars.add(registrar);
            LOG.debug("Discovered registrar: " + registrar.getServiceID());
            try {
                LOG.debug(registrar.getGroups());
            } catch (RemoteException e) {
                LOG.warn("Exception getting groups from discovered ServiceRegistrar");
            }
        }
    }

    public void discarded(DiscoveryEvent evt) {
        ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar = null;
        for (int n = 0; n < registrarsArray.length; n++) {
            registrar = registrarsArray[n];
            registrars.remove(registrar);
            LOG.debug("Discarded registrar: " + registrar.getServiceID());
            try {
                LOG.debug(registrar.getGroups());
            } catch (RemoteException e) {
                LOG.warn("Exception getting groups from discarded ServiceRegistrar");
            }
        }
    }

    /**
     * @return
     */
    public List getRegistrars() {
        return registrars;
    }

    /**
     * @param timeout
     * @return
     */
    public ServiceRegistrar getRegistrar(long timeout) throws UnknownServiceException {
        long endTime = System.currentTimeMillis() + timeout;
        long sleepTime = Math.min(30000, Math.max(1000, timeout / 5));
        do {
            if (registrars.size() > 0) {
                break;
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
            }
        } while (System.currentTimeMillis() < endTime);
        if (registrars == null) {
            String message = "No registrar found before timeout";
            LOG.debug(message);
            System.err.println(message);
            throw new UnknownServiceException(message);
        }
        return (ServiceRegistrar) registrars.get(0);
    }

}
