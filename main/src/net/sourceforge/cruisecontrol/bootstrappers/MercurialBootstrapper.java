/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
 * The MercurialBootstrapper will handle updating the local working repository before the build begins.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @see <a href="http://www.selenic.com/mercurial">Mercurial web site</a>
 */
public class MercurialBootstrapper implements Bootstrapper {
    private static final Logger LOG = Logger.getLogger(SVNBootstrapper.class);

    /** Configuration parameters */
    private String localWorkingCopy;

    /**
     * Sets the local working copy to use when making calls to Mercurial.
     *
     * @param localWorkingCopy
     *            String indicating the relative or absolute path to the local working copy of the Mercurial repository
     *            on which to execute the update command.
     */
    public void setLocalWorkingCopy(String localWorkingCopy) {
        this.localWorkingCopy = localWorkingCopy;
    }

    /**
     * This method validates that at least the filename or the local working copy location has been specified.
     *
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *             Thrown when the repository location and the local working copy location are both null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(localWorkingCopy != null,
                "'localWorkingCopy' is a required attribute on the Mercurial bootstrapper task");

        if (localWorkingCopy != null) {
            File workingDir = new File(localWorkingCopy);
            ValidationHelper.assertTrue(workingDir.exists() && workingDir.isDirectory(),
                    "'localWorkingCopy' must be an existing directory. Was" + workingDir.getAbsolutePath());
        }
    }

    /**
     * Update from the mercurial repository.
     *
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     */
    public void bootstrap() throws CruiseControlException {
        buildPullUpdateCommand().executeAndWait(LOG);
    }

    /**
     * Generates the command line for the mercurial pull update command.
     *
     * For example:
     *
     * 'hg pull -u'
     *
     * @return the command line for the mercurial pull update command
     * @throws net.sourceforge.cruisecontrol.CruiseControlException
     *             if the working directory is not valid.
     */
    Commandline buildPullUpdateCommand() throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable("hg");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("pull");
        command.createArgument("-u");

        LOG.debug("Executing command = " + command);

        return command;
    }
}