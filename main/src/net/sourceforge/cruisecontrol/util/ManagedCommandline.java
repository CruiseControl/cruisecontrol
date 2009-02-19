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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Extends <cod>EnvCommandline</code> by adding stdout and stderr
 * stream handling as well as some assertions to check for proper
 * execution of the command.
 *
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
 */
public class ManagedCommandline extends EnvCommandline {

    private static final Logger LOG = Logger.getLogger(ManagedCommandline.class);

    /**
     * Holds the exit code from the command
     */
    private int exitCode;

    /**
     * The stdout from the command as a string
     */
    private String stdout;

    /**
     * The stdout from the command as a List of output lines
     */
    private final List<String> stdoutLines = new ArrayList<String>();

    /**
     * The stderr from the command as a string
     */
    private String stderr;

    /**
     * The stderr from the command as a List of output lines
     */
    private final List<String> stderrLines  = new ArrayList<String>();

    /**
     * Constructor which takes a command line string and attempts
     * to parse it into it's various components.
     *
     * @param command The command
     */
    public ManagedCommandline(String command) {
        super(command);
    }

    /**
     * Default constructor
     */
    public ManagedCommandline() {
        super();
    }

    /**
     * Returns the exit code of the command as reported by the OS.
     *
     * @return The exit code of the command
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Returns the stdout from the command as a String
     *
     * @return The standard output of the command as a <code>String</code>
     */
    public String getStdoutAsString() {
        return stdout;
    }

    /**
     * Returns the stdout from the command as a List of Strings where each
     * String is one line of the output.
     *
     * @return The standard output of the command as a <code>List</code> of
     *         output lines.
     */
    public List<String> getStdoutAsList() {
        return stdoutLines;
    }

    /**
     * Returns the stderr from the command as a String
     *
     * @return The standard error of the command as a <code>String</code>
     */
    public String getStderrAsString() {
        return stderr;
    }

    /**
     * Returns the stderr from the command as a List of Strings where each
     * String is one line of the error.
     *
     * @return The standard error of the command as a <code>List</code> of
     *         output lines.
     */
    public List<String> getStderrAsList() {
        return stderrLines;
    }

    /**
     * Clear out the whole command line.
     */
    public void clear() {
        super.clear();
        clearArgs();
    }

    /**
     * Clear out the arguments and stored command output, but leave the
     * executable in place for another operation.
     */
    public void clearArgs() {
        exitCode = -1;
        stdout = "";
        stderr = "";
        stdoutLines.clear();
        stderrLines.clear();
        super.clearArgs();
    }

    /**
     * Asserts that the stdout of the command does not contain a
     * given <code>String</code>. Throws a
     * <code>CruiseControlException</code> if it does.
     *
     * @param string
     *            The forbidden <code>String</code>
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertStdoutDoesNotContain(final String string) throws CruiseControlException {
        if (stdout.indexOf(string) > -1) {
            throw new CruiseControlException(
                "The command \""
                    + this.toString()
                    + "\" returned the forbidden string \""
                    + string
                    + "\". \n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Asserts that the stdout of the command contains a
     * given <code>String</code>. Throws a
     * <code>CruiseControlException</code> if it does not.
     *
     * @param string
     *            The required <code>String</code>
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertStdoutContains(final String string) throws CruiseControlException {
        if (stdout.indexOf(string) < 0) {
            throw new CruiseControlException(
                "The stdout of the command \""
                    + this.toString()
                    + "\" did not contain the required string \""
                    + string
                    + "\". \n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Asserts that the stderr of the command does not contain a
     * given <code>String</code>. Throws a
     * <code>CruiseControlException</code> if it does.
     *
     * @param string
     *            The forbidden <code>String</code>
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertStderrDoesNotContain(final String string) throws CruiseControlException {
        if (stderr.indexOf(string) > -1) {
            throw new CruiseControlException(
                "The command \""
                    + this.toString()
                    + "\" returned the forbidden string \""
                    + string
                    + "\". \n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Asserts that the exit code of the command matches an expected value.
     * Throws a <code>CruiseControlException</code> if it does not.
     *
     * @param code
     *            The expected exit code of the command
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertExitCode(final int code) throws CruiseControlException {
        if (exitCode != code) {
            throw new CruiseControlException(
                "The command \""
                    + this.toString()
                    + "\" returned exit code \""
                    + exitCode
                    + "\" when \""
                    + code
                    + "\" was expected.\n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Asserts that the exit code of the command is not a given value. Throws a
     * <code>CruiseControlException</code> if it is.
     *
     * @param code
     *            The expected exit code of the command
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertExitCodeNot(final int code) throws CruiseControlException {
        if (exitCode == code) {
            throw new CruiseControlException(
                "The command \""
                    + this.toString()
                    + "\" returned exit code \""
                    + exitCode
                    + "\".\n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Asserts that the exit code of the command is greater than a given value.
     * Throws a <code>CruiseControlException</code> if it is not.
     *
     * @param code
     *            The expected exit code of the command
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertExitCodeGreaterThan(final int code)
        throws CruiseControlException {
        if (exitCode <= code) {
            throw new CruiseControlException(
                "The command \""
                    + this.toString()
                    + "\" returned exit code \""
                    + exitCode
                    + "\" when a value greater than \""
                    + code
                    + "\" was expected.\n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Asserts that the exit code of the command is less than a given value.
     * Throws a <code>CruiseControlException</code> if it is not.
     *
     * @param code
     *            The expected exit code of the command
     *
     * @throws CruiseControlException if something breaks
     */
    public void assertExitCodeLessThan(final int code)
        throws CruiseControlException {
        if (exitCode >= code) {
            throw new CruiseControlException(
                "The command \""
                    + this.toString()
                    + "\" returned exit code \""
                    + exitCode
                    + "\" when a value less than \""
                    + code
                    + "\" was expected.\n"
                    + "Stdout: "
                    + stdout
                    + "Stderr: "
                    + stderr);
        }
    }

    /**
     * Executes the command.
     */
    public Process execute() throws IOException {

        // Execute the command using the specified environment
        final Process proc = super.execute();

        // Capture the output of the command
        final BufferedReader stdoutStream = new BufferedReader(new InputStreamReader(
                proc.getInputStream()));
        final BufferedReader stderrStream = new BufferedReader(new InputStreamReader(
                proc.getErrorStream()));

        // Parse the stdout of the command
        String line;
        final StringBuilder buff = new StringBuilder();
        while ((line = stdoutStream.readLine()) != null) {
            stdoutLines.add(line);
            buff.append(line).append('\n');
        }
        stdout = buff.toString();

        // Parse the stderr of the command
        buff.setLength(0);
        while ((line = stderrStream.readLine()) != null) {
            stderrLines.add(line);
            buff.append(line).append('\n');
        }
        stderr = buff.toString();

        // Wait for the command to complete
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            LOG.error("Thread was interrupted while executing command \""
                    + this.toString() + "\".", e);
        }

        // Close down our streams
        stdoutStream.close();
        stderrStream.close();

        // Set the exit code
        exitCode = proc.exitValue();

        // Just to be compatible
        return proc;
    }
}
