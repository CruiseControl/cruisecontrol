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
import org.jdom.Element;
import org.apache.log4j.PropertyConfigurator;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class LinkEmailPublisherTest extends TestCase {

    private XMLLogHelper _successLogHelper;
    private XMLLogHelper _fixedLogHelper;
    private XMLLogHelper _failureLogHelper;
    private EmailPublisher _emailPublisher;

    public LinkEmailPublisherTest(String name) {
        super(name);
    }

    protected XMLLogHelper createLogHelper(boolean success, boolean fixed) {
        Element buildElement = new Element("build");
        Element modificationsElement = new Element("modifications");
        String[] users = new String[]{"user1", "user2", "user2", "user3"};
        for(int i=0; i<users.length; i++) {
            Element modificationElement = new Element("modification");
            Element userElement = new Element("user");
            userElement.addContent(users[i]);
            modificationElement.addContent(userElement);
            modificationsElement.addContent(modificationElement);
        }
        buildElement.addContent(modificationsElement);

        Element propertiesElement = new Element("properties");
        Element propertyElement = new Element("property");
        propertyElement.setAttribute("name", "ant.project.name");
        propertyElement.setAttribute("value", "some project");
        propertiesElement.addContent(propertyElement);
        buildElement.addContent(propertiesElement);

        if(!success) {
            buildElement.setAttribute("error", "Build Not Necessary");
        }

        Element cruisecontrolElement = new Element("cruisecontrol");
        Element lastBuildSuccessfulElement = new Element("lastbuildsuccessful");
        Element labelElement = new Element("label");
        labelElement.setAttribute("value", "somelabel");
        Element logFileElement = new Element("logfile");
        logFileElement.setAttribute("value", "log20020206120000.xml");
        if(fixed) {
            lastBuildSuccessfulElement.setAttribute("value", "true");
        } else {
            lastBuildSuccessfulElement.setAttribute("value", "false");
        }
        cruisecontrolElement.addContent(lastBuildSuccessfulElement);
        cruisecontrolElement.addContent(labelElement);
        cruisecontrolElement.addContent(logFileElement);
        buildElement.addContent(cruisecontrolElement);

        return new XMLLogHelper(buildElement);
    }

    public void setUp() {
        //write out emailmap.properties
        FileOutputStream fos = null;
        Properties props = new Properties();
        props.setProperty("user3", "user3@host2.com");
        try {
            fos = new FileOutputStream("_emailmap.properties");
            props.store(fos, "");
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos = null;
        }

        _successLogHelper = createLogHelper(true, true);
        _failureLogHelper = createLogHelper(false, false);
        _fixedLogHelper = createLogHelper(true, false);
        _emailPublisher = new LinkEmailPublisher();
        _emailPublisher.setDefaultSuffix("@host.com");
        _emailPublisher.setEmailMap("_emailmap.properties");
        _emailPublisher.addAlwaysAddress("always1");
        _emailPublisher.addAlwaysAddress("always2@host.com");
        _emailPublisher.addFailureAddress("failure1");
        _emailPublisher.addFailureAddress("failure2@host.com");
    }

    public void testShouldSend() {
        //build not necessary, spam while broken=true
        _emailPublisher.setSpamWhileBroken(true);
        assertEquals(_emailPublisher.shouldSend(_failureLogHelper), true);

        //build necessary, spam while broken = true
        assertEquals(_emailPublisher.shouldSend(_successLogHelper), true);

        //build not necessary, spam while broken=false
        _emailPublisher.setSpamWhileBroken(false);
        assertEquals(_emailPublisher.shouldSend(_failureLogHelper), false);

        //build necessary, spam while broken = false
        assertEquals(_emailPublisher.shouldSend(_successLogHelper), true);


    }

    public void testCreateMessage() {
        _emailPublisher.setServletUrl("http://mybuildserver.com:8080/buildservlet/BuildServlet");
        assertEquals(_emailPublisher.createMessage(_successLogHelper), "View results here -> http://mybuildserver.com:8080/buildservlet/BuildServlet?log20020206120000");
    }

    public void testCreateSubject() {
        _emailPublisher.setReportSuccess("always");
        assertEquals(_emailPublisher.createSubject(_successLogHelper), "some project somelabel Build Successful");
        _emailPublisher.setReportSuccess("fixes");
        assertEquals(_emailPublisher.createSubject(_fixedLogHelper), "some project somelabel Build Fixed");

        assertEquals(_emailPublisher.createSubject(_failureLogHelper), "some project Build Failed");
    }

    public void testCreateUserList() {
        PropertyConfigurator.configure("log4j.properties");
        assertEquals(_emailPublisher.createUserList(_successLogHelper), "always1@host.com,always2@host.com,user1@host.com,user2@host.com,user3@host2.com");
        assertEquals(_emailPublisher.createUserList(_failureLogHelper), "always1@host.com,always2@host.com,failure1@host.com,failure2@host.com,user1@host.com,user2@host.com,user3@host2.com");
    }
}