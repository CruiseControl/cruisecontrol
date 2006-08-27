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

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamConsumer;


/**
 * Exec script class.
 *
 * Script support to execute a command and logs the results.
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ExecScript implements Script, StreamConsumer {
    private static final Logger LOG = Logger.getLogger(ExecScript.class);
     
    private String execCommand;    
    private String execArgs;
    private String errorStr;
    private int exitCode;
    private boolean foundError = false;
    private Element buildLogElement;
    private Element currentElement = null;

    /**
     * construct the command that we're going to execute.
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
            StringTokenizer stok = new StringTokenizer(execArgs, " \t\r\n");
            while (stok.hasMoreTokens()) {
                cmdLine.createArgument(stok.nextToken());
            }
        }

        // log the command if debug is enabled
        if (LOG.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Executing Command: ");
            String[] args = cmdLine.getCommandline();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                sb.append(arg);
                sb.append(" ");
            }
            LOG.debug(sb.toString());
        }
        return cmdLine;
    } // buildCommandLine

    /**
     * Ugly parsing of Exec output into some Elements.
     * Gets called from StreamPumper.
     * @param line the line of output to parse
     */
    public synchronized void consumeLine(String line) {
        if (line == null || line.length() == 0 || buildLogElement == null) {
            return;
        }

        synchronized (buildLogElement) {
            // check if the output contains the error string   
            if (errorStr != null) {
                // YES: set error flag
                if (line.indexOf(errorStr) >= 0) {    
                    foundError = true;
                }
            } else {
                // NO: just write the ouput to the log
                Element msg = new Element("message");
                msg.addContent(new CDATA(line));
                msg.setAttribute("priority", "info");
                if (currentElement == null) {
                    buildLogElement.addContent(msg);
                } else {
                    currentElement.addContent(msg);
                }
            }
        }
    } // consumeLine
    
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
                    List lst = currentElement.getChildren("message");
                    if (lst != null) {
                        Iterator it = lst.iterator();
                        while (it.hasNext()) {
                            Element msg = (Element) it.next();
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
     * set the "header" for this part of the build log.
     * turns it into an Ant target/task style element for reporting purposes
     * @param buildLogElement the element of the build log
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
     * @param execArgs The execArgs to set.
     */
    public void setExecArgs(String execArgs) {
        this.execArgs = execArgs;
    } // setExecArgs
    
    /**
     * @param execCommand The execCommand to set.
     */
    public void setExecCommand(String execCommand) {
        this.execCommand = execCommand;
    } // setExecCommand
    
    /**
     * @return returns the exitcode of the command
     */
    public int getExitCode() {
        return exitCode;
    } // getExitCode
    
    /**
     * @param exitCode the exit code value to set.
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    } // setExitCode
    
    /**
     * @param errStr the error string to search for
     */
    public void setErrorStr(String errStr) {
        this.errorStr = errStr;
    } // setErrorStr
    
    /**
     * @param buildLogElement The buildLogElement to set.
     */
    public void setBuildLogElement(Element buildLogElement) {
        this.buildLogElement = buildLogElement;
    } // setBuildLogElement
    
    /**
     * @return true if error occurred, else false
     */
    public boolean wasError() {
        return this.foundError;
    } // wasError
    
} // ExecScript
