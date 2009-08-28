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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.LogFile;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

public class BuildCommandTest extends MockObjectTestCase {
    private LogFile logFile;

    private LogFile passedLogFile;

    protected void setUp() throws Exception {
        super.setUp();
        logFile = new LogFile("log19991212050505.xml");
        passedLogFile = new LogFile("log19991212050505Lbuild.9.xml");
    }

    public void testBuildCommandShouldTakeBuildAsContructor() {
        Map props = new HashMap();
        props.put("projectname", "project 1");
        BuildDetail build = new BuildDetail(logFile, props);
        BuildCommand command = new BuildCommand(build, null);
        assertEquals("project 1", command.getBuild().getProjectName());
        assertFalse(command.getBuild().hasPassed());
    }

    public void testShouldReturnClassNameAsLongFailedWhenBuildIsMoreThanFailed24HoursAgo() {
        BuildDetail build = new BuildDetail(logFile);
        BuildCommand command = new BuildCommand(build, null);
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        command.updateCssLevel(buildSummary);
        assertEquals("8", command.getLevel());
    }

    public void testShouldReturnClassNameAsFailedWhenBuildIsLessThanFailed24HoursAgo() {
        BuildDetail build = new BuildDetail(logFile);
        BuildCommand command = new BuildCommand(build, null);
        String logFileName = "log" + CCDateFormatter.yyyyMMddHHmmss(new DateTime()) + ".xml";
        BuildSummary buildSummary = new BuildSummary("", PreviousResult.PASSED, logFileName);
        command.updateCssLevel(buildSummary);
        assertEquals("0", command.getLevel());
    }

    public void testShouldReturnClassNameAsFailedWhenBuildNeverPassed() {
        BuildDetail build = new BuildDetail(new LogFile("log20051209122103.xml"));
        BuildCommand command = new BuildCommand(build, null);
        command.updateCssLevel(null);
        assertEquals("8", command.getLevel());
    }

    public void testCassNameShouldBeLevel8WhenTheLatestSuccessfulBuildIs24HoursAgo() throws Exception {
        BuildDetail build = new BuildDetail(passedLogFile);
        BuildCommand buildCommand = new BuildCommand(build, null);
        DateTime buildDate = new DateTime().minusYears(2);
        String logFileName = "log" + CCDateFormatter.yyyyMMddHHmmss(buildDate) + ".xml";
        BuildSummary buildSummary = new BuildSummary("", PreviousResult.PASSED, logFileName);
        buildCommand.updateCssLevel(buildSummary);
        assertEquals("8", buildCommand.getLevel());
    }

