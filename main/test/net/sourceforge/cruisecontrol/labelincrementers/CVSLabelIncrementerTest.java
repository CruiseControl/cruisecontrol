package net.sourceforge.cruisecontrol.labelincrementers;

import net.sourceforge.cruisecontrol.LabelIncrementer;
import junit.framework.TestCase;

public class CVSLabelIncrementerTest extends TestCase {

    private LabelIncrementer _incrementer;

    public CVSLabelIncrementerTest(String name) {
        super(name);
    }

    public void setUp() {
        _incrementer = new CVSLabelIncrementer();
    }

    public void testIsValidLabel() {
        assertEquals(_incrementer.isValidLabel("x-88"), true);
        assertEquals(_incrementer.isValidLabel("x-y"), false);
        assertEquals(_incrementer.isValidLabel("x88"), false);
    }

    public void testIncrementLabel() {
        assertEquals(_incrementer.incrementLabel("x-88", null), "x-89");
    }
}