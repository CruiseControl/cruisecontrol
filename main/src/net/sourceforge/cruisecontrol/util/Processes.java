package net.sourceforge.cruisecontrol.util;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility methods for interacting with Java processes.
 *
 * @see Process
 */
public final class Processes {
    private Processes() {
        //utility methods only.
    }

    public static void executeFully(Commandline c) throws IOException, InterruptedException {
        Process p = execute(c);
        p.waitFor();
        IO.close(p);
    }

    public static Process execute(Commandline c) throws IOException {
        Process p = Runtime.getRuntime().exec(c.getCommandline());
        StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
        new Thread(errorPumper).start();
        return p;
    }
}
