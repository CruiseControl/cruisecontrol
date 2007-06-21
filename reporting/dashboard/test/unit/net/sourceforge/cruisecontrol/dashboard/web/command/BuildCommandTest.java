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

import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;

import org.jmock.cglib.MockObjectTestCase;

public class BuildCommandTest extends MockObjectTestCase {
    public void testBuildCommandShouldTakeBuildAsContructor() {
        Map props = new HashMap();
        props.put("projectname", "project 1");
        props.put("logfile", new File("log19991212050505.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build);
        assertEquals("project 1", command.getBuild().getProjectName());
        assertFalse(command.getBuild().hasPassed());
    }

    public void testShouldReturnClassNameAsLongFailedWhenBuildIsMoreThanFailed24HoursAgo() {
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build);
        BuildSummary buildSummary =
                new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.PASSED, "");
        command.updateFailedCSS(buildSummary);
        assertEquals("failed", command.getCssClassName());
        assertEquals("failed_level_8", command.getCssClassNameForDashboard());
    }

    public void testShouldReturnClassNameAsFailedWhenBuildIsLessThanFailed24HoursAgo() {
        String dateBuildSummary =
                new SimpleDateFormat("yyyy-MM-dd HH:mm.ss", Locale.ENGLISH).format(new Date());
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build);
        BuildSummary buildSummary =
                new BuildSummary("", dateBuildSummary, "", ProjectBuildStatus.PASSED, "");
        command.updateFailedCSS(buildSummary);
        assertEquals("failed", command.getCssClassName());
        assertEquals("failed_level_0", command.getCssClassNameForDashboard());
    }

    public void testShouldReturnClassNameAsFailedWhenBuildNeverPassed() {
        Map props = new HashMap();
        props.put("logfile", new File("log20051209122103.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand command = new BuildCommand(build);
        command.updateFailedCSS(null);
        assertEquals("failed", command.getCssClassName());
        assertEquals("failed_level_8", command.getCssClassNameForDashboard());
    }

    public void testShouldNotReturnDarkRedWhenBuildIsPassed() {
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505Lbuild.9.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand buildCommand = new BuildCommand(build);
        assertEquals("passed", buildCommand.getCssClassName());
    }

    public void testCassNameShouldBeLevel8WhenTheLatestSuccessfulBuildIs24HoursAgo()
            throws Exception {
        Map props = new HashMap();
        props.put("logfile", new File("log19991212050505Lbuild.9.xml"));
        BuildDetail build = new BuildDetail(props);
        BuildCommand buildCommand = new BuildCommand(build);
        BuildSummary buildSummary =
                new BuildSummary("", "2005-12-09 12:21.10", "", ProjectBuildStatus.PASSED, "");
        buildCommand.updatePassedCss(buildSummary);
        assertEquals("passed", buildCommand.getCssClassName());
        assertEquals("passed_level_8", buildCommand.getCssClassNameForDashboard());
    }
}
