package net.sourceforge.cruisecontrol.testutil;

import java.io.File;

public class UnreadableMockFile extends File {
    private boolean canReadWasCalled = false;

    public UnreadableMockFile() {
        super("DOESNTMATTER");
    }

    public boolean canRead() {
        canReadWasCalled = true;
        return false;
    }

    public boolean exists() {
        return true;
    }

    public boolean canReadWasCalled() {
        return canReadWasCalled;
    }
}
