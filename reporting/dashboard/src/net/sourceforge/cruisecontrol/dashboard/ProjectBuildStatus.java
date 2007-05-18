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
package net.sourceforge.cruisecontrol.dashboard;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public abstract class ProjectBuildStatus {
    public static final ProjectBuildStatus BOOTSTRAPPING = new StatusBootStrapping();

    public static final ProjectBuildStatus MODIFICATIONSET = new StatusModificationSet();

    public static final ProjectBuildStatus BUILDING = new StatusBuilding();

    public static final ProjectBuildStatus WAITING = new StatusWaiting();

    public static final ProjectBuildStatus PASSED = new StatusPassed();

    public static final ProjectBuildStatus FAILED = new StatusFailed();

    public static final ProjectBuildStatus INACTIVE = new StatusInactive();

    public static List values() {
        List list = new ArrayList();
        list.add(BOOTSTRAPPING);
        list.add(MODIFICATIONSET);
        list.add(BUILDING);
        list.add(WAITING);
        list.add(PASSED);
        list.add(FAILED);
        list.add(INACTIVE);
        return list;
    }

    public static ProjectBuildStatus getProjectBuildStatus(String statusStr) {
        List values = ProjectBuildStatus.values();
        for (int i = 0; i < values.size(); i++) {
            ProjectBuildStatus status = (ProjectBuildStatus) values.get(i);
            if (StringUtils.indexOf(statusStr, status.toString()) == 0) {
                return status;
            }
        }
        return INACTIVE;
    }

    public static DateTime getTimestamp(String statusStr) {
        String[] parts = StringUtils.defaultString(statusStr).split(" since ");
        if (parts.length < 2) {
            return null;
        }
        return CCDateFormatter.format(parts[1], "yyyyMMddHHmmss");
    }

    protected ProjectBuildStatus() {
    }

    public abstract String getStatus();

    public boolean isBuilding() {
        return false;
    }
}
