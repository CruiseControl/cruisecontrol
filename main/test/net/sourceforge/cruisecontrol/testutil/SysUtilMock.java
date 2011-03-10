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

package net.sourceforge.cruisecontrol.testutil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

/* Non system classes, needs to be added to SysUtilMock#java */
import net.sourceforge.cruisecontrol.builders.PipedExecBuilder.Script;
import net.sourceforge.cruisecontrol.util.MainArgs;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;  // required by StreamPumper

/**
 * Class implementing some system utilities, usually found on Unix systems, in order to
 * run tests on platforms not shipping those utilities.
 *
 * The class is not intended to be instantiated from the CruiseControl. Instead, it is
 * expected to called from the separate Java process invoked to simulate the call of a
 * system command.
 *
 * Note that the "utilities" implemented here do not follow the exact behavior of the
 * real utilities they are replacement for. Neither the options are the same in many cases.
 * All the "utilities" also expect textual data only!
 */
public class SysUtilMock {

    /** The java command used to invoke this class. The "classpath" path is tried to
     *  be built automatically */
    private static final String javaArgs = "-cp " + getClassPath(new Class[]{SysUtilMock.class,
                                                                             MainArgs.class,
                                                                             StreamConsumer.class,
                                                                             StreamPumper.class,
                                                                             Logger.class})
                                         + " "    + SysUtilMock.class.getCanonicalName();

