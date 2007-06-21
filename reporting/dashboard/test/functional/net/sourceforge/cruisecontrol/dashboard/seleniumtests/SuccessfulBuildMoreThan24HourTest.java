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

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class SuccessfulBuildMoreThan24HourTest extends SeleniumTestCase {

    private File succeed4daysAgo;

    private File failed3DaysAgo;

    private File succeed2dayAgo;

    private File passedLevel0;

    private File passedLevel7;

    private File passedLevel6;

    private File passedLevel5;

    private File passedLevel4;

    private File passedLevel3;

    private File passedLevel2;

    private File passedLevel1;

    private File justFailed;

    private File justSucceeded;

    protected void doSetUp() throws Exception {
        File projectWithoutPublishers =
                new File(DataUtils.getLogRootOfWebapp(), "projectWithoutPublishers");
        DateTime now = new DateTime();
        succeed4daysAgo =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusDays(4)) + "Lbuild.510.xml");
        failed3DaysAgo =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusDays(3)) + ".xml");
        succeed2dayAgo =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusDays(2)) + "Lbuild.511.xml");
        passedLevel7 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(7 * 180 + 12))
                        + "Lbuild.513.xml");
        passedLevel6 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(6 * 180 + 12))
                        + "Lbuild.514.xml");
        passedLevel5 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(5 * 180 + 12))
                        + "Lbuild.515.xml");
        passedLevel4 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(4 * 180 + 12))
                        + "Lbuild.516.xml");
        passedLevel3 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(3 * 180 + 12))
                        + "Lbuild.516.xml");
        passedLevel2 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(2 * 180 + 12))
                        + "Lbuild.518.xml");
        passedLevel1 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(1 * 180 + 12))
                        + "Lbuild.519.xml");
        passedLevel0 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(0 * 180 + 12))
                        + "Lbuild.520.xml");
        justFailed =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(0 * 180 + 8)) + ".xml");
        justSucceeded =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(0 * 180 + 5))
                        + "Lbuild.521.xml");
    }

    protected void doTearDown() throws Exception {
        File[] files =
                new File[] {succeed4daysAgo, succeed2dayAgo, passedLevel0, failed3DaysAgo,
                        passedLevel7, passedLevel6, passedLevel5, passedLevel4, passedLevel3,
                        passedLevel2, passedLevel1, justFailed, justSucceeded};
        for (int i = 0; i < files.length; i++) {
            try {
                FileUtils.forceDelete(files[i]);
            } catch (Exception e) {
                continue;
            }
        }
    }

    public void testChangeColorWhenFailedDateChanges() throws Exception {
        selenium.open("/dashboard/dashboard");
        assertClassName(failed3DaysAgo, "failed_level_8");
        assertClassName(passedLevel0, "passed_level_0");
        assertClassName(passedLevel1, "passed_level_1");
        assertClassName(passedLevel2, "passed_level_2");
        assertClassName(passedLevel3, "passed_level_3");
        assertClassName(passedLevel4, "passed_level_4");
        assertClassName(passedLevel5, "passed_level_5");
        assertClassName(passedLevel6, "passed_level_6");
        assertClassName(passedLevel7, "passed_level_7");
        assertClassName(succeed2dayAgo, "passed_level_8");
        assertClassName(succeed4daysAgo, "passed_level_8");
        assertClassName(justFailed, "failed_level_0");
        assertClassName(justSucceeded, "passed_level_0");
    }

    private void assertClassName(File file, String className) throws Exception {
        String exptectedBar =
                "id=\"projectWithoutPublishers_bar\" class=\"bar round_corner " + className;
        file.createNewFile();
        Thread.sleep(7000);
        String htmlSource = selenium.getHtmlSource();
        assertTrue(StringUtils.contains(htmlSource, exptectedBar));
    }

}
