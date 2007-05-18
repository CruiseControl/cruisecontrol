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

import java.net.InetAddress;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;


public class LatestBuildsTest extends BaseFunctionalTest {

    protected void onSetUp() throws Exception {
        setConfigFileAndSubmitForm(DataUtils.getConfigXmlAsFile().getAbsolutePath());
    }

    public void testShouldBeAbleToVisitTheButton() throws Exception {
        tester.beginAt("/dashboard");
        tester.assertTextPresent("project1");
        tester.assertTextPresent("project2");
        tester.assertTextPresent("1 project build(s) succeed");
        tester.assertTextPresent("0 project(s) building");
        tester.assertTextPresent("2 project build(s) failed");
        tester.assertTextPresent("Server : " + InetAddress.getLocalHost().getHostName());
    }

    public void testShouldUseDifferentClassNameForProjectSummary() throws Exception {
        tester.beginAt("/dashboard?s=1");
        tester.assertElementPresentByXPath("//div[@id='project_summary_panel'][contains(@class, 'yui-g')]");
        tester.beginAt("/dashboard");
        tester.assertElementPresentByXPath("//div[@id='project_summary_panel'][contains(@class, 'yui-u')]");
    }

    public void testShouldNavigateToBuildDetailPageWhenClickTheBar() throws Exception {
        tester.beginAt("/dashboard");
        tester.assertLinkPresent("project1_bar_link");
        tester.clickLink("project1_bar_link");
        tester.assertTitleMatch("Project project1 - Build \\w");
    }
}
