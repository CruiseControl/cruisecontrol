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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.web.command.ConfigurationCommand;
import org.apache.commons.io.FileUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class AdminController implements Controller {

    public static final String CONFIGURATION_HAS_BEEN_UPDATED_SUCCESSFULLY = "Configuration has been updated "
            + "successfully";

    public static final String CRUISE_CONFIGURATION_FILE_MISSING = "Cruise configuration file missing. Please check "
            + "your <a href=\"admin/config\">configuration.";

    private net.sourceforge.cruisecontrol.dashboard.Configuration configuration;

    public static final String CONFIGURATION_FILE_HAS_BEEN_SET_SUCCESSFULLY =
            "Configuration file has been set successfully. "
                    + "Click <a href='projects.html'>here</a> to go to the project dashboard.";

    public AdminController(Configuration configuration) {
        this.configuration = configuration;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ConfigurationCommand adminCommand = new ConfigurationCommand();
        boolean configured = false;
        String location = configuration.getCruiseConfigLocation();
        if (location != null) {
            adminCommand.setConfigFileLocation(location);
            adminCommand.setConfigFileContent(FileUtils.readFileToString(new File(location), null));
            configured = true;
        }
        Map model = new HashMap();
        model.put("configured", Boolean.valueOf(configured));
        model.put("isConfigFileEditable", Boolean.valueOf(configuration.isConfigFileEditable()));
        model.put("command", adminCommand);
        model.put("flash_message", request.getParameter("flash_message"));
        return new ModelAndView("admin", model);
    }
}
