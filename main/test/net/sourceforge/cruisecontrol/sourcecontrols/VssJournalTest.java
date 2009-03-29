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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

/**
 * @author Eli Tucker
 * @author Simon Brandhof
 */
public class VssJournalTest extends TestCase {
    private static final String SS_DIR = "/";
    private static final String PROPERTY_ON_DELETE = "deletedfiles";

    private VssJournal element;

    public VssJournalTest(String name) {
        super(name);
    }

    protected void setUp() {
        // Set up so that this element will match all tests.
        element = new VssJournal();
        element.setSsDir(SS_DIR);
        element.setLastBuildDate(new Date(0));
        element.setPropertyOnDelete(PROPERTY_ON_DELETE);
    }

    public void testValidate() {
        VssJournal vj = new VssJournal();

        try {
            vj.validate();
            fail("VssJournal should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        vj.setJournalFile("journalfile");
        vj.setSsDir("ssdir");

        try {
            vj.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("VssJournal should not throw exceptions when required attributes are set.");
        }
    }

    public void testSubstringToLastSlash() {
        assertTrue("$/Eclipse/src/main/com/itxc".equals(
         element.substringToLastSlash("$/Eclipse/src/main/com/itxc/eclipse")));
    }

    public void testSubstringFromLastSlash() {
        assertTrue("eclipse".equals(
         element.substringFromLastSlash("$/Eclipse/src/main/com/itxc/eclipse")));
    }

    public void testIsInSsDir() {
        VssJournal element1 = new VssJournal();
        element1.setSsDir("/somedir");
        assertTrue(element1.isInSsDir("$/somedir"));
        assertTrue(element1.isInSsDir("$/somedir/Hello/There"));
        assertFalse(element1.isInSsDir("$/somedir2/Another/Directory/page.htm"));
        // Should be case insensitive
        assertTrue(element1.isInSsDir("$/SomeDir/Another/Directory/page.htm"));

        element1.setSsDir("/somedir/");
        assertTrue(element1.isInSsDir("$/somedir"));
        assertTrue(element1.isInSsDir("$/somedir/Hello/There"));
        assertFalse(element1.isInSsDir("$/somedir2/Another/Directory/page.htm"));

        element1.setSsDir("/");
        assertTrue(element1.isInSsDir("$/anythingCouldBeHere/Blah/blah"));
        assertTrue(element1.isInSsDir("$/"));

        element1.setSsDir("$/");
        assertTrue(element1.isInSsDir("$/anythingCouldBeHere/Blah/blah"));
        assertTrue(element1.isInSsDir("$/"));

        element1.setSsDir("$/somedir/");
        assertTrue(element1.isInSsDir("$/somedir"));
        assertTrue(element1.isInSsDir("$/somedir/Hello/There"));
        assertFalse(element1.isInSsDir("$/somedir2/Another/Directory/page.htm"));
    }

    public void testIsBeforeLastBuild() {
        VssJournal element1 = new VssJournal();
        long beforeTime = System.currentTimeMillis();
        long afterTime = beforeTime + 50000;

        element1.setLastBuildDate(new Date(beforeTime));
        assertTrue(!element1.isBeforeLastBuild(new Date(afterTime)));
    }

    public void testHandleEntryCheckin() {
        final List<String> entry = new ArrayList<String>();
        entry.add("$/AutoBuild/conf/cruisecontrol.properties");
        entry.add("Version: 5");
        entry.add("User: Etucker         Date:  7/06/01  Time:  2:11p");
        entry.add("Checked in");
        entry.add("Comment: Making cc email users when build failed");

        Modification mod = element.handleEntry(entry);
        assertEquals(mod.getFileName(), "cruisecontrol.properties");
        assertEquals(mod.getFolderName(), "$/AutoBuild/conf");
        assertEquals(mod.comment, "Comment: Making cc email users when build failed");
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.type, "vss");

        Modification.ModifiedFile modfile = mod.files.get(0);
        assertEquals(modfile.action, "checkin");
        assertNull(element.getProperties().get(PROPERTY_ON_DELETE));
    }

    public void testHandleEntryRename() {
        final List<String> entry = new ArrayList<String>();
        entry.add("$/WILD/Client/English");
        entry.add("Version: 15");
        entry.add("User: Ddavis          Date:  7/10/01  Time: 10:41a");
        entry.add("body3.htm renamed to step3.htm ");

        Modification mod = element.handleEntry(entry);
        assertEquals(mod.getFileName(), "body3.htm");
        assertEquals(mod.getFolderName(), "$/WILD/Client/English");
        assertEquals(mod.comment, "");
        assertEquals(mod.userName, "Ddavis");
        assertEquals(mod.type, "vss");

        Modification.ModifiedFile modfile = mod.files.get(0);
        assertEquals(modfile.action, "delete");
        assertNotNull(element.getProperties().get(PROPERTY_ON_DELETE));
    }

    public void testHandleEntryLabel() {
        final List<String> entry = new ArrayList<String>();
        entry.add("$/ThirdPartyComponents/jakarta-ant-1.3/lib");
        entry.add("Version: 7");
        entry.add("User: Etucker         Date:  7/06/01  Time: 10:28a");
        entry.add("Labeled test_label");
        entry.add("Comment: Just testing to see what all gets put in the log file");

        Modification mod = element.handleEntry(entry);

        assertEquals("Label entry added. Labels shouldn't be added.",
                     null, mod);
        assertNull(element.getProperties().get(PROPERTY_ON_DELETE));
    }

    public void testParseDate() throws ParseException {
        Date date = element.parseDate("User: Etucker         Date:  7/25/01  Time:  2:11p");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy hh:mm", Locale.US);
        assertEquals(sdf.parse("07/25/01 14:11"), date);

        element.setDateFormat("d.MM.yy");
        element.setTimeFormat("hh:mm");
        date = element.parseDate("User: Brandhof        Date: 15.11.05  Time:  16:54");
        assertEquals(sdf.parse("11/15/05 16:54"), date);
    }

}

