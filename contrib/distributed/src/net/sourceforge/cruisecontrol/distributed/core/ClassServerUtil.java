package net.sourceforge.cruisecontrol.distributed.core;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.text.MessageFormat;

/**
 * Adapted from com.sun.jini.tool.ClassServer
 */
final class ClassServerUtil {

    private static final Logger LOG = Logger.getLogger(ClassServerUtil.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private ClassServerUtil() {
    }

    // Read up to CRLF, return false if EOF
    private static boolean readLine(final InputStream in, final StringBuffer buf)
            throws IOException {
        while (true) {
            int c = in.read();
            if (c < 0) {
                return buf.length() > 0;
            }
            if (c == '\r') {
                in.mark(1);
                c = in.read();
                if (c != '\n') {
                    in.reset();
                }
                return true;
            }
            if (c == '\n') {
                return true;
            }
            buf.append((char) c);
        }
    }

    // Read the request/response and return the initial line.
    private static String getInput(final Socket sock, final boolean isRequest)
            throws IOException {
        final BufferedInputStream in = new BufferedInputStream(sock.getInputStream(), 256);
        final StringBuffer buf = new StringBuffer(80);
        do {
            if (!readLine(in, buf)) {
                return null;
            }
        } while (isRequest && buf.length() == 0);
        final String initial = buf.toString();
        do {
            buf.setLength(0);
        } while (readLine(in, buf) && buf.length() > 0);
        return initial;
    }

    private static void print(final String key, final String val) {
//        String fmt = getString(key);
//        if (fmt == null)
//            fmt = "no text found: \"" + key + "\" {0}";
        final String fmt = "no text found: \"" + key + "\" {0}";
        //System.out.println(MessageFormat.format(fmt, new String[]{val}));
        LOG.warn(MessageFormat.format(fmt, (Object) new String[]{val}));
    }

    private static void print(final String key, final String[] vals) {
//        String fmt = getString(key);
//        if (fmt == null)
//            fmt = "no text found: \"" + key + "\" {0} {1} {2}";
        final String fmt = "no text found: \"" + key + "\" {0} {1} {2}";
//        System.out.println(MessageFormat.format(fmt, vals));
        LOG.warn(MessageFormat.format(fmt, (Object) vals));
    }

    /**
     * Shutsdown the Jini ClassServer on the given host and port ONLY if host is the local host.
     * @param host hostname of the ClassServer to stop
     * @param port port of the ClassServer to sop
     */
    static void shutdownClassServer(final String host, final int port) {
        try {
            final Socket sock = new Socket(host, port);
            try {
                final DataOutputStream out = new DataOutputStream(sock.getOutputStream());
                out.writeBytes("SHUTDOWN *\r\n\r\n");
                out.flush();
                String status = getInput(sock, false);
                if (status != null && status.startsWith("HTTP/")) {
                    status = status.substring(status.indexOf(' ') + 1);
                    if (status.startsWith("403 ")) {
                        print("classserver.forbidden", status);
                    } else if (!status.startsWith("200 ")
                            && status.indexOf(' ') == 3) {
                        print("classserver.status",
                                new String[]{status.substring(0, 3),
                                        status.substring(4)});
                    }
                }
            } finally {
                try {
                    sock.close();
                } catch (IOException e) {
                    // ignore any exception
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to shutdown ClassServer at: " + host + ":" + port, e);
        }
    }
    // End - dreived from com.sun.jini.tool.ClassServer

}
