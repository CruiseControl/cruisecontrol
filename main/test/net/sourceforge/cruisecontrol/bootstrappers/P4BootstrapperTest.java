package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Should also test bootstrap() but to do that we need to mock calling the command line
 * @author <a href="mailto:mroberts@thoughtworks.com">Mike Roberts</a>
 * @author <a href="mailto:cstevenson@thoughtworks.com">Chris Stevenson</a>
 */
public class P4BootstrapperTest extends TestCase {
    private P4Bootstrapper p4Bootstrapper;

    public P4BootstrapperTest(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        p4Bootstrapper = new P4Bootstrapper();
    }

    public void testPathNotSet() {
        try {
            p4Bootstrapper.validate();
            fail("Should be Exception if path is not set.");
        } catch (CruiseControlException e) {
            //expected
        }
    }

    public void testInvalidPath() {
        p4Bootstrapper.setPath("");
        try {
            p4Bootstrapper.validate();
            fail("Empty path not allowed");
        } catch (CruiseControlException e) {
            //expected
        }
    }

    public void testInvalidPort() {
        p4Bootstrapper.setPath("foo");
        p4Bootstrapper.setP4Port("");
        try {
            p4Bootstrapper.validate();
            fail("Empty port not allowed");
        } catch (CruiseControlException e) {
            //expected
        }
    }

    public void testInvalidClient() {
        p4Bootstrapper.setPath("foo");
        p4Bootstrapper.setP4Client("");
        try {
            p4Bootstrapper.validate();
            fail("Empty client not allowed");
        } catch (CruiseControlException e) {
            //expected
        }
    }

    public void testInvalidUser() {
        p4Bootstrapper.setPath("foo");
        p4Bootstrapper.setP4User("");
        try {
            p4Bootstrapper.validate();
            fail("Empty user not allowed");
        } catch (CruiseControlException e) {
            //expected
        }
    }

    public void testCreateCommandlineWithPathSet () throws CruiseControlException {
        p4Bootstrapper.setPath("foo");
        assertEquals("p4 -s sync foo", p4Bootstrapper.createCommandline());
    }

    public void testCreateCommandlineWithP4PortSet() throws CruiseControlException {
        p4Bootstrapper.setPath("foo");
        p4Bootstrapper.setP4Port("testhost:1666");
        checkEnvironmentSpecification(" -p testhost:1666 ");
    }

    public void testCreateCommandlineWithP4ClientSet() throws CruiseControlException {
        p4Bootstrapper.setPath("foo");
        p4Bootstrapper.setP4Client("testclient");
        checkEnvironmentSpecification(" -c testclient ");
    }

    public void testCreateCommandlineWithP4UserSet() throws CruiseControlException {
        p4Bootstrapper.setPath("foo");
        p4Bootstrapper.setP4User("testuser");
        checkEnvironmentSpecification(" -u testuser ");
    }

    /**
     * Checks that a P4 environment command line option is created correctly in a P4 command line specification
     */
    private void checkEnvironmentSpecification(String expectedSetting) throws CruiseControlException {
        String commandline = p4Bootstrapper.createCommandline();
        int specicationPosition = commandline.indexOf(expectedSetting);
        int syncPosition = commandline.indexOf(" sync ");
        assertTrue(specicationPosition != -1);
        assertTrue(syncPosition != -1);
        assertTrue(specicationPosition < syncPosition);
    }
}