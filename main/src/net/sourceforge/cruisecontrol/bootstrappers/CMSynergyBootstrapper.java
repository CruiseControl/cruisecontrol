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
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * The CMSynergyBootstrapper will reconfigure the project (and
 * by default all subprojects) in order to pull in the latest changes.
 * <p>
 * If you do not wish to reconfigure subprojects, please set the
 * recurse attribute to false.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
public class CMSynergyBootstrapper implements Bootstrapper {

    /**
     * The CM Synergy executable used for executing commands. If not set,
     * we will use the default value "ccm".
     */
    private String ccmExe;

    /**
     * The CM Synergy project spec (2 part name) of the project we will
     * use as a template to determine if any new tasks have been completed.
     */
    private String projectSpec;

    /**
     * If set to true, all subprojects will also be reconfigured.
     */
    private boolean recurse = true;

    /**
     * The file which contains the mapping between CM Synergy session names
     * and IDs.
     */
    private File sessionFile;

    /**
     * The given name of the CM Synergy session to use.
     */
    private String sessionName;

    /**
     * The logger for this class
     */
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
     * @param recurse
     */
    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }

    /**
     * Sets the file which contains the mapping between CM Synergy session names
     * and IDs. This file should be in the standard properties file format. Each
     * line should map one name to a CM Synergy session ID (as returned by the
     * "ccm status" command).
     * <p>
     * example:
     * <br><br>
     * session1=localhost:65024:192.168.1.17
     *
     * @param sessionFile
     *            The session file
     */
    public void setSessionFile(String sessionFile) {
        this.sessionFile = new File(sessionFile);
    }

    /**
     * Sets the name of the CM Synergy session to use with this plugin. This
     * name should appear in the specified session file.
     *
     * @param sessionName
     *            The session name
     *
     * @see #setSessionFile(String)
     */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public void bootstrap() throws CruiseControlException {
        LOG.info("Reconfiguring project \"" + projectSpec + "\".");

        // Create a managed command line
        ManagedCommandline cmd = CMSynergy.createCcmCommand(
                ccmExe, sessionName, sessionFile);
        cmd.createArgument("reconfigure");
        cmd.createArguments("-project", projectSpec);
        if (recurse) {
            cmd.createArgument("-recurse");
        }

        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            throw new CruiseControlException("Could not reconfigure the project \"" + projectSpec + "\".", e);
        }
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Bootstrapper#validate()
     */
    public void validate() throws CruiseControlException {
        // We must know which project to reconfigure
        ValidationHelper.assertIsSet(projectSpec, "project", this.getClass());
    }
}
