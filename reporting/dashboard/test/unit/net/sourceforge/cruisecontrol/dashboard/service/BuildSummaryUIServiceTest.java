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
import java.util.List;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

public class BuildSummaryUIServiceTest extends MockObjectTestCase {
    private static final String THREE_HOURS_AGO = CCDateFormatter
            .format(new DateTime().minusHours(3), "yyyy-MM-dd HH:mm.ss");

    private static final String THREE_DAYS_AGO = CCDateFormatter
            .format(new DateTime().minusDays(3), "yyyy-MM-dd HH:mm.ss");

    private static final String NOW = CCDateFormatter.format(new DateTime(), "yyyy-MM-dd HH:mm.ss");

    private Mock mockService;

    private BuildSummariesService buildSummariesService;

    private BuildSummaryUIService service;

    protected void setUp() throws Exception {
        mockService = mock(BuildSummariesService.class, new Class[]{Configuration.class, BuildSummaryService.class},
                new Object[]{null, null});
        buildSummariesService = (BuildSummariesService) mockService.proxy();
        service = new BuildSummaryUIService(buildSummariesService);
    }

    public void testShouldBeAbleIngoreInactiveCaseAndReturnCommand() {
        mockService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockService.expects(never()).method("getLastSucceed").withAnyArguments();
        BuildSummary buildSummary = new BuildSummary("", "2007-12-14", "build.489", ProjectBuildStatus.INACTIVE,
                "log.xml");
        List summaries = new ArrayList();
        summaries.add(buildSummary);
        List transformed = service.transform(summaries);
        assertEquals(1, transformed.size());
        assertTrue(transformed.get(0) instanceof BuildCommand);
        assertEquals(ProjectBuildStatus.INACTIVE.getStatus(), ((BuildCommand) transformed.get(0)).getBuild()
                .getStatus());
        assertEquals(ProjectBuildStatus.INACTIVE.getStatus().toLowerCase(),
                ((BuildCommand) transformed.get(0)).getCssClassName());
    }

    public void testShouldBeAbleIngoreBuildingCaseAndReturnCommand() {
        mockService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockService.expects(never()).method("getLastSucceed").withAnyArguments();
        BuildSummary buildSummary = new BuildSummary("", "2007-12-14", "build.489", ProjectBuildStatus.BUILDING,
                "log.xml");
        List summaries = new ArrayList();
        summaries.add(buildSummary);
        List transformed = service.transform(summaries);
        assertEquals(1, transformed.size());
        assertTrue(transformed.get(0) instanceof BuildCommand);
        assertEquals(ProjectBuildStatus.BUILDING.getStatus(),
                ((BuildCommand) transformed.get(0)).getBuild().getStatus());
        assertEquals(ProjectBuildStatus.BUILDING.getStatus().toLowerCase(),
                ((BuildCommand) transformed.get(0)).getCssClassName());
    }

    public void testShouldBeAbleToUpdateTheFailedBuildToLongFailedBaseOnLastFailedBuildAndReturn() {
        BuildSummary earilestFailed = new BuildSummary("", THREE_DAYS_AGO, "", ProjectBuildStatus.FAILED, "log.xml");
        mockService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments()
                .will(returnValue(earilestFailed));
        mockService.expects(never()).method("getLastSucceed").withAnyArguments();
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.FAILED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries);
        assertEquals(ProjectBuildStatus.FAILED.getStatus(), ((BuildCommand) transformed.get(0)).getBuild().getStatus());
        assertEquals("long_failed", ((BuildCommand) transformed.get(0)).getCssClassName());
    }

    public void testShouldBeAbleToUpdateTheFailedBuildFailedBaseOnLastFailedBuildAndReturn() {
        BuildSummary earilestFailed = new BuildSummary("", THREE_HOURS_AGO, "", ProjectBuildStatus.FAILED, "log.xml");
        mockService.expects(atLeastOnce()).method("getEaliestFailed").withAnyArguments()
                .will(returnValue(earilestFailed));
        mockService.expects(never()).method("getLastSucceed").withAnyArguments();
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.FAILED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries);
        assertEquals("failed", ((BuildCommand) transformed.get(0)).getCssClassName());
    }

    public void testShouldBeAbleToUpdateThePassedBuildToLongPassedBaseOnLastSucceedBuildAndReturn() {
        BuildSummary earilestSuccess = new BuildSummary("", THREE_DAYS_AGO, "", ProjectBuildStatus.PASSED, "log.xml");
        mockService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockService.expects(atLeastOnce()).method("getLastSucceed").withAnyArguments()
                .will(returnValue(earilestSuccess));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.PASSED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries);
        assertEquals(ProjectBuildStatus.PASSED.getStatus(), ((BuildCommand) transformed.get(0)).getBuild()
                .getStatus());
        assertEquals("long_passed", ((BuildCommand) transformed.get(0)).getCssClassName());
    }

    public void testShouldBeAbleToUpdateThePassedBuildToPassedBaseOnLastSucceedBuildAndReturn() {
        BuildSummary earilestSuccess = new BuildSummary("", THREE_HOURS_AGO, "", ProjectBuildStatus.PASSED, "log.xml");
        mockService.expects(never()).method("getEaliestFailed").withAnyArguments();
        mockService.expects(atLeastOnce()).method("getLastSucceed").withAnyArguments()
                .will(returnValue(earilestSuccess));
        BuildSummary current = new BuildSummary("", NOW, "", ProjectBuildStatus.PASSED, "log.xml");
        List summaries = new ArrayList();
        summaries.add(current);
        List transformed = service.transform(summaries);
        assertEquals("passed", ((BuildCommand) transformed.get(0)).getCssClassName());
    }
}
