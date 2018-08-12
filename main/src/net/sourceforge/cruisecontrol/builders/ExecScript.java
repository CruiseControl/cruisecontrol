/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.StreamConsumer;

import org.apache.log4j.Logger;
import org.jdom2.CDATA;
import org.jdom2.Element;

/**
 * Exec script class. Script support to execute a command and logs the results.
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecScript implements Script, StreamConsumer {
    private static final Logger LOG = Logger.getLogger(ExecScript.class);

    private String execCommand;
    private String execArgs;
    private String errorStr;
    private OSEnvironment execEnv;
    private Progress progress;
    private int exitCode;
    private boolean foundError;
    private Element buildLogElement;
    private Element currentElement;

    /**
     * construct the command that we're going to execute.
     *
     * @return Commandline holding command to be executed
     * @throws CruiseControlException
     */
    public Commandline buildCommandline() throws CruiseControlException {
        Commandline cmdLine = new Commandline();

        // make sure we have a command
        if (execCommand != null) {
            cmdLine.setExecutable(execCommand);
        } else {
            throw new CruiseControlException("no command to be executed");
        }

        // add the arguments if necessary
        if (execArgs != null) {
            cmdLine.addArguments(Commandline.translateCommandline(execArgs));
        }
        // add the environment values if necessary
        if (execEnv != null) {
            cmdLine.setEnv(this.execEnv);
        }


        // log the command if debug is enabled
        if (LOG.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Executing Command: ");
            final String[] args = cmdLine.getCommandline();
            for (final String arg : args) {
                sb.append(arg);
                sb.append(" ");
            }
            LOG.debug(sb.toString());
        }
        return cmdLine;
    } // buildCommandLine

    /**
     * StreamConsumer.consumeLine(String), Called from StreamPumper.
     *
     * @param line
     *            the line of output to parse
     */
    public synchronized void consumeLine(final String line) {
        if (line == null || line.length() == 0 || buildLogElement == null) {
            return;
        }

        final Element message;
        final String messageLevel;
        if (errorStr != null && line.contains(errorStr)) {
            foundError = true;
            messageLevel = "error";
            message = messageFromLine(line, messageLevel);
        } else {
            messageLevel = "info";
            message = messageFromLine(line, messageLevel);
        }

        synchronized (buildLogElement) {
            if (currentElement == null) {
                buildLogElement.addContent(message);
            } else {
                currentElement.addContent(message);
            }
        }

        if (progress != null) {
            progress.setValue(line);
        }
    }

    private Element messageFromLine(final String line, String level) {
        final Element msg = new Element("message");
        msg.addContent(new CDATA(line));
        msg.setAttribute("priority", level);
        return msg;
    }

    /**
     * flush the current log element
     */
    protected void flushCurrentElement() {
        if (buildLogElement == null) {
            return;
        }
        synchronized (buildLogElement) {
            if (currentElement != null) {
                if (buildLogElement.getAttribute("error") != null) {
                    // All the messages of the last (failed) goal should be
                    // switched to priority error
                    final List lst = currentElement.getChildren("message");
                    if (lst != null) {
                        for (final Object aLst : lst) {
                            final Element msg = (Element) aLst;
                            msg.setAttribute("priority", "error");
                        }
                    }
                }
                buildLogElement.addContent(currentElement);
            }
            currentElement = null;
        }
    } // flushCurrentElement

    /**
     * set the "header" for this part of the build log. turns it into an Ant target/task style element for reporting
     * purposes
     *
     * @param buildLogElement
     *            the element of the build log
     * @return updated element
     */
    public Element setBuildLogHeader(Element buildLogElement) {
        Element target = new Element("target");
        target.setAttribute("name", "exec");
        buildLogElement.addContent(target);
        Element task = new Element("task");
        task.setAttribute("name", this.execCommand);
        target.addContent(task);
        return task;
    } // setBuildLogHeader

    /**
     * @param execArgs
     *            The execArgs to set.
     */
    public void setExecArgs(String execArgs) {
        this.execArgs = execArgs;
    } // setExecArgs

    /**
     * @param execCommand
     *            The execCommand to set.
     */
    public void setExecCommand(String execCommand) {
        this.execCommand = execCommand;
    } // setExecCommand

    /**
     * @param env
     *            The environment variables of the script, or <code>null</code> if to
     *            inherit the environment of the current process
     */
    public void setExecEnv(final OSEnvironment env) {
        this.execEnv = env;
    } // setExecEnv

    /**
     * @return returns the exitcode of the command
     */
    public int getExitCode() {
        return exitCode;
    } // getExitCode

    /**
     * @param exitCode
     *            the exit code value to set.
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    } // setExitCode

    /**
     * @param errStr
     *            the error string to search for
     */
    public void setErrorStr(String errStr) {
        this.errorStr = errStr;
    } // setErrorStr

    /**
     * @param buildLogElement
     *            The buildLogElement to set.
     */
    public void setBuildLogElement(Element buildLogElement) {
        this.buildLogElement = buildLogElement;
    } // setBuildLogElement

    /** @param progress The progress callback object to set. */
    public void setProgress(final Progress progress) {
        this.progress = progress;
    }

    /**
     * @return true if error occurred, else false
     */
    public boolean wasError() {
        return this.foundError;
    } // wasError

} // ExecScript
