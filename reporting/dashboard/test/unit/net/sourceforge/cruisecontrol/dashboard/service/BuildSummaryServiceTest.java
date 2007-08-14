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

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;

public class BuildSummaryServiceTest extends TestCase {
    private static final String LOG20060704155710_LBUILD_489_XML = "log20060704155710Lbuild.489.xml";

    private static final String LOG20060704155710_XML = "log20060704155710.xml";

    public void testTheStatusIsInactiveWhenCreateNewBuildSummary() throws Exception {
        Build summary = new BuildSummaryService().createInactive(new File("project1"));
        assertEquals("project1", summary.getProjectName());
        assertEquals(ProjectBuildStatus.INACTIVE, summary.getStatus());
    }

    public void testShouldParseBuildSummaryWithLabelForPassingBuild() throws Exception {
        Build actual = new BuildSummaryService().createBuildSummary(new File("", LOG20060704155710_LBUILD_489_XML));
        assertEquals("build.489", actual.getLabel());
        assertEquals("2006-07-04 15:57.10", actual.getName());
        assertEquals(LOG20060704155710_LBUILD_489_XML, actual.getBuildLogFilename());
    }

    public void testShouldParseBuildSummaryWithLabelForFailBuild() throws Exception {
        Build actual = new BuildSummaryService().createBuildSummary(new File("", LOG20060704155710_XML));
        assertEquals("", actual.getLabel());
        assertEquals("2006-07-04 15:57.10", actual.getName());
        assertEquals(LOG20060704155710_XML, actual.getBuildLogFilename());
    }

}
