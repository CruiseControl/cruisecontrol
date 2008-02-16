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

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.ModificationSet;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.ViewableStatusHelper;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.web.view.JsonView;

import org.joda.time.DateTime;

public class BuildCommand {
    public static final String CSS_LEVEL = "level";

    private Build build;

    private Map jsonParams = new HashMap();

    private static final int MILLI_SECOND_TO_SECOND = 1000;

    private static final int STATUS_STANDING_SECTION = 24 * 60 * 60 * 1000 / 8;

    private final StoryTracker storyTracker;

    public BuildCommand(Build build, StoryTracker storyTracker) {
        this.build = build;
        this.storyTracker = storyTracker;
        jsonParams.put(CSS_LEVEL, "unknown");
    }

    public Build getBuild() {
        return this.build;
    }

    public Collection getModifications() {
        Collection modificationCmds = new ArrayList();
        ModificationSet modificationSet = build.getModificationSet();
        if (modificationSet != null) {
            Collection modifications = modificationSet.getModifications();
            for (Iterator iterator = modifications.iterator(); iterator.hasNext();) {
                Modification modification = (Modification) iterator.next();
                modificationCmds.add(new ModificationCommand(modification, storyTracker));
            }
        }
        return modificationCmds;
    }

    public String getDateStringInHumanBeingReadingStyle() {
        return CCDateFormatter.getDateStringInHumanBeingReadingStyle(build.getBuildDate());
    }

    public Long getElapsedTimeBuilding(DateTime date) {
        return new Long((date.getMillis() - build.getBuildingSince().getMillis()) / MILLI_SECOND_TO_SECOND);
    }

    public void updateCssLevel(Build last) {
        jsonParams.put(CSS_LEVEL, String.valueOf(statusStandingLevel(last)));
    }

    public String getLevel() {
        return (String) jsonParams.get(CSS_LEVEL);
    }

    public String getDuration() {
        return build.getDuration() == null ? "Unknown" : build.getDuration();
    }

    public Map toJsonHash() {
        jsonParams.put("project_name", build.getProjectName());
        jsonParams.put("latest_build_date", build.getConvertedTime());
        jsonParams.put("current_status", build.getCurrentStatus().getStatus());
        jsonParams.put("previous_result", build.getPreviousBuildResult().getStatus());
        if (CurrentStatus.BUILDING.equals(build.getCurrentStatus())) {
            jsonParams.put("build_duration", build.getDuration());
            jsonParams.put("build_time_elapsed", getElapsedTimeBuilding(new DateTime()));
        }
        return jsonParams;
    }

    public String toJsonString() {
        Map info = new HashMap();
        info.put("building_info", this.toJsonHash());
        return new JsonView().renderJson(info);
    }

    public String getViewableStatus() {
        return new ViewableStatusHelper().getVmStatus(build);
    }

    private long statusStandingLevel(Build lastSuccessful) {
        Build baseBuild = lastSuccessful == null ? build : lastSuccessful;
        long duration = (new DateTime().getMillis() - baseBuild.getBuildDate().getMillis());
        if (duration > 0) {
            return Math.min(duration / STATUS_STANDING_SECTION, 8);
        } else {
            return 0;
        }
    }
}
