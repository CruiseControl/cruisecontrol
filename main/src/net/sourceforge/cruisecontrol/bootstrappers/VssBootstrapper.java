/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.EnvCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

public class VssBootstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(VssBootstrapper.class);

    private String ssDir;
    private String serverPath;

    private String vssPath;
    private String localDirectory;
    private String login;

    public void bootstrap() throws CruiseControlException {
        generateCommandLine().executeAndWait(LOG);
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(vssPath != null && localDirectory != null,
                "VssBootstrapper has required attributes vssPath and localDirectory");

        File localDirForFile = new File(localDirectory);
        ValidationHelper.assertTrue(localDirForFile.exists(), "file path attribute value " + localDirectory
                + " must specify an existing directory.");

        ValidationHelper.assertTrue(localDirForFile.isDirectory(), "file path attribute value " + localDirectory
                + " must specify an existing directory, not a file.");

        setLocalDirectory(localDirForFile.getAbsolutePath());
    }

    private String getExecutable() {
        String executable = "";
        if (ssDir != null) {
            executable = ssDir;
            if (ssDir.charAt(ssDir.length() - 1) != '\\') {
                executable += '\\';
            }
        }
        executable += "ss.exe";
        return executable;
    }

    Commandline generateCommandLine() {
        EnvCommandline command = new EnvCommandline();
        command.setExecutable(getExecutable());
        command.createArgument("get");

        if (serverPath != null) {
            command.setVariable("SSDIR", serverPath);
        }

        // check for leading "$", to be argument-compatible with other tasks
        if (vssPath != null) {
            String pathPrefix = vssPath.startsWith("$") ? "" : "$";
            command.createArgument('"' + pathPrefix + vssPath + '"');
        }
        command.createArgument("-GL\"" + localDirectory + '"');
        command.createArgument("-I-N");
        if (login != null) {
            command.createArgument("-Y" + login);
        }

        return command;
    }

    /**
     * Required.
     *
     * @param vssPath
     *            fully qualified VSS path to the file ($/Project/subproject/filename.ext)
     */
    public void setVssPath(String vssPath) {
        this.vssPath = vssPath;
    }

    /**
     * Optional.
     *
     * @param ssDir
     *            Path to the directory containing ss.exe. Assumes that ss.exe is in the path by default.
     */
    public void setSsDir(String ssDir) {
        this.ssDir = ssDir;
    }

    /**
     * Optional.
     *
     * @param serverPath
     *            The path to the directory containing the srcsafe.ini file.
     */
    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    /**
     * Required.
     *
     * @param localDirectory
     *            fully qualified path for the destination directory (c:\directory\subdirectory\)
     */
    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    /**
     * Optional.
     *
     * @param login
     *            vss login information in the form username,password\
     */
    public void setLogin(String login) {
        this.login = login;
    }
}
