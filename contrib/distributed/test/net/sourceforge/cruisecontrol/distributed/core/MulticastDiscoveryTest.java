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
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryEvent;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;

public class MulticastDiscoveryTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(MulticastDiscoveryTest.class);


    /** Common error message if multicast is being blocked. */
    public static final String MSG_DISOCVERY_CHECK_FIREWALL =
            "1. Make sure MULTICAST is enabled on your network devices (ifconfig -a).\n"
            + "2. No Firewall is blocking multicasts.\n"
            + "3. Using an IDE? See @todo in setUp/tearDown (LUS normally started by LUSTestSetup decorator).\n";


    private static DistributedMasterBuilderTest.ProcessInfoPump jiniProcessPump;


    public static void locateLocalhostMulticastDiscovery() throws MalformedURLException, InterruptedException {
        if (!isDiscoverySet()) {
            final long begin = System.currentTimeMillis();

            final MulticastDiscovery discovery = getLocalDiscovery();
            setDiscovery(discovery);
            // wait to discover lookupservice
            final DiscoveryListener discoveryListener = new DiscoveryListener() {

                public void discovered(DiscoveryEvent e) {
                    synchronized (discovery) {
                        discovery.notifyAll();
                    }
                }

                public void discarded(DiscoveryEvent e) {
                    synchronized (discovery) {
                        discovery.notifyAll();
                    }
                }
            };
            addDiscoveryListener(discoveryListener);
            try {
                synchronized (discovery) {
                    int count = 0;
                    while (!isDiscovered() && count < 6) {
                        discovery.wait(10 * 1000);
                        count++;
                    }
                }
            } finally {
                removeDiscoveryListener(discoveryListener);
            }

            final float elapsedSecs = (System.currentTimeMillis() - begin) / 1000f;

            assertTrue("MulticastDiscovery was not discovered before timeout. elapsed: \n" + elapsedSecs + " sec\n"
                    + MSG_DISOCVERY_CHECK_FIREWALL,
                    isDiscovered());

            LOG.info(DistributedMasterBuilderTest.MSG_PREFIX_STATS + "Unit test MulticastDiscovery took: "
                    + elapsedSecs + " sec");
        }
    }


    // Do not alter this test case to use a single TestDecorator to start a LUS, even though doing so would reduce
    // the number of times LUS needs to be started. Since this test destroys the LUS, it must be run by a class that
    // always starts a LUS before every test case.
    protected void setUp() throws Exception {
        jiniProcessPump = DistributedMasterBuilderTest.startJini();
        locateLocalhostMulticastDiscovery();
    }
    protected void tearDown() throws Exception {
        DistributedMasterBuilderTest.killJini(jiniProcessPump);
    }


    /**
     * Do not relocate this test case in MulticastDiscoveryTest, even though doing so would reduce the number of times
     * LUS needs to be started. Since this test destroys the LUS, it must be run by a class that always starts a LUS
     * before every test case.
     * @throws Exception if the test fails
     */
    public void testDestroyLocalLUS() throws Exception {
        destroyLocalLUS();
    }


    
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
        final long begin = System.currentTimeMillis();

        MulticastDiscovery.setDiscovery(multicastDiscovery);

        LOG.info(DistributedMasterBuilderTest.MSG_PREFIX_STATS + "Unit test Agent discovery took: "
                + (System.currentTimeMillis() - begin) / 1000f + " sec");
    }
    /** @return true if the discovery singleton variable is set, intended only for unit tests.  */
    private static boolean isDiscoverySet() {
        return MulticastDiscovery.isDiscoverySet();
    }
    
    /**
     * Expose method intended only for use by unit tests.
     * @return true if any LUS has been found.
     */
    private static boolean isDiscovered() {
        return MulticastDiscovery.isDiscovered();
    }

    private static void addDiscoveryListener(final DiscoveryListener discoveryListener) {
        MulticastDiscovery.addDiscoveryListener(discoveryListener);
    }
    private static void removeDiscoveryListener(final DiscoveryListener discoveryListener) {
        MulticastDiscovery.removeDiscoveryListener(discoveryListener);
    }

    public static void destroyLocalLUS() throws Exception {
        final ServiceRegistrar localLUS = DistributedMasterBuilderTest.findTestLookupService(1000);

        MulticastDiscovery.destroyLookupService(localLUS, 0);

        // Note: destroy() call is asynchronous

        try {
            localLUS.lookup(new ServiceTemplate(null, null, null));
            fail("Call to destroyed local LUS should have failed.");
        } catch (Exception e) {
            // ignore, LUS call should fail
        }
    }
}
