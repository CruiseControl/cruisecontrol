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
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;

import org.apache.log4j.Logger;

/**
 * The CMSynergyBootstrapper will reconfigure the project (and
 * by default all subprojects) in order to pull in the latest changes.
 * <p>
 * If you do not wish to reconfigure subprojects, please set the
 * recurse attribute to false.
 * 
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
 */
public class CMSynergyBootstrapper implements Bootstrapper {

    /**
     * The CM Synergy executable used for executing commands. If not set,
     * we will use the default value "ccm".
     */
    private String ccmExe = "ccm";
    
    /**
     * The CM Synergy project spec (2 part name) of the project we will
     * use as a template to determine if any new tasks have been completed.
     */
    private String projectSpec = null;
    
    /**
     * If set to true, all subprojects will also be reconfigured.
     */
    private boolean recurse = true;
    
    /**
     * The ID of the CM Synergy ccmSession we will use to execute commands. If this
     * value is not set, we will defer the decision to the ccm client.
     */
    private String ccmSession = null;

    /** enable logging for this class */
    private static final Logger LOG = Logger.getLogger(CMSynergyBootstrapper.class);

    /**
     * Sets the name of the CM Synergy executable to use when issuing
     * commands.
     * 
     * @param ccmExe the name of the CM Synergy executable
     */
    public void setCcmExe(String ccmExe) {
        this.ccmExe = ccmExe;
    }
    
    /**
     * Sets the CM Synergy project you wish to reconfigure
     * 
     * @param projectSpec
     *            The project spec (in 2 part name format).
     */
    public void setProject(String projectSpec) {
        this.projectSpec = projectSpec;
    }
        
    /**
     * Sets the value of the recurse attribute. If set to true, all subprojects
     * will be reconfigured.
     * 
     * @param updateFolders
     */
    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }
    
    /**
     * Sets the CM Synergy ccmSession ID to use while executing ccm commands. If
     * this value is not set, we will defer the decision to the client.
     * 
     * @param ccmSession
     *            The ccmSession ID
     */
    public void setCcmSession(String session) {
        this.ccmSession = session;
    }
 
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Bootstrapper#bootstrap()
     */
    public void bootstrap() {

        LOG.info("Reconfiguring project \"" + projectSpec + "\".");

        // Create a managed command line
        ManagedCommandline cmd = new ManagedCommandline(ccmExe);
        cmd.createArgument().setValue("reconfigure");
        cmd.createArgument().setValue("-project");
        cmd.createArgument().setValue(projectSpec);
        if (recurse) {
            cmd.createArgument().setValue("-recurse");
        }
        
        // If we were given a ccmSession ID, use it
        if (ccmSession != null) {
            cmd.setVariable(CMSynergy.CCM_SESSION_VAR, ccmSession);
        }
                
        // execute
        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            LOG.error(
                "Could not reconfigure the project \"" + projectSpec + "\"." ,
                e);
        }
        LOG.debug(cmd.getStdoutAsString());
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Bootstrapper#validate()
     */
    public void validate() throws CruiseControlException {
        if (projectSpec == null) {
            throw new CruiseControlException("'project' is required for CMSynergyBootstrapper.");
        }
    }
}
