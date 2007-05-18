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
package net.sourceforge.cruisecontrol.dashboard.web.command;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import org.joda.time.DateTime;

public class BuildCommand {
    private static final int DAY_TO_SECOND = 24 * 3600;

    private Build build;

    private Map jsonParams = new HashMap();

    private static final int MILLI_SECOND_TO_SECOND = 1000;

    public BuildCommand(Build build) {
        this.build = build;
        jsonParams.put("css_class_name", build.getStatus().toLowerCase());
    }

    public Build getBuild() {
        return this.build;
    }

    public String getDateStringInHumanBeingReadingStyle() {
        return CCDateFormatter.getDateStringInHumanBeingReadingStyle(build.getBuildDate());
    }

    public Long getElapsedTimeBuilding(DateTime date) {
        return new Long((date.getMillis() - build.getBuildingSince().getMillis())
                / MILLI_SECOND_TO_SECOND);
    }

    public void updateFailedCSS(Build last) {
        if (isFailedMoreThan24Hours(last)) {
            jsonParams.put("css_class_name", "long_failed");
        } else {
            jsonParams.put("css_class_name", "failed");
        }
    }

    public void updatePassedCss(Build last) {
        if (isPassedMoreThan24Hours(last)) {
            jsonParams.put("css_class_name", "long_passed");
        } else {
            jsonParams.put("css_class_name", "passed");
        }
    }

    public void updateDefaultCss() {
        jsonParams.put("css_class_name", build.getStatus().toLowerCase());
    }

    public String getCssClassName() {
        return (String) jsonParams.get("css_class_name");
    }

    private boolean isPassedMoreThan24Hours(Build lastPassed) {
        long between =
                (new Date().getTime() - lastPassed.getBuildDate().getMillis())
                        / MILLI_SECOND_TO_SECOND;
        return between > DAY_TO_SECOND;
    }

    private boolean isFailedMoreThan24Hours(Build lastSuccessful) {
        long between;
        if (lastSuccessful == null) {
            between = (new Date().getTime() - build.getBuildDate().getMillis());
        } else {
            between = (new Date().getTime() - lastSuccessful.getBuildDate().getMillis());
        }
        between /= MILLI_SECOND_TO_SECOND;
        return between > DAY_TO_SECOND;
    }

    public Map toJsonHash() {
        jsonParams.put("building_status", build.getStatus());
        jsonParams.put("project_name", build.getProjectName());
        jsonParams.put("force_build_action_text", "Force Build");
        jsonParams.put("latest_build_log_file_name", build.getBuildLogFilename());
        jsonParams.put("latest_build_date", getDateStringInHumanBeingReadingStyle());
        jsonParams.put("build_duration", "");
        if ("building".equalsIgnoreCase(build.getStatus())) {
            jsonParams.put("build_duration", build.getDuration());
            jsonParams.put("latest_build_date", CCDateFormatter
                    .getDateStringInHumanBeingReadingStyle(build.getBuildingSince()));
            jsonParams.put("build_time_elapsed", getElapsedTimeBuilding(new DateTime()));
        }
        return jsonParams;
    }
}
