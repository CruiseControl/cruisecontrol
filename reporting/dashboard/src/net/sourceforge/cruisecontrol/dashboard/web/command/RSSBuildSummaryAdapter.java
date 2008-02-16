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

import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

public class RSSBuildSummaryAdapter implements XmlAdapter {
    private final BuildSummary summary;

    private String baseURL;

    public RSSBuildSummaryAdapter(String baseURL, BuildSummary summary) {
        this.summary = summary;
        this.baseURL = baseURL;
    }

    public String getProjectName() {
        return summary.getProjectName();
    }

    public String toXml() {
        StringBuffer sb = new StringBuffer();
        sb.append("  <item>").append('\n').append("    <title>").append(getProjectName()).append(
                " ").append(getStatus()).append(" ").append(getPubDate()).append("</title>")
                .append('\n').append("    <description>Build ").append(getStatus()).append(
                        "</description>").append('\n').append("    <pubDate>").append(getPubDate())
                .append("</pubDate>").append('\n').append("    <link>").append(baseURL).append(
                        "tab/build/detail/").append(getProjectName()).append("</link>").append('\n').append(
                        "  </item>").append('\n');
        return sb.toString();
    }

    public Object getPubDate() {
        if (PreviousResult.UNKNOWN.equals(summary.getPreviousBuildResult())) {
            return "";
        } else {
            return CCDateFormatter.format(summary.getBuildDate(), "EEE, dd MMM yyyy HH:mm:ss Z");
        }
    }

    public String getStatus() {
        PreviousResult previousBuildResult = summary.getPreviousBuildResult();
        String status = previousBuildResult.getStatus();
        if (PreviousResult.PASSED.equals(previousBuildResult)) {
            return status.toLowerCase();
        } else {
            return status.toUpperCase();
       }
    }
}
