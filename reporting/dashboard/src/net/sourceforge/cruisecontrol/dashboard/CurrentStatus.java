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

import org.apache.commons.lang.StringUtils;

/**
 * Understands the current status of a build.
 */
public final class CurrentStatus implements ViewableStatus {
    public static final CurrentStatus QUEUED = new CurrentStatus("Queued", "in build queue");

    public static final CurrentStatus BOOTSTRAPPING = new CurrentStatus("Bootstrapping", "bootstrapping");

    public static final CurrentStatus MODIFICATIONSET =
            new CurrentStatus("ModificationSet", "checking for modifications");

    public static final CurrentStatus BUILDING = new CurrentStatus("Building", "now building");

    public static final CurrentStatus WAITING =
            new CurrentStatus("Waiting", "waiting for next time to build");

    public static final CurrentStatus PAUSED = new CurrentStatus("Paused", "paused");

    public static final CurrentStatus DISCONTINUED = new CurrentStatus("Discontinued", "Discontinued");

    private static final CurrentStatus[] STATUSES =
            new CurrentStatus[] {QUEUED, BOOTSTRAPPING, MODIFICATIONSET, BUILDING, WAITING, PAUSED,
                    DISCONTINUED};

    private String status;

    private String cruiseStatus;

    public static CurrentStatus getProjectBuildStatus(String statusStr) {
        for (int i = 0; i < STATUSES.length; i++) {
            CurrentStatus status = STATUSES[i];
            if (StringUtils.indexOf(statusStr, status.getCruiseStatus()) == 0) {
                return status;
            }
        }
        return DISCONTINUED;
    }

    private CurrentStatus(String status, String cruiseStatus) {
        this.status = status;
        this.cruiseStatus = cruiseStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getCruiseStatus() {
        return cruiseStatus;
    }

    public String toString() {
        return getCruiseStatus();
    }
}
