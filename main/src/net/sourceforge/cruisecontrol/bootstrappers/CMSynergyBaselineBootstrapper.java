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
import net.sourceforge.cruisecontrol.publishers.CMSynergyBaselinePublisher;

import org.apache.log4j.Logger;

/**
 * The CMSynergyBaselineBootstrapper create a baseline prior to checking
 * for modifications. An example usage of this would be the following:
 * <ul>
 *  <li>CMSynergyBootstrap an Integration Testing project</li>
 *  <li>AntBootstrap a build of the Integration Testing Project</li>
 *  <li>CMSynergyBaselineBootstrap to create a system testing baseline</li>
 *  <li>CMSynergy Modificationset the System Testing project</li>
 *  <li>Build the System Testing Project</li>
 * </ul>
 * <p>
 * It's intended to functionally mirror the CMSynergyBaselinePublisher.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
public class CMSynergyBaselineBootstrapper extends CMSynergyBaselinePublisher implements Bootstrapper {

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
     * The file which contains the mapping between CM Synergy session names
     * and IDs.
     */
    private File sessionFile;

    /**
     * The given name of the CM Synergy session to use.
     */
    private String sessionName;
    
    /**
     * The name of the baseline
     */
    private String name;

    
    /**
     * The purpose of the baseline. Defaults to "Integration Testing"
     */
    private String purpose = "Integration Testing";
    
    /**
     * The description of the baseline
     */
    private String description;
    
    /**
     * The state of the created baseline
     */
    private String state = "published_baseline";
    
    /**
     * The value for the build attribute of the baseline
     */
    private String build;
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
    public void setCcmExe(final String ccmExe) {
        this.ccmExe = ccmExe;
    }

    /**
     * Sets the CM Synergy project you wish to reconfigure
     *
     * @param projectSpec
     *            The project spec (in 2 part name format).
     */
    public void setProject(final String projectSpec) {
        this.projectSpec = projectSpec;
    }
    
    /**
     * Sets the purpose of the baseline. Default is "Integration Testing".
     *
     * @param purpose The baseline's purpose
     */
    public void setPurpose(final String purpose) {
        this.purpose = purpose;
    }

    /**
     * Sets the name (version label) which will be given to the newly created
     * project versions. You may use macros to specify any of the
     * default properties set by CruiseControl (i.e. those which appear in the
     * info section of the log file).
     * <p>
     * example:
     * <br><br>
     * name="BUILD_@{cctimestamp}"
     *
     * @param name The name of the baseline
     */
    public void setBaselineName(final String name) {
        this.name = name;
    }

    /**
     * Sets the description of the baseline.
     *
     * @param description The description
     */
    public void setDescription(final String description) {
        this.description = description;
    }
    
    /**
     * Sets the build of the baseline.
     *
     * @param build The build number
     */
    public void setBuild(final String build) {
        this.build = build;
    }
    
    /**
     * Sets the state of the baseline.
     *
     * @param state The state (published_baseline, test_baseline, released)
     */
    public void setState(final String state) {
        this.state = state;
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
    public void setSessionFile(final String sessionFile) {
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
    public void setSessionName(final String sessionName) {
        this.sessionName = sessionName;
    }

    public void bootstrap() throws CruiseControlException {
        LOG.info("Creating baseline \"" + name + "\".");

        // Create a managed command line
        final ManagedCommandline cmd = CMSynergy.createCcmCommand(
                ccmExe, sessionName, sessionFile);
        cmd.createArgument("baseline");
        cmd.createArgument("-create");
        if (name != null) {
            cmd.createArgument(name);
        }
        if (description != null) {
            cmd.createArguments("-description", description);
        }
        cmd.createArguments("-release", getProjectRelease(projectSpec, sessionName));
        cmd.createArguments("-purpose", purpose);
        cmd.createArguments("-project", projectSpec);
        cmd.createArgument("-subprojects");
        
        final double version = getVersion();
        // If the build switch is available and the attribute is
        // set to a non-null value, use the build and state attribute values
        // in the baseline creation
        if (version >= 6.4 && build != null) {
            cmd.createArguments("-build", build);
        }
        if (version >= 6.4) {
            cmd.createArguments("-state", state);
        }
        // Create the baseline
        try {
            LOG.info("Creating Synergy baseline...");
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            final StringBuilder error = new StringBuilder(
                    "Failed to create intermediate baseline for project \"");
            error.append(getProject());
            error.append("\".");
            throw new CruiseControlException(error.toString(), e);
        }

        // Log the success
        final StringBuilder message = new StringBuilder("Created baseline");
        if (name != null) {
            message.append(" ").append(name);
        }
        message.append(".");
        LOG.info(message.toString());
    }
    
    

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Bootstrapper#validate()
     */
    public void validate() throws CruiseControlException {
        // We must know which project to reconfigure
        ValidationHelper.assertIsSet(projectSpec, "project", this.getClass());
    }
}
