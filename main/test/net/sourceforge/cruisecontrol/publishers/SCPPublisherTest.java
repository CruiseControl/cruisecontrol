/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;

import java.io.File;

import org.jdom.Element;

public class SCPPublisherTest extends TestCase {

    private SCPPublisher publisher;

    protected void setUp() throws Exception {
        publisher = new SCPPublisher();
    }

    protected void tearDown() throws Exception {
        publisher = null;
    }

    public void testIfFileNotSetShouldGetLatestLogNameEachTime() throws CruiseControlException {
        TestSCPPublisher testpublisher = new TestSCPPublisher();
        assertFalse(testpublisher.getLogFileNameWasCalled);
        testpublisher.publish(null);
        assertTrue(testpublisher.getLogFileNameWasCalled);

        testpublisher.getLogFileNameWasCalled = false;
        assertFalse(testpublisher.getLogFileNameWasCalled);
        testpublisher.publish(null);
        assertTrue(testpublisher.getLogFileNameWasCalled);        
    }
    
    public void testValidate() throws CruiseControlException {
        publisher.setSourceUser("user1");
        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions when only user is set.");
        } catch (CruiseControlException e) {
        }
        
        publisher.setSourceUser(null);
        publisher.setSourceHost("host1");
        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions when only host is set.");
        } catch (CruiseControlException e) {
        }
        
        publisher.setSourceUser("user1");
        publisher.setSourceHost("host1");
        publisher.validate();        
    }
    
    public void testValidateShouldFailWithInvalidExecutableName() throws CruiseControlException {
        publisher.setSourceUser("user1");
        publisher.setSourceHost("host1");
        publisher.setExecutableName(null);
        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions with null executableName.");
        } catch (CruiseControlException e) {
        }
        
        publisher.setExecutableName("");
        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions with empty executableName.");
        } catch (CruiseControlException e) {
        }
        
        publisher.setExecutableName("plink");
        publisher.validate();
    }

    public void testCreateCommandline() {
        publisher.setSourceUser("user1");
        publisher.setSourceHost("host1");
        publisher.setTargetUser("user2");
        publisher.setTargetHost("host2");
        assertEquals(
            "scp -S ssh user1@host1:."
                + File.separator
                + "filename "
                + "user2@host2:."
                + File.separator,
            publisher.createCommandline("filename").toString());
        publisher.setOptions("-P 1000");
        assertEquals(
            "scp -P 1000 -S ssh user1@host1:."
                + File.separator
                + "filename "
                + "user2@host2:."
                + File.separator,
            publisher.createCommandline("filename").toString());
        publisher.setSSH("plink");
        assertEquals(
            "scp -P 1000 -S plink user1@host1:."
                + File.separator
                + "filename "
                + "user2@host2:."
                + File.separator,
            publisher.createCommandline("filename").toString());
        publisher.setTargetDir(File.separator + "home" + File.separator + "httpd");
        assertEquals(
            "scp -P 1000 -S plink user1@host1:."
                + File.separator
                + "filename "
                + "user2@host2:"
                + File.separator
                + "home"
                + File.separator
                + "httpd"
                + File.separator,
            publisher.createCommandline("filename").toString());
    }
    
    public void testCreateCommandlineWithAlternateExectuable() {
        publisher.setExecutableName("plink");
        publisher.setSourceUser("user1");
        publisher.setSourceHost("host1");
        publisher.setTargetUser("user2");
        publisher.setTargetHost("host2");
        assertEquals(
            "plink -S ssh user1@host1:."
                + File.separator
                + "filename "
                + "user2@host2:."
                + File.separator,
            publisher.createCommandline("filename").toString());
    }
    
    private class TestSCPPublisher extends SCPPublisher {
        private boolean getLogFileNameWasCalled = false;

        protected String getLogFileName(Element cruisecontrolLog) throws CruiseControlException {
            getLogFileNameWasCalled = true;
            return "fileName";
        }
        
        protected void executeCommand(Commandline command) throws CruiseControlException {
        }
    }
}
