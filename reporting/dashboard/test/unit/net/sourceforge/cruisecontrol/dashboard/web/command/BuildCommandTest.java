/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.dashboard.web.command;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Collection;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class BuildCommandTest extends MockObjectTestCase {
    public void testBuildCommandShouldTakeBuildAsContructor() {
        Map props = new HashMap();
        props.put("projectname", "project 1");
        props.put("logfile", new File("log19991212050505.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build, null);
        assertEquals("project 1", command.getBuild().getProjectName());
        assertFalse(command.getBuild().hasPassed());
    }

    public void testShouldReturnClassNameAsLongFailedWhenBuildIsMoreThanFailed24HoursAgo() {
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build, null);
        BuildSummary buildSummary =
                new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.PASSED, "");
        command.updateCssLevel(buildSummary);
        assertEquals("failed", command.getCssClassName());
        assertEquals("8", command.getLevel());
    }

    public void testShouldReturnClassNameAsFailedWhenBuildIsLessThanFailed24HoursAgo() {
        String dateBuildSummary =
                new SimpleDateFormat("yyyy-MM-dd HH:mm.ss", Locale.ENGLISH).format(new Date());
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build, null);
        BuildSummary buildSummary = new BuildSummary("", dateBuildSummary, "", ProjectBuildStatus.PASSED, "");
        command.updateCssLevel(buildSummary);
        assertEquals("failed", command.getCssClassName());
        assertEquals("0", command.getLevel());
    }

    public void testShouldReturnClassNameAsFailedWhenBuildNeverPassed() {
        Map props = new HashMap();
        props.put("logfile", new File("log20051209122103.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build, null);
        command.updateCssLevel(null);
        assertEquals("failed", command.getCssClassName());
        assertEquals("8", command.getLevel());
    }

    public void testShouldNotReturnDarkRedWhenBuildIsPassed() {
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505Lbuild.9.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand buildCommand = new BuildCommand(build, null);
        assertEquals("passed", buildCommand.getCssClassName());
    }

    public void testCassNameShouldBeLevel8WhenTheLatestSuccessfulBuildIs24HoursAgo() throws Exception {
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505Lbuild.9.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand buildCommand = new BuildCommand(build, null);
        BuildSummary buildSummary =
                new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.PASSED, "");
        buildCommand.updateCssLevel(buildSummary);
        assertEquals("passed", buildCommand.getCssClassName());
        assertEquals("8", buildCommand.getLevel());
    }

    public void testShouldBeAbleToDelegateTheInvocationToBuildSummary() throws Exception {
        Build summary = new BuildSummary("project1", "", "", ProjectBuildStatus.PASSED, "");
        BuildCommand command = new BuildCommand(summary, null);
        assertEquals("project1", command.getBuild().getProjectName());
    }

    public void testCalculatesElapsedBuildTime() throws Exception {
        Build buildSummary = new BuildSummary("", "", "", ProjectBuildStatus.PASSED, "");
        buildSummary.updateStatus("now building since 20070420170000");
        BuildCommand command = new BuildCommand(buildSummary, null);
        Long elapsedSeconds =
                command.getElapsedTimeBuilding(CCDateFormatter.format("2007-04-20 18:00:00",
                        "yyyy-MM-dd HH:mm:ss"));
        assertEquals(new Long(3600), elapsedSeconds);
    }

    public void testShouldClassNameAsDrakRedWhenBuildIsFailed24HoursAgo() {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.FAILED, "");
        Build lastSuccessful = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.PASSED, "");
        BuildCommand command = new BuildCommand(buildSummary, null);
        command.updateCssLevel(lastSuccessful);
        assertEquals("failed", command.toJsonHash().get("css_class_name"));
        assertEquals("8", command.toJsonHash().get(BuildCommand.CSS_LEVEL));
    }

    public void testShouldClassNameAsFailedWhenBuildIsLessThanFailed24HoursAgo() {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss", Locale.ENGLISH).format(new Date());
        Build currentBuildSummary = new BuildSummary("", dateStr, "", ProjectBuildStatus.FAILED, "");
        Build lastSuccessfualBuild = new BuildSummary("", dateStr, "", ProjectBuildStatus.PASSED, "");

        BuildCommand command = new BuildCommand(currentBuildSummary, null);
        Map json = command.toJsonHash();
        command.updateCssLevel(lastSuccessfualBuild);
        assertEquals("failed", json.get("css_class_name"));
        assertEquals("0", json.get(BuildCommand.CSS_LEVEL));
    }

    public void testJsonHashShouldNotReturnDarkRedWhenBuildIsPassed() {
        Build buildSummary = new BuildSummary("", "2005-12-07 12:21.10", "", ProjectBuildStatus.PASSED, "");
        BuildCommand command = new BuildCommand(buildSummary, null);
        BuildSummary lastSuccessful =
                new BuildSummary("", "2005-12-07 12:21.10", "", ProjectBuildStatus.PASSED, "");
        command.updateCssLevel(lastSuccessful);
        Map json = command.toJsonHash();
        assertEquals("passed", json.get("css_class_name"));
        assertEquals("8", json.get(BuildCommand.CSS_LEVEL));
    }

    public void testJsonHashShouldNotReturnCurrentStatusWhenLastSuccessfulBuildIsEmpty() {
        Build buildSummary = new BuildSummary("", "2005-12-07 12:21.10", "", ProjectBuildStatus.FAILED, "");
        BuildCommand command = new BuildCommand(buildSummary, null);
        command.updateCssLevel(null);
        Map json = command.toJsonHash();
        assertEquals("failed", json.get("css_class_name"));
        assertEquals("8", json.get(BuildCommand.CSS_LEVEL));
    }

    public void testJsonHashShouldReturnBuildSinceForActiveBuild() throws Exception {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.FAILED, "");
        buildSummary.updateStatus("now building since 19990420170000");
        BuildCommand command = new BuildCommand(buildSummary, null);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertTrue(StringUtils.contains((String) json.get("latest_build_date"), "1999"));
        assertEquals("building", json.get("css_class_name"));
    }

    public void testJsonHashShouldReturnLowerCaseOfStatusWhenInvokeDetaultCss() throws Exception {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.INACTIVE, "");
        BuildCommand command = new BuildCommand(buildSummary, null);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertEquals("inactive", json.get("css_class_name"));
    }

    public void testJsonHashShouldNotReturnBuildSinceForNonActiveBuild() throws Exception {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.FAILED, "");
        buildSummary.updateStatus("");
        BuildCommand command = new BuildCommand(buildSummary, null);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertTrue(StringUtils.contains((String) json.get("latest_build_date"), "2005"));
    }

    public void testShouldReturnUnknownIfDurationIsNull() throws Exception {
        Mock build = mock(Build.class);
        build.expects(atLeastOnce()).method("getStatus").will(returnValue(ProjectBuildStatus.FAILED));
        build.expects(atLeastOnce()).method("getDuration").will(returnValue(null));
        BuildCommand command = new BuildCommand((Build) build.proxy(), null);

        assertEquals("Unknown", command.getDuration());
    }
    
    public void testShouldReturnCurrentStatus() throws Exception {
        Mock build = mock(Build.class);
        build.expects(atLeastOnce()).method("getStatus").will(returnValue(ProjectBuildStatus.PASSED));
        BuildCommand command = new BuildCommand((Build) build.proxy(), null);
        command.updateBuildingCss(null);
        assertEquals("passed", command.getCssClassName());
    }
    
    public void testShouldReturnBuildingFailedIfCurrentStatusIsBuildingAndLastStatusIsFailed() throws Exception {
        Mock build = mock(Build.class);
        build.expects(atLeastOnce()).method("getStatus").will(returnValue(ProjectBuildStatus.BUILDING));
        BuildCommand command = new BuildCommand((Build) build.proxy(), null);
        command.updateBuildingCss(ProjectBuildStatus.FAILED);
        assertEquals("building_failed", command.getCssClassName());
    }

    public void testShouldReturnEmptyCollectionWhenNoModificationsAvailableFromLogFile() throws Exception {
        Mock build = mock(Build.class);
        build.expects(atLeastOnce()).method("getStatus").will(returnValue(ProjectBuildStatus.BUILDING));
        build.expects(atLeastOnce()).method("getModificationSet").will(returnValue(null));
        BuildCommand command = new BuildCommand((Build) build.proxy(), null);
        final Collection modifications = command.getModifications();
        assertTrue("Modifications should be empty", modifications.isEmpty());
    }
}
