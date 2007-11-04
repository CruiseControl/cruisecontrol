/*****************************************************************************
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
 *****************************************************************************/
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

import java.io.File;

/**
 * The GitBootstrapper will handle updating a git repository before the build begins.
 *
 * @see <a href="http://git.or.cz/">git.or.cz</a>
 * @author <a href="rschiele@gmail.com">Robert Schiele</a>
 */
public class GitBootstrapper implements Bootstrapper {
    private static final Logger LOG = Logger.getLogger(GitBootstrapper.class);

    private String lwc;

    /**
     * Sets the local working copy to use when making calls to git.
     *
     * @param d
     *            String indicating the relative or absolute path to the local working copy of the git repository on
     *            which to execute the pull command.
     */
    public void setLocalWorkingCopy(String d) {
        lwc = d;
    }

    /**
     * This method validates that the local working copy location has been specified.
     *
     * @throws CruiseControlException
     *             Thrown when the local working copy location is null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(lwc != null, "'localWorkingCopy' is a required "
                + "attribute on the Git bootstrapper " + "task");

        final File wd = new File(lwc);
        ValidationHelper.assertTrue(wd.exists() && wd.isDirectory(), "'localWorkingCopy' must be an existing "
                + "directory. Was" + wd.getAbsolutePath());
    }

    /**
     * Update the git repository.
     *
     * @throws CruiseControlException
     */
    public void bootstrap() throws CruiseControlException {
        final Commandline cmd = new Commandline();
        cmd.setExecutable("git");
        cmd.setWorkingDirectory(lwc);
        cmd.createArgument("pull");

        cmd.executeAndWait(LOG);
    }
}
