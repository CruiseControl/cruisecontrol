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

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

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
public class JiniLookUpUtility {

    private static final Logger LOG = Logger.getLogger(JiniLookUpUtility.class);

    MulticastDiscovery discovery = new MulticastDiscovery();

    public JiniLookUpUtility() {
        String waitMessage = "Waiting 5 seconds for registrars to report in...";
        System.out.println(waitMessage);
        LOG.info(waitMessage);
        try {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
            }
            List registrars = discovery.getRegistrars();
            for (Iterator iter = registrars.iterator(); iter.hasNext();) {
                ServiceRegistrar registrar = (ServiceRegistrar) iter.next();
                String registrarInfo = "Registrar: " + registrar.getServiceID();
                System.out.println();
                System.out.println(registrarInfo);
                LOG.debug(registrarInfo);
                ServiceTemplate template = new ServiceTemplate(null, null, null);
                ServiceMatches matches = registrar.lookup(template, Integer.MAX_VALUE);
                ServiceItem[] items = matches.items;
                for (int i = 0; i < items.length; i++) {
                    String serviceInfo = "  Service: " + items[i].service;
                    System.out.println(serviceInfo);
                    LOG.debug(serviceInfo);
                }
            }
        } catch (RemoteException e) {
            String message = "Search failed due to an unexpected error";
            LOG.error(message, e);
            System.err.println(waitMessage + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    public static void main(String[] args) {
        JiniLookUpUtility tester = new JiniLookUpUtility();
    }
}
