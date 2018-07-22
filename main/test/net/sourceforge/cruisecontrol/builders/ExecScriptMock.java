/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
 *       products derive from this software without specific prior
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

package net.sourceforge.cruisecontrol.builders;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;

/**
 * Class implementing mock object behaving as an external process invoked by {@link ExecBuilder}.
 * Although the mock provides only few base "commands", they behave similarly as the command-line
 * tools of that names (if they would be invoked by {@link ExecBuilder}.
 *
 * @see ExecScriptMock.Cat
 * @see ExecScriptMock.Sort
 * @see ExecScriptMock.Shuf
 * @see ExecScriptMock.Grep
 * @see ExecScriptMock.Add
 * @see ExecScriptMock.Del
 * @see ExecScriptMock.Uniq
 */
public abstract class ExecScriptMock extends PipedScriptBase {

    /**
     * Default constructor. It reads data from input stream and passes data to the output stream.
     */
    protected ExecScriptMock() {
        this(new String[] {"-"}, "-");
    }
    /**
     * Constructor. Reads data from the given set of files (in the order they were set) and passes
     * it to the output file.
     *
     * @param inputs the array of input files. Use "-" to read data from input stream set by
     *  {@link #setInputProvider(InputStream)} (can only be set once)
     * @param output the output file. Use "-" to pass data to the output stream set by #set???
     */
    protected ExecScriptMock(String[] inputs, String output) {
        inpFiles = inputs == null ? new String[] {"-"} : inputs;
        outFile = output == null ? "-" : output;
    }

    @Override
    protected Element build() throws CruiseControlException {

        final ByteArrayOutputStream buff = new ByteArrayOutputStream(1024);
        final PrintStream err = new PrintStream(buff);
        OutputStream out = null;
        InputStream inp = null;

        try {
            /* Prepare output stream */
            if ("-".equals(outFile)) {
                out = getOutputBuffer();
            }
            else {
                assertNotNull("Output file cannot be NULL", outFile);
                out = new BufferedOutputStream(new FileOutputStream(outFile));
            }

            /* Read all the inputs */
            for (String s : inpFiles) {
                /* Prepare input stream */
                if ("-".equals(s)) {
                    inp = getInputProvider();
                    assertNotNull("Input stream required but not set", inp);
                }
                else if ("ZERO".equals(s)) {
                    inp = new InputStream() {
                        /* Returns '\n' */
                        @Override
                        public int read() {
                            return '\n';
                        }
                    };
                }
                else {
                    assertNotNull("Input file cannot be NULL", s);
                    inp = new BufferedInputStream(new FileInputStream(s));
                }

                try {
                    final InputStream i = inp;
                    final OutputStream o = out;
                    final Thread build = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            build(i, o, err);
                        }
                    });
                    // Run in independent thread, like an independent process
                    build.start();
                    build.join();

                } catch (InterruptedException e) {
                    throw new CruiseControlException(e);
                }

