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
package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class AddProjectTest extends SeleniumTestCase {

    private String originalText = "";

    private File addProjectTestProject;

    public void testShouldBeAbleToAddNewProjectToConfigXml() throws Exception {
        selenium.open("/dashboard/admin/config");
        String contentOfConfigXml = selenium.getText("configFileContent");
        assertFalse(StringUtils.contains(contentOfConfigXml, "addProjectTestProject"));
        openAndWaiting("/dashboard/admin/action/add", 5);
        selenium.type("projectName", "addProjectTestProject");
        selenium.type("url", "valid");
        selenium.click("add_project_btn");
        waitingForTextAppear("Project added.", 1 * AJAX_DURATION);
        openAndWaiting("/dashboard/admin/config", 5);
        contentOfConfigXml = selenium.getText("configFileContent");
        assertTrue(StringUtils.contains(contentOfConfigXml, "addProjectTestProject"));
        openAndWaiting("/dashboard/dashboard?s=1", 5);
        assertTrue(selenium.isTextPresent("addProjectTestProject"));

        selenium.open("/dashboard/forcebuild.ajax?projectName=addProjectTestProject");
        openAndWaiting("/dashboard/dashboard?s=1", 5);
        waitingForTextAppear(BUILDING_STARTED, FORCE_BUILD_DURATION);
        createFailedLogFile();
        String htmlSource = selenium.getHtmlSource();
        assertTrue(StringUtils.contains(htmlSource, "detail/live/addProjectTestProject"));
        assertTrue(StringUtils.contains(htmlSource, "list/all/addProjectTestProject"));
        assertTrue(StringUtils.contains(htmlSource, "list/passed/addProjectTestProject"));
        assertFalse(selenium.isTextPresent("NaN"));

        selenium.click("addProjectTestProject_config_panel");
        assertTrue(selenium.isVisible("toolkit_addProjectTestProject"));

        waitingForTextDisappear(BUILDING_STARTED, BUILD_DURATION);
        htmlSource = selenium.getHtmlSource();
        hasClassName(htmlSource, "addProjectTestProject_bar", "failed");
        assertTrue(StringUtils.contains(htmlSource, "detail/addProjectTestProject"));
    }

    private void createFailedLogFile() throws Exception {
        addProjectTestProject = new File(DataUtils.getLogRootOfWebapp(), "addProjectTestProject");
        addProjectTestProject.mkdir();
        File log = new File(addProjectTestProject, "log20051209122103.xml");
        log.createNewFile();
    }

    protected void doTearDown() throws Exception {
        FileUtils.deleteDirectory(addProjectTestProject);
        FileUtils.writeStringToFile(DataUtils.getConfigXmlInArbitraryCCHome(), originalText);
    }

    protected void doSetUp() throws Exception {
        originalText = FileUtils.readFileToString(DataUtils.getConfigXmlInArbitraryCCHome(), null);
    }
}