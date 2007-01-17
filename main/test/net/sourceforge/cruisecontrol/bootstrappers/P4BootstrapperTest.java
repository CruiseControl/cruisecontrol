/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Should also test bootstrap() but to do that we need to mock calling the command line
 *
 * @author <a href="mailto:mroberts@thoughtworks.com">Mike Roberts</a>
 * @author <a href="mailto:cstevenson@thoughtworks.com">Chris Stevenson</a>
 */
public class P4BootstrapperTest extends TestCase {
    private P4Bootstrapper p4Bootstrapper;

    public void setUp() throws Exception {
        p4Bootstrapper = new P4Bootstrapper();
    }

    public void testViewNotSet() {
        try {
            p4Bootstrapper.validate();
            fail("Should be Exception if view is not set.");
        } catch (CruiseControlException expected) {
        }
    }

    public void testInvalidView() {
        p4Bootstrapper.setView("");
        try {
            p4Bootstrapper.validate();
            fail("Empty path not allowed");
        } catch (CruiseControlException expected) {
        }
    }

    public void testInvalidPort() {
        p4Bootstrapper.setView("foo");
        p4Bootstrapper.setPort("");
        try {
            p4Bootstrapper.validate();
            fail("Empty port not allowed");
        } catch (CruiseControlException expected) {
        }
    }

    public void testInvalidClient() {
        p4Bootstrapper.setView("foo");
        p4Bootstrapper.setClient("");
        try {
            p4Bootstrapper.validate();
            fail("Empty client not allowed");
        } catch (CruiseControlException expected) {
        }
    }

    public void testInvalidUser() {
        p4Bootstrapper.setView("foo");
        p4Bootstrapper.setUser("");
        try {
            p4Bootstrapper.validate();
            fail("Empty user not allowed");
        } catch (CruiseControlException expected) {
        }
    }

    public void testCreateCommandlineWithViewSet() throws CruiseControlException {
        p4Bootstrapper.setView("foo");
        assertEquals("p4 -s sync foo", p4Bootstrapper.createCommandline().toString());

        p4Bootstrapper.setView("foo bar");
        assertEquals("p4 -s sync \"foo bar\"", p4Bootstrapper.createCommandline().toString());
    }

    public void testCreateCommandlineWithP4PortSet() throws CruiseControlException {
        p4Bootstrapper.setView("foo");
        p4Bootstrapper.setPort("testhost:1666");
        checkEnvironmentSpecification(" -p testhost:1666 ");
    }

    public void testCreateCommandlineWithP4ClientSet() throws CruiseControlException {
        p4Bootstrapper.setView("foo");
        p4Bootstrapper.setClient("testclient");
        checkEnvironmentSpecification(" -c testclient ");
    }

    public void testCreateCommandlineWithP4UserSet() throws CruiseControlException {
        p4Bootstrapper.setView("foo");
        p4Bootstrapper.setUser("testuser");
        p4Bootstrapper.setPasswd("password");
        checkEnvironmentSpecification(" -u testuser -P password ");
    }

    /**
     * Checks that a P4 environment command line option is created correctly in a P4 command line specification
     */
    private void checkEnvironmentSpecification(String expectedSetting)
            throws CruiseControlException {
        String commandline = p4Bootstrapper.createCommandline().toString();
        int specicationPosition = commandline.indexOf(expectedSetting);
        int syncPosition = commandline.indexOf(" sync ");
        assertTrue(specicationPosition != -1);
        assertTrue(syncPosition != -1);
        assertTrue(specicationPosition < syncPosition);
    }
}
