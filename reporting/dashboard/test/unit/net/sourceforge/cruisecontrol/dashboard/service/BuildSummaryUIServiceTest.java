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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junitx.util.PrivateAccessor;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

public class BuildSummaryUIServiceTest extends MockObjectTestCase {
    private static final String THREE_HOURS_AGO =
            CCDateFormatter.format(new DateTime().minusHours(3), "yyyy-MM-dd HH:mm.ss");

    private static final String THREE_DAYS_AGO =
            CCDateFormatter.format(new DateTime().minusDays(3), "yyyy-MM-dd HH:mm.ss");

    private static final String NOW = CCDateFormatter.format(new DateTime(), "yyyy-MM-dd HH:mm.ss");

    private Mock mockBuildSummariesService;

    private BuildSummariesService buildSummariesService;

    private BuildSummaryUIService service;

    private Mock mockConfigService;

    protected void setUp() throws Exception {
        mockBuildSummariesService =
                mock(BuildSummariesService.class, new Class[] {Configuration.class,
                        BuildSummaryService.class, CruiseControlJMXService.class}, new Object[] {null, null,
                        null});
        buildSummariesService = (BuildSummariesService) mockBuildSummariesService.proxy();
        mockConfigService =
                mock(DashboardXmlConfigService.class, new Class[] {SystemService.class},
                        new Object[] {new SystemService()});
        service =
                new BuildSummaryUIService(buildSummariesService,
                        (DashboardXmlConfigService) mockConfigService.proxy());
    }

    public void testShouldBeAbleToPutStoryTrackerIntoBuildSummaryInToCommand() throws Exception {
        BuildSummary buildSummary =
                new BuildSummary("", THREE_DAYS_AGO, "", ProjectBuildStatus.FAILED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(buildSummary);
        Map expectedMap = new HashMap();
        StoryTracker expectedStoryTracker = new StoryTracker("", "", "");
        expectedMap.put(buildSummary.getProjectName(), expectedStoryTracker);
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(expectedMap));
        List commands = service.transform(summaries, false);
        BuildCommand buildCommand = (BuildCommand) commands.get(0);
        assertNotNull(PrivateAccessor.getField(buildCommand, "storyTracker"));
        assertEquals(expectedStoryTracker, PrivateAccessor.getField(buildCommand, "storyTracker"));
    }

    public void testShouldBeAbleToPutStoryTrackerIntoBuildSummary() throws Exception {
        BuildSummary earilestFailed =
                new BuildSummary("", THREE_DAYS_AGO, "", ProjectBuildStatus.FAILED, "log.xml");
        Map expectedMap = new HashMap();
        StoryTracker expectedStoryTracker = new StoryTracker("", "", "");
        expectedMap.put(earilestFailed.getProjectName(), expectedStoryTracker);
        mockBuildSummariesService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earilestFailed));
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(expectedMap));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.FAILED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries, true);
        BuildCommand buildCommand = (BuildCommand) transformed.get(0);
        assertNotNull(PrivateAccessor.getField(buildCommand, "storyTracker"));
        assertEquals(expectedStoryTracker, PrivateAccessor.getField(buildCommand, "storyTracker"));
    }

    public void testShouldBeAbleIngoreInactiveCaseAndReturnCommand() {
        mockBuildSummariesService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary buildSummary =
                new BuildSummary("", "2007-12-14", "build.489", ProjectBuildStatus.INACTIVE, "log.xml");
        List summaries = new ArrayList();
        summaries.add(buildSummary);
        List transformed = service.transform(summaries, true);
        assertEquals(1, transformed.size());
        assertTrue(transformed.get(0) instanceof BuildCommand);
        assertEquals(ProjectBuildStatus.INACTIVE, ((BuildCommand) transformed.get(0)).getBuild().getStatus());
        assertEquals(ProjectBuildStatus.INACTIVE.getStatus().toLowerCase(), ((BuildCommand) transformed
                .get(0)).getCssClassName());
    }

    public void testShouldBeAbleToUpdateTheFailedBuildToLongFailedBaseOnLastFailedBuildAndReturn() {
        BuildSummary earilestFailed =
                new BuildSummary("", THREE_DAYS_AGO, "", ProjectBuildStatus.FAILED, "log.xml");
        mockBuildSummariesService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earilestFailed));
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.FAILED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries, true);
        assertEquals(ProjectBuildStatus.FAILED, ((BuildCommand) transformed.get(0)).getBuild().getStatus());
        assertEquals("failed", ((BuildCommand) transformed.get(0)).getCssClassName());
        assertEquals("8", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldBeAbleToUpdateTheFailedBuildFailedBaseOnLastFailedBuildAndReturn() {
        BuildSummary earilestFailed =
                new BuildSummary("", THREE_HOURS_AGO, "", ProjectBuildStatus.FAILED, "log.xml");
        mockBuildSummariesService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earilestFailed));
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.FAILED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries, true);
        assertEquals("failed", ((BuildCommand) transformed.get(0)).getCssClassName());
        assertEquals("1", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldBeAbleToUpdateThePassedBuildToLongPassedBaseOnLastSucceedBuildAndReturn() {
        BuildSummary earilestSuccess =
                new BuildSummary("", THREE_DAYS_AGO, "", ProjectBuildStatus.PASSED, "log.xml");
        mockBuildSummariesService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockBuildSummariesService.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(earilestSuccess));
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.PASSED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries, true);
        assertEquals(ProjectBuildStatus.PASSED, ((BuildCommand) transformed.get(0)).getBuild().getStatus());
        assertEquals("passed", ((BuildCommand) transformed.get(0)).getCssClassName());
        assertEquals("8", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldBeAbleToUpdateThePassedBuildToPassedBaseOnLastSucceedBuildAndReturn() {
        BuildSummary earilestSuccess =
                new BuildSummary("", THREE_HOURS_AGO, "", ProjectBuildStatus.PASSED, "log.xml");
        mockBuildSummariesService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockBuildSummariesService.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(earilestSuccess));
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.PASSED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries, true);
        assertEquals("passed", ((BuildCommand) transformed.get(0)).getCssClassName());
        assertEquals("1", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldReturnUnknownStatusWhenFailedToFindLastBuild() throws Exception {
        mockBuildSummariesService.expects(once()).method("getLatest").will(returnValue(null));
        ProjectBuildStatus status = service.getLastBuildStatus("new_project");
        assertEquals(ProjectBuildStatus.UNKNOWN, status);
    }

    public void testShouldReturnLastBuildStatusWhenLastBuildExist() throws Exception {
        mockBuildSummariesService.expects(once()).method("getLatest").will(
                returnValue(new BuildSummary("project1", "", "", ProjectBuildStatus.FAILED, "")));
        ProjectBuildStatus status = service.getLastBuildStatus("project1");
        assertEquals(ProjectBuildStatus.FAILED, status);
    }
}
