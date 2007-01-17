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

public class SourceControlDetailsWebTest extends WebTestCase {
    private static final String BASE = "/cruisecontrol/load-details.jspa?"
        + "project=connectfour&pluginType=sourcecontrol";
    private static final String CVS_URL = BASE + "&pluginName=cvs";
    private static final String SVN_URL = BASE + "&pluginName=svn";

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

    public void testShouldBeAccessibleFromSourceControlsPage() {
        String pluginsUrl = "/cruisecontrol/plugins.jspa?project=connectfour&pluginType=sourcecontrol";

        beginAt(pluginsUrl);
        assertLinkPresentWithText("cvs");
    }

    public void testShouldLoadCVSConfiguration() {
        beginAt(CVS_URL);
        assertFormPresent("cvs-details");
        assertFormElementPresent("localWorkingCopy");
        assertTextPresent("projects/${project.name}");
        assertFormElementPresent("cvsRoot");
        assertFormElementPresent("module");
        assertFormElementPresent("tag");
        assertFormElementPresent("property");
        assertFormElementPresent("propertyOnDelete");
    }

    public void testShouldLoadSVNConfiguration() {
        beginAt(SVN_URL);
        assertFormPresent("svn-details");
        assertFormElementPresent("localWorkingCopy");
        assertFormElementPresent("password");
        assertFormElementPresent("property");
        assertFormElementPresent("propertyOnDelete");
        assertFormElementPresent("repositoryLocation");
        assertFormElementPresent("username");
    }

    public void testShouldSaveCVSConfiguration() {
        beginAt(CVS_URL);
        setWorkingForm("cvs-details");
        setFormElement("localWorkingCopy", "foo/bar");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("cvs-details");
        assertFormElementPresent("localWorkingCopy");
        assertTextPresent("foo/bar");
    }

    public void testShouldSaveSVNConfiguration() {
        beginAt(SVN_URL);
        setWorkingForm("svn-details");
        setFormElement("localWorkingCopy", "repos/trunk/foobar");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("svn-details");
        assertFormElementPresent("localWorkingCopy");
        assertTextPresent("repos/trunk/foobar");
    }

    public void testShouldAllowUsersToClearCVSAttributes() {
        String cvsroot = "/cvs/foo";

        beginAt(CVS_URL);
        setWorkingForm("cvs-details");
        setFormElement("cvsRoot", cvsroot);
        submit();
        assertTextPresent("Updated configuration.");
        assertTextPresent(cvsroot);

        gotoPage(CVS_URL);
        assertTextPresent(cvsroot);
        setWorkingForm("cvs-details");
        setFormElement("cvsRoot", "");
        submit();
        assertTextPresent("Updated configuration.");
        assertTextNotPresent(cvsroot);

        gotoPage(CVS_URL);
        assertTextNotPresent(cvsroot);
    }

    public void testShouldAllowUsersToClearSVNAttributes() {
        String repositoryLocation = "/cvs/foo";

        beginAt(SVN_URL);
        setWorkingForm("svn-details");
        setFormElement("repositoryLocation", repositoryLocation);
        submit();
        assertTextPresent("Updated configuration.");
        assertTextPresent(repositoryLocation);

        gotoPage(SVN_URL);
        assertTextPresent(repositoryLocation);
        setWorkingForm("svn-details");
        setFormElement("repositoryLocation", "");
        submit();
        assertTextPresent("Updated configuration.");
        assertTextNotPresent(repositoryLocation);

        gotoPage(SVN_URL);
        assertTextNotPresent(repositoryLocation);
    }
}
