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

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

public class CCTrayBuildSummaryAdapter implements XmlAdapter {

    private final Build summary;

    private String baseUrl;

    private String lastBuildStatus;

    public CCTrayBuildSummaryAdapter(String baseUrl, Build summary) {
        this.summary = summary;
        this.baseUrl = baseUrl;
        this.lastBuildStatus = summary.hasPassed() ? "Success" : "Failure";
    }

    public String getName() {
        return summary.getProjectName();
    }

    public String getActivity() {
        String status = summary.getStatus();
        if (status.equals(ProjectBuildStatus.BOOTSTRAPPING.getStatus()) || status
                .equals(ProjectBuildStatus.MODIFICATIONSET.getStatus())) {
            return "CheckingModifications";
        } else if (status.equals(ProjectBuildStatus.BUILDING.getStatus())) {
            return "Building";
        } else {
            return "Sleeping";
        }
    }

    public String getLastBuildStatus() {
        return lastBuildStatus;
    }

    public String getLastBuildLabel() {
        return summary.getLabel();
    }

    public String getLastBuildTime() {
        return CCDateFormatter.format(summary.getBuildDate(), "yyyy-MM-dd'T'HH:mm:ss");
    }

    public String toXml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<Project").append(" name=").append(quote(this.getName())).append(" activity=")
                .append(quote(this.getActivity())).append(" lastBuildStatus=").append(quote(this.getLastBuildStatus()))
                .append(" lastBuildLabel=").append(quote(this.getLastBuildLabel())).append(" lastBuildTime=")
                .append(quote(this.getLastBuildTime())).append(" webUrl=")
                .append(quote(baseUrl + "build/detail/" + this.getName()))
                .append(" />").append("\n");
        return sb.toString();
    }

    private String quote(String text) {
        return '"' + text + '"';
    }
}
