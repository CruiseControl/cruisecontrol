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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.BuildLoopQueryServiceStub;
import net.sourceforge.cruisecontrol.util.OSEnvironment;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public abstract class SeleniumTestCase extends TestCase {

    private String originalContent;

    private File configXmlOfWebApp = null;

    protected Selenium user;

    private String browser = "";
    private boolean didChangeConfigFile;

    public SeleniumTestCase() {
        String browserPath = new OSEnvironment().getVariable("BROWSER_PATH");
        if (StringUtils.isEmpty(browserPath)) {
            throw new RuntimeException("You must define browser " + "path using env variable BROWSER_PATH");
        }
        if (StringUtils.containsIgnoreCase(browserPath, "firefox")) {
            browser = "*firefox";
        } else if (StringUtils.containsIgnoreCase(browserPath, "iexplore")) {
            browser = "*iexplore";
        } else {
            throw new RuntimeException("BROWSER_PATH should either point to firefox or IE");
        }
    }

    public final void setUp() throws Exception {
        user = new DefaultSelenium("localhost", 4444, browser, "http://localhost:9090");
        user.start();
        DataUtils.cloneCCHome();
        didChangeConfigFile = false;
        doSetUp();
    }

    public final void tearDown() throws Exception {
        doTearDown();
        rollbackConfigFile();
        user.stop();
    }

    protected void doSetUp() throws Exception {
    }

    protected void doTearDown() throws Exception {
    }

    protected void willChangeConfigFile() throws Exception {
        configXmlOfWebApp = DataUtils.getConfigXmlOfWebApp();
        originalContent = FileUtils.readFileToString(configXmlOfWebApp, "UTF-8");
    }

    protected Document getHtmlDom() throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new ByteArrayInputStream(user.getHtmlSource().getBytes())));
        return parser.getDocument();
    }

    protected void hasClassName(String id, String className) throws Exception {
        String classNames = getClassName(id);
        assertTrue(StringUtils.contains(classNames, className));
    }

    protected String getAttribute(String id, String attributeName) throws Exception {
        Element element = getHtmlDom().getElementById(id);
        return element.getAttribute(attributeName);
    }

    protected void hasNoClassName(String id, String className) throws Exception {
        String classNames = getClassName(id);
        assertFalse(StringUtils.contains(classNames, className));
    }

    private String getClassName(String id) throws Exception {
        Document htmlDom = getHtmlDom();
        String classNames = htmlDom.getElementById(id).getAttribute("class");
        return classNames;
    }

    protected static final int AJAX_DURATION = 5;

    protected static final int FORCE_BUILD_DURATION = AJAX_DURATION * 3;

    protected static final int BUILD_DURATION =
            AJAX_DURATION * (BuildLoopQueryServiceStub.BUILD_TIMES.intValue() * 2);

    protected static final int LONG_TIME = 60;

    protected static final String BUILDING_STARTED = "Elapsed";

    protected void forceBuildByClick(String projectName) {
        user.click(projectName + "_forcebuild");
    }

    protected void forceBuildByRequest(String projectName) {
        user.open("/dashboard/forcebuild.ajax?projectName=" + projectName);
        user.goBack();
    }

    protected void textShouldAppearInCertainTime(String text, int seconds) {
        String textPresent = "selenium.isTextPresent(\"" + text + "\")";
        user.waitForCondition(textPresent, "" + seconds * 1000);
    }

    protected void textShouldDisappearInCertainTime(String text, int seconds) {
        String textPresent = "!selenium.isTextPresent(\"" + text + "\")";
        user.waitForCondition(textPresent, "" + seconds * 1000);
    }

    protected void waitingForElementDisappear(String id, int seconds) {
        String isElementHidden = "!selenium.isVisible(\"" + id + "\")";
        user.waitForCondition(isElementHidden, "" + seconds * 1000);
    }

    protected void elementShouldAppearInCertainTime(String id, int seconds) {
        String isElementAppear = "selenium.isVisible(\"" + id + "\")";
        user.waitForCondition(isElementAppear, "" + seconds * 1000);
    }

    protected void textShouldPresent(String text) {
        assertTrue(user.isTextPresent(text));
    }

    protected void textShouldNOTPresent(String text) {
        assertFalse(user.isTextPresent(text));
    }

    protected void textShouldBeContainedInHtmlSource(String text) {
        assertTrue(user.getHtmlSource().toLowerCase(), StringUtils.contains(user.getHtmlSource()
                .toLowerCase(), text));
    }

    protected void elementShouldPresent(String elementId) {
        assertTrue(user.isElementPresent(elementId));
    }

    protected void elementShouldNotBePresent(String elementId) {
        assertFalse(user.isElementPresent(elementId));
    }

    protected void elementShouldBeVisible(String elementId) {
        assertTrue(user.isVisible(elementId));
    }

    protected void elementShouldNOTBeVisible(String elementId) {
        assertFalse(user.isVisible(elementId));
    }

    protected void elementShouldContainText(String elementId, String text) {
        assertEquals(text, user.getText(elementId));
    }

    protected void openDashboardPage() {
        openAndWaiting("/dashboard/tab/dashboard");
    }

    protected void openBuildsPage() {
        openAndWaiting("/dashboard/tab/builds");
    }

    protected void openAllBuilsPage(String projectName) {
        openAndWaiting("/dashboard/project/list/all/" + projectName);
    }

    protected void openAllSuccessfulBuilsPage(String projectName) {
        openAndWaiting("/dashboard/project/list/all/" + projectName);
    }

    protected void openBuildDetailPage(String projectName, String logFile) {
        openAndWaiting("/dashboard/tab/build/detail/" + projectName + "/" + logFile);
    }

    protected void openBuildDetailPageDirectly(String projectName) {
        openAndWaiting("/dashboard/tab/build/detail/" + projectName);
    }

    protected void clickToOpenBuildDetailPageOf(String projectName) throws Exception {
        clickAndWait(projectName + "_build_detail");
    }

    protected void clickConfigPanelOf(String projectName) throws Exception {
        user.click(projectName + "_config_panel");
    }

    protected void clickLinkWithTextAndWait(String text) throws Exception {
        clickElementWithTextAndWait("a", text);
    }

    protected void clickElementWithText(String tag, String text) {
        user.click("//" + tag + "[text()='" + text + "']");
    }

    protected void clickElementWithTextAndWait(String tag, String text) throws Exception {
        clickElementWithText(tag, text);
        Thread.sleep(1000);
    }

    protected void clickAndWait(String elementSelector) throws Exception {
        user.click(elementSelector);
        user.waitForPageToLoad("50000");
    }

    protected void waitUntilStatisticsChange(String textBefore, String status, int seconds) throws Exception {
        int elapsed = 0;
        while (elapsed < seconds * 1000) {
            if (!StringUtils.equalsIgnoreCase(textBefore, user.getText("statistics_" + status))) {
                return;
            }
            Thread.sleep(500);
            elapsed += 500;
        }
        throw new Exception("Time out after " + seconds);
    }

    private void openAndWaiting(String url) {
        user.open(url);
        user.waitForPageToLoad("20000");
    }

    protected boolean waitForElementPresentWithRefresh(String id, int seconds) throws Exception {
        int total = seconds * 1000;
        int elapsed = 0;

        while (elapsed < total) {
            if (user.isElementPresent(id)) {
                return true;
            }
            Thread.sleep(100);
            elapsed += 100;
            user.refresh();
        }

        return false;
    }

    protected void addProjectToConfigFile(String projectName) throws IOException {
        if (configXmlOfWebApp == null) { throw new RuntimeException("Must call willChangeConfigFile() in doSetUp()"); }
        didChangeConfigFile = true;
        String newConfig = StringUtils.replace(originalContent,
                "</cruisecontrol>",
                xmlSnippet(projectName) + "</cruisecontrol>");
        FileUtils.writeStringToFile(configXmlOfWebApp, newConfig);
    }

    protected void rollbackConfigFile() throws IOException {
        if (didChangeConfigFile) {
            FileUtils.writeStringToFile(configXmlOfWebApp, originalContent);
        }
    }

    private String xmlSnippet(String projectName) {
        return "<project name=\"" + projectName + "\">"
                + "<listeners>"
                + "<currentbuildstatuslistener file=\"logs/${project.name}/status.txt\"/>"
                + "</listeners>"
                + "<bootstrappers>"
                + "<svnbootstrapper localWorkingCopy=\"projects/${project.name}\"/>"
                + "</bootstrappers>"
                + "<modificationset>"
                + "<svn localWorkingCopy=\"projects/${project.name}\"/>"
                + "</modificationset>"
                + "<schedule>"
                + "<ant buildfile=\"projects/${project.name}/build.xml\"/>"
                + "</schedule>"
                + "<publishers>"
                + "<onsuccess>"
                + "<artifactspublisher dest=\"artifacts/${project.name}\" file=\"projects/${project.name}/target/${project.name}.jar\"/>"
                + "</onsuccess>" + "</publishers>" + "</project>";
    }
}
