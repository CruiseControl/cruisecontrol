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
package net.sourceforge.cruisecontrol.element;

import java.text.*;
import java.util.*;
import junit.framework.*;
import net.sourceforge.cruisecontrol.Modification;

/**
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Arun Aggarwal
 */
public class VSSElementTest extends TestCase {

    private VssElement _element;
    
    private final String DATE_TIME_STRING = "Date:  6/20/01   Time:  10:36a";
    private final String ALTERNATE_DATE_TIME_STRING = "Date:  20/6/01   Time:  10:36a";    
    private final String STRANGE_DATE_TIME_STRING = "Date:  6/20/:1   Time:  10:36a";
    
    public VSSElementTest(String name) {
        super(name);
    }

    protected void setUp() {
        _element = new VssElement();
    }
    
    //(PENDING) test the form of the history command used

    public void testParseUserSingleCharName() {
        String testName = "1";
        assertEquals(testName, _element.parseUser(createVSSLine(testName, DATE_TIME_STRING)));
    }
    
    public void testParseDateSingleCharName() {
        String testName = "1";
        try {
            assertEquals(
             _element.vssDateTimeFormat.parse(DATE_TIME_STRING.trim() + "m"), 
             _element.parseDate(createVSSLine(testName, DATE_TIME_STRING)));
            
        } catch (ParseException e) {
            fail("Could not parse date string: " + e.getMessage());
        }
    }

    /**
     * Parse a user supplied date format.
     */
    public void testParseDateAlternate() {
        String testName = "1";
        VssElement vssElement = new VssElement();
        vssElement.setDateFormat("dd/MM/yy");
        try {
            assertEquals(
              vssElement.vssDateTimeFormat.parse(ALTERNATE_DATE_TIME_STRING.trim() + "m"), 
              vssElement.parseDate(createVSSLine(testName, ALTERNATE_DATE_TIME_STRING)));
        } catch (ParseException e) {
            fail("Could not parse date string: " + e.getMessage());
        }
    }
    
    /**
     * Some people are seeing strange date outputs from their VSS history that
     * looks like this:
     *  User: Aaggarwa     Date:  6/29/:1   Time:  3:40p
     * Note the ":" rather than a "0"
     */
    public void testParseDateStrangeDate() {
        String strangeDateLine = "User: Aaggarwa     Date:  6/20/:1   Time: 10:36a";
        try {
            assertEquals(
             _element.vssDateTimeFormat.parse(DATE_TIME_STRING.trim() + "m"),
             _element.parseDate(strangeDateLine));
        } catch (ParseException e) {
            fail("Could not parse strange date string: " + e.getMessage());
        }
    }
    
    public void testParseUser10CharName() {
        String testName = "1234567890";
        assertEquals(testName, _element.parseUser(createVSSLine(testName, DATE_TIME_STRING)));
    }
    
    public void testParseUser20CharName() {
        String testName = "12345678900987654321";
        assertEquals(testName, _element.parseUser(createVSSLine(testName, DATE_TIME_STRING)));
    }

    public void testHandleEntryUnusualLabel() {
        int modificationsSizeBefore = _element.getModifications().size();
        
        List entry = new ArrayList();
        entry.add("*****  built  *****");
        entry.add("Version 4");
        entry.add("Label: \"autobuild_test\"");
        entry.add("User: Etucker      Date:  6/26/01   Time: 11:53a");
        entry.add("Labeled");
        
        _element.handleEntry(entry);
        assertEquals("Unusual label entry added. Labels shouldn't be added.",
                     _element.getModifications().size(), modificationsSizeBefore);
    }

    public void testHandleEntryCheckinWithComment() {
        int modificationsSizeBefore = _element.getModifications().size();
        
        List entry = new ArrayList();        
        entry.add("*****  ttyp_direct.properties  *****");
        entry.add("Version 10");
        entry.add("User: Etucker      Date:  7/03/01   Time:  3:24p");
        entry.add("Checked in $/Eclipse/src/main/com/itxc/eclipse/some/path/here");
        entry.add("Comment: updated country codes for Colombia and Slovokia");
        
        _element.handleEntry(entry);
        assertEquals(_element.getModifications().size(), modificationsSizeBefore + 1);
        Modification mod = (Modification) _element.getModifications().get(_element.getModifications().size() - 1);
        assertEquals(mod.fileName, "ttyp_direct.properties");
        assertEquals(mod.folderName, "/Eclipse/src/main/com/itxc/eclipse/some/path/here");
        assertEquals(mod.comment, "Comment: updated country codes for Colombia and Slovokia");
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.type, "checkin");        
    }

    public void testHandleEntryAdded() {
        int modificationsSizeBefore = _element.getModifications().size();
        
        List entry = new ArrayList();        
        entry.add("*****  core  *****");
        entry.add("Version 19");
        entry.add("User: Etucker      Date:  7/03/01   Time: 11:16a");
        entry.add("SessionIdGenerator.java added");
        
        _element.handleEntry(entry);
        assertEquals(_element.getModifications().size(), modificationsSizeBefore + 1);
        Modification mod = (Modification) _element.getModifications().get(_element.getModifications().size() - 1);
        assertEquals(mod.fileName, "SessionIdGenerator.java");        
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.type, "add");
    }    
    
    /**
     * Produces a VSS line that looks something like this:
     * User: Username     Date:  6/14/01   Time:  6:39p
     *
     * @param testName Replaces the Username in above example
     */
    private String createVSSLine(String testName, String dateTimeString) {
        return "User: " + testName + " " + dateTimeString;
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(VSSElementTest.class);
    }    
    
}
