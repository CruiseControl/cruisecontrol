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

public class BootstrapperDetailsWebTest extends WebTestCase {
    private static final String BASE = "/cruisecontrol/load-details.jspa?"
            + "project=connectfour&pluginType=bootstrapper";

    private static final String CVS_URL = BASE + "&pluginName=cvsbootstrapper";

    private static final String SVN_URL = BASE + "&pluginName=svnbootstrapper";

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

    public void testShouldBeAccessibleFromBootstrappersPage() {
        String pluginsUrl = "/cruisecontrol/plugins.jspa?project=connectfour&pluginType=bootstrapper";

        beginAt(pluginsUrl);
        assertLinkPresentWithText("cvsbootstrapper");
    }

    public void testShouldLoadCVSBootstrapperConfiguration() {
        beginAt(CVS_URL);
        assertFormPresent("cvsbootstrapper-details");
        assertFormElementPresent("cvsroot");
        assertFormElementPresent("file");
        assertFormElementPresent("localWorkingCopy");
        assertFormElementPresent("overwriteChanges");
        assertFormElementPresent("resetStickyTags");
    }

    public void testShouldLoadSVNBootstrapperConfiguration() {
        beginAt(SVN_URL);
        assertFormPresent("svnbootstrapper-details");
        assertFormElementPresent("file");
        assertFormElementPresent("localWorkingCopy");
        assertFormElementPresent("password");
        assertFormElementPresent("username");
    }

    public void testShouldSaveCVSBootstrapperConfiguration() {
        beginAt(CVS_URL);
        setWorkingForm("cvsbootstrapper-details");
        setFormElement("localWorkingCopy", "projects/jakarta-commons/cli");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("cvsbootstrapper-details");
        assertFormElementPresent("localWorkingCopy");
        assertTextPresent("projects/jakarta-commons/cli");
    }

    public void testShouldSaveSVNBootstrapperConfiguration() {
        beginAt(SVN_URL);
        setWorkingForm("svnbootstrapper-details");
        setFormElement("localWorkingCopy", "repos/trunk/foobar");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("svnbootstrapper-details");
        assertFormElementPresent("localWorkingCopy");
        assertTextPresent("repos/trunk/foobar");
    }

    public void testShouldAllowUsersToClearCVSBootstrapperAttributes() {
        String cvsroot = "/cvs/foo";

        beginAt(CVS_URL);
        setWorkingForm("cvsbootstrapper-details");
        setFormElement("cvsroot", cvsroot);
        submit();
        assertTextPresent("Updated configuration.");
        assertTextPresent(cvsroot);

        gotoPage(CVS_URL);
        assertTextPresent(cvsroot);
        setWorkingForm("cvsbootstrapper-details");
        setFormElement("cvsroot", "");
        submit();
        assertTextPresent("Updated configuration.");
        assertTextNotPresent(cvsroot);

        gotoPage(CVS_URL);
        assertTextNotPresent(cvsroot);
    }

    public void testShouldAllowUsersToClearSVNBootstrapperAttributes() {
        String localWorkingCopy = "/cvs/foo";

        beginAt(SVN_URL);
        setWorkingForm("svnbootstrapper-details");
        setFormElement("localWorkingCopy", localWorkingCopy);
        submit();
        assertTextPresent("Updated configuration.");
        assertTextPresent(localWorkingCopy);

        gotoPage(SVN_URL);
        assertTextPresent(localWorkingCopy);
        setWorkingForm("svnbootstrapper-details");
        setFormElement("localWorkingCopy", "");
        submit();
        assertTextPresent("Updated configuration.");
        assertTextNotPresent(localWorkingCopy);

        gotoPage(SVN_URL);
        assertTextNotPresent(localWorkingCopy);
    }
}
