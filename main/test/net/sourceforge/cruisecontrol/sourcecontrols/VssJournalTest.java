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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

/**
 * @author Eli Tucker
 */
public class VssJournalTest extends TestCase {

    private VssJournal element;
    
    public VssJournalTest(String name) {
        super(name);
    }

    protected void setUp() {
        // Set up so that this element will match all tests.
        element = new VssJournal();
        element.setSsDir("/");
        element.setLastBuildDate(new Date(0));
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
        assertTrue(element1.isInSsDir("$/somedir/Hello/There"));
        // Should be case insensitive
        assertTrue(element1.isInSsDir("$/SomeDir/Another/Directory/page.htm"));  
        
        element1.setSsDir("/");
        assertTrue(element1.isInSsDir("$/anythingCouldBeHere/Blah/blah"));
    }
    
    public void testIsBeforeLastBuild() {
        VssJournal element1 = new VssJournal();
        long beforeTime = System.currentTimeMillis();
        long afterTime = beforeTime + 50000;

        element1.setLastBuildDate(new Date(beforeTime));
        assertTrue(!element1.isBeforeLastBuild(new Date(afterTime)));
    }
    
    public void testHandleEntryCheckin() {
        List entry = new ArrayList();
        entry.add("$/AutoBuild/conf/cruisecontrol.properties");
        entry.add("Version: 5");
        entry.add("User: Etucker         Date:  7/06/01  Time:  2:11p");
        entry.add("Checked in");
        entry.add("Comment: Making cc email users when build failed");
        
        Modification mod = element.handleEntry(entry);
        assertEquals(mod.fileName, "cruisecontrol.properties");
        assertEquals(mod.folderName, "$/AutoBuild/conf");
        assertEquals(mod.comment, "Comment: Making cc email users when build failed");
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.type, "checkin");                
    }
    
    public void testHandleEntryRename() {
        List entry = new ArrayList();
        entry.add("$/WILD/Client/English");
        entry.add("Version: 15");
        entry.add("User: Ddavis          Date:  7/10/01  Time: 10:41a");
        entry.add("body3.htm renamed to step3.htm ");
        
        Modification mod = element.handleEntry(entry);
        assertEquals(mod.fileName, "body3.htm");
        assertEquals(mod.folderName, "$/WILD/Client/English");
        assertEquals(mod.comment, "");
        assertEquals(mod.userName, "Ddavis");
        assertEquals(mod.type, "delete");                
    }    
    
    public void testHandleEntryLabel() {
        List entry = new ArrayList();
        entry.add("$/ThirdPartyComponents/jakarta-ant-1.3/lib");
        entry.add("Version: 7");
        entry.add("User: Etucker         Date:  7/06/01  Time: 10:28a");
        entry.add("Labeled test_label");
        entry.add("Comment: Just testing to see what all gets put in the log file");
        
        Modification mod = element.handleEntry(entry);
        
        assertEquals("Label entry added. Labels shouldn't be added.",
                     null, mod);
    }

}
    