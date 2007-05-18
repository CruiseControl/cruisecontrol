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
import net.sourceforge.cruisecontrol.dashboard.web.command.ConfigurationCommand;
import net.sourceforge.cruisecontrol.dashboard.web.validator.ConfigXmlLocationValidator;
import net.sourceforge.cruisecontrol.dashboard.web.validator.ConfigXmlNameValidator;
import org.springframework.validation.BindException;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class UpdateConfigXmlLocationController extends SimpleFormController {
    public static final String CONFIGURATION_FILE_HAS_BEEN_SET_SUCCESSFULLY =
            "Configuration file has been set successfully. "
                    + "Click <a href='projects.html'>here</a> to go to the project dashboard.";

    private net.sourceforge.cruisecontrol.dashboard.Configuration configuration;

    public UpdateConfigXmlLocationController(net.sourceforge.cruisecontrol.dashboard.Configuration configuration) {
        this.configuration = configuration;
        this.setValidators(new Validator[]{new ConfigXmlNameValidator(), new ConfigXmlLocationValidator()});
        this.setCommandClass(ConfigurationCommand.class);
        this.setFormView("admin");
    }

    protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors)
            throws Exception {
        ModelAndView view = new ModelAndView("admin");
        Map model = view.getModel();
        model.put("isConfigFileEditable", Boolean.valueOf(configuration.isConfigFileEditable()));
        model.putAll(errors.getModel());
        return view;
    }

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
                                    BindException errors) throws Exception {
        Map model = new HashMap();
        if (configuration.isConfigFileEditable()) {
            configuration.setCruiseConfigLocation(request.getParameter("configFileLocation"));
            model.put("flash_message", CONFIGURATION_FILE_HAS_BEEN_SET_SUCCESSFULLY);
        }
        return new ModelAndView("redirect:/admin/config", model);
    }
}
