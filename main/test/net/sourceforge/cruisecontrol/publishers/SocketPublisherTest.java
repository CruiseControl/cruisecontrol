package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class SocketPublisherTest extends TestCase {

    public void testSocketPublisherSendsSuccess() throws Exception {
        final MockSocket mockSocket = new MockSocket();
        SocketFactory factory = new SocketFactory() {
            public Socket createSocket(String server, int port) {
                return mockSocket;
            }
        };

        SocketPublisher socketPublisher = new SocketPublisher(factory);
        socketPublisher.setSocketServer("localhost");
        socketPublisher.setPort("1555");
        socketPublisher.writeToSocket("Success");

        assertEquals("Success", mockSocket.toString());
    }

    private static class MockSocket extends Socket {

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public OutputStream getOutputStream() throws IOException {
            return out;
        }

        public synchronized void close() throws IOException {
        }

        public String toString() {
            return new String(out.toByteArray());
        }
    }



}
