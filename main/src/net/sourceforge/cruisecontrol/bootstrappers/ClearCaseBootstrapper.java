/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Logger;

import java.io.PrintWriter;

/**
 * Since we rely on our build.xml to handle updating our source code, there has
 * always been a problem with what happens when the build.xml file itself
 * changes.  Previous workarounds have included writing a wrapper build.xml that
 * will check out the "real" build.xml.  This class is a substitute for that
 * practice.
 *
 * The ClearCaseBootstrapper will handle updating a single file from
 * ClearCase before the build begins.
 *
 * Usage:
 *
 *     &lt;clearcasebootstrapper file="" viewpath=""/&gt;
 *
 */
public class ClearCaseBootstrapper implements Bootstrapper {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(ClearCaseBootstrapper.class);

    private String filename;
    private String viewpath;

    public void setViewpath(String path) {
        viewpath = path;
    }

    public void setFile(String name) {
        filename = name;
    }

    /**
     *  Update the specified file.
     */
    public void bootstrap() {
        Commandline commandLine = buildUpdateCommand();
        Process p = null;

        log.debug("Executing: " + commandLine.toString());
        try {
            p = Runtime.getRuntime().exec(commandLine.getCommandline());
            StreamPumper errorPumper =
                new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
            new Thread(errorPumper).start();
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (Exception e) {
            log.error("Error executing ClearCase update command", e);
        }
    }

    public void validate() throws CruiseControlException {
        if (filename == null) {
            throw new CruiseControlException("'file' is required for ClearCaseBootstrapper");
        }
    }

    protected Commandline buildUpdateCommand() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");

        commandLine.createArgument().setValue("update");
        commandLine.createArgument().setValue("-force");
        commandLine.createArgument().setValue("-log");
        commandLine.createArgument().setValue(isWindows() ? "NUL" : "/dev/nul");
        commandLine.createArgument().setValue(getFullPathFileName());

        return commandLine;
    }

    private String getFullPathFileName() {
        return viewpath == null
            ? filename
            : new StringBuffer(viewpath).append("/").append(filename).toString();
    }

    protected boolean isWindows() {
        return getOsName().indexOf("Windows") >= 0;
    }

    protected String getOsName() {
        return System.getProperty("os.name");
    }

    /** for testing */
    public static void main(String[] args) {
        ClearCaseBootstrapper bootstrapper = new ClearCaseBootstrapper();
        bootstrapper.setViewpath("C:/views/integration_view/project");
        bootstrapper.setFile("build.xml");
        bootstrapper.bootstrap();
    }

}