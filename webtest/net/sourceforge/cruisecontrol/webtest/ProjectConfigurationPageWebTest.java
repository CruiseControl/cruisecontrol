/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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

    public void testShouldLoadRawXMLConfigurationData() {
        beginAt(CONFIG_URL);
        assertFormPresent("project-config");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
    }

    public void testShouldSaveChangesToXMLConfigurationData() throws Exception {
        beginAt(CONFIG_URL);
        setWorkingForm("project-config");
        setFormElement("contents", contents + "<!-- Hello, world! -->");
        submit();
        assertFormPresent("project-config");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
        assertTextPresent("&lt;!-- Hello, world! --&gt;");
    }

    public void testLoad() throws Exception {
        String newContent = "&lt;!-- Hello, world! --&gt;";

        beginAt(CONFIG_URL);
        assertFormPresent("reload-configuration");
        setWorkingForm("project-config");
        setFormElement("contents", contents + "<!-- Hello, world! -->");
        submit();
        assertFormPresent("project-config");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
        assertTextPresent(newContent);
        setWorkingForm("reload-configuration");
        submit();
        assertTextPresent("Reloaded configuration.");
        assertTextNotPresent(newContent);
    }

    private static Configuration createConfig() throws IOException, MalformedObjectNameException {
        return new Configuration("localhost", 7856);
    }
}
