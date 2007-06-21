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

import org.apache.commons.io.FileUtils;

public class SpecifyConfigLocationTest extends BaseFunctionalTest {
    private static final String PAGE_TITLE = "CruiseControl configuration file admin";

    public void testShouldBeAbleToShowSpecifyConfigLocationForm() {
        tester.assertTitleEquals(PAGE_TITLE);
        tester.assertFormPresent(SET_CONFIG_FILE_LOCATION_FORM);
        tester.assertFormElementPresent(CONFIG_FILE_LOCATION_FIELD_NAME);
    }

    public void testShouldBeAbleToSecifyConfigurationFileLocationSuccessfully() throws Exception {
        File configxml = DataUtils.createTempFile("config", ".xml");
        FileUtils.writeStringToFile(configxml, "<cruisecontrol/>");
        setConfigFileAndSubmitForm(configxml.getAbsolutePath());
        tester.assertTitleEquals(PAGE_TITLE);
        tester.assertTextPresent("Configuration file has been set successfully");
    }

    public void testShouldShowErrorMessageIfFileLocationIsBlank() throws IOException {
        setConfigFileAndSubmitForm(" ");

        tester.assertTitleEquals(PAGE_TITLE);
        tester.assertTextPresent("The configuration file path cannot be blank.");
    }

    public void testShouldShowErrorMessageIfConfigFileDoesNotExist() throws IOException {
        setConfigFileAndSubmitForm("/I/dont/exist.xml");

        tester.assertTitleEquals(PAGE_TITLE);
        tester.assertTextPresent("There is no configuration file at");
    }

    protected void onSetUp() {
        tester.beginAt(SPECIFY_CONFIG_FILE_URL);
    }

}
