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
import org.joda.time.DateTime;

public class FailedBuildMoreThan24HourTest extends SeleniumTestCase {

    private File failedLevel8;

    private File failedLevel7;

    private File failedLevel6;

    private File failedLevel5;

    private File failedLevel4;

    private File failedLevel3;

    private File failedLevel2;

    private File failedLevel1;

    private File failedLevel0;

    private File justSucceeded;

    private File justFailed;

    protected void doSetUp() throws Exception {
        File projectWithoutPublishers = new File(DataUtils.getLogRootOfWebapp(), "projectWithoutPublishers");
        DateTime now = new DateTime();
        failedLevel8 =
                new File(projectWithoutPublishers, "log" + CCDateFormatter.yyyyMMddHHmmss(now.minusDays(2))
                        + ".xml");
        failedLevel7 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(7 * 180 + 12)) + ".xml");
        failedLevel6 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(6 * 180 + 12)) + ".xml");
        failedLevel5 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(5 * 180 + 12)) + ".xml");
        failedLevel4 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(4 * 180 + 12)) + ".xml");
        failedLevel3 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(3 * 180 + 12)) + ".xml");
        failedLevel2 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(2 * 180 + 12)) + ".xml");
        failedLevel1 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(1 * 180 + 12)) + ".xml");
        failedLevel0 =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(0 * 180 + 12)) + ".xml");
        justSucceeded =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(0 * 180 + 8)) + "Lbuild.123.xml");
        justFailed =
                new File(projectWithoutPublishers, "log"
                        + CCDateFormatter.yyyyMMddHHmmss(now.minusMinutes(0 * 180 + 5)) + ".xml");
    }

    protected void doTearDown() throws Exception {
        File[] files =
                new File[] {failedLevel8, failedLevel7, failedLevel6, failedLevel5, failedLevel4,
                        failedLevel3, failedLevel2, failedLevel1, failedLevel0, justSucceeded, justFailed};
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
        assertClassName(failedLevel0,  "level_0");
        assertClassName(failedLevel1,  "level_1");
        assertClassName(failedLevel2,  "level_2");
        assertClassName(failedLevel3,  "level_3");
        assertClassName(failedLevel4,  "level_4");
        assertClassName(failedLevel5,  "level_5");
        assertClassName(failedLevel6,  "level_6");
        assertClassName(failedLevel7,  "level_7");
        assertClassName(failedLevel8,  "level_8");
        assertClassName(justSucceeded, "level_0");
        assertClassName(justFailed,    "level_0");
    }

    private void assertClassName(File file, String className) throws Exception {
        file.createNewFile();
        String textPresent =
                "parent.frames['myiframe'].document.getElementById('projectWithoutPublishers_level').className.indexOf('"
                        + className + "') >= 0";
        selenium.waitForCondition(textPresent, "7000");
    }
}
