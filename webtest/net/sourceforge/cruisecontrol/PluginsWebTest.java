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

public class PluginsWebTest extends WebTestCase {
    private static final String BASE = "/cruisecontrol/plugins.jspa?project=commons-math";

    private static final String BOOTSTRAPPERS_URL = BASE + "&pluginType=bootstrappers";
    
    private static final String PUBLISHERS_URL = BASE + "&pluginType=publishers";
    
    private static final String SOURCE_CONTROLS_URL = BASE + "&pluginType=modificationset";
    
    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
    }
    
    public void testShouldBeAccessibleFromBasicConfigPage() throws Exception {
        beginAt("/cruisecontrol/config.jspa");
        assertLinkPresentWithText("Configure Bootstrappers");
        clickLinkWithText("Configure Bootstrappers");
        assertTextPresent("bootstrappers Configuration");

        gotoPage("/cruisecontrol/config.jspa");
        assertLinkPresentWithText("Configure Publishers");
        clickLinkWithText("Configure Publishers");
        assertTextPresent("publishers Configuration");
        
        gotoPage("/cruisecontrol/config.jspa");
        assertLinkPresentWithText("Configure Source Control");
        clickLinkWithText("Configure Source Control");
        assertTextPresent("modificationset Configuration");
    }

    public void testShouldListAvailableBootstrappers() {
        beginAt(BOOTSTRAPPERS_URL);
        assertLinkPresentWithText("Configure accurevbootstrapper");
        assertLinkPresentWithText("Configure alienbrainbootstrapper");
        assertLinkPresentWithText("Configure antbootstrapper");
        assertLinkPresentWithText("Configure clearcasebootstrapper");
        assertLinkPresentWithText("Configure cmsynergybootstrapper");
        assertLinkPresentWithText("Configure currentbuildstatusbootstrapper");
//        assertLinkPresentWithText("Configure currentbuildstatusftpbootstrapper");
        assertLinkPresentWithText("Configure cvsbootstrapper");
        assertLinkPresentWithText("Configure p4bootstrapper");
        assertLinkPresentWithText("Configure snapshotcmbootstrapper");
        assertLinkPresentWithText("Configure svnbootstrapper");
        assertLinkPresentWithText("Configure vssbootstrapper");
    }
    
    public void testShouldListAvailablePublishers() {
        beginAt(PUBLISHERS_URL);
        assertLinkPresentWithText("Configure antpublisher");
        assertLinkPresentWithText("Configure cmsynergybaselinepublisher");
        assertLinkPresentWithText("Configure cmsynergytaskpublisher");
        assertLinkPresentWithText("Configure currentbuildstatuspublisher");
//        assertLinkPresentWithText("Configure currentbuildstatusftppublisher");
        assertLinkPresentWithText("Configure email");
        assertLinkPresentWithText("Configure execute");
        assertLinkPresentWithText("Configure ftppublisher");
        assertLinkPresentWithText("Configure htmlemail");
        assertLinkPresentWithText("Configure jabber");
        assertLinkPresentWithText("Configure onfailure");
        assertLinkPresentWithText("Configure onsuccess");
//        assertLinkPresentWithText("Configure rss");
        assertLinkPresentWithText("Configure scp");
        assertLinkPresentWithText("Configure socket");
        assertLinkPresentWithText("Configure x10");
        assertLinkPresentWithText("Configure xsltlogpublisher");
    }
    
    public void testShouldListAvailableSourceControls() {
        beginAt(SOURCE_CONTROLS_URL);
        assertLinkPresentWithText("Configure accurev");
        assertLinkPresentWithText("Configure alienbrain");
        assertLinkPresentWithText("Configure alwaysbuild");
        assertLinkPresentWithText("Configure buildstatus");
        assertLinkPresentWithText("Configure clearcase");
        assertLinkPresentWithText("Configure cmsynergy");
        assertLinkPresentWithText("Configure compound");
        assertLinkPresentWithText("Configure cvs");
        assertLinkPresentWithText("Configure filesystem");
        assertLinkPresentWithText("Configure forceonly");
//        assertLinkPresentWithText("Configure httpfile");
        assertLinkPresentWithText("Configure mks");
        assertLinkPresentWithText("Configure p4");
        assertLinkPresentWithText("Configure pvcs");
        assertLinkPresentWithText("Configure snapshotcm");
        assertLinkPresentWithText("Configure svn");
        assertLinkPresentWithText("Configure vss");
        assertLinkPresentWithText("Configure vssjournal");
    }
}
