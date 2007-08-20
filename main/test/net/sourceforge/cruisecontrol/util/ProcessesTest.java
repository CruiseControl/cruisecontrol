/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProcessesTest extends TestCase {

    public void testShouldReturnProcessWhenCommandProvided() throws IOException {
        Processes.setRuntime(new MockExecutor());
        Commandline c = new Commandline() {
        };
        assertNotNull(Processes.execute(c));
    }

    public void testShouldStartStreamPumperForErrorStream() throws Exception {
        Processes.setRuntime(new MockExecutor());
        Commandline c = new Commandline();
        c.setExecutable("UnitTestDummyExcectuable");

        // try to ensure pending thread cleanups occur, so thread counts include only threads
        // created by this unit test
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        System.gc();
        System.gc();
        Thread.sleep(500);

        int preCount = Thread.activeCount();
        assertNotNull(Processes.execute(c));

        // allow some time for thread to spin up. can be longer in java 5
        int postCount = Thread.activeCount();
        int waitCount = 0;
        while ((postCount <= preCount) && (waitCount < 40)) {
            waitCount++;
            Thread.sleep(10);
            postCount = Thread.activeCount();
        }
        final String msg = "A StreamPumper Thread wasn't started. postCount: " + postCount
                + "; preCount: " + preCount + "; waitCount: " + waitCount
                + (postCount < preCount
                        ? "\n\tWARNING: Thread counts might include threads from prior tests."
                        : "");
        assertTrue(msg, postCount > preCount);
    }

    public void testShouldCloseStreamsWhenExecutingFully() throws IOException, InterruptedException {
        MockExecutor executor = new MockExecutor();
        Processes.setRuntime(executor);
        Commandline c = new Commandline() {
        };
        Processes.executeFully(c);
        assertTrue(executor.streamsClosed());
    }

    private static class CloseableProcess extends MockProcess {
        private CloseAwareInputStream error = new CloseAwareInputStream(4 * 1000);
        private CloseAwareInputStream input = new CloseAwareInputStream(4 * 1000);
        private CloseAwareOutputStream output = new CloseAwareOutputStream();

        public CloseableProcess() {
            super();
            setErrorStream(error);
            setInputStream(input);
            setOutputStream(output);
        }

        public boolean streamsClosed() {
            return error.isClosed() && input.isClosed() && output.isClosed();
        }
    }

    private static final class CloseAwareInputStream extends InputStream {
        private final int millisTillEndOfStream;
        private long starttime;
        private boolean closed;

        private CloseAwareInputStream(final int millisTillEndOfStream) {
            this.millisTillEndOfStream = millisTillEndOfStream;
        }

        public void close() throws IOException {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        public int read() throws IOException {
            if (starttime == 0) {
                starttime = System.currentTimeMillis();
            }

            if ((System.currentTimeMillis() - starttime) < millisTillEndOfStream) {
                Thread.yield();
                //return 0;
                // return a character value that allows Readers to read a new line,
                // otherwise they buffer all reads until the final -1.
                return '\n';
            }
            return -1;
        }
    }

    static class CloseAwareOutputStream extends OutputStream {

        private boolean closed;

        public void close() throws IOException {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        public void write(int i) throws IOException {
        }
    }

    private static class MockExecutor implements Executor {
        private CloseableProcess mockProcess;

        public Process exec(Commandline c) throws IOException {
            mockProcess = new CloseableProcess();
            return mockProcess;
        }

        public boolean streamsClosed() {
            return mockProcess.streamsClosed();
        }
    }
}
