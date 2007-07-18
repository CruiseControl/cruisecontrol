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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * A simple utility class for obtaining and parsing system environment
 * variables. It has been tested on Windows 2000, Windows XP, Solaris, and
 * HP-UX, though it should work with any Win32 (95+) or Unix based palatform.
 *
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith </a>
 */
public class OSEnvironment {

    private static final Logger LOG = Logger.getLogger(OSEnvironment.class);

    /**
     * Internal representation of the system environment
     */
    private Properties variables = new Properties();

    /**
     * Constructor
     *
     * Creates an instance of OSEnvironment, queries the OS to discover it's
     * environment variables and makes them available through the getter methods
     */
    public OSEnvironment() {
        parse();
    }

    /**
     * Parses the OS environment and makes the environment variables available
     * through the getter methods
     */
    private void parse() {

        String command;

        // Detemine the correct command to run based on OS name
        String os = System.getProperty("os.name").toLowerCase();
        if (isWindows9x(os)) {
            command = "command.com /c set";
        } else if ((os.indexOf("nt") > -1) || (os.indexOf("windows") > -1)
                || (os.indexOf("os/2") > -1)) {
            command = "cmd.exe /c set";
        } else {
            // should work for just about any Unix variant
            command = "env";
        }

        // Get our environment
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.getOutputStream().close();

            // Capture the output of the command
            BufferedReader stdoutStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stderrStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Parse the output
            String line;
            String key = null;
            while ((line = stdoutStream.readLine()) != null) {
                int idx = line.indexOf('=');
                String value;
                if (idx == -1) {
                    if (key == null) {
                        continue;
                    }
                    // potential multi-line property. Let's rebuild it
                    value = variables.getProperty(key);
                    value += "\n" + line;
                } else {
                    key = line.substring(0, idx);
                    value = line.substring(idx + 1);
                }
                variables.setProperty(key, value);
            }

            // Close down our streams
            stdoutStream.close();
            stderrStream.close();

        } catch (Exception e) {
            LOG.error("Failed to parse the OS environment.", e);
        }
    }

    private boolean isWindows9x(String os) {
        return os.indexOf("windows 9") > -1;
    }

    /**
     * Gets the value of an environment variable. The variable name is case
     * sensitive.
     *
     * @param variable
     *            The variable for which you wish the value
     *
     * @return The value of the variable, or <code>null</code> if not found
     *
     * @see #getVariable(String variable, String defaultValue)
     */
    public String getVariable(String variable) {
        return variables.getProperty(variable);
    }

    /**
     * Gets the value of an environment variable. The variable name is case
     * sensitive.
     *
     * @param variable
     *            the variable for which you wish the value
     *
     * @param defaultValue
     *            The value to return if the variable is not set in the
     *            environment.
     *
     * @return The value of the variable. If the variable is not found, the
     *         defaultValue is returned.
     */
    public String getVariable(String variable, String defaultValue) {
        return variables.getProperty(variable, defaultValue);
    }

    /**
     * Gets the value of an environment variable. The variable name is NOT case
     * sensitive. If more than one variable matches the pattern provided, the
     * result is unpredictable. You are greatly encouraged to use
     * <code>getVariable()</code> instead.
     *
     * @param variable
     *            the variable for which you wish the value
     *
     * @see #getVariable(String variable)
     * @see #getVariable(String variable, String defaultValue)
     */
    public String getVariableIgnoreCase(String variable) {
        Enumeration keys = variables.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.equalsIgnoreCase(variable)) {
                return variables.getProperty(key);
            }
        }
        return null;
    }

    /**
     * Adds a variable to this representation of the environment. If the
     * variable already existed, the value will be replaced.
     *
     * @param variable
     *            the variable to set
     * @param value
     *            the value of the variable
     */
    public void add(String variable, String value) {
        variables.setProperty(variable, value);
    }

    /**
     * Returns all environment variables which were set at the time the class
     * was instantiated, as well as any which have been added programatically.
     *
     * @return a <code>List</code> of all environment variables. The
     *         <code>List</code> is made up of <code>String</code>s of the
     *         form "variable=value".
     *
     * @see #toArray()
     */
    public List getEnvironment() {
        List env = new ArrayList();
        Enumeration keys = variables.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            env.add(key + "=" + variables.getProperty(key));
        }
        return env;
    }

    /**
     * Returns all environment variables which were set at the time the class
     * was instantiated, as well as any which have been added programatically.
     *
     * @return a <code>String[]</code> containing all environment variables.
     *         The <code>String</code>s are of the form "variable=value".
     *         This is the format expected by
     *         <code>java.lang.Runtime.exec()</code>.
     *
     * @see java.lang.Runtime
     */
    public String[] toArray() {
        List list = getEnvironment();
        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Returns a <code>String<code> representation of the
     * environment.
     *
     * @return A <code>String<code> representation of the environment
     */
    public String toString() {
        return variables.toString();
    }
}
