package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class PVCSTest extends TestCase {

    public PVCSTest(String name) {
        super(name);
    }

    public void testValidate() {

        PVCS pvcs = new PVCS();

        try {
            pvcs.validate();
            fail("PVCS should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        pvcs.setPvcsproject("project");
        pvcs.setPvcssubproject("subproject");


        try {
            pvcs.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("PVCS should not throw exceptions when required attributes are set.");
        }
    }
}
