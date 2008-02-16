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

import junitx.util.PrivateAccessor;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildSummaryUIServiceTest extends MockObjectTestCase {
    private static final String THREE_HOURS_AGO =
            CCDateFormatter.format(new DateTime().minusHours(3), "yyyy-MM-dd HH:mm.ss");
    private static final String THREE_HOURS_AGO_LOG_FILE =
            "log" + CCDateFormatter.yyyyMMddHHmmss(new DateTime().minusHours(3)) + ".xml";

    private static final String THREE_DAYS_AGO =
            CCDateFormatter.format(new DateTime().minusDays(3), "yyyy-MM-dd HH:mm.ss");
    private static final String THREE_DAYS_AGO_LOG_FILE =
            "log" + CCDateFormatter.yyyyMMddHHmmss(new DateTime().minusDays(3)) + ".xml";

    private static final String NOW = CCDateFormatter.format(new DateTime(), "yyyy-MM-dd HH:mm.ss");

    private Mock mockBuildSummariesService;

    private HistoricalBuildSummariesService buildSummariesService;

    private BuildSummaryUIService service;

    private Mock mockConfigService;
    private static final String PASSING_LOGFILE = DataUtils.PASSING_BUILD_LBUILD_0_XML;

    protected void setUp() throws Exception {
        mockBuildSummariesService =
                mock(
                        HistoricalBuildSummariesService.class, new Class[]{ConfigurationService.class,
                        BuildSummaryService.class}, new Object[]{null, null});
        buildSummariesService = (HistoricalBuildSummariesService) mockBuildSummariesService.proxy();
        mockConfigService =
                mock(
                        DashboardXmlConfigService.class, new Class[]{DashboardConfigFileFactory.class},
                        new Object[]{null});
        service =
                new BuildSummaryUIService(
                        buildSummariesService,
                        (DashboardXmlConfigService) mockConfigService.proxy());
    }

    public void testShouldBeAbleToPutStoryTrackerIntoBuildSummaryInToCommand() throws Exception {
        BuildSummary buildSummary =
                new BuildSummary("project1", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        List summaries = new ArrayList();
        summaries.add(buildSummary);
        Map expectedMap = new HashMap();
        StoryTracker expectedStoryTracker = new StoryTracker(buildSummary.getProjectName(), "", "");
        expectedMap.put(buildSummary.getProjectName(), expectedStoryTracker);
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(expectedMap));
        List commands = service.transform(summaries);
        BuildCommand buildCommand = (BuildCommand) commands.get(0);
        assertNotNull(PrivateAccessor.getField(buildCommand, "storyTracker"));
        assertEquals(expectedStoryTracker, PrivateAccessor.getField(buildCommand, "storyTracker"));
    }

    public void testShouldBeAbleIngoreInactiveCaseAndReturnCommand() {
        mockBuildSummariesService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary buildSummary =
                new BuildSummary("", PreviousResult.UNKNOWN, PASSING_LOGFILE);
        buildSummary.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        List summaries = new ArrayList();
        summaries.add(buildSummary);
        List transformed = service.transformWithLevel(summaries);
        assertEquals(1, transformed.size());
        assertTrue(transformed.get(0) instanceof BuildCommand);
        assertEquals(
                CurrentStatus.WAITING, ((BuildCommand) transformed.get(0)).getBuild()
                .getCurrentStatus());
    }

    public void testShouldBeAbleIngoreInactiveBuilding() throws Exception {
         BuildSummary buildSummary =
                 new BuildSummary("project1", PreviousResult.UNKNOWN, PASSING_LOGFILE);
         buildSummary.updateStatus(CurrentStatus.BUILDING.getCruiseStatus());
         List summaries = new ArrayList();
         summaries.add(buildSummary);
         String transformed = service.toXml(summaries, "", "rss");
         assertFalse(StringUtils.contains(transformed, "project1"));
    }

    public void testShouldBeAbleToUpdateTheFailedBuildToLongFailedBaseOnLastFailedBuildAndReturn() {
        BuildSummary earilestFailed =
                new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        mockBuildSummariesService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earilestFailed));
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transformWithLevel(summaries);
        assertEquals(
                PreviousResult.FAILED, ((BuildCommand) transformed.get(0)).getBuild()
                .getPreviousBuildResult());
        assertEquals("8", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldBeAbleToUpdateTheFailedBuildFailedBaseOnLastFailedBuildAndReturn() {
        BuildSummary earilestFailed =
                new BuildSummary("", PreviousResult.FAILED, THREE_HOURS_AGO_LOG_FILE);
        mockBuildSummariesService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments().will(
                returnValue(earilestFailed));
        mockBuildSummariesService.expects(never()).method("getLastSucceed").withAnyArguments();
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML);
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transformWithLevel(summaries);
        assertEquals("1", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldBeAbleToUpdateThePassedBuildToLongPassedBaseOnLastSucceedBuildAndReturn() {
        BuildSummary earilestSuccess =
                new BuildSummary("", PreviousResult.PASSED, THREE_DAYS_AGO_LOG_FILE);
        mockBuildSummariesService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockBuildSummariesService.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(earilestSuccess));
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", PreviousResult.PASSED, PASSING_LOGFILE);
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transformWithLevel(summaries);
        assertEquals(
                PreviousResult.PASSED, ((BuildCommand) transformed.get(0)).getBuild()
                .getPreviousBuildResult());
        assertEquals("8", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldBeAbleToUpdateThePassedBuildToPassedBaseOnLastSucceedBuildAndReturn() {
        BuildSummary earilestSuccess =
                new BuildSummary("", PreviousResult.PASSED, THREE_HOURS_AGO_LOG_FILE);
        mockBuildSummariesService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockBuildSummariesService.expects(atLeastOnce()).method("getEarliestSucceeded").withAnyArguments()
                .will(returnValue(earilestSuccess));
        mockConfigService.expects(once()).method("getStoryTrackers").will(returnValue(new HashMap()));
        BuildSummary current = new BuildSummary("", PreviousResult.PASSED, PASSING_LOGFILE);
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transformWithLevel(summaries);
        assertEquals("1", ((BuildCommand) transformed.get(0)).getLevel());
    }

    public void testShouldReturnUnknownStatusWhenFailedToFindLastBuild() throws Exception {
        mockBuildSummariesService.expects(once()).method("getLatest").will(returnValue(null));
        PreviousResult status = service.getLastBuildStatus("new_project");
        assertEquals(PreviousResult.UNKNOWN, status);
    }

    public void testShouldReturnLastBuildStatusWhenLastBuildExist() throws Exception {
        mockBuildSummariesService.expects(once()).method("getLatest").will(
                returnValue(new BuildSummary("project1", PreviousResult.FAILED, DataUtils.FAILING_BUILD_XML)));
        PreviousResult status = service.getLastBuildStatus("project1");
        assertEquals(PreviousResult.FAILED, status);
    }

    public void testShouldReturn0SecondWhenLastBuildDoestNotExist() throws Exception {
        mockBuildSummariesService.expects(once()).method("getLatest").will(
                returnValue(null));
        assertEquals("0 second", service.getLastBuildDuration("project1"));
    }
    
    public void testShouldReturnLastBuildDurationWhenLastBuildExist() throws Exception {
        Mock mockBuildSummary = mock(BuildSummary.class, new Class[]{String.class}, new Object[]{""});
        mockBuildSummary.expects(once()).method("getDuration").will(returnValue("1 second"));
        mockBuildSummariesService.expects(once()).method("getLatest").will(
                returnValue(mockBuildSummary.proxy()));
        assertEquals("1 second", service.getLastBuildDuration("project1"));

    }
}
