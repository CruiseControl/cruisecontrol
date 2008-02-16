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

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import java.io.File;

public class ActiveBuildPageTest extends SeleniumTestCase {
    public void testShouldShowElapsedTimeOnActiveBuildPage() throws Exception {
        openBuildsPage();
        forceBuildByClick("project1");
        textShouldAppearInCertainTime("Your build is scheduled", FORCE_BUILD_DURATION);
        textShouldAppearInCertainTime(BUILDING_STARTED, FORCE_BUILD_DURATION);

        clickToOpenBuildDetailPageOf("project1");
        textShouldAppearInCertainTime("00:", 3 * AJAX_DURATION);
        shouldHaveCommitMessageInLiveBuild();
        textShouldPresent("project1 is now building");
        textShouldDisappearInCertainTime("now building", BUILD_DURATION);
        assertTrue(user.getLocation().indexOf("build/detail/project1") > -1);
    }

    private void shouldHaveCommitMessageInLiveBuild() {
        textShouldAppearInCertainTime("joe", LONG_TIME);
        textShouldAppearInCertainTime("dev", LONG_TIME);
        textShouldAppearInCertainTime("build.xml", LONG_TIME);
        textShouldAppearInCertainTime("file1.txt", LONG_TIME);
        textShouldAppearInCertainTime("file2.txt", LONG_TIME);
        textShouldAppearInCertainTime("567", LONG_TIME);
        textShouldAppearInCertainTime("123", LONG_TIME);
    }

    public void testShouldShowElapsedTimeOnMultipleActiveBuildPage() throws Exception {
        openBuildsPage();
        forceBuildByClick("project1");
        forceBuildByClick("project2");
        forceBuildByClick("cclive");
        textShouldAppearInCertainTime("3 project(s) building", FORCE_BUILD_DURATION);
        textShouldPresent("40%");
        textShouldAppearInCertainTime("0 project(s) building", 60);
    }

    public void testShouldDisplayDefaultMessageWhenNoCommitMessage() throws Exception {
        openBuildsPage();
        forceBuildByClick("projectWithoutPublishers");
        textShouldAppearInCertainTime(BUILDING_STARTED, FORCE_BUILD_DURATION);

        clickToOpenBuildDetailPageOf("projectWithoutPublishers");
        textShouldAppearInCertainTime("No new code", 2 * AJAX_DURATION);
        textShouldDisappearInCertainTime("now building", BUILD_DURATION);
    }

    public void testShouldDisplayErrorMessageWhenInactiveBuildFinishedAndDidNotGenerateAnyLog() throws Exception {
        openBuildsPage();
        forceBuildByClick("cc-live-2");
        textShouldAppearInCertainTime(BUILDING_STARTED, FORCE_BUILD_DURATION);
        clickToOpenBuildDetailPageOf("cc-live-2");
        textShouldAppearInCertainTime("Failed to find log in ", LONG_TIME);
    }
}
