package net.sourceforge.cruisecontrol.element;

import java.util.*;
import junit.framework.*;

public class VSSElementTest extends TestCase {

    private VssElement _element;
    
    private final String DATE_TIME_STRING = "Date:  6/20/01   Time:  10:36a";
    
    public VSSElementTest(String name) {
        super(name);
    }

    protected void setUp() {
        _element = new VssElement();
    }
    
    // Sample of what the VSS line will look like:
    // User: Username     Date:  6/14/01   Time:  6:39p
    public void testParseUserSingleCharName() {
        String testName = "1";
        assertEquals(testName, _element.parseUser(createVSSLine(testName)));
    }
    
    public void testParseDateSingleCharName() {
        String testName = "1";
        try {
            assertEquals(VssElement.VSS_OUT_FORMAT.parse(DATE_TIME_STRING.trim() + "m"), 
             _element.parseDate(createVSSLine(testName)));
        } catch (java.text.ParseException e) {
            fail("Could not parse date string");
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

    private String createVSSLine(String testName) {
        return "User: " + testName + " " + DATE_TIME_STRING;
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(VSSElementTest.class);
    }    
    
}
