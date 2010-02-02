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
package net.sourceforge.cruisecontrol.publishers;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

/**
 * Creates an intermediate baseline encompassing the given project and all
 * subprojects. <br>
 * <b>Note: This publisher requires CM Synergy version 6.3 or later. </b>
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
public class CMSynergyBaselinePublisher extends CMSynergyPublisher {

    /**
     * The default CM Synergy project purpose for the baseline
     */
    private static final String CCM_BASELINE_PURPOSE = "Integration Testing";
    private static final String CCM_BASELINE_STATE = "published_baseline";

    private static final Logger LOG = Logger.getLogger(CMSynergyBaselinePublisher.class);
    private static final Pattern LOG_PROPERTY_PATTERN;
    private String purpose = CCM_BASELINE_PURPOSE;
    private String name;
    private String description;
    private String build;
    private String state = CCM_BASELINE_STATE;
    
    static {
        // Create a Perl 5 pattern matcher to find embedded properties
        final PatternCompiler compiler = new Perl5Compiler();
        try {
            //                                         1           2         3
            LOG_PROPERTY_PATTERN = compiler.compile("(.*)\\@\\{([^@{}]+)\\}(.*)");
        } catch (MalformedPatternException e) {
            // shouldn't happen
            LOG.fatal("Error compiling pattern for property matching", e);
            throw new IllegalStateException();
        }

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

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
     */
    public void publish(final Element log) throws CruiseControlException {

        // Only publish upon a successful build which includes new tasks.
        if (!shouldPublish(log)) {
            return;
        }

        // Extract the build properties from the log
        final Properties logProperties = getBuildProperties(log);
        

        // If a baseline name was provided, parse it
        String baselineName = null;
        if (name != null) {
            baselineName = parsePropertiesInString(name, logProperties);
        }

        // Create the CM Synergy command line
        final ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());

        cmd.createArgument("baseline");
        cmd.createArgument("-create");
        if (baselineName != null) {
            cmd.createArgument(baselineName);
        }
        if (description != null) {
            cmd.createArguments("-description", description);
        }
        cmd.createArguments("-release", getProjectRelease());
        cmd.createArguments("-purpose", purpose);
        cmd.createArguments("-project", getProject());
        cmd.createArgument("-subprojects");
        
        final double version = getVersion();
        // If the build switch is available and the attribute is
        // set to a non-null value, use the build and state attribute values
        // in the baseline creation
        if (version >= 6.4 && build != null) {
            String buildName = parsePropertiesInString(build, logProperties);
            cmd.createArguments("-build", buildName);
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
        if (baselineName != null) {
            message.append(" ").append(baselineName);
        }
        message.append(".");
        LOG.info(message.toString());
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#validate()
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getProject(), "project", this.getClass());
    }

    /**
     * Queries CM Synergy for the release value of the project
     *
     * @return The release value of the project.
     * @throws CruiseControlException if something breaks
     */
    private String getProjectRelease() throws CruiseControlException {

        // Create the CM Synergy command line
        final ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());
        cmd.createArgument("attribute");
        cmd.createArguments("-show", "release");
        cmd.createArguments("-project", getProject());

        final String release;
        try {
            cmd.execute();
            cmd.assertExitCode(0);
            release = cmd.getStdoutAsString().trim();
        } catch (Exception e) {
            throw new CruiseControlException(
                    "Could not determine the release value of project \""
                            + getProject() + "\".", e);
        }

        return release;
    }
    
    /**
     * Queries CM Synergy for the release value of the project
     *
     * @return The release value of the project.
     * @param project The 2-part project name
     * @param sessionName session name
     * @throws CruiseControlException if something breaks
     */
    protected String getProjectRelease(final String project, final String sessionName) throws CruiseControlException {

        // Create the CM Synergy command line
        final ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), sessionName, getSessionFile());
        cmd.createArgument("attribute");
        cmd.createArguments("-show", "release");
        cmd.createArguments("-project", project);

        final String release;
        try {
            cmd.execute();
            cmd.assertExitCode(0);
            release = cmd.getStdoutAsString().trim();
        } catch (Exception e) {
            throw new CruiseControlException(
                    "Could not determine the release value of project \""
                            + project + "\".", e);
        }

        return release;
    }
    
    

    /**
     * Parses a string by replacing all occurrences of a property macro with
     * the resolved value of the property (from the info section of the log
     * file). Nested macros are allowed - the
     * inner most macro will be resolved first, moving out from there.
     * <br/>
     * Macros are of the form @{property}, so that they will not conflict with
     * properties support built into CC.
     *
     * @param string The string to be parsed
     * @param buildProperties collection of properties in which macros are to be substituted
     * @return The parsed string
     */
    private String parsePropertiesInString(String string, final Properties buildProperties) {

        final PatternMatcher matcher = new Perl5Matcher();

        // Expand all (possibly nested) properties
        while (matcher.contains(string, LOG_PROPERTY_PATTERN)) {
            final MatchResult result = matcher.getMatch();
            final String pre = result.group(1);
            final String propertyName = result.group(2);
            final String post = result.group(3);
            String value = buildProperties.getProperty(propertyName);
            if (value == null) {
                LOG.warn("Could not resolve property \"" + propertyName
                        + "\".");
                value = "_";
            }
            string = pre + value + post;
        }

        return string;
    }
}
