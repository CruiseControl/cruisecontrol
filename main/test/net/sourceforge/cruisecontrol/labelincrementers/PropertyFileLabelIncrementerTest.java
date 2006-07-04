package net.sourceforge.cruisecontrol.labelincrementers;

import junit.framework.TestCase;

public class PropertyFileLabelIncrementerTest extends TestCase {

    private PropertyFileLabelIncrementer incrementer;

    protected void setUp() {
        incrementer = new PropertyFileLabelIncrementer();
    }

    protected void tearDown() {
        incrementer = null;
    }

    public void testGetDefaultLabelShouldFailIfFileNotSpecified() {
        try {
            incrementer.getDefaultLabel();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("property file not specified", e.getMessage());
        }
    }

    public void testGetDefaultLabelShouldFailIfFileDoesntExistAndDefaultValueNotSpecified() {
        incrementer.setPropertyFile("foo.txt");
        try {
            incrementer.getDefaultLabel();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("property file does not exist: foo.txt", e.getMessage());
        }
    }

    public void testGetDefaultLabelShouldReturnDefaultValueIfFileDoesntExist() {
        incrementer.setPropertyFile("foo.txt");
        incrementer.setDefaultLabel("bar");
        assertEquals("bar", incrementer.getDefaultLabel());
    }
    
    public void testSetDefaultLabelShouldRejectNullAndEmptyString() {
        try {
            incrementer.setDefaultLabel(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("null is not valid as the default label", e.getMessage());
        }
        
        try {
            incrementer.setDefaultLabel("");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("empty string is not valid as the default label", e.getMessage());
        }
    }
}
