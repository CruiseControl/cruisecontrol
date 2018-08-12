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
package net.sourceforge.cruisecontrol.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.jdom2.Element;

/**
 * @version $Id$
 */
public class XMLLogHelperTest extends TestCase {

    private Element successfulLogElement;
    private Element failedLogElement;

    private static final Date SOME_DATE = new Date(2333);

    @Override
    protected void setUp() {
        successfulLogElement = new Element("cruisecontrol");
        successfulLogElement.addContent(TestUtil.createInfoElement("1.0", false));
        successfulLogElement.addContent(createBuildElement(true));
        successfulLogElement.addContent(createModificationsElement("username1", "username2"));

        failedLogElement = new Element("cruisecontrol");
        failedLogElement.addContent(TestUtil.createInfoElement("1.1", true));
        failedLogElement.addContent(createBuildElement(false));
        failedLogElement.addContent(createModificationsElement("username3", "username4"));
    }
    
    @Override
    protected void tearDown() {
        successfulLogElement = null;
        failedLogElement = null;
    }

    private Modification[] createModifications(String username1, String username2) {
        Modification[] mods = new Modification[3];
        mods[0] = createModification(username1, false);
        mods[1] = createModification(username2, false);
        mods[2] = createModification("user3", true);
        return mods;
    }

    private Modification createModification(String name, boolean addemail) {
        Modification mod = new Modification();
        mod.userName = name;
        mod.comment = "This is the checkin for " + name;
        if (addemail) {
            mod.emailAddress = name + "@host.com";
        }
        mod.modifiedTime = SOME_DATE;

        Modification.ModifiedFile modfile = mod.createModifiedFile("file.txt", "myfolder");
        modfile.action = "checkin";
        return mod;
    }

    private Element createModificationsElement(String username1, String username2) {
        Element modificationsElement = new Element("modifications");
        Modification[] mods = createModifications(username1, username2);
        modificationsElement.addContent(mods[0].toElement());
        modificationsElement.addContent(mods[1].toElement());
        modificationsElement.addContent(mods[2].toElement());
        return modificationsElement;
    }

    private Element createBuildElement(boolean successful) {
        Element buildElement = new Element("build");
        if (!successful) {
            buildElement.setAttribute("error", "No Build Necessary");
        }
        return buildElement;
    }

    public void testGetLabel() {
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(failedLogElement);
        try {
            assertEquals("1.0", successHelper.getLabel());
            assertEquals("1.1", failureHelper.getLabel());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetLogFileName() {
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        try {
            assertEquals("log20020313120000.xml", successHelper.getLogFileName());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testWasPreviousBuildSuccessful() {
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(failedLogElement);
        try {
            assertEquals(false, successHelper.wasPreviousBuildSuccessful());
            assertEquals(true, failureHelper.wasPreviousBuildSuccessful());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetCruiseControlInfoProperty() {
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
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
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(failedLogElement);
        assertEquals(true, successHelper.isBuildNecessary());
        assertEquals(false, failureHelper.isBuildNecessary());
    }

    public void testGetProjectName() {
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);

        try {
            assertEquals("someproject", successHelper.getProjectName());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetStatusMessageWhenPassed() throws CruiseControlException {
        XMLLogHelper successHelper = new XMLLogHelper(TestUtil.createPassingBuild());
        assertEquals("successful", successHelper.getStatusMessage());
    }

    public void testGetStatusMessageWhenFixed() throws CruiseControlException {
        XMLLogHelper fixedHelper = new XMLLogHelper(TestUtil.createFixedBuild());
        assertEquals("fixed", fixedHelper.getStatusMessage());
    }

    public void testGetStatusMessageWhenBroken() throws CruiseControlException {
        XMLLogHelper fixedHelper = new XMLLogHelper(TestUtil.createFailedBuild());
        assertEquals("failed", fixedHelper.getStatusMessage());
    }

    public void testIsBuildSuccessful() {
        XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(failedLogElement);
        assertEquals(true, successHelper.isBuildSuccessful());
        assertEquals(false, failureHelper.isBuildSuccessful());
    }

    public void testGetBuildParticipants() {
        final XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        final Set<String> successHelperParticipants = successHelper.getBuildParticipants();
        assertEquals(true, successHelperParticipants.contains("username1"));
        assertEquals(true, successHelperParticipants.contains("username2"));
        assertEquals(false, successHelperParticipants.contains("notaperson"));
        assertEquals(true, successHelperParticipants.contains("user3@host.com"));
    }

    public void testGetModifications() {
        //NOTE:  There is an issue with dateformat if you convert
        //a date to a string and parse it back to a date the milliseconds will
        //be different.  Therefore the test gets all of the modifications
        //and sets the date on all of them to account for this, after it compares the date by string
        final XMLLogHelper successHelper = new XMLLogHelper(successfulLogElement);
        final Set<Modification> modifications = successHelper.getModifications();
        Modification[] mods = createModifications("username1", "username2");
        Map map = createMapByUserName(mods);
        for (Iterator iterator = modifications.iterator(); iterator.hasNext();) {
            Modification actual = (Modification) iterator.next();
            Modification expected = (Modification) map.get(actual.userName);
            assertNotNull(actual.userName, expected);
            assertModificationsEquals(expected, actual);
        }
    }

    private Map createMapByUserName(final Modification[] modifications) {
        final Map<String, Modification> map = new HashMap<String, Modification>();
        for (int i = 0; i < modifications.length; i++) {
            map.put(modifications[i].userName, modifications[i]);
        }

        return map;
    }

    private void assertModificationsEquals(Modification expected, Modification actual) {
        assertDateEquals(expected.modifiedTime, actual.modifiedTime);
        actual.modifiedTime = SOME_DATE;
        assertEquals(expected, actual);
    }

    private void assertDateEquals(Date expected, Date actual) {
        assertEquals(DateUtil.formatIso8601(expected), DateUtil.formatIso8601(actual));
    }

}