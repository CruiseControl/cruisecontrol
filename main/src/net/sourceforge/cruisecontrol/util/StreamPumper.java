package net.sourceforge.cruisecontrol.util;

import java.io.IOException;
import java.io.InputStream;

/**
 *  Inner class to pump the error stream during Process's runtime. Copied from
 *  the Ant built-in task.
 *
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @created  June 11, 2001
 */
public class StreamPumper implements Runnable {

    private InputStream _in;
    private final static int SIZE = 128;
    private final static int SLEEP = 5;

    public StreamPumper(InputStream in) {
        _in = in;
    }

    public void run() {
        final byte[] buf = new byte[SIZE];
        int length;

        try {
            while ((length = _in.read(buf)) > 0) {
                System.err.write(buf, 0, length);
                try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                }
            }
        } catch (IOException e) {
        }
    }
}
