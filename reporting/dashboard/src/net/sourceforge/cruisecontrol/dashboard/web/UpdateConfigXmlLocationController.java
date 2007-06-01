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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ConfigurationCommand;
import net.sourceforge.cruisecontrol.dashboard.web.validator.ConfigXmlLocationValidator;
import net.sourceforge.cruisecontrol.dashboard.web.validator.ConfigXmlNameValidator;

import org.springframework.validation.BindException;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class UpdateConfigXmlLocationController extends SimpleFormController {
    private Configuration configuration;

    private final EnvironmentService service;

    public String message;

    public UpdateConfigXmlLocationController(Configuration configuration, EnvironmentService service) {
        this.configuration = configuration;
        this.service = service;
        this.setValidators(new Validator[] {new ConfigXmlNameValidator(), new ConfigXmlLocationValidator()});
        this.setCommandClass(ConfigurationCommand.class);
        this.setFormView("page_admin");
    }

    protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response,
            BindException errors) throws Exception {
        ModelAndView view = new ModelAndView("page_admin");
        Map model = view.getModel();
        model.put("isConfigFileEditable", Boolean.valueOf(service.isConfigFileEditable()));
        model.putAll(errors.getModel());
        return view;
    }

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {
        Map model = new HashMap();
        if (service.isConfigFileEditable()) {
            configuration.setCruiseConfigLocation(request.getParameter("configFileLocation"));
            model.put("location_flash_message", getMessage(request));
        }
        return new ModelAndView("redirect:/admin/config", model);
    }

    // TODO move this message to VM rather then generate here.
    private String getMessage(HttpServletRequest request) {
        if (message == null) {
            message =
                    "Configuration file has been set successfully. " + "<a href='" + request.getContextPath()
                            + "/dashboard'>Go to dashboard to see your projects</a>.";
        }
        return message;

    }
}
