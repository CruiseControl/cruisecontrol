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
import net.sourceforge.cruisecontrol.util.CompositeConsumer;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;

public class Pipe {
    private final Process process;

    private final StringBuffer errorBuffer;

    private final StringBuffer outputBuffer;

    private final String commandLine;

    private final Logger logger = Logger.getLogger(Pipe.class);

    private boolean complete;

    public Pipe(Commandline command) {
        this.commandLine = command.toString();
        try {
            this.process = command.execute();
        } catch (Exception e) {
            throw new ExecutionException("Couldn't execute command " + commandLine, e);
        }

        errorBuffer = getStringBuffer(process.getErrorStream());
        outputBuffer = getStringBuffer(process.getInputStream());
    }

    private StringBuffer getStringBuffer(InputStream stream) {
        StringBuffer buffer = new StringBuffer();
        StreamConsumer consumer = assembleStreamConsumers(buffer);
        new Thread(new StreamPumper(stream, consumer)).start();
        return buffer;
    }

    private StreamConsumer assembleStreamConsumers(final StringBuffer buffer) {
        StreamConsumer stringConsumer = new StreamConsumer() {
            public void consumeLine(String line) {
                buffer.append(line + '\n');
            }
        };
        StreamConsumer consoleConsumer = new StreamConsumer() {
            public void consumeLine(String line) {
                logger.info(line);
            }
        };
        CompositeConsumer consumer = new CompositeConsumer(stringConsumer);
        consumer.add(consoleConsumer);
        return consumer;
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

    public String error() {
        waitFor();
        return errorBuffer.toString();
    }

    public String output() {
        waitFor();
        return outputBuffer.toString();
    }
}
