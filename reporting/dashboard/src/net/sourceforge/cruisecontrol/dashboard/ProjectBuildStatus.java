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

import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public final class ProjectBuildStatus {
    public static final ProjectBuildStatus BOOTSTRAPPING =
            new ProjectBuildStatus("Bootstrapping", "bootstrapping");

    public static final ProjectBuildStatus MODIFICATIONSET =
            new ProjectBuildStatus("ModificationSet", "checking for modifications");

    public static final ProjectBuildStatus BUILDING = new ProjectBuildStatus("Building", "now building");

    public static final ProjectBuildStatus WAITING =
            new ProjectBuildStatus("Waiting", "waiting for next time to build");

    public static final ProjectBuildStatus PASSED = new ProjectBuildStatus("Passed", "Passed");

    public static final ProjectBuildStatus FAILED = new ProjectBuildStatus("Failed", "Failed");

    public static final ProjectBuildStatus INACTIVE = new ProjectBuildStatus("Inactive", "Inactive");

    private static final ProjectBuildStatus[] STATUSES =
            new ProjectBuildStatus[] {BOOTSTRAPPING, MODIFICATIONSET, BUILDING, WAITING, PASSED, FAILED,
                    INACTIVE};

    private String status;

    private String cruiseStatusString;

    public static ProjectBuildStatus getProjectBuildStatus(String statusStr) {
        for (int i = 0; i < STATUSES.length; i++) {
            ProjectBuildStatus status = STATUSES[i];
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

    private ProjectBuildStatus(String status, String cruiseStatusString) {
        this.status = status;
        this.cruiseStatusString = cruiseStatusString;
    }

    public String getStatus() {
        return status;
    }

    public String toString() {
        return cruiseStatusString;
    }
}
