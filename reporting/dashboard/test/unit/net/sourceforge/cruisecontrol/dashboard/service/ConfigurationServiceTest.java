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

import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.BuildLoopQueryServiceStub;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import java.io.File;
import java.util.Collection;

public class ConfigurationServiceTest extends MockObjectTestCase {

    private ConfigurationService configuration;

    private Mock mockDashboardConfigFileService;

    private Mock mockProjects;

    private Mock mockBuildloopQueryService;

    private Projects projects;

    protected void setUp() throws Exception {
        mockDashboardConfigFileService =
                mock(
                        DashboardXmlConfigService.class, new Class[]{DashboardConfigFileFactory.class},
                        new Object[]{null});
        mockProjects =
                mock(
                        Projects.class, new Class[]{File.class, File.class, String[].class}, new Object[]{
                        new File("."), new File(""), new String[0]});
        projects = (Projects) mockProjects.proxy();
        mockBuildloopQueryService =
                mock(
                        BuildLoopQueryService.class, new Class[]{EnvironmentService.class,
                        BuildInformationRepository.class}, new Object[]{null, null});
        configuration =
                new ConfigurationService(
                        null, (DashboardXmlConfigService) mockDashboardConfigFileService
                        .proxy(), (BuildLoopQueryService) mockBuildloopQueryService.proxy());

    }

    protected void tearDown() throws Exception {
        System.setProperty(BuildLoopQueryServiceStub.PROPS_CC_CONFIG_FILE, "");
    }

    public void testShouldInitializeTheProjectsWhenInitializingConfiguration() throws Exception {
        File expectedArtifactsFile = new File("artifacts/project1");
        File expectedLogsFile = new File("logs/project1");
        mockDashboardConfigFileService.expects(once()).method("getConfigurationFile");
        mockBuildloopQueryService.expects(atLeastOnce()).method("getProjects").will(returnValue(projects));
        configuration.afterPropertiesSet();
        mockProjects.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(expectedArtifactsFile));
        mockProjects.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(expectedLogsFile));
        assertEquals(expectedArtifactsFile, configuration.getArtifactRoot("project1"));
        assertEquals(expectedLogsFile, configuration.getLogRoot("project1"));
    }

    public void testShouldNotInitalizeProjectsWhenConfigXmlIsNull() throws Exception {
        mockDashboardConfigFileService.expects(once()).method("getConfigurationFile").will(returnValue(null));
        mockBuildloopQueryService.expects(atLeastOnce()).method("getProjects").will(returnValue(null));
        configuration.afterPropertiesSet();

        assertNull(configuration.getArtifactRoot("project1"));
        assertNull(configuration.getLogRoot("project1"));
    }

    public void testShouldUpdateProjectsAfterChangeLocation2() throws Exception {
        mockDashboardConfigFileService.expects(once()).method("getConfigurationFile").will(
                returnValue(new File("")));
        configuration.afterPropertiesSet();
        mockDashboardConfigFileService.expects(once()).method("isDashboardConfigFileValid").will(
                returnValue(true));
        configuration.setDashboardConfigLocation("content");
    }


    public void testShouldReturnProjectsInBothFileSystemAndBuildLoopAsActive() {
        getProjectsFromBuildLoopAndFileSystem();
        Collection activeProjects = configuration.getActiveProjects();
        assertEquals(1, activeProjects.size());
        assertEquals(new File("1"), activeProjects.iterator().next());
    }

    public void testShouldReturnProjectsOnlyInhFileSystemAsDiscontinued() {
        getProjectsFromBuildLoopAndFileSystem();
        Collection discontinuedProjects = configuration.getDiscontinuedProjects();
        assertEquals(1, discontinuedProjects.size());
        assertEquals(new File("2"), discontinuedProjects.iterator().next());
    }


    public void testShouldReturnProjectsOnlyInBuildloopAsInactive() {
        getProjectsFromBuildLoopAndFileSystem();
        Collection inactiveProjects = configuration.getInactiveProjects();
        assertEquals(1, inactiveProjects.size());
        assertEquals(new File("3"), inactiveProjects.iterator().next());
    }

    private File[] projectsFromFileSystem() {
        return new File[]{new File("1"), new File("2")};
    }

    private File[] projectsFromBuildLoop() {
        return new File[]{new File("1"), new File("3")};
    }

    private void getProjectsFromBuildLoopAndFileSystem() {
        mockBuildloopQueryService.expects(atLeastOnce()).method("getProjects").will(returnValue(projects));
        mockProjects.expects(once()).method("getProjectsFromFileSystem").will(
                returnValue(projectsFromFileSystem()));
        mockProjects.expects(once()).method("getProjectsRegistedInBuildLoop").will(
                returnValue(projectsFromBuildLoop()));
    }
}
