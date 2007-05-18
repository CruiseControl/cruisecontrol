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
package net.sourceforge.cruisecontrol.dashboard.web;

import java.io.File;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

public class AddProjectFromVersionControlControllerWithRealDataTest extends SpringBasedControllerTests {
    private AddProjectFromVersionControlController addProject;

    private net.sourceforge.cruisecontrol.dashboard.Configuration configuration;

    private File configurationFile;

    protected void onControllerSetup() throws Exception {
        configurationFile = DataUtils.createTempFile("config", ".xml");
        DataUtils
                .writeContentToFile(configurationFile, "<cruisecontrol><project name=\"project1\"/></cruisecontrol>\n");
        this.configuration.setCruiseConfigLocation(configurationFile.getAbsolutePath());
    }

    protected void onTearDown() throws Exception {
        configurationFile.delete();
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setAddProjectFromVersionControlController(AddProjectFromVersionControlController addProject) {
        this.addProject = addProject;
    }

    public void testShouldCreateNewProjectAndRedirectToProjectsPage() throws Exception {
        getRequest().setMethod("POST");
        String projectName = "new project";
        getRequest().addParameter("projectName", projectName);
        getRequest().addParameter("url", "http://valid.url");
        getRequest().addParameter("vcsType", "svn");

        this.addProject.handleRequest(getRequest(), getResponse());
        assertTrue(isProjectAdded(projectName));
    }

    public void testShouldShowErrorMessageIfSvnAddressIsInvalid() throws Exception {
        getRequest().setMethod("POST");
        getRequest().addParameter("projectName", "another project");
        getRequest().addParameter("url", "http://wrong.url");
        getRequest().addParameter("vcsType", "svn");

        this.addProject.handleRequest(getRequest(), getResponse());
        assertFalse(isProjectAdded("another project"));
    }

    public void testShouldShouldCreateProjectForCvsProject() throws Exception {
        getRequest().setMethod("POST");
        getRequest().addParameter("projectName", "new project");
        getRequest().addParameter("url", "http://valid.url");
        getRequest().addParameter("vcsType", "cvs");
        getRequest().addParameter("moduleName", "someModule");

        this.addProject.handleRequest(getRequest(), getResponse());

        assertTrue(isProjectAdded("new project"));
    }

    public void testShouldShouldCreateProjectForPerforceProject() throws Exception {
        this.configuration.setCruiseConfigLocation(configurationFile.getAbsolutePath());
        getRequest().setMethod("POST");
        getRequest().addParameter("projectName", "new project");
        getRequest().addParameter("url", "http://valid.url");
        getRequest().addParameter("vcsType", "perforce");
        getRequest().addParameter("moduleName", "depot");

        this.addProject.handleRequest(getRequest(), getResponse());

        assertTrue(isProjectAdded("new project"));
    }

    private boolean isProjectAdded(String projectName) throws Exception {
        return configuration.hasProject(projectName);
    }

}