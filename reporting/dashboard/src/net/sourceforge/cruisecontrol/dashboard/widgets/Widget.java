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

import java.util.Map;

/**
 * &lt;pre&gt;
 * Widget is designed to help faciliate developers to contribute new output services,
 * like emma, checkstyle, panopticode. the main function is getOutput which takes Map as parameter,
 * CruiseControl reporting will pass in embedded values with keys:
 *
 * PARAM_PJT_ARTIFACTS_ROOT : Artifacts root of project, e.g. artifacts/project1
 * PARAM_BUILD_ARTIFACTS_ROOT : Artifacts root of build, e.g. artifacts/project1/20051209122103
 * PARAM_CC_ROOT : Parent folder of config.xml
 * PARAM_PJT_NAME : Name of project e.g. project1
 * PARAM_PJT_LOG_ROOT : Root of project log e.g.logs/project1
 * PARAM_BUILD_LOG_FILE : Log file of build e.g. log20051209122103.xml
 * PARAM_WEB_CONTEXT_PATH : Web context root e.g. "/dashboard"
 *
 * &lt;p&gt; In order to enable your service in the system, go to the root directory of cruisecontrol,
 * and simply add/edit widgets.cfg to include the class name. e.g. com.foo.Class &lt;p&gt;
 * &lt;/pre&gt;
 */

public interface Widget {
    /**
     * Widgets framework will associate artifacts root path with the following key.
     */
    public static final String PARAM_PJT_ARTIFACTS = "PJT_ARTIFACTS";

    /**
     * Widgets framework will associate artifacts root path of the build with the following key.
     */
    public static final String PARAM_BUILD_ARTIFACTS_ROOT = "BUILD_ARTIFACTS_ROOT";

    /**
     * Widgets framework will associate CruiseControl root path with the following key.
     */
    public static final String PARAM_CC_ROOT = "CC_ROOT";

    /**
     * Widgets framework will associate Dashboard webapp path with the following key.
     */
    public static final String PARAM_WEBAPP_ROOT = "WEBAPP_ROOT";

    /**
     * Widgets framework will associate project name with the following key.
     */
    public static final String PARAM_PJT_NAME = "PJT_NAME";

    /**
     * Widgets framework will associate log root path with the following key.
     */
    public static final String PARAM_PJT_LOG_ROOT = "PJT_LOG_ROOT";

    /**
     * Widgets framework will associate log file name with the following key.
     */
    public static final String PARAM_BUILD_LOG_FILE = "BUILD_LOG_FILE";

    /**
     * CC Reporting system will pass in its web context root e.g. "/dashboard"
     */
    public static final String PARAM_WEB_CONTEXT_PATH = "WEB_CONTEXT_ROOT";

    /**
     * @param parameters all the parameters user defined in widget, besides the embedded values.
     * @return the Object to be displayed in the tab of build detail page
     */
    public Object getOutput(Map parameters);

    /**
     * The displayed title in tab view in build detail page.
     *
     * @return the name displaied in tab view.
     */
    public String getDisplayName();

}
