/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
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
    private String compression;

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
            commandLine.executeAndWait(LOG);
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

        if (compression != null) {
            ValidationHelper.assertIntegerInRange(compression, 0, 9,
                    "'compression' must be an integer between 0 and 9, inclusive.");
        }
    }

    protected Commandline buildUpdateCommand() throws CruiseControlException {
        Commandline commandLine = new Commandline();

        if (localWorkingCopy != null) {
            commandLine.setWorkingDirectory(localWorkingCopy);
        }

        commandLine.setExecutable("cvs");

        if (compression != null) {
            commandLine.createArgument("-z" + compression);    
        }

        if (cvsroot != null) {
            commandLine.createArguments("-d", cvsroot);
        }
        commandLine.createArgument("update");

        StringBuffer flags = new StringBuffer("-dP");
        if (resetStickyTags) {
            flags.append("A");
        }
        if (overwriteChanges) {
            flags.append("C");
        }
        commandLine.createArgument(flags.toString());

        if (filename != null) {
            commandLine.createArgument(filename);
        }

        return commandLine;
    }

    public void setResetStickyTags(boolean reset) {
        resetStickyTags = reset;
    }

    public void setOverwriteChanges(boolean overwrite) {
      overwriteChanges = overwrite;
    }

    /**
     * Sets the compression level used for the call to cvs, corresponding to the "-z" command line parameter. When not
     * set, the command line parameter is NOT included.
     * 
     * @param level Valid levels are 1 (high speed, low compression) to 9 (low speed, high compression), or 0
     * to disable compression.
     */
    public void setCompression(String level) {
        compression = level;
    }
}
