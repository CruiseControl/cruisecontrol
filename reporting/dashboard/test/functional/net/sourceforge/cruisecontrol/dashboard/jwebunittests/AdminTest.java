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
package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.web.UpdateConfigXmlContentController;

import org.apache.commons.io.FileUtils;

public class AdminTest extends BaseFunctionalTest {
    private static final String CONFIG_FILE_CONTENT = "configFileContent";

    private static final String PAGE_TITLE = "CruiseControl configuration file admin";

    private static final String EDIT_CONFIG_FILE_FORM = "editConfig";

    private static final String EDIT_CONFIG_FILE_URL = "/admin/config";

    private static final String UPDATED_CONFIG_FILE_CONTENTS =
            "<cruisecontrol><project name=\"project2\"/>" + "</cruisecontrol>\n";

    private static final String INVALID_CONFIG_FILE_CONTENTS = "<cruisecontrol><project";

    private File configFile;

    public void testShouldBeAbleToShowEditConfigFormAndShowConfigFileContentsAndUpdateContents()
            throws IOException {
        setConfigFileLocationAndGoToEditConfigPage();
        assertEditConfigFormPresent();
        assertConfigFileContentsPresent(FileUtils.readFileToString(configFile, null));
        updateConfigFileContents(UPDATED_CONFIG_FILE_CONTENTS);
        assertEditConfigFormPresent();
        tester.assertTextInElement("edit_message",
                UpdateConfigXmlContentController.CONFIGURATION_HAS_BEEN_UPDATED_SUCCESSFULLY);
        assertConfigFileContentsPresent(UPDATED_CONFIG_FILE_CONTENTS);
    }

    public void testShouldShowErrorMessageAndKeepModificationIfModificationIsInvalid() throws IOException {
        setConfigFileLocationAndGoToEditConfigPage();
        updateConfigFileContents(INVALID_CONFIG_FILE_CONTENTS);
        assertEditConfigFormPresent();
        tester.assertMatchInElement("error_1", "^(The configuration file is not valid XML: )");
        assertConfigFileContentsPresent(INVALID_CONFIG_FILE_CONTENTS);
    }

    private void setConfigFileLocationAndGoToEditConfigPage() throws IOException {
        configFile = DataUtils.createDefaultCCConfigFile();
        setConfigFileAndSubmitForm(configFile.getPath());
        tester.beginAt(EDIT_CONFIG_FILE_URL);
    }

    private void updateConfigFileContents(String updatedContents) {
        tester.setTextField(CONFIG_FILE_CONTENT, updatedContents);
        tester.submit();
    }

    private void assertEditConfigFormPresent() {
        tester.assertTitleEquals(PAGE_TITLE);
        tester.assertFormPresent(EDIT_CONFIG_FILE_FORM);
        tester.assertFormElementPresent(CONFIG_FILE_CONTENT);
        tester.assertElementPresentByXPath("//textarea[@name=\"configFileContent\"]");
    }

    private void assertConfigFileContentsPresent(String configFileContents) {
        tester.assertTextInElement(CONFIG_FILE_CONTENT, configFileContents.replaceAll("\\\n", ""));
        tester.setWorkingForm(EDIT_CONFIG_FILE_FORM);
    }
}