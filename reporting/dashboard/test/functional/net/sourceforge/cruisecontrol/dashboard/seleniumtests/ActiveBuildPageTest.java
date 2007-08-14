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

public class ActiveBuildPageTest extends SeleniumTestCase {

    public void testShouldShowElapsedTimeOnActiveBuildPage() throws Exception {
        openAndWaiting("/dashboard/dashboard", 5);
        selenium.click("//li[@id='builds']/a");
        selenium.click("project1_forcebuild");
        assertTrue(selenium.isTextPresent("Your build is scheduled"));
        waitingForTextAppear(BUILDING_STARTED, FORCE_BUILD_DURATION);
        selenium.click("project1_build_detail");
        selenium.waitForPageToLoad("5000");
        waitingForTextAppear("00:", 3 * AJAX_DURATION);
        assertTrue(selenium.isTextPresent("project1 is now building"));
        assertTrue(selenium.isTextPresent("project1"));
        assertTrue(selenium.isTextPresent("joe"));
        assertTrue(selenium.isTextPresent("Some random change"));
        assertTrue(selenium.isTextPresent("dev"));
        assertTrue(selenium.isTextPresent("Fixed the build"));
        assertTrue(selenium.isTextPresent("project1 is now building"));
        waitingForTextDisappear("now building", BUILD_DURATION);
        assertTrue(selenium.getLocation().indexOf("build/detail/live/project1") > -1);
    }

    public void testShouldShowElapsedTimeOnMultipleActiveBuildPage() throws Exception {
        openAndWaiting("/dashboard/dashboard", 5);
        selenium.click("//li[@id='builds']/a");
        selenium.click("project1_forcebuild");
        selenium.click("project2_forcebuild");
        selenium.click("cclive_forcebuild");
        assertTrue(selenium.isTextPresent("Your build is scheduled"));
        waitingForTextAppear("3 project(s) building", FORCE_BUILD_DURATION);
        assertTrue(selenium.isTextPresent("40%"));
        waitingForTextAppear("0 project(s) building", BUILD_DURATION);
    }

    public void testShouldDisplayDefaultMessageWhenNoCommitMessage() throws Exception {
        openAndWaiting("/dashboard/dashboard", 5);
        selenium.click("//li[@id='builds']/a");
        selenium.click("projectWithoutPublishers_forcebuild");
        waitingForTextAppear(BUILDING_STARTED, FORCE_BUILD_DURATION);
        selenium.click("projectWithoutPublishers_build_detail");
        selenium.waitForPageToLoad("5000");
        assertTrue(selenium.isTextPresent("Build forced, No new code is committed into repository"));
        waitingForTextDisappear("now building", BUILD_DURATION);
    }
}