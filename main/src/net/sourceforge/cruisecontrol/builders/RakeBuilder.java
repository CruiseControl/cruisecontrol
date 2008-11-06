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

package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Rake builder class based on the Ant and Exec builder classes.
 * <br />
 * Attempts to mimic the behavior of Ant builds using Ruby Rake.
 *
 *
 * @author Kirk Knoernschild - Provided without any warranty
 */
public class RakeBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(RakeBuilder.class);

    private String workingDir = null;
    private String buildFile = "rakefile.rb";
    private String target = "";
    private long timeout = ScriptRunner.NO_TIMEOUT;
    private boolean wasValidated = false;

    public void validate() throws CruiseControlException {
        super.validate();

        ValidationHelper.assertIsSet(buildFile, "buildfile", this.getClass());
        ValidationHelper.assertIsSet(target, "target", this.getClass());

        wasValidated = true;
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(final Map<String, String> buildProperties, final Progress progressIn)
            throws CruiseControlException {
        
        if (!wasValidated) {
            throw new IllegalStateException("This builder was never validated."
                 + " The build method should not be getting called.");
        }

        validateBuildFileExists();

        final Progress progress = getShowProgress() ? progressIn : null;

        Element buildLogElement = new Element("build");
        final RakeScript script = this.getRakeScript();
        script.setBuildLogHeader(buildLogElement);
        script.setWindows(Util.isWindows());
        script.setBuildFile(buildFile);
        script.setTarget(target);
        script.setProgress(progress);
        long startTime = System.currentTimeMillis();

        final File workDir = workingDir != null ? new File(workingDir) : null;
        final boolean scriptCompleted = new ScriptRunner().runScript(workDir, script, timeout);
        final long endTime = System.currentTimeMillis();
        if (!scriptCompleted) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            buildLogElement = new Element("build");
            buildLogElement.setAttribute("error", "build timeout");
         } else if (script.getExitCode() != 0) {
            synchronized (buildLogElement) {
                buildLogElement.setAttribute("error", "Return code is " + script.getExitCode());
            }
         }

        buildLogElement.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));
        return buildLogElement;
    }

    public Element buildWithTarget(final Map<String, String> properties, final String buildTarget,
                                   final Progress progress)
            throws CruiseControlException {
        
        final String origTarget = target;
        try {
            target = buildTarget;
            return build(properties, progress);
        } finally {
            target = origTarget;
        }
    }

    void validateBuildFileExists() throws CruiseControlException {
        File build = new File(buildFile);
        if (!build.isAbsolute() && workingDir != null) {
            build = new File(workingDir, buildFile);
        }
        ValidationHelper.assertExists(build, "buildfile", this.getClass());
    }


    /**
     * Set the working directory where Rake will be invoked. This parameter gets
     * set in the XML file via the workingDir attribute. The directory can
     * be relative (to the cruisecontrol current working directory) or absolute.
     *
     * @param dir
     *          the directory to make the current working directory.
     */
    public void setWorkingDir(String dir) {
        workingDir = dir;
    }

    /**
     * Set the Rake target(s) to invoke.
     *
     * @param target the target(s) name.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Sets the name of the build file that Rake will use.  The Rake default is
     * rakefile or Rakefile. f the rakefile is not found in the current directory,
     * rake will search parent directories for a match. The directory where the
     * Rakefile is found will become the current directory for the actions executed
     * in the Rakefile. Use this to set the rakefile for the build.
     *
     * @param buildFile the name of the build file.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    protected RakeScript getRakeScript() {
        return new RakeScript();
    }
}
