package net.sourceforge.cruisecontrol;

/**
 *  This security manager will cause an ExitException to be thrown
 *  whenever System.exit is called instead of terminating the VM.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class NoExitSecurityManager extends SecurityManager {
    /**
     *  Throws an ExitException instead of terminating the VM.
     */
    public void checkExit(int status) {
        throw new ExitException(status);
    }

    /**
     *  Allows anything.
     */
    public void checkPermission(java.security.Permission p) {
    }
}
