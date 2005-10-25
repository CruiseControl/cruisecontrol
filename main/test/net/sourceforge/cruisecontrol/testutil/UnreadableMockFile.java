package net.sourceforge.cruisecontrol.testutil;

import java.io.File;
import java.net.URI;

public class UnreadableMockFile extends File {
    private boolean canReadWasCalled = false;

    public UnreadableMockFile(String pathname) {
        super(pathname);
    }

    public UnreadableMockFile(String parent, String child) {
        super(parent, child);
    }

    public UnreadableMockFile(File parent, String child) {
        super(parent, child);
    }

    public UnreadableMockFile(URI uri) {
        super(uri);
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
