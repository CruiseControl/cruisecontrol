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
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Element;

public class LinkEmailPublisherTest extends TestCase {

    private XMLLogHelper successLogHelper;

    public LinkEmailPublisherTest(String name) {
        super(name);
    }

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = new Element("cruisecontrol");
        Element infoElement = new Element("info");
        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", "log20020206120000.xml");
        infoElement.addContent(logFileElement);
        cruisecontrolElement.addContent(infoElement);

        return new XMLLogHelper(cruisecontrolElement);
    }

    public void setUp() throws Exception {
        successLogHelper = createLogHelper(true, true);
    }

    public void testCreateMessage() {
        EmailPublisher publisher = new LinkEmailPublisher();
        publisher.setBuildResultsURL(
            "http://mybuildserver.com:8080/buildservlet/BuildServlet");
        assertEquals(
            "View results here -> http://mybuildserver.com:8080/buildservlet/BuildServlet?log=log20020206120000",
            publisher.createMessage(successLogHelper));
    }

    public void testValidate() {
        EmailPublisher publisher = new LinkEmailPublisher();
        publisher.setMailHost("mailhost");
        publisher.setReturnAddress("returnaddress");
        try {
            publisher.validate();
            fail("should throw exception if BuildResultURL not set");
        } catch (CruiseControlException e) {
        }
        publisher.setBuildResultsURL("buildResultsURL");
        try {
            publisher.validate();
        } catch (CruiseControlException e) {
            fail("should NOT throw exception if BuildResultURL not set");
        }
    }
    
}