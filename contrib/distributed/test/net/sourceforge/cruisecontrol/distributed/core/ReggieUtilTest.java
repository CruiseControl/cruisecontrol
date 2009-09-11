package net.sourceforge.cruisecontrol.distributed.core;

import junit.framework.TestCase;

import java.security.Permission;
import java.util.List;
import java.util.Arrays;


/**
 * @author Dan Rollo
 * Date: Sep 10, 2009
 * Time: 10:35:13 PM
 */
public class ReggieUtilTest extends TestCase {

    //private static final Logger LOG = Logger.getLogger(ReggieUtilTest.class);

    public void testSetupRMISecurityManager() {
        final SecurityManager origSecurityManager = System.getSecurityManager();
        if (origSecurityManager != null) {
            // skip this test, as we're likely running under webstart and have a real SecurityManager installed.
            return;
        }

        try {

            final class MySecurityManager extends SecurityManager {
                private final List<String> expectedPermsToAllow = Arrays.asList(
                        "setSecurityManager",
                        "getProperty.networkaddress.cache.ttl",
                        "sun.net.inetaddr.ttl",
                        "getProperty.networkaddress.cache.negative.ttl");
                @Override
                public void checkPermission(Permission permission) {
                    if (expectedPermsToAllow.contains(permission.getName())) {
                        return;
                    }

                    if (!permission.getActions().contains("read")) {
                        final String msg = "testSetupRMISecurityManager() checked new permission : " + permission.getName();
                        System.out.println(msg);
                        // use of LOG here gets dicey with sec manager games...
                        //LOG.warn(msg);
                    }
                }
            }

            // setup "non-RMI" security manager
            System.setSecurityManager(new MySecurityManager());

            // should allow (and just log) unexpected existing Security Manager
            ReggieUtil.setupRMISecurityManager();

        } finally {
                System.setSecurityManager(origSecurityManager);
        }
    }
}
