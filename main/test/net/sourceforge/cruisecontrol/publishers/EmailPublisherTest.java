/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
import net.sourceforge.cruisecontrol.publishers.EmailPublisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.testutil.Util;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Hashtable;

import org.jdom.input.SAXBuilder;
import org.jdom.Element;
import org.apache.log4j.PropertyConfigurator;

public class EmailPublisherTest extends TestCase {

    private XMLLogHelper _successLogHelper;
    private XMLLogHelper _fixedLogHelper;
    private XMLLogHelper _failureLogHelper;
    private XMLLogHelper _firstFailureLogHelper;
    private EmailPublisher _emailPublisher;

    public EmailPublisherTest(String s) {
        super(s);
    }

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = Util.createElement(success, lastBuildSuccess);

        return new XMLLogHelper(cruisecontrolElement);
    }

    public void setUp() throws Exception {
        //pass in some xml and create the publisher
        StringBuffer xml = new StringBuffer();
        xml.append("<email defaultsuffix=\"@host.com\">");
        xml.append("<always address=\"always1\"/>");
        xml.append("<always address=\"always2@host.com\"/>");
        xml.append("<failure address=\"failure1\"/>");
        xml.append("<failure address=\"failure2@host.com\"/>");
        xml.append("<map alias=\"user3\" address=\"user3@host2.com\"/>");
        xml.append("</email>");

        Element emailPublisherElement = null;

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        emailPublisherElement = builder.build(new StringReader(xml.toString())).getRootElement();

        PluginXMLHelper xmlHelper = new PluginXMLHelper();
        _emailPublisher = (MockEmailPublisher) xmlHelper.configure(emailPublisherElement, "net.sourceforge.cruisecontrol.publishers.MockEmailPublisher");

        _successLogHelper = createLogHelper(true, true);
        _failureLogHelper = createLogHelper(false, false);
        _fixedLogHelper = createLogHelper(true, false);
        _firstFailureLogHelper = createLogHelper(false, true);

    }

    public void testValidate() {
        EmailPublisher publisher = new MockEmailPublisher();
        try {
            publisher.validate();
            fail("EmailPublisher should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        publisher.setBuildResultsUrl("buildResultsURL");
        publisher.setMailHost("mailhost");
        publisher.setReturnAddress("returnaddress");

        try {
            publisher.validate();
        } catch (CruiseControlException e) {
            fail("EmailPublisher should not throw exceptions when required fields are set.");
        }
    }

    public void testShouldSend() throws Exception {
        //build not necessary, spam while broken=true
        _emailPublisher.setSpamWhileBroken(true);
        _emailPublisher.setReportSuccess("success");
        assertEquals(true, _emailPublisher.shouldSend(_successLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_fixedLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_failureLogHelper));

        _emailPublisher.setReportSuccess("fixes");
        assertEquals(false, _emailPublisher.shouldSend(_successLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_fixedLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_failureLogHelper));

        _emailPublisher.setReportSuccess("never");
        assertEquals(false, _emailPublisher.shouldSend(_successLogHelper));
        assertEquals(false, _emailPublisher.shouldSend(_fixedLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_failureLogHelper));


        _emailPublisher.setSpamWhileBroken(false);
        _emailPublisher.setReportSuccess("success");
        assertEquals(true, _emailPublisher.shouldSend(_successLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_fixedLogHelper));
        assertEquals(false, _emailPublisher.shouldSend(_failureLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_firstFailureLogHelper));

        _emailPublisher.setReportSuccess("fixes");
        assertEquals(false, _emailPublisher.shouldSend(_successLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_fixedLogHelper));
        assertEquals(false, _emailPublisher.shouldSend(_failureLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_firstFailureLogHelper));

        _emailPublisher.setReportSuccess("never");
        assertEquals(false, _emailPublisher.shouldSend(_successLogHelper));
        assertEquals(false, _emailPublisher.shouldSend(_fixedLogHelper));
        assertEquals(false, _emailPublisher.shouldSend(_failureLogHelper));
        assertEquals(true, _emailPublisher.shouldSend(_firstFailureLogHelper));

    }

    public void testCreateSubject() throws Exception {
        _emailPublisher.setReportSuccess("always");
        assertEquals("TestProject somelabel Build Successful", _emailPublisher.createSubject(_successLogHelper));
        _emailPublisher.setReportSuccess("fixes");
        assertEquals("TestProject somelabel Build Fixed", _emailPublisher.createSubject(_fixedLogHelper));

        assertEquals("TestProject Build Failed", _emailPublisher.createSubject(_failureLogHelper));

        _emailPublisher.setSubjectPrefix("[CC]");
        _emailPublisher.setReportSuccess("always");
        assertEquals("[CC] TestProject somelabel Build Successful", _emailPublisher.createSubject(_successLogHelper));
        _emailPublisher.setReportSuccess("fixes");
        assertEquals("[CC] TestProject somelabel Build Fixed", _emailPublisher.createSubject(_fixedLogHelper));

        assertEquals("[CC] TestProject Build Failed", _emailPublisher.createSubject(_failureLogHelper));

    }

    public void testCreateUserList() {
        PropertyConfigurator.configure("log4j.properties");
        assertEquals("always1@host.com,always2@host.com,user1@host.com,user2@host.com,user3@host2.com", _emailPublisher.createUserList(_successLogHelper));
        assertEquals("always1@host.com,always2@host.com,failure1@host.com,failure2@host.com,user1@host.com,user2@host.com,user3@host2.com", _emailPublisher.createUserList(_failureLogHelper));

        _emailPublisher.setSkipUsers(true);
        assertEquals("always1@host.com,always2@host.com", _emailPublisher.createUserList(_successLogHelper));
        assertEquals("always1@host.com,always2@host.com,failure1@host.com,failure2@host.com", _emailPublisher.createUserList(_failureLogHelper));

    }
}