    public void testShouldBeAbleToDelegateTheInvocationToBuildSummary() throws Exception {
        Build summary = new BuildSummary("project1", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        BuildCommand command = new BuildCommand(summary, null);
        assertEquals("project1", command.getBuild().getProjectName());
    }

    public void testCalculatesElapsedBuildTime() throws Exception {
        Mock mockBuildSummary = mock(BuildSummary.class, new Class[]{String.class}, new Object[]{""});
        BuildCommand command = new BuildCommand((Build) mockBuildSummary.proxy(), null);
        DateTime fivePM = CCDateFormatter.format("2007-04-20 17:00:00", "yyyy-MM-dd HH:mm:ss");
        mockBuildSummary.expects(once()).method("getBuildingSince").will(returnValue(fivePM));
        DateTime sixPM = CCDateFormatter.format("2007-04-20 18:00:00", "yyyy-MM-dd HH:mm:ss");
        Long elapsedSeconds = command.getElapsedTimeBuilding(sixPM);
        assertEquals(new Long(3600), elapsedSeconds);
    }

    public void testShouldClassNameAsDarkRedWhenBuildIsFailed24HoursAgo() {
        Build buildSummary = new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        Build lastSuccessful =
                new BuildSummary("", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        BuildCommand command = new BuildCommand(buildSummary, null);
        command.updateCssLevel(lastSuccessful);
        assertEquals("8", command.toJsonHash().get(BuildCommand.CSS_LEVEL));
    }

    public void testJsonHashShouldNotReturnDarkRedWhenBuildIsPassed() {
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        buildSummary.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        BuildCommand command = new BuildCommand(buildSummary, null);
        BuildSummary lastSuccessful =
                new BuildSummary("", PreviousResult.PASSED, DataUtils.PASSING_BUILD_LBUILD_0_XML);
        command.updateCssLevel(lastSuccessful);
        Map json = command.toJsonHash();
        assertEquals("8", json.get(BuildCommand.CSS_LEVEL));
        assertEquals(PreviousResult.PASSED.getStatus(), json.get("previous_result"));
        assertEquals(CurrentStatus.WAITING.getStatus(), json.get("current_status"));

        String jsonString = command.toJsonString();
        assertJsonContains(jsonString, BuildCommand.CSS_LEVEL, "8");
        assertJsonContains(jsonString, "previous_result", PreviousResult.PASSED.getStatus());
        assertJsonContains(jsonString, "current_status", CurrentStatus.WAITING.getStatus());
    }

    public void testJsonHashShouldNotReturnCurrentStatusWhenLastSuccessfulBuildIsEmpty() {
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        buildSummary.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        BuildCommand command = new BuildCommand(buildSummary, null);
        command.updateCssLevel(null);
        Map json = command.toJsonHash();
        assertEquals("8", json.get(BuildCommand.CSS_LEVEL));
        assertEquals(PreviousResult.FAILED.getStatus(), json.get("previous_result"));
        assertEquals("Waiting", json.get("current_status"));

        String jsonString = command.toJsonString();
        assertJsonContains(jsonString, BuildCommand.CSS_LEVEL, "8");
        assertJsonContains(jsonString, "previous_result", PreviousResult.FAILED.getStatus());
        assertJsonContains(jsonString, "current_status", CurrentStatus.WAITING.getStatus());
    }

    public void testJsonHashShouldReturnBuildSinceForActiveBuild() throws Exception {
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        buildSummary.updateStatus("now building");
        buildSummary.updateBuildSince(new DateTime());
        BuildCommand command = new BuildCommand(buildSummary, null);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertEquals(PreviousResult.FAILED.getStatus(), json.get("previous_result"));
        assertEquals("Building", json.get("current_status"));

        String jsonString = command.toJsonString();
        assertJsonContains(jsonString, "previous_result", PreviousResult.FAILED.getStatus());
        assertJsonContains(jsonString, "current_status", CurrentStatus.BUILDING.getStatus());
    }

    public void testJsonHashShouldReturnLowerCaseOfStatusWhenInvokeDetaultCss() throws Exception {
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.UNKNOWN, DataUtils.FAILING_BUILD_XML);
        buildSummary.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        BuildCommand command = new BuildCommand(buildSummary, null);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertEquals(PreviousResult.UNKNOWN.getStatus(), json.get("previous_result"));
        assertEquals(CurrentStatus.WAITING.getStatus(), json.get("current_status"));

        String jsonString = command.toJsonString();
        assertJsonContains(jsonString, "previous_result", PreviousResult.UNKNOWN.getStatus());
        assertJsonContains(jsonString, "current_status", CurrentStatus.WAITING.getStatus());
    }

    public void testJsonHashShouldNotReturnBuildSinceForNonActiveBuild() throws Exception {
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        buildSummary.updateStatus(CurrentStatus.DISCONTINUED.getCruiseStatus());
        BuildCommand command = new BuildCommand(buildSummary, null);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertEquals(PreviousResult.FAILED.getStatus(), json.get("previous_result"));
        assertEquals(CurrentStatus.DISCONTINUED.getStatus(), json.get("current_status"));

        String jsonString = command.toJsonString();
        assertJsonContains(jsonString, "previous_result", PreviousResult.FAILED.getStatus());
        assertJsonContains(jsonString, "current_status", CurrentStatus.DISCONTINUED.getStatus());
    }

    public void testShouldReturnUnknownIfDurationIsNull() throws Exception {
        Mock build = mock(Build.class);
        build.expects(atLeastOnce()).method("getDuration").will(returnValue(null));
        BuildCommand command = new BuildCommand((Build) build.proxy(), null);
        assertEquals("Unknown", command.getDuration());
    }

    public void testShouldReturnEmptyCollectionWhenNoModificationsAvailableFromLogFile() throws Exception {
        Mock build = mock(Build.class);
        build.expects(atLeastOnce()).method("getModifications").will(returnValue(null));
        BuildCommand command = new BuildCommand((Build) build.proxy(), null);
        final Collection modifications = command.getModifications();
        assertTrue("Modifications should be empty", modifications.isEmpty());
    }

    private void assertJsonContains(String jsonString, String key, String value) {
        assertTrue("Should contain \"" + key + "\" : \"" + value + "\" in\n" + jsonString, StringUtils
                .contains(jsonString, "\"" + key + "\" : \"" + value + "\""));
    }
}
