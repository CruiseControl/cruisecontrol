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
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class ConfigurationTest extends MockObjectTestCase {

    private Configuration defaultConfiguration;

    protected void setUp() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, DataUtils.getConfigXmlAsFile()
                .getAbsolutePath());
        ConfigXmlFileService service = new ConfigXmlFileService(new EnvironmentService());
        defaultConfiguration = new Configuration(service);
    }

    protected void tearDown() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, "");
    }

    public void testShouldInitializeTheProjectsWhenInitializingConfiguration() throws Exception {
        File file = defaultConfiguration.getArtifactRoot("project1");
        assertTrue(file.exists());
        assertTrue(defaultConfiguration.getLogRoot("project1").exists());
    }

    public void testShouldNotInitalizeProjectsWhenConfigXmlIsNull() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, "");
        ConfigXmlFileService service = new ConfigXmlFileService(new EnvironmentService());
        Configuration configuration = new Configuration(service);
        assertNull(configuration.getArtifactRoot("project1"));
        assertNull(configuration.getLogRoot("project1"));
        assertFalse(configuration.hasProject("project1"));
    }

    public void testShouldThrownProjectAlreadyExistExceptionWhenProjectExist() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, DataUtils.getConfigXmlAsFile()
                .getAbsolutePath());
        ConfigXmlFileService service = new ConfigXmlFileService(new EnvironmentService());
        Configuration configuration = new Configuration(service);
        try {
            configuration.addProject("project1", null);
            fail("Exception exptected");
        } catch (ProjectAlreadyExistException e) {
            // PASS
        }
    }

    public void testShouldUpdateProjectsAfterUpdateContent2() throws ProjectAlreadyExistException {
        Mock configXmlFileServiceMock = getFileServiceMock();
        configXmlFileServiceMock.expects(once()).method("getProjects");
        configXmlFileServiceMock.expects(once()).method("addProject");
        configXmlFileServiceMock.expects(once()).method("getProjects");
        Configuration configuration =
                new Configuration((ConfigXmlFileService) configXmlFileServiceMock.proxy());
        configuration.addProject("prject1", null);
    }

    public void testShouldUpdateProjectsAfterAddProjects2() throws Exception {
        Mock configXmlFileServiceMock = getFileServiceMock();
        configXmlFileServiceMock.expects(once()).method("getProjects");
        configXmlFileServiceMock.expects(once()).method("writeContentToConfigXml");
        configXmlFileServiceMock.expects(once()).method("getProjects");
        Configuration configuration =
                new Configuration((ConfigXmlFileService) configXmlFileServiceMock.proxy());
        configuration.updateConfigFile("content");
    }

    public void testShouldUpdateProjectsAfterChangeLocation2() throws Exception {
        Mock configXmlFileServiceMock = getFileServiceMock();
        configXmlFileServiceMock.expects(once()).method("getProjects");
        configXmlFileServiceMock.expects(once()).method("isConfigFileValid")
                .will(returnValue(true));
        configXmlFileServiceMock.expects(once()).method("getProjects");
        Configuration configuration =
                new Configuration((ConfigXmlFileService) configXmlFileServiceMock.proxy());
        configuration.setCruiseConfigLocation("content");
    }

    private Mock getFileServiceMock() {
        Mock configXmlFileServiceMock =
                mock(ConfigXmlFileService.class, new Class[] {EnvironmentService.class},
                        new Object[] {null});
        configXmlFileServiceMock.expects(once()).method("getConfigXmlFile").will(
                returnValue(new File("")));
        return configXmlFileServiceMock;
    }
}
