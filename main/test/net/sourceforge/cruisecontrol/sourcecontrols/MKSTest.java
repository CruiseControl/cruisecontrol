package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class MKSTest extends TestCase {

    public MKSTest(String name) {
        super(name);
    }

    public void testValidate() {

        MKS mks = new MKS();

        try {
            mks.validate();
            fail("MKS should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        mks.setMksroot("mksroot");

        try {
            mks.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("MKS should not throw exceptions when required attributes are set.");
        }
    }
}
