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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.lang.StringUtils;

public class BuildSummaryCommandTest extends TestCase {
    public void testShouldBeAbleToDelegateTheInvocationToBuildSummary() throws Exception {
        Build summary = new BuildSummary("project1", "", "", ProjectBuildStatus.PASSED, "");
        BuildCommand command = new BuildCommand(summary);
        assertEquals("project1", command.getBuild().getProjectName());
    }

    public void testCalculatesElapsedBuildTime() throws Exception {
        Build buildSummary = new BuildSummary("", "", "", ProjectBuildStatus.PASSED, "");
        buildSummary.updateStatus("now building since 20070420170000");
        BuildCommand command = new BuildCommand(buildSummary);
        Long elapsedSeconds =
                command.getElapsedTimeBuilding(CCDateFormatter.format("2007-04-20 18:00:00",
                        "yyyy-MM-dd HH:mm:ss"));
        assertEquals(new Long(3600), elapsedSeconds);
    }

    public void testShouldClassNameAsDrakRedWhenBuildIsFailed24HoursAgo() {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.FAILED, "");
        Build lastSuccessful = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.PASSED, "");
        BuildCommand command = new BuildCommand(buildSummary);
        command.updateFailedCSS(lastSuccessful);
        assertEquals("failed", command.toJsonHash().get("css_class_name"));
        assertEquals("failed_level_8", command.toJsonHash().get("css_class_name_for_dashboard"));
    }

    public void testShouldClassNameAsFailedWhenBuildIsLessThanFailed24HoursAgo() {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss", Locale.ENGLISH).format(new Date());
        Build currentBuildSummary = new BuildSummary("", dateStr, "", ProjectBuildStatus.FAILED, "");
        Build lastSuccessfualBuild = new BuildSummary("", dateStr, "", ProjectBuildStatus.PASSED, "");

        BuildCommand command = new BuildCommand(currentBuildSummary);
        Map json = command.toJsonHash();
        command.updateFailedCSS(lastSuccessfualBuild);
        assertEquals("failed", json.get("css_class_name"));
        assertEquals("failed_level_0", json.get("css_class_name_for_dashboard"));
    }

    public void testShouldNotReturnDarkRedWhenBuildIsPassed() {
        Build buildSummary = new BuildSummary("", "2005-12-07 12:21.10", "", ProjectBuildStatus.PASSED, "");
        BuildCommand command = new BuildCommand(buildSummary);
        BuildSummary lastSuccessful =
                new BuildSummary("", "2005-12-07 12:21.10", "", ProjectBuildStatus.PASSED, "");
        command.updatePassedCss(lastSuccessful);
        Map json = command.toJsonHash();
        assertEquals("passed", json.get("css_class_name"));
        assertEquals("passed_level_8", json.get("css_class_name_for_dashboard"));
    }

    public void testShouldNotReturnCurrentStatusWhenLastSuccessfulBuildIsEmpty() {
        Build buildSummary = new BuildSummary("", "2005-12-07 12:21.10", "", ProjectBuildStatus.FAILED, "");
        BuildCommand command = new BuildCommand(buildSummary);
        command.updateFailedCSS(null);
        Map json = command.toJsonHash();
        assertEquals("failed", json.get("css_class_name"));
        assertEquals("failed_level_8", json.get("css_class_name_for_dashboard"));
    }

    public void testShouldReturnBuildSinceForActiveBuild() throws Exception {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.FAILED, "");
        buildSummary.updateStatus("now building since 19990420170000");
        BuildCommand command = new BuildCommand(buildSummary);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertTrue(StringUtils.contains((String) json.get("latest_build_date"), "1999"));
        command.updateDefaultCss();
        assertEquals("building", json.get("css_class_name"));
    }

    public void testShouldReturnLowerCaseOfStatusWhenInvokeDetaultCss() throws Exception {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.INACTIVE, "");
        BuildCommand command = new BuildCommand(buildSummary);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        command.updateDefaultCss();
        assertEquals("inactive", json.get("css_class_name"));
    }

    public void testShouldNotReturnBuildSinceForNonActiveBuild() throws Exception {
        Build buildSummary = new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.FAILED, "");
        buildSummary.updateStatus("");
        BuildCommand command = new BuildCommand(buildSummary);
        Map json = command.toJsonHash();
        assertTrue(json.containsKey("latest_build_date"));
        assertTrue(StringUtils.contains((String) json.get("latest_build_date"), "2005"));
    }
}
