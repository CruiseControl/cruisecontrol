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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;
import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.DiscoveryListener;

import java.net.MalformedURLException;

public class MulticastDiscoveryTest extends TestCase {

    /*
    private MulticastDiscovery discovery;

    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
        discovery = new MulticastDiscovery();
    }

    protected void tearDown() throws Exception {
        discovery.terminate();
    }
    */

    /*
    public void testMulticastDiscovery()  throws Exception {
        // TODO: There are certainly tests that could be written here, but without automating startup and shutdown of
        // Jini these shouldn't be run with the rest of the the Cruise Control tests. Should we look into this
        // automation for purposes of testing?
        // NOTE: I've added nasty, but working Jini startup/shutdown methods to DistributedMasterBuilderTest

        assertNull(discovery.findMatchingServiceAndClaim());
    }
    */

    /*
    public void testServiceDiscListenerToString() throws Exception {
        final MulticastDiscovery.ServiceDiscListener serviceDiscListener = discovery.getServiceDiscListener();

        final ServiceID serviceID = new ServiceID(1, 1);
        final PropertyEntry entry1 = new PropertyEntry("name1", "value1");
        final String service = "fakeService";
        final ServiceItem serviceItem = new ServiceItem(serviceID, service, new Entry[] { entry1 });
        final Object source = "fakeSource";
        final String action = "fakeAction";
        final ServiceDiscoveryEvent event = new ServiceDiscoveryEvent(source, serviceItem, serviceItem);

        assertEquals(getExpectedToString(action, service, entry1),
                serviceDiscListener.buildDiscoveryMsg(event, action));
    }
    private static String getExpectedToString(final String action, final Object service, final PropertyEntry entry) {
        return "\nService " + action + ": PostEvent: class " + service.getClass().getName()
                + "; ID:00000000-0000-0001-0000-000000000001\n"
                + "\tEntries:\n"
                + "\t[(name=" + entry.name + ",value=" + entry.value + ")]\n";
    }
    */

    public void testDummy() { }

    public static MulticastDiscovery getLocalDiscovery() throws MalformedURLException {
        final LookupLocator[] unicastLocators = new LookupLocator[] {
                new LookupLocator(DistributedMasterBuilderTest.JINI_URL_LOCALHOST)
        };

        return new MulticastDiscovery(unicastLocators);
    }

    /**
     * Expose method intended only for use by unit tests.
     * @param multicastDiscovery lookup helper
     */    
    public static void setDiscovery(final MulticastDiscovery multicastDiscovery) {
        MulticastDiscovery.setDiscovery(multicastDiscovery);
    }
    /**
     * Expose method intended only for use by unit tests.
     * @return true if any LUS has been found.
     */
    public static boolean isDiscovered() {
        return MulticastDiscovery.isDiscovered();
    }

    public static void addDiscoveryListener(final DiscoveryListener discoveryListener) {
        MulticastDiscovery.addDiscoveryListener(discoveryListener);
    }
    public static void removeDiscoveryListener(final DiscoveryListener discoveryListener) {
        MulticastDiscovery.removeDiscoveryListener(discoveryListener);
    }

}
