package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class CVSBootstrapperTest extends TestCase {
    public CVSBootstrapperTest(String name) {
        super(name);
    }

    public void testValidate() {
        CVSBootstrapper cbs = new CVSBootstrapper();

        try {
            cbs.validate();
            fail("CVSBootstrapper should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set", "'file' is required for CVSBootstrapper", e.getMessage());
        }
        cbs.setFile("somefile");
        try {
            cbs.validate();
        } catch (CruiseControlException e) {
            fail("CVSBootstrapper should not throw an exception when the required attributes are set.");
        }
    }

    public void testBuildUpdateCommand() {
        CVSBootstrapper cbs = new CVSBootstrapper();
        cbs.setFile("somefile");
        assertEquals("Update command was not created correctly.", "cvs update somefile", cbs.buildUpdateCommand().toString());

        cbs.setCvsroot("somecvsroot");
        assertEquals("Update command was not created correctly.", "cvs -d somecvsroot update somefile", cbs.buildUpdateCommand().toString());
    }
}
