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

import java.util.StringTokenizer;

import org.jdom.Element;
import org.jdom.CDATA;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamConsumer;

/**
 * Rake script class.
 *
 * Contains all the details related to running a Rake based build.
 * @author Kirk Knoernschild
 */
public class RakeScript implements Script, StreamConsumer {

    private boolean isWindows;
    private String args;
    private String buildFile = null;
    private String target = "";
    private int exitCode;
    private Element buildLogElement;


    /**
     * construct the command that we're going to execute.
     *
     * @return Commandline holding command to be executed
     * @throws CruiseControlException on unquotable attributes
     */
    public Commandline buildCommandline() throws CruiseControlException {
        Commandline cmdLine = new Commandline();

         if (isWindows) {
          //cmdLine.setExecutable("cmd /c rake");
             cmdLine.setExecutable("cmd");
             cmdLine.createArgument().setValue("/c");
             cmdLine.createArgument().setValue("rake");
         } else {
             //does this work for *nix? Needs to be tested.
             cmdLine.setExecutable("rake");
         }
         if (args != null) {
             StringTokenizer stok = new StringTokenizer(args, " \t\r\n");
             while (stok.hasMoreTokens()) {
                 cmdLine.createArgument().setValue(stok.nextToken());
             }
         }

        if (buildFile != null) {
            cmdLine.createArgument().setValue("-f");
            cmdLine.createArgument().setValue(buildFile);
        }

        StringTokenizer targets = new StringTokenizer(target);
        while (targets.hasMoreTokens()) {
            cmdLine.createArgument().setValue(targets.nextToken());
        }

        return cmdLine;
    }
     /**
     * set the "header" for this part of the build log.
     * @param buildLogElement the element of the build log
     * @return updated element
     */
    public void setBuildLogHeader(Element buildLogElement) {
        this.buildLogElement = buildLogElement;
    }

    /**
     * @param args The args to set.
     */
    public void setArgs(String args) {
        this.args = args;
    }

    /**
     * @param isWindows The isWindows to set.
     */
    public void setWindows(boolean isWindows) {
        this.isWindows = isWindows;
    }

    /**
     * @param buildFile The buildFile to set.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }
    /**
     * @param target The target to set.
     */
    public void setTarget(String target) {
        this.target = target;
    }
    /**
     * @return Returns the exitCode.
     */
    public int getExitCode() {
        return exitCode;
    }
    /**
     * @param exitCode The exitCode to set.
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

     /**
      * Ugly parsing of Rake output into some Elements.
      * Gets called from StreamPumper.
      * @param the line of output to parse
      */
    public synchronized void consumeLine(String line) {
        if (line == null || line.length() == 0 || buildLogElement == null) {
            return;
        }
        synchronized (buildLogElement) {
            if (line.startsWith("rake aborted!")) {
                buildLogElement.setAttribute("error", "BUILD FAILED detected");
            } else {
                Element msg = new Element("message");
                msg.addContent(new CDATA(line));
                msg.setAttribute("priority", "info");
                buildLogElement.addContent(msg);
            }
        }
    }
}
