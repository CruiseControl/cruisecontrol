/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Maven builder class.
 *
 * Attempts to mimic the behavior of Ant builds, at least as far as CC is
 * concerned. Basically it's a (heavily) edited version of AntBuilder.
 * No style at all, but serves its purpose. :)
 *
 * @author <a href="mailto:fvancea@maxiq.com">Florin Vancea</a>
 */
public class MavenBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(MavenBuilder.class);

    private String projectFile;
    private String goal;
    private String mavenScript;
    private long timeout = ScriptRunner.NO_TIMEOUT;

    public void validate() throws CruiseControlException {
        super.validate();

        ValidationHelper.assertIsSet(mavenScript, "mavenScript", this.getClass());

        ValidationHelper.assertIsSet(goal, "goal", this.getClass());
        if (getGoalSets().isEmpty()) {
            ValidationHelper.assertIsSet(null, "goal", this.getClass());
        }
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {

        File ckFile = new File(mavenScript);
        ValidationHelper.assertTrue(ckFile.exists(),
            "Script " + ckFile.getAbsolutePath() + " does not exist");

        ValidationHelper.assertIsSet(projectFile, "projectFile", this.getClass());
        ckFile = new File(projectFile);
        ValidationHelper.assertTrue(ckFile.exists(),
            "Project descriptor " + ckFile.getAbsolutePath() + " does not exist");

        File workingDir = (new File(projectFile)).getParentFile();

        long startTime = System.currentTimeMillis();

        Element buildLogElement = new Element("build");

        List runs = getGoalSets();
        for (int runidx = 0; runidx < runs.size(); runidx++) {
            String goalset = (String) runs.get(runidx);
            MavenScript script = new MavenScript();
            script.setGoalset(goalset);
            script.setBuildProperties(buildProperties);
            script.setMavenScript(mavenScript);
            script.setProjectFile(projectFile);
            script.setBuildLogElement(buildLogElement);
            ScriptRunner scriptRunner = new ScriptRunner();
            boolean scriptCompleted = scriptRunner.runScript(workingDir, script, timeout);
            script.flushCurrentElement();

            if (!scriptCompleted) {
                LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
                buildLogElement = new Element("build");
                buildLogElement.setAttribute("error", "build timeout");
            } else if (script.getExitCode() != 0) {
                // The maven.bat actually never returns error,
                // due to internal cleanup called after the execution itself...
                synchronized (buildLogElement) {
                    buildLogElement.setAttribute("error", "Return code is " + script.getExitCode());
                }
            }

            if (buildLogElement.getAttribute("error") != null) {
                break;
            }

        }

        long endTime = System.currentTimeMillis();

        buildLogElement.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));
        return buildLogElement;
    }

    public Element buildWithTarget(Map properties, String target) throws CruiseControlException {
        String origGoal = goal;
        try {
            goal = target;
            return build(properties);
        } finally {
            goal = origGoal;
        }
    }

    //***************************** Param setters ****************************

    /**
     * The path to the Maven script
     * (i.e. the maven.bat file, because I tested this only on w2k)
     */
    public void setMavenScript(String mavenScript) {
        this.mavenScript = mavenScript;
    }

    /**
     * Maven goal to run. Supports sets of goals.
     * Examples:
     * "clean java:compile" will run 'clean' then 'java:compile' in one single invocation of Maven
     * "clean jar:jar|site:generate" will run 'clean' and 'jar:jar' in one invocation,
     *    then 'site:generate' in _another_ invocation. Useful for updating from SCM
     *    with Maven goals, then doing the actual build with freshly loaded files.
     *    Notice the '|' as separator of sets.
     */
    public void setGoal(String goal) {
        this.goal = goal;
    }

   /**
     * project.xml to use
     */
    public void setProjectFile(String projectFile) {
        this.projectFile = projectFile;
    }

    //******************** Command line generation **********************

    /**
     * Produces sets of goals, ready to be run each in a distinct call to Maven.
     * Separation of sets in "goal" attribute is made with '|'.
     *
     * @return a List containing String elements
     */
    protected List getGoalSets() {
        List al = new ArrayList();
        if (goal != null) {
            StringTokenizer stok = new StringTokenizer(goal, "|");
            while (stok.hasMoreTokens()) {
                String subSet = stok.nextToken().trim();
                if (subSet == null || subSet.length() == 0) {
                    continue;
                }
                al.add(subSet);
            }
        }
        return al;
    }

    /**
     * Sets build timeout in seconds.
     *
     * @param timeout
     *            long build timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
