/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package net.sourceforge.cruisecontrol.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.log4j.Logger;

/**
 * Class to pump the error stream during Process's runtime. Originally copied from
 * the Ant built-in task, but changed to work with both text and binary data.
 *
 * @since  June 11, 2001
 * @author <a href="mailto:fvancea@maxiq.com">Florin Vancea</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class StreamPumper implements Runnable {

    private final InputStream in;
    private final boolean isBinary;
    private final OutputStream binConsumer;
    private final StreamConsumer txtConsumer;

    private static final int SIZE = 1024;
    private static final Logger LOG = Logger.getLogger(StreamPumper.class);

    /**
     * Constructor. Assigns the stream to read <b>text data</b> from with the consumer being
     * fed with the texts.
     *
     * @param in the stream to read texts from
     * @param consumer the consumer to feed texts into
     */
    public StreamPumper(final InputStream in, final StreamConsumer consumer) {
        this.in = in;
        this.isBinary = false;
        this.txtConsumer = consumer;
        this.binConsumer = null;
    }

    /**
     * Constructor. Assigns the stream to read <b>text or binary data</b> from with the
     * consumers being fed with data. If the data are marked as binary, summary message only
     * is written into {@link StreamConsumer}.
     *
     * @param in the stream to read data from
     * @param isBinary the consumer to feed texts into
     * @param txtConsumer the consumer to feed texts into
     * @param binConsumer the consumer to feed data into
     */
    public StreamPumper(final InputStream in, boolean isBinary, final StreamConsumer txtConsumer,
           final OutputStream binConsumer) {
        this.in = in;
        this.isBinary = isBinary;
        this.txtConsumer = txtConsumer;
        this.binConsumer = binConsumer;
    }

    /**
     * Reads data from the input stream and writes them to the consumers assigned.
     */
    public void run() {
        int bytesread = 0;

        try {
            // No stream, do not read (close outputs in finally {})
            if (this.in == null) {
                return;
            }

            // If only text stream consumer is set, pass the input stream directly through
            // Reader which will convert it to the strings ...
            if (this.binConsumer == null && !this.isBinary) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(this.in));
                String s;

                while ((s = reader.readLine()) != null) {
                    consumeLine(s, this.txtConsumer);
                }

            // Well, we have binary reader defined as well ...
            // So, chunks of binary data must be read from the input stream and directly passed to the
            // binary output. Moreover, they must be passed to the text output (for non-binary only data)
            // which is done through pipe: byte[] -> PipeOutputStream -> PipeInputStream -> BufferedReader
            } else {

                // For non-binary data create the binary->text conversion pipe
                // Use the StreamPumper class
                final OutputStream tostrOutput;
                final Thread tostrReader;
                if (!this.isBinary) {
                    tostrOutput = new PipedOutputStream();
                    tostrReader = new Thread(new StreamPumper(new PipedInputStream((PipedOutputStream) tostrOutput),
                            this.txtConsumer));
                    tostrReader.start();
                } else {
                    tostrOutput = null;
                    tostrReader = new Thread();
                }

                // Read binary data
                final byte[] binBuff = new byte[SIZE];
                int numread;

                try {
                    while ((numread = this.in.read(binBuff)) >= 0) {
                        bytesread += numread;
                        // Pass them to the binary consumer and to binary->text conversion pipe
                        consumeBytes(binBuff, numread, this.binConsumer);
                        consumeBytes(binBuff, numread, tostrOutput);
                    }

                } finally {
                    // Must close, otherwise the binary->text conversion thread will never end
                    IO.close(tostrOutput);
                }
                tostrReader.join();

                // Print summary in binary mode
                if (this.isBinary) {
                    consumeLine("Read " + bytesread + " Bytes", this.txtConsumer);
                }
            }
        } catch (Exception e) {
            LOG.error("Problem consuming input stream [" + this.in + "], " + bytesread + " Bytes passed", e);
        } finally {
            IO.close(this.in);
            IO.close(this.binConsumer);
        }
    }

    private void consumeLine(String line, final StreamConsumer consumer) {
        if (consumer != null) {
            try {
                // Remove VT100 terminal escape sequences
                line = line.replaceAll("\\e(\\[[^a-zA-Z]*[a-zA-Z]|[^\\[])", "");
                // Remove other control characters apart from tab, newline and carriage return
                line = line.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");
                consumer.consumeLine(line);

            } catch (RuntimeException e) {
                LOG.error("Problem consuming line [" + line + "]", e);
            }
        }
    }

    private void consumeBytes(final byte[] bytes, int len, final OutputStream consumer) throws IOException {
        if (consumer != null) {
            consumer.write(bytes, 0, len);
        }
    }
}
