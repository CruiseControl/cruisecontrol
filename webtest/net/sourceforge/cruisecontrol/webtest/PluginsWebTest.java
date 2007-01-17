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

public class PluginsWebTest extends WebTestCase {
    private static final String BASE = "/cruisecontrol/plugins.jspa?project=connectfour";
    private static final String ALL_BOOTSTRAPPERS_URL = BASE + "&pluginType=bootstrapper";
    private static final String ALL_PUBLISHERS_URL = BASE + "&pluginType=publisher";
    private static final String ALL_SOURCE_CONTROLS_URL = BASE + "&pluginType=sourcecontrol";

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
        beginAt(ALL_BOOTSTRAPPERS_URL);
        assertFormPresent("load-bootstrapper");
        assertFormElementPresent("pluginName");
    }

    public void testShouldListAvailablePublishers() {
        beginAt(ALL_PUBLISHERS_URL);
        assertFormPresent("load-publisher");
        assertFormElementPresent("pluginName");
    }

    public void testShouldListAvailableSourceControls() {
        beginAt(ALL_SOURCE_CONTROLS_URL);
        assertFormPresent("load-sourcecontrol");
        assertFormElementPresent("pluginName");
    }
}