                inp.close();
            }

        } catch (FileNotFoundException e) {
            throw new CruiseControlException(e);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            /* Close all streams */
            try {
                if (err != null) {
                    err.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                throw new CruiseControlException("Failed to close output stream", e);
            }
        }

        /* Write the ERR stream to the XML element text */
        final Element buildInfo = new Element("execmock");
        buildInfo.setText(new String(buff.toByteArray()));

        return buildInfo;
    }

    @Override
    public void setWorkingDir(String workingDir) {
        // Just ignored
    }

    @Override
    public String getWorkingDir() {
        return "./";
    }

    @Override
    public void setTimeout(long time) {
        timeout = time;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    protected Logger log() {
        return LOG;
    }

    /**
     * Method to carry out the particular build action. It is supposed to read data from the
     * input stream, modify them optionally, and write them to the output stream. The errors
     * and/or messages should be written to the error stream.
     *
     * @param inp the stream to read data from
     * @param out the stream to write data to
     * @param err the stream to print error/status messages to
     */
    protected abstract void build(final InputStream inp, final OutputStream out, final PrintStream err);

    /**
     * Parses the array of file names and returns all the input files, i.e. these starting with "<"
     *
     * @param files the space-separated list of file names
     * @return the names of input file (without the leading "<")
     */
    private static String[] inpFiles(final String files) {
        ArrayList<String> f = new ArrayList<String>(5);

        /* Get these starting with "<" */
        for (String s : files.split("[ ]+")) {
            if (s.startsWith("<")) {
                f.add(s.substring(1));
            }
        }
        /* None was set, add the input stream */
        if (f.size() == 0) {
            f.add("-");
        }

        /* Get it as list */
        return f.toArray(new String[f.size()]);
    }
    /**
     * Parses the array of file names and returns the output file. i.e. that starting with ">"
     *
     * @param files the space-separated list of file names
     * @return the name of output file (without the leading ">")
     * @throws CruiseControlException when multiple output files are defined
     */
    private static String outFile(final String files) throws CruiseControlException {
        String f = null;

        /* Get these starting with "<" */
        for (String s : files.split("[ ]+")) {
            if (s.startsWith(">")) {
                s = s.substring(1);
                if (f == null) {
                    f = s;
                }
                else {
                    throw new CruiseControlException("Multiple output files: " + f + " + " + s);
                }
            }
        }
        /* Get it as list */
        return f == null ? "-" : f;
    }


    /** Value set/get by {@link #setTimeout(long)} and {@link #getTimeout()} */
    private long timeout = 100;

    /** The array of files to read from */
    private final String[] inpFiles;
    /** The output file to write output into */
    private final String outFile;

    /** The logger by by {@link #log()} method */
    private static final Logger LOG = Logger.getLogger(ExecScriptMock.class);


    /**
     * The implementation of data passing from input to output.
     */
    public static class Cat extends ExecScriptMock
    {
        /** Constructor. Just passes input stream to the output stream. */
        Cat() {
            this(0);
        }
        /** Constructor
         *  @param hold how many milliseconds to wait before flushing each line to the output
         */
        Cat(int hold) {
            super();
            this.hold = hold;
        }
        /** Constructor
         *  @param files the string with space-separated file names. These starting with "<" are input
         *   files, the one starting with ">" is the output file. One of the input and output can be
         *   set to "-", meaning that the data are read/written to the input/output stream.
         *  @throws CruiseControlException
         */
        Cat(String files) throws CruiseControlException {
            super(inpFiles(files), outFile(files));
            this.hold = 0;
        }

        @Override
        protected void build(InputStream inp, OutputStream out, PrintStream err) {
            new StreamPumper(inp, new StreamFlusher(out, hold)).run();
        }

        /** Wait time set through the constructor. */
        private final int hold;
    }
    /**
     * The implementation of data filtering. It prints only the input stream lines matching the given
     * pattern, or, optionally, these not matching the pattern.
     */
    public static class Grep extends ExecScriptMock
    {
        /** Constructor.
         *  @param pattern the pattern to filter
         */
        public Grep(String pattern) {
            this(pattern, true);
        }
        /** Constructor.
         *  @param pattern the pattern to filter
         *  @param match if <code>true</code>, lines matching the patterns are get, if <code>false</code> lines
         *   not matching the pattern are get
         */
        public Grep(String pattern, boolean match) {
            super();

            this.pattern = Pattern.compile(pattern);
            this.match = match;
        }

        @Override
        protected void build(InputStream inp, OutputStream out, PrintStream err) {
            err.println("Grepping by pattern " + pattern.pattern() + ", lines " +
                    (match ? "" : "not ") + "matching the pattern are printed");

            /* Read the data from STDIO, if no file was set (in argument 1)
             * Store data to STDOUT (implements filter prior to storing the data) */
            final StreamFlusher output = new StreamFlusher(out, 0);
            final AbstractConsumer filter = new AbstractConsumer(output) {
                /** The implementation of {@link net.sourceforge.cruisecontrol.util.StreamConsumer#consumeLine(String)}
                 *  filtering the lines */
                @Override
                public void consumeLine(final String line) {
                    final boolean f = pattern.matcher(line).find();

                    if (f && match) {
                        super.consumeLine(line);
                    }
                    if (!f && ! match) {
                        super.consumeLine(line);
                    }
                }
            };
            /* Filer the input and write it to output */
            new StreamPumper(inp, filter).run();

            err.println(filter.getNumLines() + " lines checked");
            err.println(output.getNumLines() + " lines written");
        }

        /** The pattern */
        private final Pattern pattern;
        /** Do match the pattern or not */
        private final boolean match;
    }
    /**
     * The implementation of data sorting. It prints the alphabetically sorted input stream lines
     * back to the output, optionally omitting duplicate items.
     */
    public static class Sort extends ExecScriptMock
    {
        /** Constructor. Sorts the input */
        public Sort() {
            this(false);
        }
        /** Constructor.
         *  @param uniq if <code>true</code>, unique lines are written only, if <code>false</code> all
         *   lines are written after sort
         */
        public Sort(boolean uniq) {
            super();
            this.uniq = uniq;
        }

        @Override
        protected void build(InputStream inp, OutputStream out, PrintStream err) {
            err.println("Sorting " + (uniq ? "and makign unique " : "") +
                    "lines in the input unique");

            /* Read the data from STDIO, if no file was set
             * Store data to STDOUT */
            final StreamFlusher output = new StreamFlusher(out, 0);
            ArrayStoreConsumer lines = new ArrayStoreConsumer();

            new StreamPumper(inp, lines).run();
            err.println(lines.size() + "  lines read");

            /* Make the data unique (and sorted), if required */
            if (uniq) {
                lines = new ArrayStoreConsumer(new TreeSet<String>(lines));
            }
            /* Sort them only, otherwise */
            else {
                Collections.sort(lines);
            }

            /* Write unique (and sorted) lines to the output */
            for (final String s : lines) {
                output.consumeLine(s);
            }

            err.println(output.getNumLines() + (uniq ? " unique" : "") + " lines written");
        }

        /** Make lines unique after sort? */
        private final boolean uniq;
    }
    /**
     * The implementation of <code>shuf</code> command. It shuffles the input stream lines and writes them
     * back to the output.
     */
    public static class Uniq extends ExecScriptMock {

        @Override
        protected void build(InputStream inp, OutputStream out, PrintStream err) {
            err.println("Making lines in the input unique");

            /* Read the data from input and pass them to output */
            final StreamFlusher output = new StreamFlusher(out, 0);
            final ArrayStoreConsumer lines = new ArrayStoreConsumer();

            new StreamPumper(inp, lines).run();
            err.println(lines.size() + "  lines read");

            /* Write unique (and sorted) lines to the output */
            for (final String s : new TreeSet<String>(lines)) {
                output.consumeLine(s);
            }

            err.println(output.getNumLines() + " unique lines written");
        }
    }
    /**
     * The implementation of data shuffling. It shuffles the input stream lines and writes them
     * back to the output.
     */
    public static class Shuf extends ExecScriptMock {

        @Override
        protected void build(InputStream inp, OutputStream out, PrintStream err) {
            err.println("Shuffling lines in the input");

            /* Read the data from STDIO, if no file was set
             * Store data to STDOUT */
            final StreamFlusher output = new StreamFlusher(out, 0);
            final ArrayStoreConsumer lines = new ArrayStoreConsumer();

            new StreamPumper(inp, lines).run();
            /* Shuffle the lines */
            Collections.shuffle(lines);
            /* Write to the output */
            for (final String s : lines) {
                output.consumeLine(s);
            }

            err.println(output.getNumLines() + " lines were read/shuffled/written");
        }
    }

    /**
     * The implementation of data modification. It adds or removes a string to each line written
     * back to the output.
     */
    private static class Modify extends ExecScriptMock {

        /** Constructor
         * @param add set <code>true</code> if new info is to be added to the output, or <code>false</code> if
         *  (the same) info is to be removed from the input*/
        protected Modify(boolean add) {
            this.add = add;
        }

        @Override
        protected void build(InputStream inp, OutputStream out, PrintStream err) {
            err.println("Modifying input by " + (add ? "adding some items" : "removing items added before"));

            /* Read the data from STDIO, if no file was set
             * Store data to STDOUT */
            final StreamFlusher output = new StreamFlusher(out, 0) {
                /** The implementation of {@link net.sourceforge.cruisecontrol.util.StreamConsumer#consumeLine(String)}
                 *  filtering the lines */
                @Override
                public void consumeLine(String line) {
                    /* Add:
                     * - the index of the line,
                     * - the number of items in the line
                     * - the length of the line */
                    if (add) {
                        line = getNumLines() + "  " + line.split("\\s+").length + "  " +
                                line.length() + "  " + line;
                    }
                    /* Remove the 3 items added */
                    else {
                        line = line.replaceAll("^\\s*\\d+\\s+\\d+\\s+\\d+\\s+", "");
                    }
                    /* Pass it to the parent */
                    super.consumeLine(line);
                }
            };
            /* Filter the input and write it to the output */
            new StreamPumper(inp, output).run();
            err.println(output.getNumLines() + " lines were read/modified/written");
        }

        /** Holds <code>true</code> if new info is added to the output, or <code>false</code> if (the same) info
         *  is removed from the input. */
        private final boolean add;
    }
    /**
     * The implementation of data addition. It adds a data to each line read from an input stream and
     * passes it to the output stream.
     */
    public static class Add extends Modify {
        /** Constructor */
        public Add() {
            super(true);
        }
    }
    /**
     * The implementation of data removal. It removes the data added by {@link Add} from each line read from an input
     * stream and passes it to the output stream.
     */
    public static class Del extends Modify {
        /** Constructor */
        public Del() {
            super(false);
        }
    }

   /**
    * The parent of all the consumers.
    */
   private static class AbstractConsumer implements StreamConsumer {

       /** Constructor. Creates the consumer suppressing all the messages. */
       public AbstractConsumer() {
           this(null);
       }
       /** Constructor. Creates consumer passing the data to the given consumer.
        *  @param out the consumer to write to (can be <code>null</code> to suppress the
        *         output) */
       public AbstractConsumer(final AbstractConsumer out) {
           this.lines = 0;
           this.out = out;
       }

       /** The implementation of {@link StreamConsumer#consumeLine(String)}. It is highly
        *  recommended to call the method, if overriding! */
       @Override
       public void consumeLine(final String line) {
           this.lines++;
           if (this.out != null) {
               this.out.consumeLine(line);
           }
       }
       /** @return the number of lines written */
       public long getNumLines() {
           return lines;
       }

       /** The consumer to write to */
       private final AbstractConsumer out;
       /** The number of lines passed through the consumer */
       private long lines;
   }

   /**
    * The implementation of {@link StreamConsumer} which stores the lines in the array
    * represented by this class
    */
   private static class ArrayStoreConsumer extends ArrayList<String> implements StreamConsumer {

       /** Serialization UID  */
       private static final long serialVersionUID = -3226412369210939346L;

       /** Default constructor */
       public ArrayStoreConsumer() {
           super();
       }
       /** Copy constructor. Fills the class by items from the given collection
        *  @param parent the collection to fill from */
       public ArrayStoreConsumer(final Collection<String> parent) {
           super(parent);
       }

       /** The implementation of {@link StreamConsumer#consumeLine(String)} */
       @Override
       public void consumeLine(final String line) {
           add(line);
       }
   }

   /**
    * The implementation of {@link AbstractConsumer} which passes the lines into the given
    * output stream
    */
   private static class StreamFlusher extends AbstractConsumer {
       /** Constructor
        *  @param outStream the stream to write to, or <code>null</code> not to write the
        *         lines anywhere
        *  @param waitMSec the time[ms] to wait before flushing each line to the output
        *         stream; if value is <= 0, line is flushed immediately.
        */
       public StreamFlusher(final OutputStream outStream, final int waitMSec) {
           waitMS = waitMSec;
           out = outStream != null ? new PrintStream(outStream) : null;
       }

       /** The implementation of {@link StreamConsumer#consumeLine(String)} */
       @Override
       public void consumeLine(final String line) {
           /* Call the parent */
           super.consumeLine(line);
           /* if wait, wait */
           if (waitMS > 10) {
               try {
                   Thread.sleep(waitMS);
               } catch (InterruptedException e) {
                   System.err.println("Exception caught when waiting for " + waitMS + "[msec]." +
                           "Message is: " + e.getMessage());
               }
           }
           /* Flush the line */
           if (out != null) {
               out.println(line);
           }
           ///* Print the line to STDERR in verbose mode */
           //if (verbose) {
           //    System.err.println(line);
           // }
       }

       /** Wait time[ms] before flushing it to output stream (if > 10) */
       private final int waitMS;
       /** The stream to write to */
       private final PrintStream out;
   }
}
