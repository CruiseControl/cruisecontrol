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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import org.apache.commons.io.FileUtils;

public class PluginOutputService {
    private final Configuration configuration;

    public PluginOutputService(Configuration configuration) {
        this.configuration = configuration;
    }

    private List lines() {
        try {
            String configdir = new File(configuration.getCruiseConfigLocation()).getParent();
            return FileUtils.readLines(new File(configdir, "plugin.cfg"));
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    public void mergePluginOutput(BuildDetail build, Map parameters) {
        Iterator iterator = lines().iterator();
        while (iterator.hasNext()) {
            String className = (String) iterator.next();
            ContentDigesterService digesterService = null;
            try {
                Class clazz = Class.forName(className.trim());
                digesterService = (ContentDigesterService) clazz.newInstance();
                mergeParameters(build, parameters);
                build.addPluginOutput(digesterService.getDisplayName(), digesterService
                        .getOutput(parameters));
            } catch (Exception e) {
                continue;
            }
        }
    }

    private void mergeParameters(BuildDetail build, Map parameters) {
        parameters.put(ContentDigesterService.PARAM_CC_ROOT, new File(configuration
                .getCruiseConfigLocation()).getParentFile());
        parameters.put(ContentDigesterService.PARAM_PJT_NAME, build.getProjectName());
        parameters.put(ContentDigesterService.PARAM_PJT_LOG_ROOT, build.getLogFolder());
        parameters.put(ContentDigesterService.PARAM_BUILD_LOG_FILE, build.getLogFile());
        parameters
                .put(ContentDigesterService.PARAM_BUILD_ARTIFACTS_ROOT, build.getArtifactFolder());
    }
}
