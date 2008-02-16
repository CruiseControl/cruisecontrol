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

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ForceBuildCommand;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.SimpleFormController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class ForceBuildController extends SimpleFormController {
    private final BuildLoopQueryService buildLoopQueryService;
    private final BuildInformationRepository buildInformationRepository;

    public ForceBuildController(BuildLoopQueryService buildLoopQueryService,
                                BuildInformationRepository buildInformationRepository) {
        this.buildLoopQueryService = buildLoopQueryService;
        this.buildInformationRepository = buildInformationRepository;
        this.setCommandClass(ForceBuildCommand.class);
    }

    protected boolean isFormSubmission(HttpServletRequest request) {
        return true;
    }

    protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response,
                                    BindException errors, Map controlModel) throws Exception {
        throw new Exception("Only accepts POSTs");
    }

    protected ModelAndView onSubmit(Object object, BindException arg1) throws Exception {
        ForceBuildCommand command = (ForceBuildCommand) object;
        String projectName = command.getProjectName();

        try {
            buildLoopQueryService.forceBuild(projectName);
            return new ModelAndView(new TextView("Your build is scheduled"));
        } catch (Exception e) {
            String message = "Error communicating with build loop";
            BuildLoopInformation buildLoopInfo = buildInformationRepository.getBuildLoopInfo(projectName);
            if (buildLoopInfo != null) {
                message += " on: " + buildLoopInfo.getJmxInfo().getRmiUrl();
            }
            return new ModelAndView(new TextView(message, HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    private class TextView implements View {
        private final String text;
        private final int status;

        public TextView(String text) {
            this(text, HttpServletResponse.SC_OK);
        }

        public TextView(String text, int status) {
            this.text = text;
            this.status = status;
        }

        public String getContentType() {
            return "text/plain";
        }

        public void render(Map map,
                           HttpServletRequest httpServletRequest,
                           HttpServletResponse httpServletResponse) throws Exception {
            httpServletResponse.setStatus(status);
            httpServletResponse.getWriter().print(text);
        }
    }
}
