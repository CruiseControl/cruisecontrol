package net.sourceforge.cruisecontrol.dashboard.service;

import junit.framework.TestCase;

public class SystemServiceTest extends TestCase {

    public void testShouldReturnFalseWhenPathIsNull() {
        assertFalse(new SystemService().isAbsolutePath(null));
    }

    public void testShouldRetrieveJvmVersion() {
        assertNotNull(new SystemService().getJvmVersion());
    }

    public void testShouldRetrieveOsInfo() {
        assertNotNull(new SystemService().getOsInfo());
    }
}
