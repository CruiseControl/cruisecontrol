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
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

import java.io.File;

/**
 * Since we rely on our build.xml to handle updating our source code, there has
 * always been a problem with what happens when the build.xml file itself
 * changes.  Previous workarounds have included writing a wrapper build.xml that
 * will check out the "real" build.xml.  This class is a substitute for that
 * practice.
 *
 * The CVSBootstrapper will handle updating a single file from CVS before the
 * build begins.
 *
 * Usage:
 *
 *     &lt;cvsbootstrapper cvsroot="" file=""/&gt;
 *
 */
public class CVSBootstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(CVSBootstrapper.class);

    private String localWorkingCopy;
    private String filename;
    private String cvsroot;
    private boolean resetStickyTags = false;
    private boolean overwriteChanges = false;

    public void setCvsroot(String cvsroot) {
        this.cvsroot = cvsroot;
    }

    public void setFile(String filename) {
        this.filename = filename;
    }

    /**
     * Sets the local working copy to use when making calls to CVS.
     *
     *@param local String relative or absolute path to the local
     *      working copy of the CVS module which contains the target file.
     */
    public void setLocalWorkingCopy(String local) {
        localWorkingCopy = local;
    }

    /**
     *  Update the specified file.
     * @throws CruiseControlException 
     */
    public void bootstrap() throws CruiseControlException {
        try {
            Commandline commandLine = buildUpdateCommand();
            Process p = commandLine.execute();
            StreamConsumer infoConsumer = new StreamConsumer() {
                public void consumeLine(String line) {
                    LOG.warn(line);
                }
            };
            StreamConsumer warnConsumer = new StreamConsumer() {
                public void consumeLine(String line) {
                    LOG.warn(line);
                }
            };
            StreamPumper errorPumper =
                new StreamPumper(p.getErrorStream(), null, warnConsumer);
            StreamPumper outPumper = new StreamPumper(p.getInputStream(), null, infoConsumer);
            Thread errorPumperThread = new Thread(errorPumper);
            Thread outPumperThread = new Thread(outPumper);
            errorPumperThread.start();
            outPumperThread.start();
            p.waitFor();
            errorPumperThread.join();
            outPumperThread.join();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (Exception e) {
            throw new CruiseControlException("Error executing CVS update command", e);
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(filename != null || cvsroot != null || localWorkingCopy != null,
            "at least one of 'file', 'cvsroot' or 'localworkingcopy' is required as an attribute for CVSBootstrapper");

        if (localWorkingCopy != null) {
            File workingDir = new File(localWorkingCopy);

            ValidationHelper.assertTrue(workingDir.exists(),
                        "'localWorkingCopy' must be an existing directory. Was <"
                        + localWorkingCopy + ">");
            ValidationHelper.assertTrue(workingDir.isDirectory(),
                        "'localWorkingCopy' must be an existing directory, not a file. Was <"
                        + localWorkingCopy + ">");
        }
    }

    protected Commandline buildUpdateCommand() throws CruiseControlException {
        Commandline commandLine = new Commandline();

        if (localWorkingCopy != null) {
            commandLine.setWorkingDirectory(localWorkingCopy);
        }        

        commandLine.setExecutable("cvs");

        if (cvsroot != null) {
            commandLine.createArgument().setValue("-d");
            commandLine.createArgument().setValue(cvsroot);
        }
        commandLine.createArgument().setValue("update");
        
        StringBuffer flags = new StringBuffer("-dP");
        if (resetStickyTags) {
            flags.append("A");
        }
        if (overwriteChanges) {
            flags.append("C");
        }
        commandLine.createArgument().setValue(flags.toString());

        if (filename != null) {
            commandLine.createArgument().setValue(filename);
        }

        return commandLine;
    }

    public void setResetStickyTags(boolean reset) {
        resetStickyTags = reset;
    }

    public void setOverwriteChanges(boolean overwrite) {
      overwriteChanges = overwrite;
    }

}
