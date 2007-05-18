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
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

public class MBeanConsoleController extends MultiActionController {

    private static final String MBEAN_ROOT = "mbean?objectname=CruiseControl+Project%3Aname%3D";

    private CruiseControlJMXService jmxService;

    public MBeanConsoleController(CruiseControlJMXService service) {
        this.jmxService = service;
        this.setSupportedMethods(new String[]{"GET"});
    }

    public ModelAndView server(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mov = renderMbeanConsole(request);
        mov.getModel().put("context", "");
        mov.getModel().put("projectName", "CruiseControl");
        return mov;
    }

    public ModelAndView mbean(HttpServletRequest request, HttpServletResponse response) {
        String[] url = StringUtils.split(request.getRequestURI(), '/');
        String projectName = url[url.length - 1];
        ModelAndView mov = renderMbeanConsole(request);
        mov.getModel().put("projectName", projectName);
        mov.getModel().put("context", getContextForProject(projectName));
        return mov;
    }

    private ModelAndView renderMbeanConsole(HttpServletRequest request) {
        ModelAndView mov = new ModelAndView("mbeanConsole");
        mov.getModel().put("port", "" + jmxService.getHttpPortForMBeanConsole());
        return mov;
    }

    private String getContextForProject(String projectName) {
        String context = MBEAN_ROOT + projectName;
        return StringUtils.replace(context, " ", "+");
    }
}
