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

import java.net.UnknownServiceException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.sourceforge.cruisecontrol.distributed.PropertyEntry;

import org.apache.log4j.Logger;

public class ReggieUtil {

    private static final Logger LOG = Logger.getLogger(ReggieUtil.class);

    /**
     * 
     * @param entriesList
     * @param klass
     * @return list of services
     */
    public static List findServicesForEntriesList(ServiceRegistrar registrar, List entriesList, Class klass) {
        if (entriesList == null) {
            entriesList = new ArrayList();
        }
        Entry[] entries = (Entry[]) entriesList.toArray(new PropertyEntry[entriesList.size()]);
        return findServicesForEntriesArray(registrar, entries, klass);
    }

    /**
     * 
     * @param registrar
     * @param entriesList
     * @param klass
     * @param timeout
     * @return
     * @throws UnknownServiceException
     */
    public static List findServicesForEntriesList(ServiceRegistrar registrar, List entriesList, Class klass,
            long timeout) throws UnknownServiceException {
        List services = new ArrayList();
        long endTime = System.currentTimeMillis() + timeout;
        long sleepTime = Math.min(30000, Math.max(1000, timeout / 5));
        do {
            services = findServicesForEntriesList(registrar, entriesList, klass);
            if (services != null) {
                break;
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
            }
        } while (System.currentTimeMillis() < endTime);
        if (services == null) {
            String message = "No matching services found before timeout";
            LOG.debug(message);
            System.err.println(message);
            throw new UnknownServiceException(message);
        }
        return services;
    }

    /**
     * 
     * @param entries
     * @param klass
     * @return list of services
     */
    public static List findServicesForEntriesArray(ServiceRegistrar registrar, Entry[] entries, Class klass) {
        try {
            ServiceTemplate template = new ServiceTemplate(null, new Class[] { klass }, entries);
            ServiceMatches matches = registrar.lookup(template, Integer.MAX_VALUE);
            ServiceItem[] items = matches.items;
            List list = new ArrayList();
            for (int i = 0; i < items.length; i++) {
                Object proxy = items[i].service;
                list.add(proxy);
            }
            return list;
        } catch (RemoteException e) {
            String message = "Search failed due to an unexpected error";
            LOG.error(message + " - " + e.getMessage(), e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    /**
     * converts an entries search string ento a list of entries. An example
     * entries search string: "hostname=gandalf;os.name=Windows XP"
     * 
     * @param searchString
     * @return entriesList
     */
    public static List convertStringEntriesToList(String searchString) {
        searchString.replace(',', ';');
        StringTokenizer tokenizer = new StringTokenizer(searchString.trim(), ";");
        ArrayList entriesList = new ArrayList();
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            PropertyEntry entry = new PropertyEntry();
            entry.name = token.substring(0, token.indexOf("=")).trim();
            entry.value = token.substring(token.indexOf("=") + 1).trim();
            entriesList.add(entry);
        }
        return entriesList;
    }

}
