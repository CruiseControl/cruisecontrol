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

import net.sourceforge.jwebunit.WebTestCase;
import net.sourceforge.cruisecontrol.Configuration;

public class PublisherDetailsWebTest extends WebTestCase {
    private static final String BASE = "/cruisecontrol/load-details.jspa?"
        + "project=connectfour&pluginType=publisher";
    private static final String FTP_URL = BASE + "&pluginName=ftppublisher";
    private static final String XSLT_URL = BASE + "&pluginName=xsltlogpublisher";

    private Configuration configuration;
    private String contents;

    protected void setUp() throws Exception {
        super.setUp();

        getTestContext().setBaseUrl("http://localhost:7854");

        configuration = new Configuration("localhost", 7856);
        contents = configuration.getConfiguration();
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        configuration.setConfiguration(contents);
    }

    public void testShouldBeAccessibleFromPublishersPage() {
        String pluginsUrl = "/cruisecontrol/plugins.jspa?project=connectfour&pluginType=publisher";

        beginAt(pluginsUrl);
        assertLinkPresentWithText("onsuccess");
    }

    public void testShouldLoadFTPPublisherConfiguration() {
        beginAt(FTP_URL);
        assertFormPresent("ftppublisher-details");
        assertFormElementPresent("deleteArtifacts");
        assertFormElementPresent("destDir");
        assertFormElementPresent("srcDir");
    }

    public void testShouldLoadXSLTLogPublisherConfiguration() {
        beginAt(XSLT_URL);
        assertFormPresent("xsltlogpublisher-details");
        assertFormElementPresent("directory");
        assertFormElementPresent("outFileName");
        assertFormElementPresent("publishOnFail");
        assertFormElementPresent("xsltFile");
    }

    public void testShouldSaveFTPPublisherConfiguration() {
        beginAt(FTP_URL);
        setWorkingForm("ftppublisher-details");
        setFormElement("destDir", "/tmp");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("ftppublisher-details");
        assertFormElementPresent("destDir");
        assertTextPresent("/tmp");
    }

    public void testShouldSaveXSLTLogPublisherConfiguration() {
        beginAt(XSLT_URL);
        setWorkingForm("xsltlogpublisher-details");
        setFormElement("xsltFile", "templates/foobar.xslt");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("xsltlogpublisher-details");
        assertFormElementPresent("xsltFile");
        assertTextPresent("templates/foobar.xslt");
    }

    public void testShouldAllowUsersToClearFTPPublisherAttributes() {
        String destDir = "/tmp";

        beginAt(FTP_URL);
        setWorkingForm("ftppublisher-details");
        setFormElement("destDir", destDir);
        submit();
        assertTextPresent("Updated configuration.");
        assertTextPresent(destDir);

        gotoPage(FTP_URL);
        assertTextPresent(destDir);
        setWorkingForm("ftppublisher-details");
        setFormElement("destDir", "");
        submit();
        assertTextPresent("Updated configuration.");
        assertTextNotPresent(destDir);

        gotoPage(FTP_URL);
        assertTextNotPresent(destDir);
    }

    public void testShouldAllowUsersToClearXSLTLogPublisherAttributes() {
        String xsltFile = "templates/foo.xslt";

        beginAt(XSLT_URL);
        setWorkingForm("xsltlogpublisher-details");
        setFormElement("xsltFile", xsltFile);
        submit();
        assertTextPresent("Updated configuration.");
        assertTextPresent(xsltFile);

        gotoPage(XSLT_URL);
        assertTextPresent(xsltFile);
        setWorkingForm("xsltlogpublisher-details");
        setFormElement("xsltFile", "");
        submit();
        assertTextPresent("Updated configuration.");
        assertTextNotPresent(xsltFile);

        gotoPage(XSLT_URL);
        assertTextNotPresent(xsltFile);
    }
}
