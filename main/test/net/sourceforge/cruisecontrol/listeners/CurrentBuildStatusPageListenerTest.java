package net.sourceforge.cruisecontrol.listeners;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;
import java.io.IOException;

public class CurrentBuildStatusPageListenerTest extends TestCase {

    public void testShouldFormatZeroCorrectly() {
        assertFormat("0.000", 0);
    }

    public void testShouldFormatOneCorrectly() {
        assertFormat("0.001", 1);
    }

    public void testShouldFormat999Correctly() {
        assertFormat("0.999", 999);
    }

    public void testShouldFormatOneSecondCorrectly() {
        assertFormat("1.000", 1000);
    }

    public void testShouldFormat60SecondsCorrectly() {
        assertFormat("01:00.000", 60000);
    }

    public void testShouldFormat61SecondsCorrectly() {
        assertFormat("01:01.000", 61000);
    }

    public void testShouldFormat61Seconds1MilliCorrectly() {
        assertFormat("01:01.001", 61001);
    }

    public void testShouldFormat60MinutesCorrectly() {
        assertFormat("1:00:00.000", 3600000);
    }

    public void testShouldFormat24HoursCorrectly() {
        assertFormat("24:00:00.000", 86400000);
    }

    public void testShouldFormat99HoursCorrectly() {
        assertFormat("99:00:00.000", 356400000);
    }

    public void testShouldFormat100HoursCorrectly() {
        assertFormat("100:00:00.000", 360000000);
    }

    public void testShouldFormatMaxLongCorrectly() {
        assertFormat("2562047788015:12:55.807", Long.MAX_VALUE);
    }

    public void testShouldFormatNegativeOneMillis() {
        assertFormat("0.00-1", -1);
    }

    public void testShouldFormatNegative999Millis() {
        assertFormat("0.00-999", -999);
    }

    private void assertFormat(String expected, long input) {
        assertEquals(expected, CurrentBuildStatusPageListener.formatDuration(input));
    }

    public void testShouldBeInvalidWhenFileNotSet() {
        assertInvalid(new CurrentBuildStatusPageListener());
    }

    public void testShouldBeInvalidWhenFileInDirThatCannotBeMkdired() {
        CurrentBuildStatusPageListener c = new CurrentBuildStatusPageListener();
        c.setFile("BADDRIVE:\\foo.file");
        assertInvalid(c);
    }

    public void testShouldBeValidWhenFileSet() throws Exception {
        File temp = File.createTempFile(getClass().getName(), "tmp");
        temp.deleteOnExit();
        CurrentBuildStatusPageListener c = new CurrentBuildStatusPageListener();
        c.setFile(temp.getAbsolutePath());
        c.validate();
    }

    public void testShouldBeInvalidWhenSourceFileSetButDoesntExist() throws IOException {
        File temp = File.createTempFile(getClass().getName(), "tmp");
        temp.deleteOnExit();
        CurrentBuildStatusPageListener c = new CurrentBuildStatusPageListener();
        c.setFile(temp.getAbsolutePath());
        c.setSourceFile("NONEXISTENTFILE");
        assertInvalid(c);
    }

    public void testShouldBeInvalidWhenSourceFileSetToDir() throws Exception {
        File temp = File.createTempFile(getClass().getName(), "tmp");
        temp.deleteOnExit();
        CurrentBuildStatusPageListener c = new CurrentBuildStatusPageListener();
        c.setFile(temp.getAbsolutePath());
        c.setSourceFile(System.getProperty("java.io.tmpdir"));
        assertInvalid(c);
    }

    public void testShouldBeValidWhenFileSetAndSourceSetToRealFile() throws Exception {
        File temp = File.createTempFile(getClass().getName(), "tmp");
        temp.deleteOnExit();
        CurrentBuildStatusPageListener c = new CurrentBuildStatusPageListener();
        c.setFile(temp.getAbsolutePath());
        c.setSourceFile(temp.getAbsolutePath());
        c.validate();
    }

    private void assertInvalid(CurrentBuildStatusPageListener c) {
        try {
            c.validate();
            fail("expected an exception");
        } catch (CruiseControlException expected) {
            //good
        }
    }

}
