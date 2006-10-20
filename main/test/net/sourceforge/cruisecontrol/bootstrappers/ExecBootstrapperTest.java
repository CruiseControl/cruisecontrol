package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.builders.ExecBuilderTest;
import net.sourceforge.cruisecontrol.CruiseControlException;

/** @author Dan Rollo */
public class ExecBootstrapperTest extends ExecBuilderTest {

    public ExecBootstrapperTest(String name) {
        super(name);
    }

    public void testBootstrapperValidate() throws Exception {
        ExecBootstrapper ebt = new ExecBootstrapper();

        // test missing "command" attribute
        try {
            ebt.validate();
            fail("ExecBuilder should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set",
                    "'command' is required for ExecBootstrapper", e.getMessage());
        }

        // test no error with all required attributes
        ebt.setCommand("dir");
        try {
            ebt.validate();
        } catch (CruiseControlException e) {
            fail("ExecBuilder should not throw an exception when the required attributes are set.");
        }
    }
}
