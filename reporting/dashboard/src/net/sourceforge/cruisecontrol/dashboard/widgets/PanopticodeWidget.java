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
package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class PanopticodeWidget implements Widget {
    private static final String VIEW_TEMPLATE =
            "<div id='panopticode_summary' style='font-family: Arial, Verdana, sans-serif;'>" + "<h2>Code Coverage</h2>"
                    + "<iframe src=\"$contextPath/tab/build/download/artifacts/$project/$build/"
                    + "interactive-coverage-treemap.svg\" " + "width='100%' height='768px'>"
                    + "</iframe>" + "<br/><br/><h2>Code Complexity</h2>"
                    + "<iframe src=\"$contextPath/tab/build/download/artifacts/$project/$build/"
                    + "interactive-complexity-treemap.svg\""
                    + "width='100%' height='768px'></iframe>" + "</div>";

    public String getDisplayName() {    
        return "Panopticode Summary";
    }

    public Object getOutput(Map parameters) {
        String output =
                StringUtils.replace(VIEW_TEMPLATE, "$project", (String) parameters
                        .get(Widget.PARAM_PJT_NAME));
        File buildFile = (File) parameters.get(Widget.PARAM_BUILD_LOG_FILE);
        output = StringUtils.replace(output, "$build", buildFile.getName());
        return StringUtils.replace(output, "$contextPath", (String) parameters
                .get(Widget.PARAM_WEB_CONTEXT_PATH));
    }
}