    /**
     * <code>cat</code> command with some information/debugging messages printed to
     * STDERR. It has signature <code>[options] [file1 file2 ... fileN] [>[>]output]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     * <code>[-D ms]</code> waits <i>ms</i> milliseconds before each line is written
     * to the output.
     *
     * The space-separated list if files to join together can follow the options. The file
     * may be string "-" to read from STDIN, or <i>ZERO</i> to read from infinite source
     * returning newlines only (similar to <code>/dev/zero</code>). If there is no file set,
     * the data are read from STDIN
     *
     * The output is STDOUT by default, or to file when redirected using "> filename" (to
     * new file) or ">> filename" (to append to the existing file). The filename can be
     * text <i>NULL</i> to simulate printing to <code>/dev/null</code>.
     *
     * Use {@link SysUtilMock#cat}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#cat}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] cat  = {"java", javaArgs + " -C cat " };
    /**
     * <code>grep</code> command with some information/debugging messages printed to
     * STDERR. Contrary to the standard linux <code>grep</code>, this command expects
     * regular expression in Java {@link Pattern} compatible form. It has signature
     * <code>[options] pattern [file]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     * <code>[-v]</code> revert match, lines not matching the pattern will be printed
     * to the output.
     *
     * The <code>pattern</code> is {@link Pattern} compatible regular expression
     *
     * If <code>file</code> is set, the data are read from it. Otherwise, the data are read
     * from STDIN. The command does not support either "-" or <i>ZERO</i> as source.
     *
     * The input filtered through the pattern is written to STDOUT, the redirection is not
     * supported (pipe it with {@link #cat} if required).
     *
     * Use {@link SysUtilMock#grep}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#grep}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] grep = {"java", javaArgs + " -C grep "};
    /**
     * <code>sort</code> command with some information/debugging messages printed to
     * STDERR. It has signature <code>[options] [file]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     * <code>[-u]</code> make the output unique ()
     *
     * If <code>file</code> is set, the data are read from it. Otherwise, the data are read
     * from STDIN. The command does not support either "-" or <i>ZERO</i> as source.
     *
     * The sorted (and unique, is set) input is written to STDOUT, the redirection is not
     * supported (pipe it with {@link #cat} if required).
     *
     * Use {@link SysUtilMock#sort}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#sort}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] sort = {"java", javaArgs + " -C sort "};
    /**
     * <code>unique</code> command with some information/debugging messages printed to
     * STDERR. It has signature <code>[options] [file]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     *
     * If <code>file</code> is set, the data are read from it. Otherwise, the data are read
     * from STDIN. The command does not support either "-" or <i>ZERO</i> as source.
     *
     * The unique items from the input is written to STDOUT, the redirection is not supported
     * (pipe it with {@link #cat} if required).
     *
     * Use {@link SysUtilMock#uniq}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#uniq}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] uniq = {"java", javaArgs + " -C uniq "};
    /**
     * <code>shuf</code> command with some information/debugging messages printed to
     * STDERR. It has signature <code>[options] [file]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     *
     * If <code>file</code> is set, the data are read from it. Otherwise, the data are read
     * from STDIN. The command does not support either "-" or <i>ZERO</i> as source.
     *
     * The shuffled input is written to STDOUT, the redirection is not supported (pipe it
     * with {@link #cat} if required).
     *
     * Use {@link SysUtilMock#shuf}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#shuf}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] shuf = {"java", javaArgs + " -C shuf "};

    /**
     * Special command prefixing each input line by "NL NT NC ", where <i>NL</i> is the
     * index of line, <i>NT</i> is the number of tokens (space separated fields) in the line,
     * and <i>NC</i> is the numbed of characters in the line. The prefix can then be removed
     * by {@link #del} command. Some information/debugging messages are printed to STDERR.
     * The command has signature <code>[options] [file]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     *
     * If <code>file</code> is set, the data are read from it. Otherwise, the data are read
     * from STDIN. The command does not support either "-" or <i>ZERO</i> as source.
     *
     * The prefixed input is written to STDOUT, the redirection is not supported (pipe it
     * with {@link #cat} if required).
     *
     * Use {@link SysUtilMock#add}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#add}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] add  = {"java", javaArgs + " -C add "};
    /**
     * Special command deleting the prefix added by {@link #add} command. Some
     * information/debugging messages are printed to STDERR. The command has signature
     * <code>[options] [file]</code>.
     *
     * The options are:
     * <code>[-V]</code> prints the data written to he output to the STDERR as well
     * <code>[-P text]</code> text prefixing the messages printed to STDERR
     *
     * If <code>file</code> is set, the data are read from it. Otherwise, the data are read
     * from STDIN. The command does not support either "-" or <i>ZERO</i> as source.
     *
     * The modified input is written to STDOUT, the redirection is not supported (pipe it
     * with {@link #cat} if required).
     *
     * Use {@link SysUtilMock#del}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#del}[1] + <code>"required options list"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] del  = {"java", javaArgs + " -C del "};

    /**
     * Special command representing not existing command (the attempt to invoke it fails
     * on system level by <i>command not found</i> error).
     *
     * Use {@link SysUtilMock#BAD}[0] as argument for {@link Script#setCommand(String)},
     * and {@link SysUtilMock#BAD}[1] + <code>"whatever"</code> as argument
     * for {@link Script#setArgs(String)}.
     */
    public static final String[] BAD  = {"BAD",  ""};


    /** Should the data printed to STDOUT (or file) be printed to STDERR as well? Se to
     *  <code>true</code> to do so. */
    private static boolean verbose = false;


