package net.sourceforge.cruisecontrol;

/**
 *  Thrown by the NoExitSecurityManager whenever System.exit() is called.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class ExitException extends RuntimeException {
    private int status;

    public ExitException(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
