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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.Modification;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.joda.time.DateTime;

public class BuildCommand {

    private Build build;

    private Map jsonParams = new HashMap();

    private static final int MILLI_SECOND_TO_SECOND = 1000;

    private static final int STATUS_STANDING_SECTION = 24 * 60 * 60 * 1000 / 8;

    private final StoryTracker storyTracker;

    public BuildCommand(Build build, StoryTracker storyTracker) {
        this.build = build;
        this.storyTracker = storyTracker;
        jsonParams.put("css_class_name", build.getStatus().toLowerCase());
        jsonParams.put("css_class_name_for_dashboard", build.getStatus().toLowerCase());
    }

    public Build getBuild() {
        return this.build;
    }

    public Collection getModifications() {
        Collection modifications = build.getModificationSet().getModifications();
        Collection modificationCmds = new ArrayList();
        for (Iterator iterator = modifications.iterator(); iterator.hasNext();) {
            Modification modification = (Modification) iterator.next();
            modificationCmds.add(new ModificationCommand(modification, storyTracker));
        }
        return modificationCmds;

    }

    public String getDateStringInHumanBeingReadingStyle() {
        return CCDateFormatter.getDateStringInHumanBeingReadingStyle(build.getBuildDate());
    }

    
    public Long getElapsedTimeBuilding(DateTime date) {
        return new Long((date.getMillis() - build.getBuildingSince().getMillis()) / MILLI_SECOND_TO_SECOND);
    }

    public void updateFailedCSS(Build last) {
        jsonParams.put("css_class_name_for_dashboard", "failed_level_" + statusStandingLevel(last));
    }

    public void updatePassedCss(Build last) {
        jsonParams.put("css_class_name_for_dashboard", "passed_level_" + statusStandingLevel(last));
    }

    public void updateDefaultCss() {
        jsonParams.put("css_class_name_for_dashboard", build.getStatus().toLowerCase());
    }

    public String getCssClassName() {
        return (String) jsonParams.get("css_class_name");
    }

    public String getCssClassNameForDashboard() {
        return (String) jsonParams.get("css_class_name_for_dashboard");
    }

    private long statusStandingLevel(Build lastSuccessful) {
        Build baseBuild = lastSuccessful == null ? build : lastSuccessful;
        long duration = (new DateTime().getMillis() - baseBuild.getBuildDate().getMillis());
        return Math.min(duration / STATUS_STANDING_SECTION, 8);
    }

    public Map toJsonHash() {
        jsonParams.put("building_status", build.getStatus());
        jsonParams.put("project_name", build.getProjectName());
        jsonParams.put("latest_build_log_file_name", build.getBuildLogFilename());
        jsonParams.put("latest_build_date", getDateStringInHumanBeingReadingStyle());
        if ("building".equalsIgnoreCase(build.getStatus())) {
            jsonParams.put("build_duration", build.getDuration());
            jsonParams.put("latest_build_date", CCDateFormatter.getDateStringInHumanBeingReadingStyle(build
                    .getBuildingSince()));
            jsonParams.put("build_time_elapsed", getElapsedTimeBuilding(new DateTime()));
        }
        return jsonParams;
    }
}
