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

public class BuildDetailTest extends SeleniumTestCase {

    public void testShouldNotContainCodeLeakWhenAFailedTestcaseHasNoMessage() throws Exception {
        openBuildDetailPage("project1", "20051209122104");
        clickTab("Tests", "Test Suites");
        String testName = "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest (3)";
        clickElementWithText("h3", testName);
        textShouldNOTPresent("$esc");
    }

    public void testShouldShowUnknownIfCannotGetDuration() throws Exception {
        openBuildDetailPage("cclive", "20051209122104");
        textShouldPresent("Unknown");

        openBuildDetailPage("project space", "20051209122104");
        textShouldPresent("Unknown");
    }

    public void testShouldShowDurationIfBeAbleToGetDuration() throws Exception {
        openBuildDetailPage("project2", "20051209122103");
        textShouldNOTPresent("Unknown");
        textShouldPresent("3 minutes 10 seconds");
    }

    public void testShouldDisplayToolkitInBuildDetailPage() throws Exception {
        openBuildDetailPage("project2", "20051209122103");
        elementShouldPresent("project2_forcebuild");
        elementShouldPresent("project2_config_panel");

        clickConfigPanelOf("project2");
        elementShouldBeVisible("toolkit_project2");
        clickElementWithTextAndWait("span", "X");
        elementShouldNOTBeVisible("toolkit_project2");

        forceBuildByClick("project2");
        elementShouldAppearInCertainTime("trans_message", 6);

        openBuildsPage();
        textShouldAppearInCertainTime(BUILDING_STARTED, FORCE_BUILD_DURATION);

        openBuildDetailPageDirectly("project2");
        //Work around Time Dependency
        if (user.isTextPresent("now building")) {
            elementShouldNOTBeVisible("project2_forcebuild");
            elementShouldNOTBeVisible("project2_config_panel");
            textShouldAppearInCertainTime("see details", 30);
        }
        elementShouldPresent("project2_forcebuild");
        elementShouldPresent("project2_config_panel");
    }

    public void testShouldShowHyperLinkOnCommitMessageForActiveBuild() throws Exception {
        openBuildsPage();
        forceBuildByClick("project1");
        textShouldAppearInCertainTime(BUILDING_STARTED, FORCE_BUILD_DURATION);

        clickToOpenBuildDetailPageOf("project1");
        textShouldAppearInCertainTime("joe", BUILD_DURATION);
        textShouldBeContainedInHtmlSource("fixed the <a href=\"https://mingle05.thoughtworks.com/projects/project1/cards/456\">build456</a>");
    }

    public void testShouldDisplayErrorPageWhenBuildFileIsMissing() throws Exception {
        openBuildDetailPage("bbc", "12312313");
        textShouldPresent("The requested build log 12312313 does not exist in project bbc");
    }

    public void testShouldDisplayErrorPageWhenProjectIsMissing() throws Exception {
        openBuildDetailPageDirectly("bbc");
        textShouldPresent("The requested project bbc does not exist or does not have any logs");
    }

    public void testShouldDisplayErrorPageWhenNoProjectSpecified() throws Exception {
        openBuildDetailPageDirectly("");
        textShouldPresent("No project specified");
    }

    private void clickTab(String tabName, String textToBeShown) {
        clickElementWithText("span", tabName);
        textShouldAppearInCertainTime(textToBeShown, 10);
    }

//TODO check whether selenium has the bug on detecting the visibility of element. 
//    private void assertHiddenElementNowCanBeVisible(String text) throws Exception {
//    	assertFalse(this.user.isVisible("//h2[text()='" + text + "']/../div[@class='collapsible_content']/pre"));
//        clickElementWithText("h2", text);
//        Thread.sleep(3000);
//        assertTrue(this.user.isVisible("//h2[text()='" + text + "']/../div[@class='collapsible_content']/pre"));
//    }

//    private void assertHiddenElementIdNowCanBeVisible(String hiddenElement, String clickElement) throws Exception {
//        elementShouldNOTBeVisible(hiddenElement);
//        user.click(clickElement);
//        Thread.sleep(4000);
//        elementShouldBeVisibleUsingPrototype(hiddenElement);
//    }
//    public void testShouldExpandTestsWhenClicked() throws Exception {
//        openBuildDetailPage("project2", "20051209122103");
//        clickTab("Tests", "Test Suites");
//
//        assertHiddenElementNowCanBeVisible("Test Suites (1)");
//
//        String testName = "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest";
//        assertHiddenElementIdNowCanBeVisible("error_" + testName, "title_error_" + testName);
//        assertHiddenElementIdNowCanBeVisible("failed_" + testName, "title_failed_" + testName);
//    }
//
//    public void testShouldNotDisplayTestErrorAndFailuresWhenThereArentAny() throws Exception {
//        openBuildDetailPage("queuedPassed", "20051209122103");
//        clickTab("Tests", "Test Suites");
//
//        assertHiddenElementNowCanBeVisible("Test Suites (2)");
//        textShouldNOTPresent("Test Errors");
//        textShouldNOTPresent("Test Failures");
//    }
//    public void testShouldExpandErrorsAndWarningElementsWhenTheyExist() throws Exception {
//        openBuildDetailPage("project2", "20051209122103");
//        clickTab("Errors and Warnings", "Stacktrace");
//
//        assertHiddenElementNowCanBeVisible("Stacktrace");
//        assertHiddenElementNowCanBeVisible("Errors and Warnings");
//    }
//    public void testShouldIndicateNotExistsWhenTheyDoNotExist() throws Exception {
//        openBuildDetailPage("queuedPassed", "log20051209122103Lbuild.489.xml");
//        clickTab("Errors and Warnings", "Stacktrace");
//
//        elementShouldBeVisibleUsingPrototype("stacktrace");
//        elementShouldContainText("stacktrace", "No stacktrace");
//
//        elementShouldBeVisibleUsingPrototype("errors_and_warnings_element");
//        elementShouldContainText("errors_and_warnings_element", "No errors or warnings");
//    }
//    private void elementShouldBeVisibleUsingPrototype(String element) {
//        String isVisibleString = "this.browserbot.findElement('id=" + element + "').visible()";
//        assertEquals("true", user.getEval(isVisibleString));
//    }



}

