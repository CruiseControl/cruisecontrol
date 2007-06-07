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

import java.io.File;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Simply updates the current workspace. Accepts specify branch/repository for
 * the update operation. Do the update forced is an option (false by default).
 *
 * @author <a href="mailto:rdealba@codicesoftware.com">Rubén de Alba</a>
 */
public class PlasticSCMBootstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(PlasticSCMBootstrapper.class);

    private String branch;
    private String wkspath;
    private String repository;
    private String pathtoupdate;
    private boolean forced = false;

    /**
     * Selects a workspace
     *
     * @param wkspath
     *          the path of the workspace to work in, in the local filesystem
     */
    public void setWkspath(String wkspath) {
        this.wkspath = wkspath;
    }
    /**
     * Selects a branch
     *
     * @param branch
     *          the branch from which to get the source.
     */
    public void setBranch (String branch) {
        this.branch = branch;
    }

    /**
     * Selects a repository
     *
     * @param repository
     *          the repository from which to get the source
     */    
    public void setRepository(String repository) {
        this.repository = repository;
    }
    
    public void setPathtoupdate (String pathtoupdate) {
        this.pathtoupdate = pathtoupdate;
    }

    /**
     * Enables/disables the forced update
     *
     * @param forced
     *          if true, "cm update" is run whith the "--forced" option
     */
    public void setForced (boolean forced) {
        this.forced = forced;
    } 
    
    /**
     * Update the workspace.
     */
    public void bootstrap() throws CruiseControlException {
        Commandline commandLine;

        try {
            if (branch != null) {
                commandLine = buildSwitchToBranchCommand();
                commandLine.executeAndWait(LOG); 
            }      
            commandLine = buildUpdateCommand();
            commandLine.executeAndWait(LOG);
        } catch (Exception e) {
            throw new CruiseControlException("Error updating PlasticSCM workspace", e);
        }
    }

    /**
     * Validate the attributes.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet (wkspath, "wkspath", this.getClass());

        File workingDir = new File(wkspath);
        ValidationHelper.assertTrue(workingDir.exists(),
                    "'wkspath' must be an existing directory. Was <" + wkspath + ">");
        ValidationHelper.assertTrue(workingDir.isDirectory(),
                    "'wkspath' must be an existing directory, not a file. Was <"
                    + wkspath + ">");
        
        if (repository != null) {
             ValidationHelper.assertIsDependentSet(repository, "repository", branch, "branch", this.getClass());
        }
    }         

    /**
     * Build the Plastic SCM update command.
     */
    protected Commandline buildUpdateCommand() throws CruiseControlException {
        Commandline commandLine = new Commandline();
        commandLine.setWorkingDirectory(wkspath);
        commandLine.setExecutable("cm");

        commandLine.createArgument("update");
        
        if (pathtoupdate != null) {
            commandLine.createArgument(pathtoupdate);
        } else {
            commandLine.createArgument(".");
        }
        if (forced) {
            commandLine.createArgument("--forced");
        }
        return commandLine;
    }
    /**
     * Build the Plastic SCM switchtobranch command.
     */
    protected Commandline buildSwitchToBranchCommand() throws CruiseControlException 
    {
        Commandline commandLine = new Commandline();
        commandLine.setWorkingDirectory(wkspath);
        commandLine.setExecutable("cm");

        commandLine.createArgument("stb");
        commandLine.createArgument(branch);
        if (repository != null) {
            commandLine.createArgument("-repository=".concat(repository));
        }
        commandLine.createArgument("--noupdate");
        return commandLine;
    }    

}
