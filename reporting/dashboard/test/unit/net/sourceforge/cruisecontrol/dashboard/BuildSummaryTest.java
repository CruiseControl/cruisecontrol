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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

public class BuildSummaryTest extends TestCase {
    private Build buildSummary;

    protected void setUp() throws Exception {
        String date = "2005-12-09 12:21.10";
        buildSummary = new BuildSummary("", date, null, ProjectBuildStatus.PASSED, "");
    }

    public void testShouldBeAbleToUpdateStatusWithJMXReturnStatus() {
        assertFalse(ProjectBuildStatus.BUILDING.equals(buildSummary.getStatus()));
        buildSummary.updateStatus("now building since 20070420174744");
        assertEquals(ProjectBuildStatus.BUILDING, buildSummary.getStatus());
        assertEquals(CCDateFormatter.format("2007-04-20 17:47:44", "yyyy-MM-dd HH:mm:ss"), buildSummary
                .getBuildingSince());
    }

    public void testShouldReturnBuildingAsStatusWhenTheStatusIsNotWaiting() {
        assertFalse(ProjectBuildStatus.BUILDING.equals(buildSummary.getStatus()));
        buildSummary.updateStatus("checking for modifications");
        assertEquals(ProjectBuildStatus.MODIFICATIONSET, buildSummary.getStatus());
    }

    public void testShouldCompareTheProjectNameIgnoreCase() throws Exception {
        Build buildSummary1 = new BuildSummary("project1", "", "", ProjectBuildStatus.PASSED, "");
        Build buildSummary2 = new BuildSummary("Project2", "", "", ProjectBuildStatus.PASSED, "");
        assertTrue(buildSummary1.compareTo(buildSummary2) < 0);
    }

    public void testShouldCompareTheOriginalProjectNameIfTheyAreEqualsIngoreCase() throws Exception {
        Build buildSummary1 = new BuildSummary("project1", "", "", ProjectBuildStatus.PASSED, "");
        Build buildSummary2 = new BuildSummary("Project1", "", "", ProjectBuildStatus.PASSED, "");
        assertTrue(buildSummary1.compareTo(buildSummary2) > 0);
    }

    public void testShouldReturn0SecondAsDefaultDuration() throws Exception {
        assertEquals("0 second", buildSummary.getDuration());
    }
}
