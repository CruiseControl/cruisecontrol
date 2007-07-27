package net.sourceforge.cruisecontrol.dashboard.service;

import junit.framework.TestCase;

public class SystemServiceTest extends TestCase {
    public void testShouldReturnFalseWhenPathIsNull() {
        assertFalse(new SystemService().isAbsolutePath(null));
    }
}
