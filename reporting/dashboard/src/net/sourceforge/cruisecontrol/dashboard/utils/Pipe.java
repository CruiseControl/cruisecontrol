/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.utils;

import java.io.InputStream;
import net.sourceforge.cruisecontrol.dashboard.exception.ExecutionException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;

public class Pipe {
    private final Process process;
    private final StringBuffer errorBuffer;
    private final StringBuffer outputBuffer;
    private final String commandLine;
    private boolean complete;

    public Pipe(Commandline command) {
        this.commandLine = command.toString();
        try {
            this.process = command.execute();
        } catch (Exception e) {
            throw new ExecutionException("Couldn't execute command " + commandLine, e);
        }

        InputStream error = process.getErrorStream();
        //i.e. the process' output. What a stupid method name.
        InputStream output = process.getInputStream();
        errorBuffer = getStringBuffer(error);
        outputBuffer = getStringBuffer(output);
    }

    private StringBuffer getStringBuffer(InputStream stream) {
        final StringBuffer buffer = new StringBuffer();

        StreamConsumer consumer = new StreamConsumer() {
            public void consumeLine(String line) {
                buffer.append(line + '\n');
            }
        };

        StreamPumper pumper = new StreamPumper(stream, consumer);
        pumper.run();
        return buffer;
    }

    public void waitFor() {
        if (complete) {
            return;
        }
        try {
            process.waitFor();
            IO.close(process);
            complete = true;
        } catch (InterruptedException ie) {
            throw new RuntimeException("Got interrupted exception while running: " + commandLine);
        }
    }

    public boolean isComplete() {
        if (complete) {
            return true;
        }
        try {
            process.exitValue();
            IO.close(process);
            complete = true;
            return true;
        } catch (IllegalThreadStateException itse) {
            return false;
        }
    }

    public StringBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public String error() {
        return errorBuffer.toString();
    }

    public String output() {
        return outputBuffer.toString();
    }
}
