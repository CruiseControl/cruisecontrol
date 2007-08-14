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
package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import org.apache.commons.lang.StringUtils;

public class BuildDetailTest extends SeleniumTestCase {

    public void testShouldShowUnknownIfCannotGetDuration() throws Exception {
        selenium.open("/dashboard/build/detail/cclive/log20051209122104.xml");
        assertTrue(selenium.isTextPresent("Unknown"));

        selenium.open("/dashboard/build/detail/project space/log20051209122104Lbuild.467.xml");
        assertTrue(selenium.isTextPresent("Unknown"));
    }

    public void testShouldShowDurationIfBeAbleToGetDuration() throws Exception {
        selenium.open("/dashboard/build/detail/project2/log20051209122103.xml");
        assertFalse(selenium.isTextPresent("Unknown"));
        assertTrue(selenium.isTextPresent("3 minutes 10 seconds"));
    }

    public void testShouldShowHyperLinkOnCommitMessageForActiveBuild() throws Exception {
        openAndWaiting("/dashboard/dashboard", 5);
        selenium.click("//li[@id='builds']/a");
        selenium.click("project1_forcebuild");
        waitingForTextAppear(BUILDING_STARTED, FORCE_BUILD_DURATION);
        selenium.click("project1_build_detail");
        selenium.waitForPageToLoad("5000");
        waitingForTextAppear("build456", 5);
        String actual = selenium.getHtmlSource().toLowerCase();
        String expected =
                "fixed the <a href=\"https://mingle05.thoughtworks.com/projects/project1/cards/456\">build456</a>";
        assertTrue(actual, StringUtils.contains(actual, expected));
    }
}