    /**
     * Main method, calls individual utilities according to the <code>-C command</code>
     * arguments passed.
     * @param args the arguments passed to the method
     */
    public static void main(String[] args) {

        try {
            String prefix;
            String command;

            /* Get the prefix as the very first action */
            if ((prefix = MainArgs.parseArgument(args, "P", null, null)) != null) {
                args = removeArgs(args, MainArgs.findIndex(args, "P"), 2);
                System.setErr(new PrefixedStream(prefix, System.err));
            }

            /* Print how the command was called */
            System.err.print("Executing command: ");
            System.err.print("java[" + SysUtilMock.class.getCanonicalName() + "]");
            for (String a : args) {
                System.err.print(" ");
                System.err.print(a);
            }
            System.err.println();

            /* Get the command */
            if ((command = MainArgs.parseArgument(args, "C", null, null)) == null) {
                throw new IllegalArgumentException("Command (-C option) not set");
            }
            /* Remove the command from the array. It will contain only the command
             * options now */
            args = removeArgs(args, MainArgs.findIndex(args, "C"), 2);

            /* Get the verbose output flag */
            if (MainArgs.argumentPresent(args, "V")) {
                verbose = true;
                args = removeArgs(args, MainArgs.findIndex(args, "V"), 1);
            }


            /* Call the command */
            if ("cat".equals(command)) {
                cat(args);
                System.exit(0);
            }
            if ("grep".equals(command)) {
                grep(args);
                System.exit(0);
            }
            if ("sort".equals(command)) {
                sort(args);
                System.exit(0);
            }
            if ("uniq".equals(command)) {
                uniq(args);
                System.exit(0);
            }
            if ("shuf".equals(command)) {
                shuf(args);
                System.exit(0);
            }
            if ("add".equals(command)) {
                modify(args, true);
                System.exit(0);
            }
            if ("del".equals(command)) {
                modify(args, false);
                System.exit(0);
            }

            /* Command not found */
            throw new IllegalArgumentException("Command '" + command + "' not supported");

        } catch(Throwable e) {
            System.err.println("Command failed with exception:");
            /* Print the exception */
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }


    /**
     * The implementation of {@link #cat} command.
     * @param  args the arguments passed to the {@link #cat} command. Options not
     *         expected cause error.
     * @throws IOException
     */
    private static final void cat(String[] args) throws IOException {
        List<InputStream> inputs = new ArrayList<InputStream>();
        StreamFlusher output = null;
        int hold;

        /* Is -D set? */
        hold = MainArgs.parseInt(args, "D", -1, -1);
        if (hold > -1) {
            args = removeArgs(args, MainArgs.findIndex(args, "D"), 2);
        }

        /* Create the array of inputs - process argument-by-argument */
        for (String src : args) {

             /* Redirection found, leave */
             if (src.startsWith(">")) {
                 break;
             }
             /* Read from STDIO */
             if ("-".equals(src)) {
                 inputs.add(System.in);
                 continue;
             }
             /* Read from "ZERO". It returns '\n' instead of an empty character to prevent
              * "line overflow" (one line cannot be read as it would be infinite ...) */
             if ("ZERO".equals(src)) {
                 inputs.add(new InputStream() {
                                /* Returns '\n' */
                                @Override
                                public int read() {
                                    return '\n';
                                }
                            });
                 continue;
             }
             /* Read from "classic" file */
             inputs.add(new FileInputStream(src));
        }

        /* Create the output. If all attributes were not processed, the output is redirected
         * to a file */
        if (args.length > inputs.size()) {
            String ofname = null;
            boolean append = false;

            /* Just one remaining: ">file" */
            if (args.length == inputs.size() +1) {
                append = args[args.length -1].startsWith(">>");
                ofname = args[args.length -1].replaceFirst("^>+", "");
            }
            /* more remaining "> file"*/
            else {
                append = args[args.length -2].equals(">>");
                ofname = args[args.length -1];
            }
            /* Create output stream. If output filename is "NULL", ignore the output */
            if ("NULL".equals(ofname)) {
                output = new StreamFlusher(null,  hold);
            }
            else {
                output = new StreamFlusher(new FileOutputStream(ofname, append), hold);
            }
        }
        /* If not output stream was set, print to STDOUT */
        if (output == null) {
            output = new StreamFlusher(System.out, hold);
        }
        /* If not input was set, read from STDIN */
        if (inputs.size() == 0) {
            inputs.add(System.in);
        }

        /* Process source-by-source */
        for (InputStream i : inputs) {
             new StreamPumper(i, output).run();
             i.close();
        }
        /* Close the output */
        output.close();
    } /* cat */

    /**
     * The implementation of {@link #grep} command.
     * @param args the arguments passed to the command. Options not expected cause error.
     * @throws IOException
     */
    private static final void grep(String[] args) throws IOException {
        InputStream  input;
        StreamFlusher output;
        AbstractConsumer filter;
        final int invert = MainArgs.findIndex(args, "v");

        /* Is -v set? */
        if (invert != MainArgs.NOT_FOUND) {
            args = removeArgs(args, invert, 1);
        }

        /* No pattern set? Error */
        if (args.length == 0) {
            throw new IllegalArgumentException("Not enough arguments for 'grep'");
        }

        System.err.println("Grepping by pattern " + args[0] + ", lines " +
                (invert != MainArgs.NOT_FOUND ? "not " : "") + "matching the pattern are printed");

        /* Compile the pattern */
        final Pattern regexp = Pattern.compile(args[0]);
        /* Read the data from STDIO, if no file was set (in argument 1)
         * Store data to STDOUT (implements filter prior to storig the data) */
        input = args.length == 1 ? System.in : new FileInputStream(args[1]);
        output = new StreamFlusher(System.out, 0);
        filter = new AbstractConsumer(output) {
                         /** The implementation of {@link StreamConsumer#consumeLine(String)}
                          *  filtering the lines */
                         @Override
                         public void consumeLine(String line) {
                             boolean f = regexp.matcher(line).find();

                             if (f && invert == MainArgs.NOT_FOUND) {
                                 super.consumeLine(line);
                             }
                             if (! f && invert != MainArgs.NOT_FOUND) {
                                 super.consumeLine(line);
                             }
                         }
                     };
        /* Filer the input and write it to output */
        new StreamPumper(input, filter).run();
        input.close();
        output.close();

        System.err.println(filter.getNumLines() + " lines checked");
        System.err.println(output.getNumLines() + " lines written");
    } /* grep */

    /**
     * The implementation of {@link #sort} command.
     * @param args the arguments passed to the command. Options not expected cause error.
     * @throws IOException
     */
    private static final void sort(String[] args) throws IOException {
        InputStream  input;
        StreamFlusher output;
        ArrayStoreConsumer lines;
        int uniq;

        /* Is -u set? */
        if((uniq = MainArgs.findIndex(args, "u")) != MainArgs.NOT_FOUND) {
            args = removeArgs(args, uniq, 1);
        }

        System.err.println("Sorting " + (uniq != MainArgs.NOT_FOUND ? "and makign unique " :
            "") + "lines in the input unique");

        /* Read the data from STDIO, if no file was set
         * Store data to STDOUT */
        input = args.length == 0 ? System.in : new FileInputStream(args[0]);
        output = new StreamFlusher(System.out, 0);
        lines = new ArrayStoreConsumer();

        new StreamPumper(input, lines).run();
        System.err.println(lines.size() + "  lines read");

        /* Make the data unique (and sorted), if required */
        if (uniq != MainArgs.NOT_FOUND) {
            lines = new ArrayStoreConsumer(new TreeSet<String>(lines));
        }
        /* Sort them only, otherwise */
        else {
            Collections.sort(lines);
        }

        /* Write unique (and sorted) lines to the output */
        for (String s : lines) {
            output.consumeLine(s);
        }
        input.close();
        output.close();

        System.err.println(output.getNumLines() + (uniq != MainArgs.NOT_FOUND ? " unique" : "")
                + " lines written");
    } /* sort */

    /**
     * The implementation of {@link #uniq} command.
     * @param args the arguments passed to the command. Options not expected cause error.
     * @throws IOException
     */
    private static final void uniq(String[] args) throws IOException {
        InputStream  input;
        StreamFlusher output;
        ArrayStoreConsumer lines;

        System.err.println("Making lines in the input unique");

        /* Read the data from STDIO, if no file was set
         * Store data to STDOUT */
        input = args.length == 0 ? System.in : new FileInputStream(args[0]);
        output = new StreamFlusher(System.out, 0);
        lines = new ArrayStoreConsumer();

        new StreamPumper(input, lines).run();
        System.err.println(lines.size() + "  lines read");

        /* Write unique (and sorted) lines to the output */
        for (String s : new TreeSet<String>(lines)) {
            output.consumeLine(s);
        }
        input.close();
        output.close();

        System.err.println(output.getNumLines() + " unique lines written");
    } /* uniq */

    /**
     * The implementation of {@link #shuf} command.
     * @param args the arguments passed to the command. Options not expected cause error.
     * @throws IOException
     */
    private static final void shuf(String[] args) throws IOException {
        InputStream  input;
        StreamFlusher output;
        ArrayStoreConsumer lines;

        System.err.println("Shuffling lines in the input");

        /* Read the data from STDIO, if no file was set
         * Store data to STDOUT */
        input = args.length == 0 ? System.in : new FileInputStream(args[0]);
        output = new StreamFlusher(System.out, 0);
        lines = new ArrayStoreConsumer();

        new StreamPumper(input, lines).run();
        /* Shuffle the lines */
        Collections.shuffle(lines);
        /* Write to the output */
        for (String s : lines) {
            output.consumeLine(s);
        }
        input.close();
        output.close();

        System.err.println(output.getNumLines() + " lines were read/shuffled/written");
    } /* shuff */

    /**
     * The implementation of {@link #add} and {@link #del} commands.
     * @param args the arguments passed to the command. Options not expected cause error.
     * @param add <code>true</code> if to all {@link #add}, <code>false</code> if to call
     *        {@link #del}
     * @throws IOException
     */
    private static final void modify(String[] args, final boolean add) throws IOException {
        InputStream  input;
        StreamFlusher output;

        System.err.println("Modifying input by " + (add ? "adding some items"
                : "removing items added before"));

        /* Read the data from STDIO, if no file was set
         * Store data to STDOUT */
        input = args.length == 0 ? System.in : new FileInputStream(args[0]);
        output = new StreamFlusher(System.out, 0) {
                     /** The implementation of {@link StreamConsumer#consumeLine(String)}
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
        new StreamPumper(input, output).run();
        input.close();
        output.close();

        System.err.println(output.getNumLines() + " lines were read/modified/written");
    } /* add */


    /**
     * Removes the given range of arguments from the input array of arguments.
     *
     * @param args the array of arguments
     * @param from the index of the first argument to remove
     * @param  num the number of arguments to remove
     * @return the copy of array without the indexes specified
     */
    private static final String[] removeArgs(final String[] args, final int from, final int num) {
        List<String> arglist = new ArrayList<String>(args.length);

        /* Add the first part up to from */
        for (int i = 0; i < from; i++) {
            arglist.add(args[i]);
        }
        /* and the second part */
        for (int i = from + num; i < args.length; i++) {
            arglist.add(args[i]);
        }
        /* Get the reduced array */
        return arglist.toArray(new String[arglist.size()]);
    }

    /**
     * Gets the string with classpath for <code>java</code> command (<code>-cp</code> option).
     * The string is build from <code>CLASSPATH</code> environment variable, and from
     * directories where the <code>.class</code> files are located for the given classes.
     *
     * @param clazz the classes for which to find the classpath direcory
     * @return the string to be set as "classpath"
     */
    private static final String getClassPath(final Class<?>[] clazz) {
        HashSet<String> cpset = new HashSet<String>();
        StringBuffer cpopt = new StringBuffer();

        /* Environment variable */
        if (System.getenv("CLASSPATH") != null) {
            for (String s : System.getenv("CLASSPATH").split(":")) {
                cpset.add(s);
            }
        }
        /* Individual classes */
        for (Class<?> c : clazz) {
            try {
                String classFile = c.getCanonicalName().replace('.', '/') + ".class";
                URL    classPath = c.getClassLoader().getResource(classFile);
                /* The URL points to JAR file */
                if ("jar".equals(classPath.getProtocol())) {
                    classPath = ((JarURLConnection) classPath.openConnection()).getJarFileURL();
                }
                /* Add the basic directory (without class namespace) to the set */
                cpset.add(classPath.getPath().replace(classFile, ""));

            } catch(Exception e) {
                System.err.println("Cannot build classpath for class " + c.getCanonicalName());
                e.printStackTrace(System.err);
            }
        }

        /* Format to the classpath option */
        for(String s : cpset) {
            cpopt.append(cpopt.lastIndexOf(":") < cpopt.length() -1 ? ":" : ""); // endswith(":")
            cpopt.append(s);
        }
        /* Get the result */
        return cpopt.toString();
   }


   /**
    * The parent of all the consumers.
    */
   private static class AbstractConsumer implements StreamConsumer
   {
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
       //@Override
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
       /** Closes the output stream. This implementation just calls parent */
       public void close() {
           if (this.out != null) {
               this.out.close();
           }
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
    private static class ArrayStoreConsumer extends ArrayList<String> implements StreamConsumer
    {
        /** Serialization UID  */
        private static final long serialVersionUID = -3226412369210939346L;

        /** Default constructor */
        public ArrayStoreConsumer() {
            /* Just call parent */
        }
        /** Copy constructor. Fills the class by items from the given collection
         *  @param parent the collection to fill from */
        public ArrayStoreConsumer(final Collection<String> parent) {
            super(parent);
        }

        /** The implementation of {@link StreamConsumer#consumeLine(String)} */
        //@Override
        public void consumeLine(final String line) {
            add(line);
        }
    }

    /**
     * The implementation of {@link AbstractConsumer} which passes the lines into the given
     * output stream
     */
    private static class StreamFlusher extends AbstractConsumer
    {
        /** Constructor
         *  @param outStream the stream to write to, or <code>null</code> not to write the
         *         lines anywhere
         *  @param waitMSec the time[ms] to wait before flushing each line to the output
         *         stream; if value is <= 0, line is flushed immediately.
         */
        public StreamFlusher(final OutputStream outStream, final int waitMSec) {
            waitMS = waitMSec;
            out = outStream != null ? new PrintStream(outStream)
                                    : null;
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
            /* Print the line to STDERR in verbose mode */
            if (verbose) {
                System.err.println(line);
            }
        }
        /** Closes the output stream */
        @Override
        public void close() {
            out.close();
        }

        /** Wait time[ms] before flushing it to output stream (if > 10) */
        private final int waitMS;
        /** The stream to write to */
        private final PrintStream out;
    }



    /**
     * Overriding of {@link PrintStream} prefixing each line by the given string. The stream
     * is used as the replacement of {@link System#err}, prefixing the output by the ID of
     * the command (passed through <code>-P</code> option, set as the prefix) for debugging
     * purposes.
     */
    private static class PrefixedStream extends PrintStream
    {
        /** Constructor
         *  @param prefix the string print before each line
         *  @param out the stream to which values and objects will be printed */
        public PrefixedStream(final String prefix, final PrintStream out) {
            super(out);
            this.prefix = (prefix + ": ").getBytes();
            printPrefix = true;
        }

        /** The overriding of {@link PrintStream#write(byte[], int, int)} */
        @Override
        public void write(final byte[] buf, final int off, final int len) {
            /* Possibly performace killer, but acceptabe for now ... */
            for (int i = 0; i < len; i++) {
                write(buf[i]);
            }
        }
        /** The overriding of {@link PrintStream#write(int)} */
        @Override
        public void write(final int b) {
            /* Print prefix, if scheduled */
            if (printPrefix) {
                printPrefix = false;
                for (byte p : this.prefix) {
                    super.write(p);
                }
            }
            super.write(b);
            /* Schedule prefix printing, if newline was found */
            if (b == '\n') {
                printPrefix = true;
            }
        }

        /** The prefix */
        private final byte[] prefix;
        /** Should the prefix be printed after new call? */
        private boolean printPrefix;
    }


}
