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
 * Creates an intermediate baseline encompasing the given project and all
 * subprojects. <br>
 * <b>Note: This publisher requires CM Synergy version 6.3 or later. </b>
 * 
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
public class CMSynergyBaselinePublisher extends CMSynergyPublisher {
    
    /**
     * The default CM Synergy project purpose for the baseline
     */
    public static final String CCM_BASELINE_PURPOSE = "Integration Testing";
    
    private static final Logger LOG = Logger.getLogger(CMSynergyBaselinePublisher.class);
    private static final Pattern LOG_PROPERTY_PATTERN;
    private String purpose = CCM_BASELINE_PURPOSE;
    private String name;
    private String description;
    
    static {
        // Create a Perl 5 pattern matcher to find embedded properties
        PatternCompiler compiler = new Perl5Compiler();
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
    public void setPurpose(String purpose) {
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
    public void setBaselineName(String name) {
        this.name = name;
    }
    
    /**
     * Sets the description of the baseline.
     * 
     * @param description The description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
     */
    public void publish(Element log) throws CruiseControlException { 
        
        // Only publish upon a successful build which includes new tasks.
        if (!shouldPublish(log)) {
            return;
        }
                
        // Extract the build properties from the log
        Properties logProperties = getBuildProperties(log);
        
        // If a baseline name was provided, parse it
        String baselineName = null;
        if (name != null) {
            baselineName = parsePropertiesInString(name, logProperties);
        }

        // Create the CM Synergy command line
        ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());
        
        cmd.createArgument().setValue("baseline");
        cmd.createArgument().setValue("-create");
        if (baselineName != null) {
            cmd.createArgument().setValue(baselineName);
        }
        if (description != null) {
            cmd.createArgument().setValue("-description");
            cmd.createArgument().setValue(description);
        }
        cmd.createArgument().setValue("-release");
        cmd.createArgument().setValue(getProjectRelease());
        cmd.createArgument().setValue("-purpose");
        cmd.createArgument().setValue(purpose);
        cmd.createArgument().setValue("-project");
        cmd.createArgument().setValue(getProject());
        cmd.createArgument().setValue("-subprojects");

        // Create the baseline
        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            StringBuffer error = new StringBuffer(
                    "Failed to create intermediate baseline for project \"");
            error.append(getProject());
            error.append("\".");
            throw new CruiseControlException(error.toString(), e);
        }
        
        // Log the success
        StringBuffer message = new StringBuffer("Created baseline");
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
     */
    private String getProjectRelease() throws CruiseControlException {
        String release;
        
        // Create the CM Synergy command line
        ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());
        cmd.createArgument().setValue("attribute");
        cmd.createArgument().setValue("-show");
        cmd.createArgument().setValue("release");
        cmd.createArgument().setValue("-project");
        cmd.createArgument().setValue(getProject());

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
     * Parses a string by replacing all occurences of a property macro with
     * the resolved value of the property (from the info section of the log
     * file). Nested macros are allowed - the 
     * inner most macro will be resolved first, moving out from there.
     * <br/>
     * Macros are of the form @{property}, so that they will not conflict with
     * properties support built into CC. 
     *  
     * @param string The string to be parsed
     * @return The parsed string
     */
    private String parsePropertiesInString(String string, Properties buildProperties) {

        PatternMatcher matcher = new Perl5Matcher();

        // Expand all (possibly nested) properties
        while (matcher.contains(string, LOG_PROPERTY_PATTERN)) {
            MatchResult result = matcher.getMatch();
            String pre = result.group(1);
            String propertyName = result.group(2);
            String post = result.group(3);
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
