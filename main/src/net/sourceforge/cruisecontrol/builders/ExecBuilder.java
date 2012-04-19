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

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;


/**
 * Exec builder class.
 *
 * Executes a command line as a builder and determines whether the command
 * was successful or not. A string can be supplied to additionally check for
 * certain error messages in the output
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecBuilder extends Builder {

    private static final long serialVersionUID = 895840816672824159L;

    static final Logger LOG = Logger.getLogger(ExecBuilder.class);

    private String command;
    private String args;
    private String workingDir;
    private String errorStr;
    private long timeout = ScriptRunner.NO_TIMEOUT;
    private Element buildLogElement; // global log to produce


    /*
     * validate the attributes for the plugin
     */
    public void validate() throws CruiseControlException {
        super.validate();

        // need at least an command argument
        ValidationHelper.assertIsSet(command, "command", this.getClass());

        // has the working directory been specified?
        if (this.workingDir == null) {
            // NO: use the Java temp directory
            this.workingDir = System.getProperty("java.io.tmpdir");
        }

    } // validate

    /**
     * execute the command and return the results as XML
     */
    public Element build(final Map<String, String> buildProperties, final Progress progressIn)
            throws CruiseControlException {
        return build(buildProperties, progressIn, null);
    } // build

    /**
     * Execute the command and return the results as XML. The script may optionally be fed up
     * by data through its STDIN, and its STDOUT may optionally be stored in for later use.
     * In any case, STDOUT and STDERR of the script are read and print to log as well as checked
     * against the error string set by {@link #setErrorStr(String)}. They are also included in
     * the XML element with the build result.
     *
     * @param buildProperties see {@link Builder#build(Map, Progress)}
     * @param progressIn see {@link Builder#build(Map, Progress)}
     * @param stdinProvider Optional script input provider. If set, data read from it are passed into the STDIN
     *        of the script.
     * @return the result of the command.
     */
    Element build(final Map<String, String> buildProperties, final Progress progressIn,
            final InputStream stdinProvider) {

        final Progress progress = getShowProgress() ? progressIn : null;
        final OSEnvironment buildEnv = new OSEnvironment();
        // time the command started
        final long startTime = System.currentTimeMillis();

        // Merge the environment with the configuration
        mergeEnv(buildEnv);

        buildLogElement = new Element("build");

        // setup script handler
        final ExecScript script = createExecScript();
        script.setExecCommand(this.command);
        script.setExecArgs(substituteProperties(buildProperties, this.args));
        script.setExecEnv(buildEnv);
        script.setErrorStr(this.errorStr);
        script.setProgress(progress);

        final String projectName = buildProperties.get(Builder.BUILD_PROP_PROJECTNAME);

        // mimic Ant target/task logging
        final Element task = script.setBuildLogHeader(buildLogElement);
        script.setBuildLogElement(task);

        // execute the command
        final ScriptRunner scriptRunner = createScriptRunner();
        boolean scriptCompleted = false;
        boolean scriptIOError = false;
        try {
            scriptCompleted = runScript(script, scriptRunner, workingDir, projectName, stdinProvider);
        } catch (CruiseControlException ex) {
          LOG.error("Could not execute command: " + command, ex);
          scriptIOError = true;
        }

        // set the time it took to exec command
        final long endTime = System.currentTimeMillis();
        buildLogElement.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));

        // did the exec fail in anyway?
        if (scriptIOError) {
            final StringBuilder message = new StringBuilder("Could not execute command: " + command);
            if (args == null) {
                message.append(" with no arguments");
            } else {
                message.append(" with arguments: ").append(args);
            }
            // YES: could fin or execute command
            LOG.warn(message.toString());
            synchronized (buildLogElement) {
                buildLogElement.setAttribute("error", "exec error");
                final Element msg = new Element("message");
                msg.addContent(new CDATA(message.toString()));
                msg.setAttribute("priority", "error");
                task.addContent(msg);
            }
        } else if (script.wasError()) {
            // YES: detected the error string in the command output
            synchronized (buildLogElement) {
                LOG.warn("Detected error string string in build output");
                buildLogElement.setAttribute("error", "error string found");
                final Element msg = new Element("message");
                msg.addContent(new CDATA("Detected error string: " + errorStr));
                msg.setAttribute("priority", "error");
                task.addContent(msg);
            }
        } else if (!scriptCompleted) {
            // YES: timeout was exceeded
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            synchronized (buildLogElement) {
                buildLogElement.setAttribute("error", "build timeout");
            }
        } else if (script.getExitCode() != 0) {
            // YES: the command returned non-zero value
            LOG.warn("Exec return code is " + script.getExitCode());
            synchronized (buildLogElement) {
                buildLogElement.setAttribute("error", "return code is " + script.getExitCode());
            }
        }

        script.flushCurrentElement();
        return buildLogElement;
    } // build

    boolean runScript(final ExecScript script, final ScriptRunner scriptRunner, final String dir,
                                final String projectName, final InputStream stdinProvider)
      throws CruiseControlException {
        final File workDir = new File(dir);
        return scriptRunner.runScript(workDir, script, timeout, stdinProvider,
                getBuildOutputConsumer(projectName, workDir, null));
    }

    private String substituteProperties(final Map<String, String> properties, final String string) {
        String value = string;
        try {
            value = Util.parsePropertiesInString(properties, string, false);
        } catch (CruiseControlException e) {
            LOG.error("exception substituting properties into arguments: " + string, e);
        }
        return value;
    }

    ExecScript createExecScript() {
        return new ExecScript();
    }

    ScriptRunner createScriptRunner() {
        return new ScriptRunner();
    }

    public Element buildWithTarget(final Map<String, String> properties, final String target, final Progress progress)
            throws CruiseControlException {

        final String origArgs = args;
        try {
            args = target;
            return build(properties, progress);
        } finally {
            args = origArgs;
        }
    }

    /**
     * Sets build timeout in seconds.
     * @param timeout long build timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    } // setTimeout

    /**
     * Gets the timeout set by {@link #setTimeout(long)}, or {@link ScriptRunner#NO_TIMEOUT}
     * if not set yet.
     * @return the build timeout in seconds.
     */
    public long getTimeout() {
        return this.timeout;
    } // getTimeout

    /**
     * Sets the command to execute
     * @param cmd the command to execute
     */
    public void setCommand(String cmd) {
        this.command = cmd;
    } // setCommand

    /**
     * Gets the command to execute.
     * @return the command to execute.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the arguments for the command to execute
     * @param args arguments for the command to execute
     */
    public void setArgs(String args) {
        this.args = args;
    } // setArgs

    /**
     * Gets the arguments for the command to execute
     * @return the arguments for the command
     */
    public String getArgs() {
        return this.args;
    } // getArgs

    /**
     * Sets the error string to search for in the command output
     * @param errStr the error string to search for in the command output
     */
    public void setErrorStr(String errStr) {
        this.errorStr = errStr;
    } // setErrorStr

    /**
     * Sets the working directory where the command is to be executed
     * @param dir the directory where the command is to be executed
     */
    public void setWorkingDir(String dir) {
        this.workingDir = dir;
    } // setWorkingDir

    /**
     * Gets the working directory set by {@link #setWorkingDir(String)}, or
     * <code>null</code> if not set yet.
     * @return the working directory where the command is to be executed.
     */
    public String getWorkingDir() {
        return this.workingDir;
    } // getworkingDir


    /**
     * Get whether there was n error written to the build log
     * @return the error string otherwise null
     */
    public String getBuildError() {
        if (this.buildLogElement.getAttribute("error") != null) {
            return this.buildLogElement.getAttribute("error").getValue();
        } else {
            return "none";
        }
    } // getBuildError

} // ExecBuilder
