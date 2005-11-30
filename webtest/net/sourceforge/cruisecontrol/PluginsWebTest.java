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

    private static final String BOOTSTRAPPERS_URL = BASE + "&pluginType=bootstrapper";
    
    private static final String PUBLISHERS_URL = BASE + "&pluginType=publisher";
    
    private static final String SOURCE_CONTROLS_URL = BASE + "&pluginType=sourcecontrol";
    
    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
    }
    
    public void testShouldBeAccessibleFromBasicConfigPage() throws Exception {
        beginAt("/cruisecontrol/config.jspa");
        assertLinkPresentWithText("Bootstrappers");

        gotoPage("/cruisecontrol/config.jspa");
        assertLinkPresentWithText("Publishers");
        
        gotoPage("/cruisecontrol/config.jspa");
        assertLinkPresentWithText("Source Controls");
    }

    public void testShouldListAvailableBootstrappers() {
        beginAt(BOOTSTRAPPERS_URL);
        assertLinkPresentWithText("accurevbootstrapper");
        assertLinkPresentWithText("alienbrainbootstrapper");
        assertLinkPresentWithText("antbootstrapper");
        assertLinkPresentWithText("clearcasebootstrapper");
        assertLinkPresentWithText("cmsynergybootstrapper");
        assertLinkPresentWithText("currentbuildstatusbootstrapper");
        assertLinkPresentWithText("cvsbootstrapper");
        assertLinkPresentWithText("p4bootstrapper");
        assertLinkPresentWithText("snapshotcmbootstrapper");
        assertLinkPresentWithText("svnbootstrapper");
        assertLinkPresentWithText("vssbootstrapper");
    }
    
    public void testShouldListAvailablePublishers() {
        beginAt(PUBLISHERS_URL);
        assertLinkPresentWithText("antpublisher");
        assertLinkPresentWithText("cmsynergybaselinepublisher");
        assertLinkPresentWithText("cmsynergytaskpublisher");
        assertLinkPresentWithText("currentbuildstatuspublisher");
        assertLinkPresentWithText("email");
        assertLinkPresentWithText("execute");
        assertLinkPresentWithText("ftppublisher");
        assertLinkPresentWithText("htmlemail");
        assertLinkPresentWithText("jabber");
        assertLinkPresentWithText("onfailure");
        assertLinkPresentWithText("onsuccess");
        assertLinkPresentWithText("scp");
        assertLinkPresentWithText("socket");
        assertLinkPresentWithText("x10");
        assertLinkPresentWithText("xsltlogpublisher");
    }
    
    public void testShouldListAvailableSourceControls() {
        beginAt(SOURCE_CONTROLS_URL);
        assertLinkPresentWithText("accurev");
        assertLinkPresentWithText("alienbrain");
        assertLinkPresentWithText("alwaysbuild");
        assertLinkPresentWithText("buildstatus");
        assertLinkPresentWithText("clearcase");
        assertLinkPresentWithText("cmsynergy");
        assertLinkPresentWithText("compound");
        assertLinkPresentWithText("cvs");
        assertLinkPresentWithText("filesystem");
        assertLinkPresentWithText("forceonly");
        assertLinkPresentWithText("mks");
        assertLinkPresentWithText("p4");
        assertLinkPresentWithText("pvcs");
        assertLinkPresentWithText("snapshotcm");
        assertLinkPresentWithText("svn");
        assertLinkPresentWithText("vss");
        assertLinkPresentWithText("vssjournal");
    }
}
