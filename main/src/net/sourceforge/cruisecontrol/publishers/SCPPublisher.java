/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.Commandline.Argument;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Used to scp a file to a remote location
 *
 * @author <a href="orenmnero@sourceforge.net">Oren Miller</a>
 */

public class SCPPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(SCPPublisher.class);

    private String executableName = "scp";
    private String sourceUser;
    private String sourceHost;
    private String sourceDir = ".";
    private String targetUser;
    private String targetHost;
    private String targetDir = ".";
    private String ssh = "ssh";
    private String options;
    private String file;
    private String targetSeparator = File.separator;
    private String sourceSeparator = File.separator;

    public void setExecutableName(String executableName) {        
        this.executableName = executableName;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public void setSSH(String ssh) {
        this.ssh = ssh;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setTargetSeparator(String targetSeparator) {
        this.targetSeparator = targetSeparator;
    }
    
    public void setSourceSeparator(String sourceSeparator) {
        this.sourceSeparator = sourceSeparator;
    }

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(executableName, "executableName", this.getClass());
        ValidationHelper.assertNotEmpty(executableName, "executableName", this.getClass());
        ValidationHelper.assertFalse(sourceUser == null && sourceHost != null,
            "'sourceuser' not specified in configuration file");

        ValidationHelper.assertFalse(sourceHost == null && sourceUser != null,
            "'sourcehost' not specified in configuration file");

        ValidationHelper.assertFalse(targetUser == null && targetHost != null,
            "'targetuser' not specified in configuration file");

        ValidationHelper.assertFalse(targetHost == null && targetUser != null,
            "'targethost' not specified in configuration file");
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        boolean publishCurrentLogFile = file == null;
        if (publishCurrentLogFile) {
            file = getLogFileName(cruisecontrolLog);
            LOG.debug(file);
        }

        try {
            Commandline command = createCommandline(file);
            executeCommand(command);
        } finally {
            if (publishCurrentLogFile) {
                file = null;
            }
        }
    }

    protected String getLogFileName(Element cruisecontrolLog) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        return helper.getLogFileName();
    }

    protected void executeCommand(Commandline command) throws CruiseControlException {
        LOG.info("executing command: " + command);
        try {
            Process p = command.execute();
            LOG.debug("Runtime after.");
            p.waitFor();
            LOG.debug("waitfor() ended with exit code " + p.exitValue());

            try {
                BufferedReader commandErrorResult = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String outputLine;
                while ((outputLine = commandErrorResult.readLine()) != null) {
                    LOG.warn("Runtime.exec error returned: " + outputLine);
                }

            } catch (IOException e) {
                LOG.warn("Runtime.exec: reading errorStream failed");
                throw new CruiseControlException(e);
            }

        } catch (Exception e) {
            LOG.warn("Runtime.exec exception.");
            throw new CruiseControlException(e);
        }
    }

    protected Commandline createCommandline(String file) {
        String sourcefile = sourceSeparator + file;
        String targetfile = targetSeparator;

        Commandline command = new Commandline();
        command.setExecutable(executableName);
        command.createArgument().setLine(options);
        command.createArguments("-S", ssh);

        createFileArgument(
            command.createArgument(),
            sourceUser,
            sourceHost,
            sourceDir,
            sourcefile);

        createFileArgument(
            command.createArgument(),
            targetUser,
            targetHost,
            targetDir,
            targetfile);

        return command;

    }

    private void createFileArgument(
        Argument arg,
        String user,
        String host,
        String dir,
        String file) {

        String argValue = "";

        if (user != null && host != null) {
            argValue = user + "@" + host + ":";
        }

        argValue += dir;
        argValue += file;
        arg.setValue(argValue);
    }

}
