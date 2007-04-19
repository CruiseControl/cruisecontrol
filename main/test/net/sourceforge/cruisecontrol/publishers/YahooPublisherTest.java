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
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Element;

/**
 * Unit test for the Jabber publisher which publishes
 * a link to the build results via Jabber Instant Messaging framework.
 * Currently, tests are limited to message generation and parameter
 * validation. Testing of the XMPPConnection mechanism will require a
 * publicly accessible Jabber server to order to login as the recipient.
 * Only then can this test represent 100% code coverage.
 *
 * @see net.sourceforge.cruisecontrol.publishers.JabberPublisher
 * @see net.sourceforge.cruisecontrol.publishers.LinkJabberPublisher
 *
 * @author <a href="mailto:jonas_edgeworth@cal.berkeley.edu">Jonas Edgeworth</a>
 * @version 1.0
 */
public class YahooPublisherTest extends TestCase {

    private YahooPublisher publisher;

    private XMLLogHelper successLogHelper;

    private String baseURLString =
        "http://mybuildserver.com:8080/buildservlet/BuildServlet";

    protected void setUp() throws Exception {
        successLogHelper = createLogHelper(true, true);
        publisher = new YahooPublisher();
    }

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = new Element("cruisecontrol");
        Element infoElement = new Element("info");

        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", "log20020206120000.xml");
        infoElement.addContent(logFileElement);
        cruisecontrolElement.addContent(infoElement);

        Element projectNameElement = new Element("property");
        projectNameElement.setAttribute("name", "projectname");
        projectNameElement.setAttribute("value", "Yahootest");
        infoElement.addContent(projectNameElement);

        Element buildElement = new Element("build");
        if (!success) {
            buildElement.setAttribute("error", "Something went wrong");
        }
        cruisecontrolElement.addContent(buildElement);

        return new XMLLogHelper(cruisecontrolElement);
    }

    /**
     * Test the message creation process assuring that the
     * generated string matches the associated build URL
     */
    public void testCreateMessage() throws CruiseControlException {
        publisher.setBuildResultsURL(baseURLString);
        assertEquals(
            "Build results for successful build of project Yahootest: " + baseURLString + "?log=log20020206120000",
            publisher.createMessage(successLogHelper));
    }

    /**
     * Test the message creation process assuring that the
     * generated string matches the associated build URL
     * with additional parameters passed included.
     */
    public void testQuestionMarkInBuildResultsURL() throws CruiseControlException {
        publisher.setBuildResultsURL(baseURLString + "?key=value");

        assertEquals(
            "Build results for successful build of project Yahootest: "
                + baseURLString
                + "?key=value&log=log20020206120000",
            publisher.createMessage(successLogHelper));
    }

    /**
     * Test all validation scenarios related to the
     * configuration parameters for the LinkJabberPublisher.
     * Currently, the validation checks the following:<p>
     * If host is null<br>
     * If username is null<br>
     * If password is null<br>
     * If recipient is null<br>
     * If buildResultsURL is null<br>
     * If username is not of the form username@myserver.com<br>
     * If recipient is of the form recipient@myserver.com<p>
     * Validation has not been tested against groupchat configurations.
     */
    public void testValidate() {
        // Test if user is null
        publisher.setUsername(null);
        publisher.setPassword("asdfdsaf");
        publisher.setRecipient("recipient");
        try {
            publisher.validate();
            fail("should throw exception if user not set");
        } catch (CruiseControlException e) {
        }
        // Test if username is null
        publisher.setUsername("tester");
        publisher.setPassword(null);
        try {
            publisher.validate();
            fail("should throw exception if password not set");
        } catch (CruiseControlException e) {
        }
        // Test is username is of the incorrect form
        publisher.setUsername("username@adsfdsaf.com");
        try {
            publisher.validate();
            fail("should throw exception if username is of the wrong form");
        } catch (CruiseControlException e) {
        }
        // Test if recipient is null
        publisher.setPassword("adsfadsf");
        publisher.setRecipient(null);
        try {
            publisher.validate();
            fail("should throw exception if recipient not set");
        } catch (CruiseControlException e) {
        }
        // Test if recipient is of the incorrect form
        publisher.setRecipient("recipient");
        try {
            publisher.validate();
            fail("should throw exception if recipient is of the wrong form");
        } catch (CruiseControlException e) {
        }
        // Test if build results url is null
        publisher.setRecipient("recipient@foo.com");
        publisher.setBuildResultsURL(null);
        try {
            publisher.validate();
            fail("should throw exception if BuildResultURL not set");
        } catch (CruiseControlException e) {
        }
    }

}