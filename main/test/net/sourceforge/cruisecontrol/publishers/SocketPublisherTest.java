package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketPublisherTest extends TestCase {

    private Listener listener;
    private Thread listenerThread;

    protected void setUp() throws Exception {
        super.setUp();
        listener = new Listener();
        listenerThread = new Thread(listener);
        listenerThread.start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        listenerThread.interrupt();
        listener.kill();
    }

    public void testSocketPublisherSendsSuccess() throws Exception {


        SocketPublisher socketPublisher = new SocketPublisher();
        socketPublisher.setSocketServer("localhost");
        socketPublisher.setPort("1555");
        socketPublisher.writeToSocket("Success");

        listenerThread.join();
        assertEquals("Success", listener.received);
    }

    public static class Listener implements Runnable {

        public String received;
        private ServerSocket socket;
        private Socket conn;
        private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

        public void run() {
            try {
                socket = new ServerSocket(1555);
                conn = socket.accept();
                received = readToString(conn.getInputStream());
                conn.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String readToString(InputStream input) throws IOException {
            StringWriter writer = new StringWriter();
            InputStreamReader reader = new InputStreamReader(input);
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            int n = 0;
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }

            return writer.toString();
        }

        public void kill() {
            try {
                socket.close();
                conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
