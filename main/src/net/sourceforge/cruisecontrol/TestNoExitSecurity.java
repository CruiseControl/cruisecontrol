package net.sourceforge.cruisecontrol;

/**
 * This class contains a simple main method which tests
 * the implementation of the NoExitSecurityManager.
 * This class should be replaced by a jUnit test case.
 */
public class TestNoExitSecurity {
    public static void main(String[] args) {
        System.setSecurityManager(new NoExitSecurityManager());

        for (int i=0;i<3; i++) {

            try {
                System.exit(1);
                System.out.println("Test failed. An exception was not thrown.");
            } catch (ExitException ee) {
                System.out.println("Test passed. An exception WAS thrown. Stack trace is:");
                ee.printStackTrace();
            }
        }
    }
}
