package net.sourceforge.cruisecontrol.taglib;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

public class AllTests extends TestCase {

    public AllTests(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(NavigationTagTest.class);
        suite.addTestSuite(XSLTagTest.class);
        return suite;
    }
}
