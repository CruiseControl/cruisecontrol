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

import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Utility methods for interacting with Java processes.
 *
 * @see Process
 */
public final class Processes {
    private static final Logger LOG = Logger.getLogger(Processes.class);
    private static Executor runtime = new RuntimeExecutor();

    private Processes() {
        //utility methods only.
    }

    public static void executeFully(Commandline c) throws IOException, InterruptedException {
        Process p = execute(c);
        p.waitFor();
        IO.close(p);
    }

    public static Process execute(Commandline c) throws IOException {
        Process p = runtime.exec(c);
        StreamPumper errorPumper = StreamLogger.getWarnPumper(LOG, p);
        new Thread(errorPumper).start();
        return p;
    }

    /**
     * Waits for a process to finish executing and logs the output.
     *
     * @param proc the process.
     * @param log where to log both standard and error output.
     * @return the process' exit value
     */
    public static int waitFor(Process proc, Logger log) throws IOException, InterruptedException {
        return waitFor(proc, StreamLogger.getInfoLogger(log), StreamLogger.getWarnLogger(log));
    }

    /**
     * Waits for a process to finish executing.
     * @param proc the process.
     * @param output consumes the process' standard output.
     * @param error consumes the process' error output.
     * @return the process' exit value
     */
    public static int waitFor(Process proc, StreamConsumer output, StreamConsumer error)
            throws IOException, InterruptedException {
        proc.getOutputStream().close();

        Thread stderr = new Thread(new StreamPumper(proc.getErrorStream(), error));
        stderr.start();

        new StreamPumper(proc.getInputStream(), output).run();

        int exitValue = proc.waitFor();
        stderr.join();
        return exitValue;
    }

    static void setRuntime(Executor e) {
        runtime = e;
    }
}
