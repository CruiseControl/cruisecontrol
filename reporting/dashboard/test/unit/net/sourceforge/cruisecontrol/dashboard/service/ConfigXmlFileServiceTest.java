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
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Svn;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class ConfigXmlFileServiceTest extends TestCase {
    private File tempDirForSetup;

    private ConfigXmlFileService service;

    private File configFile;

    public void setUp() throws Exception {
        tempDirForSetup = DataUtils.createTempDirectory("temp");
        TemplateRenderService renderService = new TemplateRenderService();
        renderService.loadTemplates();
        service = new ConfigXmlFileService(new EnvironmentService(), renderService);
        File configDirectory = FilesystemUtils.createDirectory("tempDir");
        configFile = FilesystemUtils.createFile("config.xml", configDirectory);
    }

    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDirForSetup);
    }

    public void testShouldBeAbleToReadConfigXml() throws Exception {
        File firstFile = DataUtils.createTempFile("config", "xml");
        DataUtils.writeContentToFile(firstFile, "first");
        assertEquals("first", service.readContentFromConfigFile(firstFile.getAbsolutePath()));
    }

    public void testShouldBeAbleToWriteContentToConfigXml() throws Exception {
        File firstFile = DataUtils.createTempFile("config", "xml");
        String expected = "some thing to write";
        service.writeContentToConfigXml(firstFile.getAbsolutePath(), expected);
        assertEquals(expected, service.readContentFromConfigFile(firstFile.getAbsolutePath()));
    }

    public void testShouldReturnNullWhenInvalidConfigurationDirectoryIsSpecified() {
        assertNull(service.getConfigXmlFile(new File("")));
    }

    public void testShouldReturnNullWhenInvalidConfigurationDirectoryIsSpecifiedThoughProp() {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, "invalid");
        assertNull(service.getConfigXmlFile(new File("")));
    }

    public void testShouldBeNotNullWhenValidConfigurationDirectoryIsSpecifiedThoughProp() {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, configFile.getAbsolutePath());
        assertNotNull(service.getConfigXmlFile(new File("")));
    }

    public void testShouldReturnNullThatTheTemporaryDirectoryWithoutConfigFileIsAnInvalidLocation() {
        FilesystemUtils.createDirectory("tempDir");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FILE, "");
        assertNull(service.getConfigXmlFile(new File("")));
    }

    public void testShouldSayThatTheTemporaryDirectoryWithConfigFileIsAValidLocation()
            throws Exception {
        assertNotNull(service.getConfigXmlFile(configFile));
    }

    public void testShouldBeAbleGetAllProjectsInfoFromConfigXmlFile() throws Exception {
        Projects projects = service.getProjects(DataUtils.getConfigXmlAsFile());
        File baseRoot = new File(DataUtils.getConfigXmlAsFile().getParentFile(), "logs");
        assertEquals(new File(baseRoot, "project1"), projects.getLogRoot("project1"));
        assertEquals(new File(baseRoot, "project2"), projects.getLogRoot("project2"));
        assertEquals(new File(baseRoot, "projectWithoutPublishers"), projects
                .getLogRoot("projectWithoutPublishers"));
        assertEquals(new File(baseRoot, "project name"), projects.getLogRoot("project name"));
        File artifacts = new File(DataUtils.getConfigXmlAsFile().getParentFile(), "artifacts");
        assertEquals(new File(artifacts, "project1"), projects.getArtifactRoot("project1"));
        assertEquals(new File(artifacts, "project2"), projects.getArtifactRoot("project2"));
        assertNull(projects.getArtifactRoot("projectWithoutPublishers"));
        assertNull(projects.getArtifactRoot("project name"));
        assertEquals(4, projects.getProjectNames().length);
    }

    public void testShouldBeAbleToAddNewProjectToCurrentConfigXml() throws Exception {
        File configDirectory = FilesystemUtils.createDirectory("cruisecontrol");
        FileUtils.copyFileToDirectory(DataUtils.getConfigXmlAsFile(), configDirectory);
        File config = new File(configDirectory, "config.xml");
        service.addProject(config, "CruiseControlOSS", new Svn(null, null));
        String currentFile = FileUtils.readFileToString(config, null);
        assertTrue(StringUtils.contains(currentFile, "CruiseControlOSS"));
        assertTrue(StringUtils.contains(currentFile, "project1"));
        assertTrue(StringUtils.contains(currentFile, "project2"));
        assertTrue(StringUtils.contains(currentFile, "projectWithoutPublishers"));
    }

    public void testShouldAddBootstrapperAndRepositoryBaseOnVCS() throws Exception {
        File configDirectory = FilesystemUtils.createDirectory("cruisecontrol");
        File configxml = FilesystemUtils.createFile("config3.xml", configDirectory);
        FileUtils.writeStringToFile(configxml, "<cruisecontrol></cruisecontrol>");
        File config = new File(configDirectory, "config.xml");
        service.addProject(config, "CruiseControlOSS", new Svn(null, null));
        String currentFile = FileUtils.readFileToString(config, null);
        assertTrue(StringUtils.contains(currentFile, "project name=\"CruiseControlOSS\""));
        assertTrue(StringUtils.contains(currentFile, "<svnbootstrapper localWorkingCopy"));
        assertTrue(StringUtils.contains(currentFile, "<svn localWorkingCopy="));
    }

}
