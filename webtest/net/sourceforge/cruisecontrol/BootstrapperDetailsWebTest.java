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
package net.sourceforge.cruisecontrol;

import net.sourceforge.jwebunit.WebTestCase;

public class BootstrapperDetailsWebTest extends WebTestCase {
    private static final String BASE = "/cruisecontrol/details!default.jspa?"
            + "project=commons-math&pluginType=bootstrapper";

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
        String pluginsUrl = "/cruisecontrol/plugins.jspa?project=commons-math&pluginType=bootstrapper";

        beginAt(pluginsUrl);
        assertLinkPresentWithText("cvsbootstrapper");

        gotoPage(pluginsUrl);
        assertLinkPresentWithText("svnbootstrapper");
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
        assertFormPresent("commons-math-config");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
        assertTextPresent("&lt;cvsbootstrapper localWorkingCopy=&quot;projects/jakarta-commons/cli&quot; /&gt;");
    }

    public void testShouldSaveSVNBootstrapperConfiguration() {
        beginAt(SVN_URL);
        setWorkingForm("svnbootstrapper-details");
        setFormElement("localWorkingCopy", "repos/trunk/foobar");
        submit();
        assertTextPresent("Updated configuration.");
        assertFormPresent("commons-math-config");
        assertFormElementPresent("contents");
        assertTextPresent("&lt;cruisecontrol&gt;");
        assertTextPresent("&lt;/cruisecontrol&gt;");
        assertTextPresent("&lt;svnbootstrapper localWorkingCopy=&quot;repos/trunk/foobar&quot; /&gt;");
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
