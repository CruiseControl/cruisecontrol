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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.joda.time.DateTime;

public class BuildSummaryService {

    public Build createInactive(File logDirectory) {
        DateTime dateTime = new DateTime();
        String now = CCDateFormatter.yyyyMMddHHmmss(dateTime);
        String name = CCDateFormatter.format(dateTime, "yyyy-MM-dd HH:mm.ss");
        File file = new File(logDirectory, "log" + now + ".xml");
        return new BuildSummary(logDirectory.getName(), name, "", ProjectBuildStatus.INACTIVE, file
                .getAbsolutePath());
    }

    public Build createBuildSummary(File logFileXml) {
        if (logFileXml == null) {
            return null;
        }
        BuildMatcher buildMatcher = isPassedBuild(logFileXml);
        Matcher matcher = buildMatcher.pattern().matcher(logFileXml.getName());
        if (matcher.find()) {
            Build summary = buildMatcher.callBack(matcher);
            return summary;
        }
        return null;
    }

    private static final int YEAR_GROUP = 1;

    private static final int MONTH_GROUP = 2;

    private static final int DAY_GROUP = 3;

    private static final int HOUR_GROUP = 4;

    private static final int MINUTE_GROUP = 5;

    private static final int SECOND_GROUP = 6;

    private interface BuildMatcher {
        public Build callBack(Matcher matcher);

        public Pattern pattern();
    }

    private static class SuccessfulBuild implements BuildMatcher {
        private File filename;

        private static final Pattern SUCCESSFUL_BUILD_PATTERN =
                Pattern.compile("^log(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})L(.*)\\.xml$");

        private static final int LABEL_GROUP = 7;

        public SuccessfulBuild(File filename) {
            this.filename = filename;
        }

        public Build callBack(Matcher matcher) {
            String label = matcher.group(LABEL_GROUP);
            return new BuildSummary(filename.getParentFile().getName(), getDateTime(matcher), label,
                    ProjectBuildStatus.PASSED, filename.getAbsolutePath());
        }

        public Pattern pattern() {
            return SUCCESSFUL_BUILD_PATTERN;
        }
    }

    private static class FailedBuild implements BuildMatcher {
        private File filename;

        private static final Pattern FAILED_BUILD_PATTERN =
                Pattern.compile("^log(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})\\.xml");

        public FailedBuild(File filename) {
            this.filename = filename;
        }

        public Build callBack(Matcher matcher) {
            return new BuildSummary(filename.getParentFile().getName(), getDateTime(matcher), "",
                    ProjectBuildStatus.FAILED, filename.getAbsolutePath());
        }

        public Pattern pattern() {
            return FAILED_BUILD_PATTERN;
        }
    }

    private static String getDateTime(Matcher matcher) {
        return matcher.group(YEAR_GROUP) + "-" + matcher.group(MONTH_GROUP) + "-" + matcher.group(DAY_GROUP)
                + " " + matcher.group(HOUR_GROUP) + ":" + matcher.group(MINUTE_GROUP) + "."
                + matcher.group(SECOND_GROUP);
    }

    private static BuildMatcher isPassedBuild(File logfile) {
        return logfile.getName().indexOf("L") > 0 ? (BuildMatcher) new SuccessfulBuild(logfile)
                : (BuildMatcher) new FailedBuild(logfile);
    }
}
