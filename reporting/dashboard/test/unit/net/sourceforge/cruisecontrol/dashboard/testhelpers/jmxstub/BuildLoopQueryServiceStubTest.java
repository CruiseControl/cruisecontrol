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
package net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepositoryInMemoImpl;

public class BuildLoopQueryServiceStubTest extends TestCase {

    private BuildLoopQueryServiceStub queryServiceStub;

    private BuildInformationRepositoryInMemoImpl buildInformationRepository;

    protected void setUp() throws Exception {
        buildInformationRepository = new BuildInformationRepositoryInMemoImpl();
        queryServiceStub = new BuildLoopQueryServiceStub(null, buildInformationRepository);
    }

    public void testShouldReturnWaitAsDefaultStatus() {
        String buildStatus = queryServiceStub.getProjectStatus("test");
        assertEquals(BuildLoopQueryServiceStub.WAITING, buildStatus);
    }

    public void testForceBuildShouldTriggerBuildingProcessForNew() throws Exception {
        queryServiceStub.forceBuild("new_project");
        for (int i = 0; i < (BuildLoopQueryServiceStub.BUILD_TIMES.intValue() - 1); i++) {
            assertTrue(queryServiceStub.getProjectStatus("new_project").startsWith("now building"));
        }
        assertEquals(BuildLoopQueryServiceStub.WAITING, queryServiceStub.getProjectStatus("new_project"));
    }

    public void testTwoDifferentProjectShouldNotInfluenceEachOther() throws Exception {
        queryServiceStub.forceBuild("new_project_1");
        assertTrue(project1BuildStatus().startsWith("now building"));
        assertEquals(BuildLoopQueryServiceStub.WAITING, project2BuildStatus());
    }

    public void testShouldNotShareBuildCountdownBetweenMultipleProjects() throws Exception {
        queryServiceStub.forceBuild("new_project_1");
        queryServiceStub.forceBuild("new_project_2");
        for (int i = 0; i < 2; i++) {
            project1BuildStatus();
            project2BuildStatus();
        }
        assertTrue(project1BuildStatus().startsWith("now building"));
        assertTrue(project2BuildStatus().startsWith("now building"));
    }

    private String project2BuildStatus() {
        return queryServiceStub.getProjectStatus("new_project_2");
    }

    private String project1BuildStatus() {
        return queryServiceStub.getProjectStatus("new_project_1");
    }
}
