/**
 * 
 */
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Tests for Team Foundation Server BootStrapper
 * 
 * @author <a href="http://www.woodwardweb.com">Martin Woodward</a>
 */
public class TFSBootstrapperTest extends TestCase {

    private TFSBootstrapper bs;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // Create a TFSBootstrapper with minimium attributes set.
        bs = new TFSBootstrapper();
        bs.setItemSpec(".");
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#validate()}.
     */
    public void testValidateNoAttributesSet() {
        TFSBootstrapper bootStrapper = new TFSBootstrapper();
        try {
            bootStrapper.validate();
            fail("TFSBootstrapper should throw an exception when no attributes are set.");
        } catch (CruiseControlException e) {
        }
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#validate()}.
     */
    public void testValidateAllMandatoryAttributesSet() {
        try {
            bs.validate();
        } catch (CruiseControlException e) {
            fail("TFSBootstrapper should not throw an exception when all mandatory attributes are set.");
        }
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#buildGetCommand()}.
     */
    public void testBuildGetCommandMinimiumOptions()
            throws CruiseControlException {
        String actual = bs.buildGetCommand().toString();
        assertEquals("tf get -noprompt .", actual);
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#buildGetCommand()}.
     */
    public void testBuildGetCommandRecursive() throws CruiseControlException {
        bs.setRecursive(true);

        String actual = bs.buildGetCommand().toString();
        assertEquals("tf get -noprompt . -recursive", actual);
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#buildGetCommand()}.
     */
    public void testBuildGetCommandForce() throws CruiseControlException {
        bs.setForce(true);

        String actual = bs.buildGetCommand().toString();
        assertEquals("tf get -noprompt . -force", actual);
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#buildGetCommand()}.
     */
    public void testBuildGetCommandWithLogin() throws CruiseControlException {
        bs.setUsername("username@DOMAIN");
        bs.setPassword("password");
        String actual = bs.buildGetCommand().toString();
        assertEquals("tf get -noprompt . -login:username@DOMAIN,password",
                actual);

        bs = new TFSBootstrapper();
        bs.setItemSpec(".");
        bs.setUsername("username@DOMAIN");

        actual = bs.buildGetCommand().toString();
        assertEquals(
                "No login option should be generated if password not supplied",
                "tf get -noprompt .", actual);

        bs = new TFSBootstrapper();
        bs.setItemSpec(".");
        bs.setPassword("password");

        actual = bs.buildGetCommand().toString();
        assertEquals(
                "No login option should be generated if username not supplied",
                "tf get -noprompt .", actual);

    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#buildGetCommand()}.
     */
    public void testBuildGetCommandOptions() throws CruiseControlException {
        bs.setOptions("-proxy:http://tfsproxy:8081");

        String actual = bs.buildGetCommand().toString();
        assertEquals("tf get -noprompt . -proxy:http://tfsproxy:8081", actual);
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.bootstrappers.TFSBootStrapper#buildGetCommand()}.
     */
    public void testBuildGetCommandTfPath() throws CruiseControlException {
        bs.setTfPath("some.strange.tf.path");

        String actual = bs.buildGetCommand().toString();
        assertEquals("some.strange.tf.path get -noprompt .", actual);
    }

}
