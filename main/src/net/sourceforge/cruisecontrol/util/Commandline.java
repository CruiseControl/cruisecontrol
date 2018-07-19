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
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Commandline objects help handling command lines specifying processes to execute.
 *
 * The class can be used to define a command line as nested elements or as a helper to define a command line by an
 * application.
 * <p>
 * <code>
 * &lt;someelement&gt;<br>
 * &nbsp;&nbsp;&lt;acommandline executable="/executable/to/run"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 1" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument line="argument_1 argument_2 argument_3" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 4" /&gt;<br>
 * &nbsp;&nbsp;&lt;/acommandline&gt;<br>
 * &lt;/someelement&gt;<br>
 * </code> The element <code>someelement</code> must provide a method <code>createAcommandline</code> which returns
 * an instance of this class.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Commandline implements Cloneable {

    private static final Logger LOG = Logger.getLogger(Commandline.class);

    private final Vector<Argument> arguments = new Vector<Argument>();

    private String executable;
    private String[] execEnv;

    private File workingDir;
    private final CruiseRuntime runtime;

    private boolean closeStdIn = true; // close it by default to prevent deadlocks (see revision 3143)

    public Commandline(String toProcess, CruiseRuntime cruiseRuntime) {
        super();
        this.runtime = cruiseRuntime;
        if (toProcess != null) {
            String[] tmp = new String[0];
            try {
                tmp = translateCommandline(toProcess);
            } catch (CruiseControlException e) {
                LOG.error("Error translating Commandline.", e);
            }
            if (tmp != null && tmp.length > 0) {
                setExecutable(tmp[0]);
                for (int i = 1; i < tmp.length; i++) {
                    createArgument().setValue(tmp[i]);
                }
            }
        }
    }

    public Commandline(String toProcess) {
        this(toProcess, new CruiseRuntime());
    }

    public Commandline() {
        this(null);
    }

    protected File getWorkingDir() {
        return workingDir;
    }

    /**
     * Used for nested xml command line definitions.
     */
    public static class Argument {
        private String[] parts;

        /**
         * Sets a single commandline argument.
         *
         * @param value
         *            a single commandline argument.
         */
        public void setValue(String value) {
            parts = new String[] { value };
        }

        /**
         * Line to split into several commandline arguments.
         *
         * @param line
         *            line to split into several commandline arguments
         */
        public void setLine(String line) {
            if (line == null) {
                return;
            }
            try {
                parts = translateCommandline(line);
            } catch (CruiseControlException e) {
                LOG.error("Error translating Commandline.", e);
            }
        }

        /**
         * Sets a single commandline argument to the absolute filename of the given file.
         *
         * @param value
         *            a single commandline argument.
         */
        public void setFile(File value) {
            parts = new String[] { value.getAbsolutePath() };
        }

        /**
         * @return the parts this Argument consists of.
         */
        public String[] getParts() {
            return parts;
        }

    }

    /**
     * Class to keep track of the position of an Argument.
     */
    // <p>This class is there to support the srcfile and targetfile
    // elements of &lt;execon&gt; and &lt;transform&gt; - don't know
    // whether there might be additional use cases.</p> --SB
    public class Marker {
        private final int position;

        private int realPos = -1;

        Marker(int position) {
            this.position = position;
        }

        /**
         * Return the number of arguments that preceeded this marker.
         *
         * <p>
         * The name of the executable - if set - is counted as the very first argument.
         * </p>
         * @return the number of arguments that preceeded this marker.
         */
        public int getPosition() {
            if (realPos == -1) {
                realPos = (executable == null ? 0 : 1);
                for (int i = 0; i < position; i++) {
                    final Argument arg = arguments.elementAt(i);
                    realPos += arg.getParts().length;
                }
            }
            return realPos;
        }

    }

    /**
     * Creates an argument object.
     *
     * <p>
     * Each commandline object has at most one instance of the argument class. This method calls
     * <code>this.createArgument(false)</code>.
     * </p>
     *
     * @see #createArgument(boolean)
     * @return the argument object.
     */
    public Argument createArgument() {
        return this.createArgument(false);
    }

    /**
     * Creates an argument object and adds it to our list of args.
     *
     * <p>
     * Each commandline object has at most one instance of the argument class.
     * </p>
     *
     * @param insertAtStart
     *            if true, the argument is inserted at the beginning of the list of args, otherwise it is appended.
     * @return an argument object.
     */
    public Argument createArgument(final boolean insertAtStart) {
        final Argument argument = new Argument();
        if (insertAtStart) {
            arguments.insertElementAt(argument, 0);
        } else {
            arguments.addElement(argument);
        }
        return argument;
    }

    /**
     * Same as calling createArgument().setValue(value), but much more convenient.
     * @param value argument object value.
     * @return the new argument object with it's value already set.
     */
    public Argument createArgument(final String value) {
        final Argument arg = this.createArgument();
        arg.setValue(value);
        return arg;
    }

    /**
     * Same as calling createArgument twice in a row, but can be used to make more obvious a relationship between to
     * command line arguments, like "-folder c:\myfolder".
     * @param first first arg to create
     * @param second second arg to create
     */
    public void createArguments(final String first, final String second) {
        createArgument(first);
        createArgument(second);
    }

    /**
     * @param env the environment prepared for the executable, or <code>null</code> if to pass default environment
     *    to the executable.
     */
    public void setEnv(final OSEnvironment env) {
        this.execEnv = env != null ? env.toArray() : null;
    }

    /**
     * @param executable the executable to run.
     */
    public void setExecutable(final String executable) {
        if (executable == null || executable.length() == 0) {
            return;
        }
        this.executable = executable.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }

    public String getExecutable() {
        return executable;
    }

    public void addArguments(final String[] line) {
        for (final String arg : line) {
            createArgument().setValue(arg);
        }
    }

    /**
     * @return the executable and all defined arguments.
     */
    public String[] getCommandline() {
        final String[] args = getArguments();
        if (executable == null) {
            return args;
        }
        final String[] result = new String[args.length + 1];
        result[0] = executable;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    /**
     * @return all arguments defined by <code>addLine</code>, <code>addValue</code> or the argument object.
     */
    public String[] getArguments() {
        final Vector<String> result = new Vector<String>(arguments.size() * 2);
        for (int i = 0; i < arguments.size(); i++) {
            final Argument arg = arguments.elementAt(i);
            final String[] s = arg.getParts();
            if (s != null) {
                for (final String value : s) {
                    result.addElement(value);
                }
            }
        }

        final String[] res = new String[result.size()];
        result.copyInto(res);
        return res;
    }

    @Override
    public String toString() {
        return toString(getCommandline(), true);
    }

    /**
     * Converts the command line to a string without adding quotes to any of the arguments.
     * @return the command line to a string without adding quotes to any of the arguments.
     */
    public String toStringNoQuoting() {
        return toString(getCommandline(), false);
    }

    /**
     * Put quotes around the given String if necessary.
     *
     * <p>
     * If the argument doesn't include spaces or quotes, return it as is. If it contains double quotes, use single
     * quotes - else surround the argument by double quotes.
     * </p>
     *
     * @param argument the arg to be quoted if needed.
     * @return the altered (possibly quoted) argument.
     * @exception CruiseControlException
     *                if the argument contains both, single and double quotes.
     */
    public static String quoteArgument(final String argument) throws CruiseControlException {
        if (argument.indexOf("\"") > -1) {
            if (argument.indexOf("\'") > -1) {
                throw new CruiseControlException("Can't handle single and double quotes in same argument");
            } else {
                return '\'' + argument + '\'';
            }
        } else if (argument.indexOf("\'") > -1 || argument.indexOf(" ") > -1) {
            return '\"' + argument + '\"';
        } else {
            return argument;
        }
    }

    public static String toString(final String[] line, final boolean quote) {
        return toString(line, quote, " ");
    }

    public static String toString(final String[] line, final boolean quote, final String separator) {
        // empty path return empty string
        if (line == null || line.length == 0) {
            return "";
        }

        // path containing one or more elements
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < line.length; i++) {
            if (i > 0) {
                result.append(separator);
            }
            if (quote) {
                try {
                    result.append(quoteArgument(line[i]));
                } catch (CruiseControlException e) {
                    LOG.error("Error quoting argument.", e);
                }
            } else {
                result.append(line[i]);
            }
        }
        return result.toString();
    }

    public static String[] translateCommandline(final String toProcess) throws CruiseControlException {
        if (toProcess == null || toProcess.length() == 0) {
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final Vector<String> v = new Vector<String>();
        final StringBuilder current = new StringBuilder();

        while (tok.hasMoreTokens()) {
            final String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (current.length() != 0) {
                        v.addElement(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(nextTok);
                }
                break;
            }
        }

        if (current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new CruiseControlException("unbalanced quotes in " + toProcess);
        }

        final String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    public int size() {
        return getCommandline().length;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();

        final Commandline c = new Commandline();
        c.setExecutable(executable);
        c.addArguments(getArguments());
        //c.useSafeQuoting(safeQuoting);

        return c;
    }

    /**
     * Clear out the whole command line.
     */
    public void clear() {
        executable = null;
        arguments.removeAllElements();
    }

    /**
     * Clear out the arguments but leave the executable in place for another operation.
     */
    public void clearArgs() {
        arguments.removeAllElements();
    }

    /**
     * Return a marker.
     *
     * <p>
     * This marker can be used to locate a position on the commandline - to insert something for example - when all
     * parameters have been set.
     * </p>
     * @return a marker
     */
    public Marker createMarker() {
        return new Marker(arguments.size());
    }

    /**
     * Sets execution directory.
     * @param path the working directory.
     * @throws CruiseControlException if something breaks
     */
    public void setWorkingDirectory(String path) throws CruiseControlException {
        if (path != null) {
            File dir = new File(path);
            checkWorkingDir(dir);
            workingDir = dir;
        } else {
            workingDir = null;
        }
    }

    public void setWorkingDir(Directory directory) throws CruiseControlException {
        directory.validate();
        this.workingDir = directory.toFile();
    }

    /**
     * Sets execution directory
     * @param workingDir the working directory.
     * @throws CruiseControlException if something breaks
     */
    public void setWorkingDir(File workingDir) throws CruiseControlException {
        checkWorkingDir(workingDir);
        this.workingDir = workingDir;
    }

    // throws an exception if the specified working directory is non null
    // and not a valid working directory
    private void checkWorkingDir(File dir) throws CruiseControlException {
        if (dir != null) {
            if (!dir.exists()) {
                throw new CruiseControlException("Working directory \"" + dir.getAbsolutePath() + "\" does not exist!");
            } else if (!dir.isDirectory()) {
                throw new CruiseControlException("Path \"" + dir.getAbsolutePath() + "\" does not specify a "
                        + "directory.");
            }
        }
    }

    public File getWorkingDirectory() {
        return workingDir;
    }

    /**
     * Should STDIN of the process be closed just after executed? By default it is closed
     * to prevent deadlocks. Set this to <code>false</code> <b>only</b> when you need to
     * read {@link Process#getOutputStream()} of the process returned by {@link #execute()}
     * (and close it when you finish the reading!).
     *
     * @param close close the STDIN or not (by default it is <code>True</code> when not set
     *        otherwise)
     * @see   #execute()
     */
    public void setCloseStdIn(boolean close) {
        this.closeStdIn = close;
    }

    /**
     * Executes the command.
     * @return command Process object
     * @throws IOException if something breaks
     */
    public Process execute() throws IOException {
        final Process process;
        final ProcessBuilder pbulder = new ProcessBuilder(getCommandline());
        final Map<String, String> pbenv = pbulder.environment();

        final String msgCommandInfo = "Executing: [" + getExecutable() + "] with parameters: ["
                + toString(getArguments(), false, "], [") + "] and with "
                + (this.execEnv != null ? "customized" : "default") + " environment variables";

        if (workingDir == null) {
            LOG.debug(msgCommandInfo);
        } else {
            LOG.debug(msgCommandInfo + " in directory " + workingDir.getAbsolutePath());
            pbulder.directory(workingDir);
        }

        // Clear the original environment and fill new values
        if (this.execEnv != null) {
            pbenv.clear();
            for (final String s : this.execEnv) {
                final String[] env = s.split("\\s*=\\s*",2);
                pbenv.put(env[0], env[1]);
            }
        }

        process = pbulder.start();

        if (closeStdIn) {
            process.getOutputStream().close();
        }

        return process;
    }

    /**
     * Executes the command and wait for it to finish.
     *
     * @param log
     *            where the output and error streams are logged
     * @throws CruiseControlException if something breaks
     */
    public void executeAndWait(Logger log) throws CruiseControlException {
        new CommandExecutor(this, log).executeAndWait();
    }
}
