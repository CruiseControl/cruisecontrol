/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.webtest;

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import net.sourceforge.jwebunit.WebTestCase;
import net.sourceforge.cruisecontrol.Configuration;

import org.jdom.JDOMException;

public class ProjectConfigurationPageWebTest extends WebTestCase {
    private static final String CONFIG_URL = "/cruisecontrol/config.jspa?project=connectfour";
    private static final String CONTENTS_URL = "/cruisecontrol/contents.jspa?project=connectfour";
    private final String contents;
    private Configuration configuration;

    public ProjectConfigurationPageWebTest() throws MalformedObjectNameException, IOException,
            AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, JDOMException {
        configuration = createConfig();
        contents = configuration.getConfiguration();
    }

    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
        configuration = createConfig();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        configuration.setConfiguration(contents);
        configuration.save();
    }

    public void testShouldLinkToRawXMLConfigurationData() {
        beginAt(CONFIG_URL);
        assertLinkPresentWithImage("xml.png");
        clickLinkWithImage("xml.png");
        assertTitleEquals("CruiseControl Raw XML Configuration");
    }

    public void testShouldLinkBackToMainPage() {
        beginAt(CONTENTS_URL);
        assertLinkPresentWithText("Return to configuration editor");
        clickLinkWithText("Return to configuration editor");
        assertFormPresent("reload-configuration");
    }

    public void testShouldReloadConfiguration() throws Exception {
        testLoad();
        gotoPage(CONFIG_URL);
        setWorkingForm("reload-configuration");
        submit();
        assertTextPresent("Reloaded configuration");
        assertTextNotPresent("Hello, world!");
    }

    public void testShouldLoadRawXMLConfigurationData() {
        beginAt(CONTENTS_URL);
        assertFormPresent("project-configuration");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
    }

    public void testShouldSaveChangesToXMLConfigurationData() throws Exception {
        beginAt(CONTENTS_URL);
        setWorkingForm("project-configuration");
        setFormElement("contents", contents + "<!-- Hello, world! -->");
        submit();
        assertFormPresent("project-configuration");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
        assertTextPresent("&lt;!-- Hello, world! --&gt;");
    }

    public void testLoad() throws Exception {
        String newContent = "&lt;!-- Hello, world! --&gt;";

        beginAt(CONTENTS_URL);
        assertFormPresent("project-configuration");
        setWorkingForm("project-configuration");
        setFormElement("contents", contents + "<!-- Hello, world! -->");
        submit();
        assertFormPresent("project-configuration");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
        assertTextPresent(newContent);
    }

    private static Configuration createConfig() throws IOException, MalformedObjectNameException {
        return new Configuration("localhost", 7856);
    }
}
