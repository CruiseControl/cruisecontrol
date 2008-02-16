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

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.BuildLoopInformation.JmxInfo;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.MBeanServerConnectionBuildOutputStub;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildLoopQueryServiceTest extends MockObjectTestCase {
    private static final String PROJECT_NAME = "connectfour";

    private BuildLoopQueryService buildLoopQueryService;

    private Mock envService;

    private Mock repository;

    private Mock projectInfo;

    protected void setUp() throws Exception {
        envService = mock(EnvironmentService.class,
                new Class[]{DashboardConfigService[].class},
                new Object[]{null});
        repository = mock(BuildInformationRepository.class);
        projectInfo = mock(BuildLoopInformation.ProjectInfo.class,
                new Class[]{String.class, String.class, String.class},
                new Object[]{null, null, null});
        buildLoopQueryService = new BuildLoopQueryService(
                (EnvironmentService) envService.proxy(),
                (BuildInformationRepository) repository.proxy());
    }

    public void testShouldReturnArrayContainsCommiterAndCommitMessage() throws Exception {
        ArrayList modifications = new ArrayList();
        Modification modification = new Modification("modification");
        modification.userName = "committer";
        modification.comment = "message 1";
        modifications.add(modification);

        projectInfo.expects(once()).method("getModifications").will(returnValue(modifications));
        repository.expects(once()).method("getProjectInfo").with(eq(PROJECT_NAME)).will(
                returnValue(projectInfo.proxy()));

        List commitMessages = buildLoopQueryService.getCommitMessages(PROJECT_NAME);
        assertEquals(1, commitMessages.size());
        Modification message = (Modification) commitMessages.get(0);
        assertEquals("modification", message.type);
        assertEquals("committer", message.userName);
        assertEquals("message 1", message.comment);
    }

    public void testShouldReturnBuildOutput() throws Exception {
        repository.expects(once()).method("getJmxConnection").will(
                returnValue(new MBeanServerConnectionBuildOutputStub()));
        String[] output = buildLoopQueryService.getBuildOutput(PROJECT_NAME, 0);
        assertEquals("Build Failed", output[0]);
        assertEquals("Build Duration: 10s", output[1]);
    }

    public void testShouldReturnStatusMapKeyedOnProjectName() throws Exception {
        Mock buildloopinfoMock =
                mock(
                        BuildLoopInformation.class, new Class[]{ProjectInfo[].class, JmxInfo.class,
                        String.class, String.class}, new Object[]{null, null, null, null});

        repository.expects(once()).method("hasBuildLoopInfoFor").with(eq("project1")).will(returnValue(true));
        repository.expects(once()).method("getBuildLoopInfo").with(eq("project1")).will(
                returnValue(buildloopinfoMock.proxy()));

        buildloopinfoMock.expects(once()).method("getServerName").will(returnValue("192.168.1.1"));

        String serverName = buildLoopQueryService.getServerName("project1");
        assertEquals("192.168.1.1", serverName);
    }

    public void testShouldReturnServerNameMapKeyedOnProjectName() throws Exception {
        Mock projectInfo2 =
                mock(
                        BuildLoopInformation.ProjectInfo.class, new Class[]{String.class, String.class,
                        String.class}, new Object[]{null, null, null});

        ArrayList infos = new ArrayList();

        projectInfo.expects(once()).method("getName").will(returnValue("project1"));
        projectInfo.expects(once()).method("getStatus").will(returnValue("now building"));
        infos.add(projectInfo.proxy());

        projectInfo2.expects(once()).method("getName").will(returnValue("project2"));
        projectInfo2.expects(once()).method("getStatus").will(returnValue("paused"));
        infos.add(projectInfo2.proxy());

        repository.expects(once()).method("getProjectInfos").will(returnValue(infos));

        Map projectsStatus = buildLoopQueryService.getAllProjectsStatus();

        assertEquals("now building", projectsStatus.get("project1"));
        assertEquals("paused", projectsStatus.get("project2"));
    }

    public void testShouldThrowExceptionWhenForceBuildIsDisabled() throws Exception {
        envService.expects(atLeastOnce()).method("isForceBuildEnabled").will(returnValue(false));
        repository.expects(never()).method("getJmxConnection");
        try {
            buildLoopQueryService.forceBuild(PROJECT_NAME);
            fail();
        } catch (Exception e) {
            // expected exception
        }
    }

    public void testShouldReturnProjectsObject() throws Exception {
        Mock projectInfo2 =
                mock(
                        BuildLoopInformation.ProjectInfo.class, new Class[]{String.class, String.class,
                        String.class}, new Object[]{null, null, null});

        ArrayList infos = new ArrayList();

        projectInfo.expects(once()).method("getName").will(returnValue("project1"));
        infos.add(projectInfo.proxy());

        projectInfo2.expects(once()).method("getName").will(returnValue("project2"));
        infos.add(projectInfo2.proxy());

        Mock mockLogDir = mock(File.class, new Class[]{String.class}, new Object[]{"logs"});
        envService.expects(once()).method("getLogDir").will(returnValue(mockLogDir.proxy()));
        envService.expects(once()).method("getArtifactsDir").will(returnValue(new File("arts")));
        repository.expects(once()).method("getProjectInfos").will(returnValue(infos));
        Projects projects = buildLoopQueryService.getProjects();

        assertEquals(2, projects.getProjectsRegistedInBuildLoop().length);
        assertEquals(mockLogDir.proxy(), projects.getLogRoot());
    }

    public void testShouldProvideDefaultServerNameForUnknownProject() throws Exception {
        repository.expects(once()).method("hasBuildLoopInfoFor").with(eq("project1")).will(returnValue(false));
        assertEquals("No server name available", buildLoopQueryService.getServerName("project1"));
    }

    public void testShouldReturnMeaningfulErrorMessageWhenUnableToConnectToJmx() throws Exception {
        Mock buildLoopInfo = mock(BuildLoopInformation.class,
                new Class[]{ProjectInfo[].class, JmxInfo.class, String.class, String.class},
                new Object[]{new ProjectInfo[]{}, null, null, null});
        repository.expects(once()).method("getJmxConnection").with(eq("project1")).will(returnValue(null));
        repository.expects(once()).method("hasBuildLoopInfoFor").with(eq("project1")).will(returnValue(true));
        repository.expects(once()).method("getBuildLoopInfo").with(eq("project1"))
                .will(returnValue(buildLoopInfo.proxy()));
        buildLoopInfo.expects(once()).method("getServerName").will(returnValue("server1"));

        assertEquals(" - Unable to connect to build loop at server1",
                buildLoopQueryService.getBuildOutput("project1", 0)[0]);
    }

}