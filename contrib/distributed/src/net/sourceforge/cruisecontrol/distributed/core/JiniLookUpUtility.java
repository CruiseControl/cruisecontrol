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

import java.rmi.RemoteException;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;

import org.apache.log4j.Logger;

/**
 * This utility class finds Jini registrars on the local subnet and itemizes
 * each of their registered services. It is needed solely for testing purposes.
 *
 * Run the main() method to see the local Jini services.
 */
public final class JiniLookUpUtility {

    private static final Logger LOG = Logger.getLogger(JiniLookUpUtility.class);

    private final MulticastDiscovery discovery = MulticastDiscovery.getDiscovery();

    private JiniLookUpUtility() {
        final String waitMessage = "Waiting 5 seconds for registrars to report in...";
        System.out.println(waitMessage);
        LOG.info(waitMessage);
        try {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                // ignore
            }
            final ServiceRegistrar[] registrars = discovery.getRegistrars();
            System.out.println("\nFound " + registrars.length + " Lookup Services.");
            for (int x = 0; x < registrars.length; x++) {
                final ServiceRegistrar registrar = registrars[x];
                final String registrarInfo = "Registrar: " + registrar.getServiceID();                
                System.out.println(registrarInfo);
                LOG.debug(registrarInfo);
                final ServiceTemplate template = new ServiceTemplate(null, null, null);
                final ServiceMatches matches = registrar.lookup(template, Integer.MAX_VALUE);
                final ServiceItem[] items = matches.items;
                for (int i = 0; i < items.length; i++) {
                    final String serviceInfo = "  Service: " + items[i].service;
                    System.out.println(serviceInfo);
                    LOG.debug(serviceInfo);
                }
            }
        } catch (RemoteException e) {
            final String message = "Search failed due to an unexpected error";
            LOG.error(message, e);
            System.err.println(waitMessage + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    public static void main(final String[] args) {
        new JiniLookUpUtility();
    }
}
