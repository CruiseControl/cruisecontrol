package net.sourceforge.cruisecontrol.labelincrementers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.LabelIncrementer;

public class DefaultLabelIncrementerTest extends TestCase {

    private LabelIncrementer _incrementer;

    public DefaultLabelIncrementerTest(String name) {
        super(name);
    }

    public void setUp() {
        _incrementer = new DefaultLabelIncrementer();
    }

    public void testIsValidLabel() {
        assertEquals(_incrementer.isValidLabel("x.88"), true);
        assertEquals(_incrementer.isValidLabel("x.y"), false);
        assertEquals(_incrementer.isValidLabel("x88"), false);
    }

    public void testIncrementLabel() {
        assertEquals(_incrementer.incrementLabel("x.88", null), "x.89");
    }
}