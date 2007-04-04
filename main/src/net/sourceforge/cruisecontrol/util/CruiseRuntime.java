package net.sourceforge.cruisecontrol.util;

import java.io.File;
import java.io.IOException;

public class CruiseRuntime {
    private final Runtime delegate;

    public CruiseRuntime() {
        delegate = Runtime.getRuntime();
    }

    public Process exec(String[] commandline) throws IOException {
        return delegate.exec(commandline);
    }

    public Process exec(String commandline) throws IOException {
        return delegate.exec(commandline);
    }

    public Process exec(String[] commandline, String[] o, File workingDir) throws IOException {
        return delegate.exec(commandline, o, workingDir);
    }

    public Process exec(String commandline, String[] o, File workingDir) throws IOException {
        return delegate.exec(commandline, o, workingDir);
    }
}
