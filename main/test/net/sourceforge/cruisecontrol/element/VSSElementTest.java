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

public class VSSElementTest extends TestCase {

    private VssElement _element;
    
    private final String DATE_TIME_STRING = "Date:  6/20/01   Time:  10:36a";
    
    private final String STRANGE_DATE_TIME_STRING = "Date:  6/20/:1   Time:  10:36a";
    
    public VSSElementTest(String name) {
        super(name);
    }

    protected void setUp() {
        _element = new VssElement();
    }

    public void testParseUserSingleCharName() {
        String testName = "1";
        assertEquals(testName, _element.parseUser(createVSSLine(testName)));
    }
    
    public void testParseDateSingleCharName() {
        String testName = "1";
        try {
            assertEquals(
             VssElement.VSS_OUT_FORMAT.parse(DATE_TIME_STRING.trim() + "m"), 
             _element.parseDate(createVSSLine(testName)));
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
             VssElement.VSS_OUT_FORMAT.parse(DATE_TIME_STRING.trim() + "m"),
             _element.parseDate(strangeDateLine));
        } catch (ParseException e) {
            fail("Could not parse strange date string: " + e.getMessage());
        }
    }
    
    public void testParseUser10CharName() {
        String testName = "1234567890";
        assertEquals(testName, _element.parseUser(createVSSLine(testName)));
    }
    
    public void testParseUser20CharName() {
        String testName = "12345678900987654321";
        assertEquals(testName, _element.parseUser(createVSSLine(testName)));
    }

    /**
     * Produces a VSS line that looks something like this:
     * User: Username     Date:  6/14/01   Time:  6:39p
     *
     * @param testName Replaces the Username in above example
     */
    private String createVSSLine(String testName) {
        return "User: " + testName + " " + DATE_TIME_STRING;
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(VSSElementTest.class);
    }    
    
}
