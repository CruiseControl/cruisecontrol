package net.sourceforge.cruisecontrol;

import java.io.*;

import junit.framework.*;

public class BuildInfoTest extends TestCase {

    private static final String STD_LABEL = "Test Label";
    private static final String STD_LAST_GOOD_BUILD = "20010503120000";
    private static final String STD_LAST_BUILD = "20010504120000";
    private static final String STD_USER_LIST = "paul,alden";
    private static final String STD_LOGFILE = "log20010504120000L1.3";
    private static final boolean STD_LAST_BUILD_SUCCESS = true;
    private static final boolean STD_BUILD_NOT_NECCESSARY = false;
    
    private static final String INCOMPLETE_LAST_BUILD = "20010504";

    /**
     * Obligitory name constructor.
     * 
     * @param name   Name of the test to run.
     */
    public BuildInfoTest(String name) {
        super(name);
    }

    /**
     * This test method checks the standard accessor behavior. It sets a bunch
     * of values and then makes sure that the instance of BuildInfo actually
     * has the same value when the getter is called.
     */
    public void testAccessors() {
        //Setup the attributes on the info instance.
        BuildInfo info = populateStandard();

        //None of the std assertions should cause a failure.
        doStandardAssertions(info);
    }

    /**
     * This method checks that timestamp related setters throw an exception 
     * when an "incomplete" time is provided
     */
    public void testIncompleteTime() {
        BuildInfo info = new BuildInfo();
        try {
            info.setLastBuild(INCOMPLETE_LAST_BUILD);
            fail("Provided an incomplete time ("+INCOMPLETE_LAST_BUILD
                 + "), but the setLastBuild method didn't throw an"
                 + " IllegalArgumentException as expected.");
        } catch (IllegalArgumentException iae) {
            //Good we expect the incomplete time to cause an 
            // IllegalArgumentException to be thrown.
        }
    }

    /**
     * Tests a standard call to write a BuildInfo instance out to disk. No
     * failures should occur. Most importantly, writing the info instance out
     * should NOT change the value of any of the info attributes.
     * 
     * @exception IOException
     *                   Creating a tempfile to write the buildinfo instance to may cause an
     *                   exception, or writing the info instance itself. In this standard
     *                   operating procedure an exception is not expected. jUnit will need to
     *                   indicate an ERROR occured.
     */
    public void testWrite() throws IOException {
        BuildInfo info = populateStandard();

        //Create a temp file to write the buildinfo instance to.
        File tempFile = File.createTempFile("CruiseControlTesting", "BuildInfo");
        tempFile.deleteOnExit();

        //Should be able to write it with no exceptions being thrown.
        info.write(tempFile.getAbsolutePath());

        //Make sure the tempfile exists and has some data in it.
        assertTrue("The temp file should have been written, but it doesn't exist.",
               tempFile.exists());
        assertTrue("Cannot read the temp file.", tempFile.canRead());
        InputStream in = new FileInputStream(tempFile);
        assertTrue("The tempfile contains zero bytes, but it should contain at"
               + " least enough bytes to represent the attributes set.", 
               in.available() > 0);

        //Writing the file out should not change the value of any of the attributes.
        doStandardAssertions(info);
    }

    public void testRead() throws IOException {
        BuildInfo info = populateStandard();

        //Create a temp file to write the buildinfo instance to.
        File tempFile = File.createTempFile("CruiseControlTesting", "BuildInfo");
        tempFile.deleteOnExit();

        //Should be able to write it with no exceptions being thrown.
        info.write(tempFile.getAbsolutePath());

        //Try reading in a new instance of BuildInfo.
        BuildInfo readInfo = new BuildInfo();
        readInfo.read(tempFile.getAbsolutePath());

        //Make sure all non-transient attributes ARE populated.
        doStdNonTransientAssertions(readInfo);

        //Make sure all transient attributes are NOT populated.
        doTransientBlankAssertions(readInfo);
    }

    /**
     * Creates a new BuildInfo instance populated with the standard values
     * set as constants in this test class. Every attribute in the info instance
     * returned should be populated with a valid value.
     * 
     * @return Fully populated BuildInfo instance.
     */
    private BuildInfo populateStandard() {
        BuildInfo info = new BuildInfo();
        info.setLabel(STD_LABEL);
        info.setLastGoodBuild(STD_LAST_GOOD_BUILD);
        info.setLastBuild(STD_LAST_BUILD);
        info.setUserList(STD_USER_LIST);
        info.setLogfile(STD_LOGFILE);
        info.setLastBuildSuccessful(STD_LAST_BUILD_SUCCESS);
        info.setBuildNotNecessary(STD_BUILD_NOT_NECCESSARY);

        return info;
    }

    /**
     * Performs the assertions required to make sure that the info instance
     * provided is populated with the values defined as standard constants
     * in this test class.
     * 
     * @param info   BuildInfo instance.
     */
    private void doStandardAssertions(BuildInfo info) {
        doStdNonTransientAssertions(info);

        //Check each attribute to make sure it worked.
        assertEquals(STD_USER_LIST, info.getUserList());
        assertEquals(STD_LOGFILE, info.getLogfile());
        assertEquals(STD_LAST_BUILD_SUCCESS, info.isLastBuildSuccessful());
        assertEquals(STD_BUILD_NOT_NECCESSARY, info.isBuildNotNecessary());
    }

    private void doStdNonTransientAssertions(BuildInfo info) {
        assertEquals(STD_LABEL, info.getLabel());
        assertEquals(STD_LAST_GOOD_BUILD, info.getLastGoodBuild());
        assertEquals(STD_LAST_BUILD, info.getLastBuild());
    }

    private void doTransientBlankAssertions(BuildInfo info) {
        assertNull(info.getUserList());
        assertNull(info.getLogfile());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BuildInfoTest.class);
    }
}
