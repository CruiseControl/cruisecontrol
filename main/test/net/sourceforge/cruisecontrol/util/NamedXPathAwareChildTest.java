package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class NamedXPathAwareChildTest extends TestCase {
    public void testNameRequiredAttribute() throws CruiseControlException {
        NamedXPathAwareChild field = new NamedXPathAwareChild();
        try {
            field.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        field.setValue("foo");
        try {
            field.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        field.setName("bar");
        field.validate();
    }
}
