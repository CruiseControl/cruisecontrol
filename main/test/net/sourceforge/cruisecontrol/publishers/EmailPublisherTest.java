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
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.publishers.email.DropLetterEmailAddressMapper;
import net.sourceforge.cruisecontrol.publishers.email.PropertiesMapper;
import net.sourceforge.cruisecontrol.testutil.Util;

import java.io.StringReader;
import java.io.File;
import java.io.FileOutputStream;

import java.util.Properties;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.jdom.input.SAXBuilder;
import org.jdom.Element;
import org.apache.log4j.PropertyConfigurator;

public class EmailPublisherTest extends TestCase {

    private XMLLogHelper successLogHelper;
    private XMLLogHelper fixedLogHelper;
    private XMLLogHelper failureLogHelper;
    private XMLLogHelper firstFailureLogHelper;
    private EmailPublisher emailPublisher;
    private File tmpFile;

    public EmailPublisherTest(String s) {
        super(s);
    }

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = Util.createElement(success, lastBuildSuccess);

        return new XMLLogHelper(cruisecontrolElement);
    }

    public void setUp() throws Exception {
        PropertiesMapper propertiesMapper = new PropertiesMapper();
        // create a temp file to test propertiesmapper
        Properties props = new Properties();
        tmpFile = File.createTempFile("cruise", "Test");
        props.setProperty("always1", "always1");
        FileOutputStream fos = new FileOutputStream(tmpFile);
        props.store(fos, null);
        fos.close();
        tmpFile.deleteOnExit();
        //pass in some xml and create the publisher
        StringBuffer xml = new StringBuffer();
        xml.append("<email defaultsuffix=\"@host.com\">");
        xml.append("<always address=\"always1\"/>");
        xml.append("<always address=\"always2@host.com\"/>");
        xml.append("<always address=\"dropletteruser1\"/>");
        xml.append("<failure address=\"failure1\"/>");
        xml.append("<failure address=\"failure2@host.com\" reportWhenFixed=\"true\"/>");
        xml.append("<success address='success1' />");
        xml.append("<success address='success2@host.com' />");
        xml.append("<map alias=\"user3\" address=\"user3@host2.com\"/>");
        xml.append("</email>");

        Element emailPublisherElement = null;

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        emailPublisherElement = builder.build(new StringReader(xml.toString())).getRootElement();

        PluginXMLHelper xmlHelper = new PluginXMLHelper(new ProjectXMLHelper());
        emailPublisher =
            (MockEmailPublisher) xmlHelper.configure(
                emailPublisherElement,
                Class.forName("net.sourceforge.cruisecontrol.publishers.MockEmailPublisher"),
                false);
        emailPublisher.add(new DropLetterEmailAddressMapper());
        propertiesMapper.setFile(tmpFile.getPath());
        emailPublisher.add(propertiesMapper);

        successLogHelper = createLogHelper(true, true);
        failureLogHelper = createLogHelper(false, false);
        fixedLogHelper = createLogHelper(true, false);
        firstFailureLogHelper = createLogHelper(false, true);

    }

    public void testValidate() {
        EmailPublisher publisher = new MockEmailPublisher();
        try {
            publisher.validate();
            fail("EmailPublisher should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

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
        emailPublisher.setSpamWhileBroken(true);
        emailPublisher.setReportSuccess("success");
        assertEquals(true, emailPublisher.shouldSend(successLogHelper));
        assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
        assertEquals(true, emailPublisher.shouldSend(failureLogHelper));

        emailPublisher.setReportSuccess("fixes");
        assertEquals(false, emailPublisher.shouldSend(successLogHelper));
        assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
        assertEquals(true, emailPublisher.shouldSend(failureLogHelper));

        emailPublisher.setReportSuccess("never");
        assertEquals(false, emailPublisher.shouldSend(successLogHelper));
        assertEquals(false, emailPublisher.shouldSend(fixedLogHelper));
        assertEquals(true, emailPublisher.shouldSend(failureLogHelper));

        emailPublisher.setSpamWhileBroken(false);
        emailPublisher.setReportSuccess("success");
        assertEquals(true, emailPublisher.shouldSend(successLogHelper));
        assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
        assertEquals(false, emailPublisher.shouldSend(failureLogHelper));
        assertEquals(true, emailPublisher.shouldSend(firstFailureLogHelper));

        emailPublisher.setReportSuccess("fixes");
        assertEquals(false, emailPublisher.shouldSend(successLogHelper));
        assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
        assertEquals(false, emailPublisher.shouldSend(failureLogHelper));
        assertEquals(true, emailPublisher.shouldSend(firstFailureLogHelper));

        emailPublisher.setReportSuccess("never");
        assertEquals(false, emailPublisher.shouldSend(successLogHelper));
        assertEquals(false, emailPublisher.shouldSend(fixedLogHelper));
        assertEquals(false, emailPublisher.shouldSend(failureLogHelper));
        assertEquals(true, emailPublisher.shouldSend(firstFailureLogHelper));

    }

    public void testCreateSubject() throws Exception {
        emailPublisher.setReportSuccess("always");
        assertEquals(
            "TestProject somelabel Build Successful",
            emailPublisher.createSubject(successLogHelper));
        emailPublisher.setReportSuccess("fixes");
        assertEquals(
            "TestProject somelabel Build Fixed",
            emailPublisher.createSubject(fixedLogHelper));

        assertEquals("TestProject Build Failed",
                emailPublisher.createSubject(failureLogHelper));

        emailPublisher.setSubjectPrefix("[CC]");
        emailPublisher.setReportSuccess("always");
        assertEquals(
            "[CC] TestProject somelabel Build Successful",
            emailPublisher.createSubject(successLogHelper));
        emailPublisher.setReportSuccess("fixes");
        assertEquals(
            "[CC] TestProject somelabel Build Fixed",
            emailPublisher.createSubject(fixedLogHelper));

        assertEquals(
            "[CC] TestProject Build Failed",
            emailPublisher.createSubject(failureLogHelper));

        //Anytime it is a "fixed" build, the subject should read "fixed".
        emailPublisher.setReportSuccess("always");
        assertEquals(
            "[CC] TestProject somelabel Build Fixed",
            emailPublisher.createSubject(fixedLogHelper));

        emailPublisher.setReportSuccess("failures");
        assertEquals(
            "[CC] TestProject somelabel Build Fixed",
            emailPublisher.createSubject(fixedLogHelper));

    }

    public void testCreateUserList() throws Exception {
        PropertyConfigurator.configure("log4j.properties");
        assertEquals(
                "always1@host.com,always2@host.com,ropletteruser1@host.com,"
                + "success1@host.com,success2@host.com,"
                + "user1@host.com,user2@host.com,user3@host2.com",
            emailPublisher.createUserList(successLogHelper));
        assertEquals(
            "always1@host.com,always2@host.com,failure1@host.com,"
                + "failure2@host.com,ropletteruser1@host.com,user1@host.com,user2@host.com,user3@host2.com",
            emailPublisher.createUserList(failureLogHelper));
        assertEquals(
            "always1@host.com,always2@host.com,"
                + "failure2@host.com,ropletteruser1@host.com,"
                + "success1@host.com,success2@host.com,"
                + "user1@host.com,user2@host.com,user3@host2.com",
            emailPublisher.createUserList(fixedLogHelper));

        emailPublisher.setSkipUsers(true);
        assertEquals(
                "always1@host.com,always2@host.com,ropletteruser1@host.com,success1@host.com,success2@host.com",
            emailPublisher.createUserList(successLogHelper));
        assertEquals(
                "always1@host.com,always2@host.com,failure1@host.com,failure2@host.com,ropletteruser1@host.com",
            emailPublisher.createUserList(failureLogHelper));

        emailPublisher.setSkipUsers(false);

        assertEquals(
                "always1@host.com,always2@host.com,failure2@host.com,"
                + "ropletteruser1@host.com,success1@host.com,success2@host.com,"
                + "user1@host.com,user2@host.com,user3@host2.com",
            emailPublisher.createUserList(fixedLogHelper));
    }

    public void testGetFromAddress() throws AddressException {
        String returnAddress = "me@you.com";
        String returnName = "Me you Me";
        emailPublisher.setReturnAddress(returnAddress);
        emailPublisher.setReturnName(returnName);
        InternetAddress fromAddress = emailPublisher.getFromAddress();
        assertEquals(returnAddress, fromAddress.getAddress());
        assertEquals(returnName, fromAddress.getPersonal());
    }

}
