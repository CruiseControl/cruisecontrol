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
package net.sourceforge.cruisecontrol.dashboard.tabs;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

/**
 * Understand the concept of "Current Tab".
 */
public abstract class BaseTabController implements TabController {
    private List tabProviders = new ArrayList();
    private static final String NO_TABS_MESSAGE = "No TabProvider added to this controller.";
    private static final String MULTIPLE_TABS_MESSAGE =
            "Multiple TabProvider added to this controller. "
            + "Must override 'getCurrentTab' to support multiple TabProviders";


    public final ModelAndView handleRequest(HttpServletRequest httpServletRequest,
                                            HttpServletResponse httpServletResponse) throws Exception {
        ModelAndView modelAndView = handleTabRequest(httpServletRequest, httpServletResponse);
        modelAndView.getModel().put("cssFiles", getCssFiles());
        return modelAndView;
    }

    public TabProvider getCurrentTab() {
        if (this.tabProviders.isEmpty()) {
            throw new RuntimeException(NO_TABS_MESSAGE);
        }
        if (this.tabProviders.size() > 1) {
            throw new RuntimeException(MULTIPLE_TABS_MESSAGE);
        }
        return (TabProvider) this.tabProviders.get(0);
    }

    public void addTabProvider(final TabProvider tabProvider) {
        this.tabProviders.add(tabProvider);
    }

    public void setTabProviders(final List tabProviders) {
        this.tabProviders = tabProviders;
    }

    protected abstract String[] getCssFiles();
    protected abstract ModelAndView handleTabRequest(HttpServletRequest httpServletRequest,
                                                     HttpServletResponse httpServletResponse) throws Exception;
}
