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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.web.binder.DownLoadLogBinder;
import net.sourceforge.cruisecontrol.dashboard.web.binder.DownloadArtifactsBinder;
import net.sourceforge.cruisecontrol.dashboard.web.command.DownLoadArtifactsCommand;
import net.sourceforge.cruisecontrol.dashboard.web.command.DownLoadFile;
import net.sourceforge.cruisecontrol.dashboard.web.command.DownloadLogCommand;
import net.sourceforge.cruisecontrol.dashboard.web.validator.DownLoadFileValidator;

import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

public class DownloadController extends BaseMultiActionController {
    private Configuration configuration;

    public DownloadController(Configuration configuration) {
        this.configuration = configuration;
        this.setSupportedMethods(new String[] {"GET"});
        this.setValidators(new Validator[] {new DownLoadFileValidator()});
    }

    protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object command)
            throws Exception {
        if (command instanceof DownloadLogCommand) {
            return new DownLoadLogBinder(command);
        } else {
            return new DownloadArtifactsBinder(command);
        }
    }

    public ModelAndView artifacts(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        return download(request, new DownLoadArtifactsCommand(configuration), "downloadBinView");
    }

    public ModelAndView log(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        return download(request, new DownloadLogCommand(configuration), "downloadXmlView");
    }

    private ModelAndView download(HttpServletRequest request, DownLoadFile command, String viewName)
            throws Exception {
        BindingResult bindingResult = bindObject(request, command);
        if (bindingResult.hasErrors()) {
            ModelAndView mov = new ModelAndView("page_error");
            mov.getModel().put("errorMessage", bindingResult.getGlobalError().getDefaultMessage());
            return mov;
        } else {
            ModelAndView mov = new ModelAndView(viewName);
            mov.getModel().put("targetFile", command.getDownLoadFile());
            return mov;
        }
    }
}
