package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;

public class NoExitSecurityManagerTest extends TestCase {

    public NoExitSecurityManagerTest(String name) {
        super(name);
    }

    public void testNoExitSecurityManager() {
        SecurityManager originalManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());

        try {
            System.exit(1);
            assertTrue(false);
        } catch(ExitException e) {
            assertTrue(true);
        }

        System.setSecurityManager(originalManager);
    }
}