/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ClearCaseModification;

/**
 * @author Eric Lefevre
 */
public class ClearCaseTest extends TestCase {

    public static final File TEST_DIR =
        new File("test/net/sourceforge/cruisecontrol/sourcecontrols");
    public static final File WINDOWS_LOG = 
        new File(TEST_DIR, "clearcase-history.txt");
    public static final File UNIX_LOG = 
        new File(TEST_DIR, "clearcase-history-alt.txt");    
    public static final File WINDOWS_XML =
        new File(TEST_DIR, "clearcase-history.xml");
    public static final File UNIX_XML =
        new File(TEST_DIR, "clearcase-history-alt.xml");

    public static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private ClearCase clearCase;
    private List mods;

    protected void setUp() throws JDOMException, IOException {
        // Initialize our ClearCase element
        clearCase = new ClearCase();
        mods = new Vector();

        File testXML;
        if (File.separatorChar == '\\') {
            testXML = WINDOWS_XML;
        } else {
            testXML = UNIX_XML;
        }

        // Set up the modification list to match against
        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(testXML);
        List elts = doc.getRootElement().getChildren();

        Iterator it = elts.iterator();
        while (it.hasNext()) {
           Element elt = (Element) it.next();
           ClearCaseModification mod = new ClearCaseModification();
           mod.fromElement(elt, DATE_FMT);
           mods.add(mod);
        }
    }

    /**
     * Tests the streams of bytes that can be returned by the ClearCase server.
     */
    public void testClearCaseStream() throws IOException {
        File testLog;
        if (File.separatorChar == '\\') {
            testLog = WINDOWS_LOG;
        } else {
            testLog = UNIX_LOG;
        }
        BufferedInputStream stream =
            new BufferedInputStream(new FileInputStream(testLog));

        List list = clearCase.parseStream(stream);
        assertEquals(mods.size(), list.size());

        for (int i = 0; i < list.size(); i++) {
            ClearCaseModification a = (ClearCaseModification) mods.get(i);
            ClearCaseModification b = (ClearCaseModification) list.get(i);
            assertEquals(a.type, b.type);
            assertEquals(a.fileName, b.fileName);
            assertEquals(a.folderName, b.folderName);
            assertEquals(a.modifiedTime, b.modifiedTime);
            assertEquals(a.userName, b.userName);
            assertEquals(a.emailAddress, b.emailAddress);
            assertEquals(a.revision, b.revision);
            assertEquals(a.labels, b.labels);
            assertEquals(a.attributes, b.attributes);
            
            StringBuffer bc = new StringBuffer(b.comment);
            for (int j = 0; j < bc.length(); j++) {
               if (bc.charAt(j) == 13) {
                  bc.deleteCharAt(j);
               }
            }
            assertEquals(a.comment, bc.toString());

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
        cc.setBranch("branch");

        try {
            cc.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("ClearCase should not throw exceptions when required attributes are set.");
        }
    }

    public void testOutput() throws IOException {
        
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(ClearCaseTest.class);
    }

}
