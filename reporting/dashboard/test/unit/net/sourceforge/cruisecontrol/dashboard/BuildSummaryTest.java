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
package net.sourceforge.cruisecontrol.dashboard;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.TimeConverter;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

public class BuildSummaryTest extends MockObjectTestCase {
    private BuildSummary buildSummary;
    private static final String PASSING_LOGFILE = DataUtils.PASSING_BUILD_LBUILD_0_XML;
    private Mock timeConverterMock;

    protected void setUp() throws Exception {
        timeConverterMock = mock(TimeConverter.class);
        buildSummary = createPassingBuildSummary("");
    }

    public void testShouldBeAbleToUpdateStatusWithJMXReturnStatus() {
        assertFalse(CurrentStatus.BUILDING.equals(buildSummary.getCurrentStatus()));
        buildSummary.updateStatus("now building");
        assertEquals(CurrentStatus.BUILDING, buildSummary.getCurrentStatus());
    }

    public void testShouldReturnBuildingAsStatusWhenTheStatusIsNotWaiting() {
        assertFalse(CurrentStatus.BUILDING.equals(buildSummary.getCurrentStatus()));
        buildSummary.updateStatus("checking for modifications");
        assertEquals(CurrentStatus.MODIFICATIONSET, buildSummary.getCurrentStatus());
    }

    public void testShouldCompareTheProjectNameIgnoreCase() throws Exception {
        Build buildSummary1 = createPassingBuildSummary("project1");
        Build buildSummary2 = createPassingBuildSummary("Project2");
        assertTrue(buildSummary1.compareTo(buildSummary2) < 0);
    }

    public void testShouldCompareTheOriginalProjectNameIfTheyAreEqualsIngoreCase() throws Exception {
        Build buildSummary1 = createPassingBuildSummary("project1");
        Build buildSummary2 = createPassingBuildSummary("Project1");
        assertTrue(buildSummary1.compareTo(buildSummary2) > 0);
    }

    public void testShouldReturn0SecondAsDefaultDuration() throws Exception {
        assertEquals("0 second", buildSummary.getDuration());
    }

    public void testShouldInvokeTheTimeConverter() throws Exception {
        String projectName = "Project1";
        buildSummary = createPassingBuildSummary(projectName);
        timeConverterMock.expects(once()).method("getConvertedTime").with(eq(buildSummary.getBuildDate().toDate()));
        buildSummary.getConvertedTime();
    }

    public void testShouldReturnWaitingForFirstBuildWhenNoLogFileSpecified() throws Exception {
        BuildSummary summary = new BuildSummary("Project1");
        assertEquals("waiting for first build...", summary.getConvertedTime());
    }

    private BuildSummary createPassingBuildSummary(String projectName) {
        BuildSummary summary = new BuildSummary(projectName, PreviousResult.PASSED, PASSING_LOGFILE);
        summary.setTimeConverter((TimeConverter) timeConverterMock.proxy());
        return summary;
    }

    public void testShouldDefaultToNAWhenNoServernameSet() throws Exception {
        BuildSummary summary = new BuildSummary("Project1", PreviousResult.PASSED, PASSING_LOGFILE);
        assertEquals("N/A", summary.getServerName());
    }

    public void testShouldUpdateBuildSinceWhenItIsBuilding() throws Exception {
        BuildSummary summary = new BuildSummary("Project1", PreviousResult.PASSED, PASSING_LOGFILE);
        DateTime expectedDateTime = new DateTime();
        summary.updateStatus(CurrentStatus.BUILDING.getCruiseStatus());
        assertEquals(CurrentStatus.BUILDING, summary.getCurrentStatus());
        summary.updateBuildSince(expectedDateTime);
        assertEquals(expectedDateTime, summary.getBuildingSince());
    }

    public void testShouldUpdateBuildSinceToNullWhenCurrentStatusIsNotBuildingAndBuildSinceIsNotNull()
            throws Exception {
        BuildSummary summary = new BuildSummary("Project1", PreviousResult.PASSED, PASSING_LOGFILE);
        DateTime expectedDateTime = new DateTime();
        summary.updateStatus(CurrentStatus.BUILDING.getCruiseStatus());
        summary.updateBuildSince(expectedDateTime);
        assertNotNull(summary.getBuildingSince());
        summary.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        assertNull(summary.getBuildingSince());
    }

    public void testShouldNotUpdateBuildSinceWhenItIsNotBuilding() throws Exception {
        BuildSummary inactiveSummary = new BuildSummary("Project1");
        DateTime expectedDateTime = new DateTime();
        inactiveSummary.updateBuildSince(expectedDateTime);
        assertNull(inactiveSummary.getBuildingSince());
    }

    public void testShouldBeAbleToExtractLogFileDateTime() throws Exception {
        BuildSummary summary = new BuildSummary("Project1", PreviousResult.PASSED, "log20051209122103Lbuild.489.xml");
        assertEquals("20051209122103", summary.getBuildLogFileDateTime());
        summary = new BuildSummary("Project1", PreviousResult.PASSED, "log20051209122103Lbuild.489.xml.gz");
        assertEquals("20051209122103", summary.getBuildLogFileDateTime());
    }

}
