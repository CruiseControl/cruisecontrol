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

import java.io.ByteArrayInputStream;
import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.TimeConverter;
import net.sourceforge.jwebunit.junit.WebTester;

import org.apache.commons.lang.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public abstract class BaseFunctionalTest extends TestCase {

    public static final String BASE_URL = "http://localhost:9090/dashboard/";

    protected static final String CONFIG_FILE_LOCATION_FIELD_NAME = "configFileLocation";

    protected static final String SPECIFY_CONFIG_FILE_URL = "/admin/config";

    protected static final String SET_CONFIG_FILE_LOCATION_FORM = "specifyConfigLocation";

    public static final String SET_CONFIGRATION_FORM = "setConfigration";

    public static final String BUILD_LOG_LOCATION = "cruiseloglocation";

    protected WebTester tester;

    public final void setUp() throws Exception {
        tester = new WebTester();
        tester.setScriptingEnabled(false);
        tester.getTestContext().setBaseUrl(BASE_URL);
        DataUtils.cloneCCHome();
        onSetUp();
    }

    protected void onSetUp() throws Exception {

    }

    protected void hasClassName(String htmlSource, String id, String className) throws Exception {
        Document htmlDom = getHtmlDom(htmlSource);
        String classNames = htmlDom.getElementById(id).getAttribute("class");
        assertTrue(StringUtils.contains(classNames, className));
    }

    protected Document getHtmlDom(String htmlSource) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new ByteArrayInputStream(htmlSource.getBytes())));
        return parser.getDocument();
    }

    protected final String getJSONWithAjaxInvocation(final String path) throws Exception {
        //The reporting system will cache the result for 5 second, jmx stub only changed
        //status when getAllProjectStatus invoked, so wait for 6 second and ping
        //server to make the jmx stub change status.
        Thread.sleep(6000);
        tester.beginAt(BASE_URL + path);
        return tester.getPageSource();
    }

    protected void typeLogLocation() throws Exception {
        File logs = new File(DataUtils.getConfigXmlOfWebApp().getParentFile(), "logs");
        tester.setTextField(CONFIG_FILE_LOCATION_FIELD_NAME, logs.getAbsolutePath());
    }

    protected String convertedTime(String time) {
        DateTime buildDateFromLogFileName = DateTimeFormat.forPattern("yyyyMMddHHmmss").parseDateTime(time);
        return new TimeConverter().getConvertedTime(buildDateFromLogFileName.toDate());
    }
}
