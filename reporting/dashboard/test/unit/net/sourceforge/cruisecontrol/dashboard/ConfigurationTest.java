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

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.exception.ProjectAlreadyExistException;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.utils.DashboardConfig;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class ConfigurationTest extends MockObjectTestCase {

    private Configuration defaultConfiguration;

    private Mock mockConfigXmlFileService;

    private Mock mockProjects;

    private Projects projects;

    protected void setUp() throws Exception {
        mockConfigXmlFileService =
                mock(ConfigXmlFileService.class, new Class[] {EnvironmentService.class},
                        new Object[] {null});
        mockProjects =
                mock(Projects.class, new Class[] {File.class, File.class, File.class,
                        DashboardConfig.class}, new Object[] {new File(""), new File("."),
                        new File(""), null});
        projects = (Projects) mockProjects.proxy();
        defaultConfiguration =
                new Configuration((ConfigXmlFileService) mockConfigXmlFileService.proxy());

    }

    protected void tearDown() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, "");
    }

    public void testShouldInitializeTheProjectsWhenInitializingConfiguration() throws Exception {
        File expectedArtifactsFile = new File("artifacts/project1");
        File expectedLogsFile = new File("logs/project1");
        mockConfigXmlFileService.expects(once()).method("getConfigXmlFile");
        mockConfigXmlFileService.expects(once()).method("getProjects").will(returnValue(projects));
        defaultConfiguration.afterPropertiesSet();
        mockProjects.expects(once()).method("getArtifactRoot").with(eq("project1")).will(
                returnValue(expectedArtifactsFile));
        mockProjects.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(expectedLogsFile));
        assertEquals(expectedArtifactsFile, defaultConfiguration.getArtifactRoot("project1"));
        assertEquals(expectedLogsFile, defaultConfiguration.getLogRoot("project1"));
    }

    public void testShouldNotInitalizeProjectsWhenConfigXmlIsNull() throws Exception {
        mockConfigXmlFileService.expects(once()).method("getConfigXmlFile").will(returnValue(null));
        mockConfigXmlFileService.expects(once()).method("getProjects").will(returnValue(null));
        defaultConfiguration.afterPropertiesSet();

        assertNull(defaultConfiguration.getArtifactRoot("project1"));
        assertNull(defaultConfiguration.getLogRoot("project1"));
        assertFalse(defaultConfiguration.hasProject("project1"));
    }

    public void testShouldThrowProjectAlreadyExistExceptionWhenProjectExist() throws Exception {
        mockConfigXmlFileService.expects(once()).method("getConfigXmlFile");
        mockConfigXmlFileService.expects(once()).method("getProjects").will(returnValue(projects));
        defaultConfiguration.afterPropertiesSet();
        mockProjects.expects(once()).method("hasProject").with(eq("project1")).will(
                returnValue(true));
        try {
            defaultConfiguration.addProject("project1", null);
            fail("Exception exptected");
        } catch (ProjectAlreadyExistException e) {
            // PASS
        }
    }

    public void testShouldUpdateProjectsAfterUpdateContent2() throws Exception {
        mockConfigXmlFileService.expects(once()).method("getConfigXmlFile");
        mockConfigXmlFileService.expects(once()).method("getProjects");
        defaultConfiguration.afterPropertiesSet();
        mockConfigXmlFileService.expects(once()).method("addProject");
        mockConfigXmlFileService.expects(once()).method("getProjects");
        defaultConfiguration.addProject("prject1", null);
    }

    public void testShouldUpdateProjectsAfterAddProjects2() throws Exception {
        mockConfigXmlFileService.expects(once()).method("getConfigXmlFile").will(
                returnValue(new File("")));
        mockConfigXmlFileService.expects(once()).method("getProjects");
        defaultConfiguration.afterPropertiesSet();

        mockConfigXmlFileService.expects(once()).method("writeContentToConfigXml");
        mockConfigXmlFileService.expects(once()).method("getProjects");
        defaultConfiguration.updateConfigFile("content");
    }

    public void testShouldUpdateProjectsAfterChangeLocation2() throws Exception {
        mockConfigXmlFileService.expects(once()).method("getConfigXmlFile").will(
                returnValue(new File("")));
        mockConfigXmlFileService.expects(once()).method("getProjects");
        defaultConfiguration.afterPropertiesSet();
        mockConfigXmlFileService.expects(once()).method("isConfigFileValid")
                .will(returnValue(true));
        mockConfigXmlFileService.expects(once()).method("getProjects");
        defaultConfiguration.setCruiseConfigLocation("content");
    }
}
