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
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.StringReader;
import java.util.Hashtable;
import java.util.Iterator;

public class LinkEmailPublisherTest extends TestCase {

    private XMLLogHelper _successLogHelper;
    private XMLLogHelper _fixedLogHelper;
    private XMLLogHelper _failureLogHelper;
    private EmailPublisher _emailPublisher;

    public LinkEmailPublisherTest(String name) {
        super(name);
    }

    protected XMLLogHelper createLogHelper(boolean success, boolean fixed) {
        Element cruisecontrolElement = new Element("cruisecontrol");
        Element buildElement = new Element("build");
        Element modificationsElement = new Element("modifications");
        String[] users = new String[]{"user1", "user2", "user2", "user3"};
        for (int i = 0; i < users.length; i++) {
            Element modificationElement = new Element("modification");
            Element userElement = new Element("user");
            userElement.addContent(users[i]);
            modificationElement.addContent(userElement);
            modificationsElement.addContent(modificationElement);
        }

        Element propertiesElement = new Element("properties");
        Element propertyElement = new Element("property");
        propertyElement.setAttribute("name", "ant.project.name");
        propertyElement.setAttribute("value", "some project");
        propertiesElement.addContent(propertyElement);
        buildElement.addContent(propertiesElement);

        if (!success) {
            buildElement.setAttribute("error", "No Build Necessary");
        }

        cruisecontrolElement.addContent(modificationsElement);
        cruisecontrolElement.addContent(buildElement);
        cruisecontrolElement.addContent(createInfoElement("somelabel", fixed));

        return new XMLLogHelper(cruisecontrolElement);
    }

    private Element createInfoElement(String label, boolean fixed) {
        Element infoElement = new Element("info");

        Hashtable properties = new Hashtable();
        properties.put("label", label);
        properties.put("lastbuildsuccessful", !fixed + "");
        properties.put("logfile", "log20020206120000.xml");

        Iterator propertyIterator = properties.keySet().iterator();
        while (propertyIterator.hasNext()) {
            String propertyName = (String) propertyIterator.next();
            Element propertyElement = new Element("property");
            propertyElement.setAttribute("name", propertyName);
            propertyElement.setAttribute("value", (String) properties.get(propertyName));
            infoElement.addContent(propertyElement);
        }

        return infoElement;
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
        _emailPublisher = (LinkEmailPublisher) xmlHelper.configure(emailPublisherElement, "net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher");

        _successLogHelper = createLogHelper(true, true);
        _failureLogHelper = createLogHelper(false, false);
        _fixedLogHelper = createLogHelper(true, true);
    }

    public void testShouldSend() throws Exception {
        //build not necessary, spam while broken=true
        _emailPublisher.setSpamWhileBroken(true);
        assertEquals(true, _emailPublisher.shouldSend(_failureLogHelper));

        //build necessary, spam while broken = true
        assertEquals(true, _emailPublisher.shouldSend(_successLogHelper));

        //build not necessary, spam while broken=false
        _emailPublisher.setSpamWhileBroken(false);
        assertEquals(false, _emailPublisher.shouldSend(_failureLogHelper));

        //build necessary, spam while broken = false
        assertEquals(true, _emailPublisher.shouldSend(_successLogHelper));
    }

    public void testCreateMessage() {
        _emailPublisher.setBuildResultsUrl("http://mybuildserver.com:8080/buildservlet/BuildServlet");
        assertEquals("View results here -> http://mybuildserver.com:8080/buildservlet/BuildServlet?log20020206120000", _emailPublisher.createMessage(_successLogHelper));
    }

    public void testCreateSubject() throws Exception {
        _emailPublisher.setReportSuccess("always");
        assertEquals("some project somelabel Build Successful", _emailPublisher.createSubject(_successLogHelper));
        _emailPublisher.setReportSuccess("fixes");
        assertEquals("some project somelabel Build Fixed", _emailPublisher.createSubject(_fixedLogHelper));

        assertEquals("some project Build Failed", _emailPublisher.createSubject(_failureLogHelper));
    }

    public void testCreateUserList() {
        PropertyConfigurator.configure("log4j.properties");
        assertEquals("always1@host.com,always2@host.com,user1@host.com,user2@host.com,user3@host2.com", _emailPublisher.createUserList(_successLogHelper));
        assertEquals("always1@host.com,always2@host.com,failure1@host.com,failure2@host.com,user1@host.com,user2@host.com,user3@host2.com", _emailPublisher.createUserList(_failureLogHelper));
    }
}