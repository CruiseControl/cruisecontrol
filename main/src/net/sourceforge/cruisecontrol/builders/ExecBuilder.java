/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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

import java.util.Map;
import java.io.File;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

/**
 * Exec builder class.
 *
 * Executes a command line as a builder and determines whether the command
 * was succesful or not. A string can be supplied to additionally check for
 * certain error messages in the output
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(ExecBuilder.class);

    private String command;
    private String args;
    private String workingDir;
    private String errorStr;
    private long timeout = ScriptRunner.NO_TIMEOUT;
    private Element buildLogElement = null; // global log to produce

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
    public Element build(Map buildProperties) throws CruiseControlException {

        // time the command started
        long startTime = System.currentTimeMillis();

        buildLogElement = new Element("build");

        // setup script handler
        ExecScript script = new ExecScript();
        script.setExecCommand(this.command);
        script.setExecArgs(this.args);
        script.setErrorStr(this.errorStr);
        //script.setBuildProperties(buildProperties); - currently ignored

        // mimic Ant target/task logging
        Element task = script.setBuildLogHeader(buildLogElement);
        script.setBuildLogElement(task);
        
        // execute the command
        ScriptRunner scriptRunner = new ScriptRunner();
        boolean scriptCompleted = false;
        boolean scriptIOError = false;
        try {
            scriptCompleted = scriptRunner.runScript(new File(this.workingDir), script, timeout);
        } catch (CruiseControlException ex) {
          scriptIOError = true;
        }

        // set the time it took to exec command
        long endTime = System.currentTimeMillis();
        buildLogElement.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));
        
        // did the exec fail in anyway?
        if (scriptIOError) {
            // YES: could fin or execute command
            LOG.warn("Could not execute command: " + command + " " + args);
            synchronized (buildLogElement) {
                buildLogElement.setAttribute("error", "exec error"); 
                Element msg = new Element("message");
                msg.addContent(new CDATA("Could not execute command: " + command + " " + args));
                msg.setAttribute("priority", "error");
                task.addContent(msg); 
            }
        } else if (script.wasError()) {
            // YES: detected the error string in the command output
            synchronized (buildLogElement) {
                LOG.warn("Detected error string string in build output");
                buildLogElement.setAttribute("error", "error string found");
                Element msg = new Element("message");
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


    public Element buildWithTarget(Map properties, String target) throws CruiseControlException {
        String origArgs = args;
        try {
            args = target;
            return build(properties);
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
     * Sets the command to execute
     * @param cmd the command to execute
     */
    public void setCommand(String cmd) {
        this.command = cmd;
    } // setCommand
    
    /**
     * Sets the arguments for the command to execute
     * @param args arguments for the command to execute
     */
    public void setArgs(String args) {
        this.args = args;
    } // setArgs
    
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
