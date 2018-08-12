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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.ArrayList;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * @author Eric Lefevre
 */
public class ClearCaseTest extends TestCase {

    private static final String WINDOWS_LOG = "clearcase-history.txt";
    private static final String UNIX_LOG = "clearcase-history-alt.txt";
    private static final String WINDOWS_XML = "clearcase-history.xml";
    private static final String UNIX_XML = "clearcase-history-alt.xml";

    private ClearCase clearCase;
    private List<Modification> cannedMods;

    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }

    protected void setUp() throws JDOMException, IOException {
        // Initialize our ClearCase element
        clearCase = new ClearCase();
        cannedMods = new ArrayList<Modification>();

        String testXML;
        if (File.separatorChar == '\\') {
            testXML = WINDOWS_XML;
        } else {
            testXML = UNIX_XML;
        }

        // Set up the modification list to match against
        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(loadTestLog(testXML));
        List elts = doc.getRootElement().getChildren();

        for (Object elt1 : elts) {
            Element elt = (Element) elt1;
            ClearCaseModification mod = new ClearCaseModification();
            mod.fromElement(elt);
            adjustDateForTimeZone(mod);
            cannedMods.add(mod);
        }
    }

    private void adjustDateForTimeZone(ClearCaseModification mod) {
        Date date = mod.modifiedTime;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        TimeZone tz = TimeZone.getDefault();
        int offset = tz.getOffset(date.getTime());
        cal.add(Calendar.MILLISECOND, -offset);
        date = cal.getTime();
        mod.modifiedTime = date;
    }

    /**
     * Tests the streams of bytes that can be returned by the ClearCase server.
     */
    public void testClearCaseStream() throws IOException {
        String testLog;
        if (File.separatorChar == '\\') {
            testLog = WINDOWS_LOG;
        } else {
            testLog = UNIX_LOG;
        }
        BufferedInputStream stream =
                new BufferedInputStream(loadTestLog(testLog));

        List<Modification> modificationsFromFile = clearCase.parseStream(stream);
        assertEquals(cannedMods.size(), modificationsFromFile.size());

        for (int i = 0; i < modificationsFromFile.size(); i++) {
            Modification expected = cannedMods.get(i);
            Modification actual = modificationsFromFile.get(i);

            assertEquals(expected.type, actual.type);
            assertEquals(expected.modifiedTime, actual.modifiedTime);
            assertEquals(expected.userName, actual.userName);
            assertEquals(expected.emailAddress, actual.emailAddress);
            assertEquals(expected.revision, actual.revision);
            assertEquals(((ClearCaseModification) expected).labels, ((ClearCaseModification) actual).labels);
            assertEquals(((ClearCaseModification) expected).attributes, ((ClearCaseModification) actual).attributes);

            assertEquals(expected.files.size(), actual.files.size());
            for (int j = 0; j < actual.files.size(); j++) {
                ClearCaseModification.ModifiedFile af = expected.files.get(j);
                ClearCaseModification.ModifiedFile bf = actual.files.get(j);
                assertEquals(af.action, bf.action);
                assertEquals(af.fileName, bf.fileName);
                assertEquals(af.folderName, bf.folderName);
                assertEquals(af.revision, bf.revision);
            }


            final StringBuilder bc = new StringBuilder(actual.comment);
            for (int j = 0; j < bc.length(); j++) {
                if (bc.charAt(j) == 13) {
                    bc.deleteCharAt(j);
                }
            }
            assertEquals(expected.comment, bc.toString());

            System.out.println("Record " + i + " OK");
        }
    }

    public void testValidate() {
        ClearCase cc = new ClearCase();

        try {
            cc.validate();
            fail("ClearCase should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        cc.setViewpath("path");

        try {
            cc.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("ClearCase should not throw exceptions when required attributes are set.");
        }
    }

    public void testRecursiveAndAll() {
        ClearCase cc = new ClearCase();
        cc.setViewpath("path");
        cc.setBranch("branch");

        // test setting just 'all'
        cc.setAll(true);
        try {
            cc.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("ClearCase should not throw exceptions when only the 'all' attribute is set.");
        }

        // test 'recursive' together with 'all'
        cc.setRecursive(true);
        try {
            cc.validate();
            fail("ClearCase should throw an exception when both 'recursive' and 'all' are set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        // reset object to make sure we are testing with defaults
        cc = new ClearCase();
        cc.setViewpath("path");
        cc.setBranch("branch");

        // test setting just 'recursive'
        cc.setRecursive(true);
        try {
            cc.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("ClearCase should not throw an exception when only the 'all' attribute is set.");
        }
    }

}
