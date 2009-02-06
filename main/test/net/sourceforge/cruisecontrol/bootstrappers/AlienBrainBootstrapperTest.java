package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class AlienBrainBootstrapperTest extends TestCase {

    public void testValidate() {
        AlienBrainBootstrapper bootStrapper = new AlienBrainBootstrapper();

        try {
            bootStrapper.validate();
            fail("should throw an exception when no attributes are set");
        } catch (CruiseControlException e) {
        }

        bootStrapper.setPath("alienbrain://A/File/That/I/Want.txt");

        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("should not throw exceptions when required "
                + "attributes are set.\n" + e);
        }

        bootStrapper.setOverwriteWritable("notSkip");
        try {
            bootStrapper.validate();
            fail("should throw an exception when an attribute has an invalid value");
        } catch (CruiseControlException e) {
        }
        
        bootStrapper.setOverwriteWritable("notReplace");
        try {
            bootStrapper.validate();
            fail("should throw an exception when an attribute has an invalid value");
        } catch (CruiseControlException e) {
        }

        //test both non-default valid value and capital handling.
        bootStrapper.setOverwriteWritable("RePlaCe");
        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("should not throw exceptions when required "
                + "attributes are set.\n" + e);
        }
    }

    public void testBuildBootstrapCommand() {
        AlienBrainBootstrapper bootStrapper = new AlienBrainBootstrapper();

        String user = "foo";
        String path = "alienbrain://A/File/That/I/Want.txt";
        String password = "foobar";
        String localpath = "c:\\My Projects";

        bootStrapper.setPath(path);
        assertEquals("ab getlatest " + path + " -overwritewritable skip",
            bootStrapper.buildBootstrapCommand().toString());
        
        bootStrapper.setUser(user);
        assertEquals("ab -u " + user + " getlatest " + path 
            + " -overwritewritable skip",
            bootStrapper.buildBootstrapCommand().toString());
        
        bootStrapper.setPassword(password);
        assertEquals("ab -u " + user + " -p " + password + " getlatest " 
            + path + " -overwritewritable skip",
            bootStrapper.buildBootstrapCommand().toString());

        bootStrapper.setLocalPath(localpath);
        assertEquals("ab -u " + user + " -p " + password + " getlatest " + path
            + " -localpath \"" + localpath + "\" -overwritewritable skip",
            bootStrapper.buildBootstrapCommand().toString());
        
        bootStrapper.setForceFileUpdate(true);
        assertEquals("ab -u " + user + " -p " + password + " getlatest " + path
            + " -localpath \"" + localpath + "\" -forcefileupdate" 
            + " -overwritewritable skip",
            bootStrapper.buildBootstrapCommand().toString());
        
        bootStrapper.setOverwriteWritable("replace");
        assertEquals("ab -u " + user + " -p " + password + " getlatest " + path
            + " -localpath \"" + localpath + "\" -forcefileupdate"
            + " -overwritewritable replace",
            bootStrapper.buildBootstrapCommand().toString());
        
    }

    //The following tests all actually use the AlienBrain executable and 
    //may need to access a server.  Therefore they can only be run if you 
    //have a licensed command-line client and access to a server.
/*
    //In order for some of the following tests to pass, these members must
    //be assigned values valid for your AlienBrain server.
    private static final String TESTING_PATH = "alienbrain://Projects/Code/Over.sln";
    private static final String TESTING_BRANCH = "Root Branch/SubBranch";
    // Set any of the following to null if you do not want to 
    // override any NXN_AB_* environment variables you may be using.
    private static final String TESTING_USERNAME = null; //"sjacobs";
    private static final String TESTING_PASSWORD = null; //"pass123";
    private static final String TESTING_SERVER = null; //"abserver";
    private static final String TESTING_DATABASE = null; //"StudioVault";

    public void testBootstrapper() throws java.io.IOException {
        AlienBrainBootstrapper bootStrapper = new AlienBrainBootstrapper();

        bootStrapper.setUser(TESTING_USERNAME);
        bootStrapper.setPassword(TESTING_PASSWORD);
        bootStrapper.setServer(TESTING_SERVER);
        bootStrapper.setDatabase(TESTING_DATABASE);
        bootStrapper.setBranch(TESTING_BRANCH);
        bootStrapper.setView(TESTING_PATH);
        bootStrapper.setForceFileUpdate(true);

        java.io.File tempFile = java.io.File.createTempFile("AlienBrainBootstrapperTest", null);
        tempFile.deleteOnExit();
        bootStrapper.setLocalPath(tempFile.getCanonicalPath());
        bootStrapper.setOverwriteWritable("replace");

        bootStrapper.bootstrap();

        System.out.println(tempFile.getCanonicalPath());
        assertTrue("Can't find " + tempFile.getCanonicalPath(), tempFile.exists());
        assertTrue(tempFile.getCanonicalPath() + " is not a file.", tempFile.isFile());
        assertTrue(tempFile.getCanonicalPath() + " is size 0.", tempFile.length() > 0);
        tempFile.delete();
    }


*/  // End of tests that require an actual AlienBrain installation.
}
