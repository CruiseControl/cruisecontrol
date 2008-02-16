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
package net.sourceforge.cruisecontrol.dashboard.web;

import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.SystemService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ConfigurationCommand;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdminController implements Controller {

    private ConfigurationService configuration;

    private SystemService systemService;

    public static final String ERROR_MESSAGE_NOT_EXIST = "Configuration file does not exist!"
            + " Please set \"dashboard.config\" system property to the current location of dashboard-config.xml.";

    public static final String ERROR_MESSAGE_NOT_SPECIFIED = "Configuration file is not specified!";

    public AdminController(ConfigurationService configuration,
            SystemService systemService) {
        this.configuration = configuration;
        this.systemService = systemService;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ConfigurationCommand adminCommand = new ConfigurationCommand();
        String location = configuration.getDashboardConfigLocation();
        String errorMessage = "";

        adminCommand.setConfigFileLocation("");
        adminCommand.setConfigFileContent("");
        if (location != null) {
            adminCommand.setConfigFileLocation(location);
            if (new File(location).exists()) {
                adminCommand.setConfigFileContent(FileUtils.readFileToString(
                        new File(location), null));
            } else {
                errorMessage = ERROR_MESSAGE_NOT_EXIST;
            }
        } else {
            errorMessage = ERROR_MESSAGE_NOT_SPECIFIED;
        }
        Map model = new HashMap();
        model.put("error_message", errorMessage);
        model.put("command", adminCommand);
        model.put("jvm_version", systemService.getJvmVersion());
        model.put("os_info", systemService.getOsInfo());
        model.put("logs_root", configuration.getLogsRoot().getCanonicalPath());
        model.put("artifacts_root", configuration.getArtifactsRoot().getCanonicalPath());
        model.put("forcebuild_enabled",
                configuration.isForceBuildEnabled() ? "Yes" : "No");
        model.put("active", StringUtils.defaultString(request.getParameter("active")));
        return new ModelAndView("page_admin", model);
    }
}
