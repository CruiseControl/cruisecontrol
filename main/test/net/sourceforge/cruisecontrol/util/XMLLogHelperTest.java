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
package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.Element;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class XMLLogHelperTest extends TestCase {

    private Element _successfulLogElement;
    private Element _failedLogElement;

    public XMLLogHelperTest(String name) {
        super(name);
    }

    private Element createModificationsElement(String username1, String username2) {
        Element modificationsElement = new Element("modifications");
        Element modificationElement1 = new Element("modification");
        Element userElement1 = new Element("user");
        userElement1.addContent(username1);
        modificationElement1.addContent(userElement1);
        Element modificationElement2 = new Element("modification");
        Element userElement2 = new Element("user");
        userElement2.addContent(username2);
        modificationElement2.addContent(userElement2);
        Element modificationElement3 = new Element("modification");
        Element userElement3 = new Element("user");
        userElement3.addContent("user3");
        Element emailElement = new Element("email");
        emailElement.addContent("user3@host.com");
        modificationElement3.addContent(userElement3);
        modificationElement3.addContent(emailElement);

        modificationsElement.addContent(modificationElement1);
        modificationsElement.addContent(modificationElement2);
        modificationsElement.addContent(modificationElement3);

        return modificationsElement;
    }

    private Element createBuildElement(boolean successful) {
        Element buildElement = new Element("build");
        if (!successful) {
            buildElement.setAttribute("error", "No Build Necessary");
        }
        return buildElement;
    }

    private Element createInfoElement(String label, boolean lastSuccessful) {
        Element infoElement = new Element("info");

        Hashtable properties = new Hashtable();
        properties.put("projectname", "someproject");
        properties.put("label", label);
        properties.put("lastbuildtime", "");
        properties.put("lastgoodbuildtime", "");
        properties.put("lastbuildsuccessful", lastSuccessful + "");
        properties.put("buildfile", "");
        properties.put("target", "");
        properties.put("logfile", "log20020313120000.xml");

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

    public void setUp() {
        _successfulLogElement = new Element("cruisecontrol");
        _successfulLogElement.addContent(createInfoElement("1.0", false));
        _successfulLogElement.addContent(createBuildElement(true));
        _successfulLogElement.addContent(createModificationsElement("username1", "username2"));

        _failedLogElement = new Element("cruisecontrol");
        _failedLogElement.addContent(createInfoElement("1.1", true));
        _failedLogElement.addContent(createBuildElement(false));
        _failedLogElement.addContent(createModificationsElement("username3", "username4"));
    }

    public void testGetLabel() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        try {
            assertEquals("1.0", successHelper.getLabel());
            assertEquals("1.1", failureHelper.getLabel());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetLogFileName() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        try {
            assertEquals("log20020313120000.xml", successHelper.getLogFileName());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }


    public void testWasPreviousBuildSuccessful() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        try {
            assertEquals(false, successHelper.wasPreviousBuildSuccessful());
            assertEquals(true, failureHelper.wasPreviousBuildSuccessful());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetCruiseControlInfoProperty() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        try {
            assertEquals("1.0", successHelper.getCruiseControlInfoProperty("label"));
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
        try {
            successHelper.getCruiseControlInfoProperty("notaproperty");
        } catch (CruiseControlException e) {
            assertEquals("Property: notaproperty not found.", e.getMessage());
        }
    }

    public void testIsBuildNecessary() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        assertEquals(true, successHelper.isBuildNecessary());
        assertEquals(false, failureHelper.isBuildNecessary());
    }

    public void testGetProjectName() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);

        try {
            assertEquals("someproject", successHelper.getProjectName());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testIsBuildSuccessful() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        assertEquals(true, successHelper.isBuildSuccessful());
        assertEquals(false, failureHelper.isBuildSuccessful());
    }

    public void testGetBuildParticipants() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        Set successHelperParticipants = successHelper.getBuildParticipants();
        assertEquals(true, successHelperParticipants.contains("username1"));
        assertEquals(true, successHelperParticipants.contains("username2"));
        assertEquals(false, successHelperParticipants.contains("notaperson"));
        assertEquals(true, successHelperParticipants.contains("user3@host.com"));

        //test P4 changelist structure
        Element ccElement = new Element("cruisecontrol");
        Element modsElement = new Element("modifications");
        Element cl1Element = new Element("changelist");
        cl1Element.setAttribute("user", "user1");
        Element cl2Element = new Element("changelist");
        cl2Element.setAttribute("user", "user2");

        modsElement.addContent(cl1Element);
        modsElement.addContent(cl2Element);
        ccElement.addContent(modsElement);

        XMLLogHelper helper = new XMLLogHelper(ccElement);
        Set p4Users = helper.getBuildParticipants();

        assertEquals(true, p4Users.contains("user1"));
        assertEquals(true, p4Users.contains("user2"));
        assertEquals(false, p4Users.contains("notaperson"));
    }
}
