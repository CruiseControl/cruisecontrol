/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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

    public void testShouldStartStreamPumperForErrorStream() throws IOException {
        Processes.setRuntime(new MockExecutor());
        Commandline c = new Commandline() {
        };
        int preCount = Thread.activeCount();
        assertNotNull(Processes.execute(c));
        assertTrue("A StreamPumper Thread wasn't started", Thread.activeCount() > preCount);
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
        private CloseAwareInputStream error = new CloseAwareInputStream();
        private CloseAwareInputStream input = new CloseAwareInputStream();
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

    private static class CloseAwareInputStream extends InputStream {
        private boolean closed;

        public void close() throws IOException {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        public int read() throws IOException {
            return 0;
        }
    }

    private static class CloseAwareOutputStream extends OutputStream {

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
