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

import org.apache.commons.lang.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.CruiseControlJMXServiceStub;
import net.sourceforge.cruisecontrol.util.OSEnvironment;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public abstract class SeleniumTestCase extends TestCase {

    protected Selenium selenium;

    private String browser = "";

    public SeleniumTestCase() {
        String browserPath = new OSEnvironment().getVariable("BROWSER_PATH");
        if (StringUtils.isEmpty(browserPath)) {
            throw new RuntimeException("You must define browser "
                    + "path using env variable BROWSER_PATH");
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
        selenium = new DefaultSelenium("localhost", 4444, browser, "http://localhost:9090");
        selenium.start();
        DataUtils.cloneCCHome();
        setConfigFileLocation();
        doSetUp();
    }

    protected void doSetUp() throws Exception {
    }

    private void setConfigFileLocation() throws Exception {
        selenium.open("/dashboard/admin/config");
        selenium.type("configFileLocation", DataUtils.getConfigXmlInArbitraryCCHome().getAbsolutePath());
        selenium.submit("specifyConfigLocation");
        selenium.waitForPageToLoad("10000");
    }

    public final void tearDown() throws Exception {
        doTearDown();
        selenium.stop();
    }

    protected void clickLinkWithTextAndWait(String text, int milliseconds) throws Exception {
        selenium.click("//a[text()='" + text + "']");
        Thread.sleep(milliseconds);
    }

    protected void clickLinkWithIdAndWait(String text) throws Exception {
        selenium.click(text);
        selenium.waitForPageToLoad("20000");
    }

    protected Document getHtmlDom(String htmlSource) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new ByteArrayInputStream(htmlSource.getBytes())));
        return parser.getDocument();
    }

    protected void doTearDown() throws Exception {
    }

    protected void hasClassName(String htmlSource, String id, String className) throws Exception {
        Document htmlDom = getHtmlDom(htmlSource);
        String classNames = htmlDom.getElementById(id).getAttribute("class");
        assertTrue(StringUtils.contains(classNames, className));
    }

    protected static final int AJAX_DURATION = 5;

    protected static final int FORCE_BUILD_DURATION = AJAX_DURATION * 3;

    protected static final int BUILD_DURATION =
            AJAX_DURATION * (CruiseControlJMXServiceStub.BUILD_TIMES.intValue() + 2);

    protected static final String BUILDING_STARTED = "remaining";

    protected void waitingForTextAppear(String text, int seconds) {
        String textPresent = "selenium.isTextPresent(\"" + text + "\")";
        selenium.waitForCondition(textPresent, "" + seconds * 1000);
    }

    protected void waitingForTextDisappear(String text, int seconds) {
        String textPresent = "!selenium.isTextPresent(\"" + text + "\")";
        selenium.waitForCondition(textPresent, "" + seconds * 1000);
    }

    protected void openAndWaiting(String url, int seconds) {
        selenium.open(url);
        selenium.waitForPageToLoad("" + seconds * 1000);
    }
}
