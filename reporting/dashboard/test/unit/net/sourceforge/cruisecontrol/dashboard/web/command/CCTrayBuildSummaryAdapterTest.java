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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;

import org.apache.commons.lang.StringUtils;

public class CCTrayBuildSummaryAdapterTest extends TestCase {

    public void testShouldConvertPassedWaiting() throws Exception {
        BuildSummary summary = new BuildSummary("", "project1", "", ProjectBuildStatus.PASSED, "");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("", summary);
        assertEquals("Sleeping", adapter.getActivity());
        assertEquals("Success", adapter.getLastBuildStatus());
    }

    public void testShouldConvertPassedFailed() throws Exception {
        BuildSummary summary = new BuildSummary("", "project1", "", ProjectBuildStatus.FAILED, "");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("", summary);
        assertEquals("Sleeping", adapter.getActivity());
        assertEquals("Failure", adapter.getLastBuildStatus());
    }

    public void testShouldConvertPassedBootstrapping() throws Exception {
        BuildSummary summary = new BuildSummary("", "project1", "", ProjectBuildStatus.PASSED, "");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("", summary);
        summary.updateStatus("bootstrapping");
        assertEquals("CheckingModifications", adapter.getActivity());
        assertEquals("Success", adapter.getLastBuildStatus());
    }

    public void testShouldConvertCheckingModifications() throws Exception {
        BuildSummary summary = new BuildSummary("", "project1", "", ProjectBuildStatus.PASSED, "");
        summary.updateStatus("checking for modifications");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("", summary);
        assertEquals("CheckingModifications", adapter.getActivity());
    }

    public void testShouldConvertBuilding() throws Exception {
        BuildSummary summary = new BuildSummary("", "project1", "", ProjectBuildStatus.PASSED, "");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("", summary);
        summary.updateStatus("now building since 20070420170000");
        assertEquals("Building", adapter.getActivity());
    }

    public void testShouldProvideDateInCorrectFormat() throws Exception {
        BuildSummary summary = new BuildSummary("", "2007-05-04 14:54.00", "", ProjectBuildStatus.PASSED, "");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("", summary);
        // TODO: We should really return the timzone, too
        assertEquals("2007-05-04T14:54:00", adapter.getLastBuildTime());
    }

    public void testShouldProvideXmlOnCCTrayFormate() throws Exception {
        BuildSummary summary = new BuildSummary("project1", "2005-12-09 12:21.03", "build1", ProjectBuildStatus.PASSED,
                "");
        CCTrayBuildSummaryAdapter adapter = new CCTrayBuildSummaryAdapter("http://localhost:8080/dashboard/",
                summary);
        String xml = adapter.toXml();
        assertTrue(StringUtils.contains(xml, "name=\"project1\""));
        assertTrue(StringUtils.contains(xml, "activity=\"Sleeping\""));
        assertTrue(StringUtils.contains(xml, "lastBuildStatus=\"Success\""));
        assertTrue(StringUtils.contains(xml, "lastBuildLabel=\"build1\""));
        assertTrue(StringUtils.contains(xml, "lastBuildTime=\"2005-12-09T12:21:03"));
        assertTrue(StringUtils.contains(xml,
                "webUrl=\"http://localhost:8080/dashboard/build/detail/project1"));

    }
}
