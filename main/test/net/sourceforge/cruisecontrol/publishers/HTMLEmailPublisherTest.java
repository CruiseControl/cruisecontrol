/******************************************************************************
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Element;

import java.io.File;

public class HTMLEmailPublisherTest extends TestCase {

    public HTMLEmailPublisherTest(String name) {
        super(name);
    }

    private HTMLEmailPublisher publisher;

    public void setUp() {
        publisher = new HTMLEmailPublisher();
    }

    private class BrokenTestPublisher extends HTMLEmailPublisher {
        private String[] newXslFileNames = null;

        BrokenTestPublisher() {
            setXSLFileNames(newXslFileNames);
        }
    }

    /**
     * test is set up this way because I expect setXSLFileNames to be used
     * from derived clases, so the test mirrors the expected use.
     */
    public void testSetXSLFileNames() {
        try {
            new BrokenTestPublisher();
            fail("setXSLFileNames should fail when called with null");
        } catch (IllegalArgumentException e) {
            // should fail
        }
    }

    public void testSetLogDir() {
        try {
            publisher.setLogDir(null);
            fail("setLogDir should fail when called with null");
        } catch (IllegalArgumentException e) {
            // should fail
        }
    }

    public void testCreateLinkLine() {
        String serverURL = "http://myserver/context/servlet";
        publisher.setBuildResultsUrl(serverURL);
        String path = "logs" + File.separator;
        String date = "20020607115519";
        String label = "mylabel.100";

        String successFilePrefix = "log" + date + "L" + label;
        String successURL = serverURL + "?log=" + successFilePrefix;
        String successLink = "View results here -> <a href=\"" + successURL + "\">" + successURL + "</a>";
        String successFile = successFilePrefix + ".xml";
        String successLogFileName = path + successFile;
        assertEquals(successLink, publisher.createLinkLine(successLogFileName));

		publisher.setBuildResultsUrl(null);
		assertEquals("", publisher.createLinkLine(successLogFileName));
    	
		publisher.setBuildResultsUrl(serverURL);
        String failFilePrefix = "log" + date;
        String failURL = serverURL + "?log=" + failFilePrefix;
        String failLink = "View results here -> <a href=\"" + failURL + "\">" + failURL + "</a>";
        String failFile = failFilePrefix + ".xml";
        String failLogFileName = path + failFile;
        assertEquals(failLink, publisher.createLinkLine(failLogFileName));
    }

    public void testValidate() {
        setEmailPublisherVariables(publisher);

        try {
            publisher.validate();
            fail("should fail if log dir is not set");
        } catch (CruiseControlException ex) {
            // should fail
        }
        publisher.setLogDir(".");

        try {
            publisher.validate();
            fail("should fail if xslDir is not set");
        } catch (CruiseControlException ex) {
            // should fail
        }
        publisher.setXSLDir(".");

        try {
            publisher.validate();
            fail("should fail if xslFileNames is null");
        } catch (CruiseControlException ex) {
            // should fail
        }

        publisher.setXSLFile("this file doesn't exist");
        try {
            publisher.validate();
            fail("should fail if the specified xslFile doesn't exist");
        } catch (CruiseControlException ex) {
            // should fail
        }
    }

    private void setEmailPublisherVariables(HTMLEmailPublisher publisher) {
        publisher.setBuildResultsUrl("url");
        publisher.setMailHost("host");
        publisher.setReturnAddress("address");
    }

    /**
     * used from main.  not a test because of the need for hardcoded paths.
     * leaving it in as it might be useful to others for local testing
     * (after editing paths)
     */
    private void generateMessage() {
        HTMLEmailPublisher publisher = new HTMLEmailPublisher();
        publisher.setLogDir("c:\\vss\\users\\jfredrick\\sourceforge\\cruisecontrol\\main");
        publisher.setXSLDir("c:\\vss\\users\\jfredrick\\sourceforge\\cruisecontrol\\reporting\\jsp\\xsl\\");
        publisher.setCSS("c:\\vss\\users\\jfredrick\\sourceforge\\cruisecontrol\\reporting\\jsp\\css\\cruisecontrol.css");
        XMLLogHelper helper = new TestHelper();
        String message = publisher.createMessage(helper);
        System.out.print(message);
    }

    public static void main(String args[]) {
        HTMLEmailPublisherTest test = new HTMLEmailPublisherTest("test");
        test.generateMessage();
    }

    private class TestHelper extends XMLLogHelper {
        TestHelper() {
            super(new Element("foo"));
        }

        public String getLogFileName() {
            return "TestLog.xml";
        }
    }

}