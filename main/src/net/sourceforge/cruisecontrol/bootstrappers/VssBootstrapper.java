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
import net.sourceforge.cruisecontrol.sourcecontrols.VSSHelper;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class VssBootstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(VssBootstrapper.class);

    private String ssDir;
    private String serverPath;

    private String vssPath;
    private String localDirectory;
    private String login;

    public void bootstrap() throws CruiseControlException {
        String commandLine = generateCommandLine();

        try {
            String[] env = VSSHelper.loadVSSEnvironment(serverPath);

            Process p = Runtime.getRuntime().exec(commandLine, env);
            InputStream errorIn = p.getErrorStream();
            PrintWriter errorOut = new PrintWriter(System.err, true);
            StreamPumper errorPumper = new StreamPumper(errorIn, errorOut);
            new Thread(errorPumper).start();
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (IOException ex) {
            LOG.debug("exception trying to exec ss.exe", ex);
            throw new CruiseControlException(ex);
        } catch (InterruptedException ex) {
            LOG.debug("interrupted during get", ex);
            throw new CruiseControlException(ex);
        }
    }

    public void validate() throws CruiseControlException {
        if (vssPath == null || localDirectory == null) {
            throw new CruiseControlException("VssBootstrapper has required attributes vssPath and localDirectory");
        }
        File localDirForFile = new File(localDirectory);
        boolean dirExists = localDirForFile.exists();
        if (!dirExists) {
            LOG.debug("local directory [" + localDirectory + "] does not exist");
            throw new CruiseControlException(
                "file path attribute value "
                    + localDirectory
                    + " must specify an existing directory.");
        }
        boolean isDir = localDirForFile.isDirectory();
        if (!isDir) {
            LOG.debug("local directory [" + localDirectory + "] is not a directory");
            throw new CruiseControlException(
                "file path attribute value "
                    + localDirectory
                    + " must specify an existing directory, not a file.");
        }
        setLocalDirectory(localDirForFile.getAbsolutePath());
    }

    String generateCommandLine() {
        StringBuffer commandLine = new StringBuffer();
        final String backslash = "\\";
        // optionally prefix the executable
        if (ssDir != null) {
            commandLine.append(ssDir).append(ssDir.endsWith(backslash) ? "" : backslash);
        }
        final String quote = "\"";
        commandLine.append("ss.exe get ");
        // check for leading "$", to be argument-compatible with other tasks
        if (vssPath != null) {
            String pathPrefix = vssPath.startsWith("$") ? "" : "$";
            commandLine.append(quote + pathPrefix + vssPath + quote);
        }
        commandLine.append(" -GL");
        commandLine.append(quote + localDirectory + quote);
        commandLine.append(" -I-N");
        if (login != null) {
            commandLine.append(" -Y" + login);
        }

        return commandLine.toString();
    }

    /**
     * Required.
     * @param vssPath fully qualified VSS path to the file ($/Project/subproject/filename.ext)
     */
    public void setVssPath(String vssPath) {
        this.vssPath = vssPath;
    }

    /***
     * Optional.
     * @param ssDir Path to the directory containing ss.exe. Assumes that ss.exe is in the path by default.
     */
    public void setSsDir(String ssDir) {
        this.ssDir = ssDir;
    }

    /**
     * Optional.
     * @param serverPath The path to the directory containing the srcsafe.ini file.
     */
    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    /**
     * Required.
     * @param localDirectory fully qualified path for the destination directory (c:\directory\subdirectory\)
     */
    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    /**
     * Optional.
     * @param login vss login information in the form username,password\
     */
    public void setLogin(String login) {
        this.login = login;
    }
}