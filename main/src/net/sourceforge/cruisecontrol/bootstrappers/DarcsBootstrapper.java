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
 * The DarcsBootstrapper will handle updating a Darcs repository before the build begins.
 *
 * Implementation is based on SVNBootstrapper and Darcs (Modificationset).
 *
 * Based on GitBootstrapper, as it has a similar behaviour.
 *
 * @see <a href="http://darcs.net/">darcs.net</a>
 * @author <a href="mailto:flo@andersground.net">Florian Gilcher</a>
 */
 
public class DarcsBootstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(DarcsBootstrapper.class);

    /** Configuration parameters */
    private String darcsBinary = "darcs";
    private String workingDir;
    private String repositoryLocation;
    private String remoteRepositoryLocation;

    /**
     * @param darcsBinary the Darcs binary to use.
     */
    public void setDarcsBinary(final String darcsBinary) {
        this.darcsBinary = darcsBinary;
    }
    
    /**
     * @param workingDir the working directory to execute the command in
     */
    public void setWorkingDir(final String workingDir) {
        this.workingDir = workingDir;
    }
    
    /**
     * @param repositoryLocation the location of the repository to pull _into_.
     */
    public void setRepositoryLocation(final String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }
    
    /**
     * @param remoteRepositoryLocation the location of the repository to pull from.
     */
    public void setRemoteRepositoryLocation(final String remoteRepositoryLocation) {
        this.remoteRepositoryLocation = remoteRepositoryLocation;
    }

    /**
     * This method validates that one of the local repository or the working dir has 
     * been specified. Also ensures that 
     *
     * @throws CruiseControlException
     *             Thrown when the repository location and the local working copy location are both null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(repositoryLocation != null || workingDir != null,
                "At least 'repositoryLocation' or 'workingDir' are "
                        + "required attributes on the Darcs bootstrapper task");

        if (repositoryLocation != null) {
            File repos = new File(repositoryLocation);
            ValidationHelper.assertTrue(repos.exists() && repos.isDirectory(),
                    "'repositoryLocation' must be an existing directory. Was" + repos.getAbsolutePath());
        }
    }

    /**
     * Update the Darcs repository.
     *
     * @throws CruiseControlException
     */
    public void bootstrap() throws CruiseControlException {
        buildUpdateCommand().executeAndWait(LOG);
    }

    /**
     * Generates the command line for the svn update command.
     *
     * For example:
     *
     * 'darcs pull --dont-allow-conflicts'
     *
     * Conflicts are not allowed.
     *
     * @return the command line for the darcs pull command
     * @throws CruiseControlException
     *             if the working directory is not valid.
     */
    Commandline buildUpdateCommand() throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable(darcsBinary);
        
        if (workingDir != null) {
            command.setWorkingDirectory(workingDir);
        }
        
        command.setExecutable(darcsBinary);
        command.createArgument("pull");
        
        if (repositoryLocation != null) {
            command.createArgument("--repodir");
            command.createArgument(repositoryLocation);
        }
        
        if (remoteRepositoryLocation != null) {
            command.createArgument("--remote-repo");
            command.createArgument(remoteRepositoryLocation);
        }
        
        command.createArgument("--dont-allow-conflicts");
        command.createArgument("--all");        
        
        LOG.debug("DarcsBootstrapper: Executing command = " + command);

        return command;
    }
}
