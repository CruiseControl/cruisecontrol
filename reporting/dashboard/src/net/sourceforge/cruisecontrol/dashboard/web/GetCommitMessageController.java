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

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ModificationCommand;
import net.sourceforge.cruisecontrol.dashboard.web.view.JsonView;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GetCommitMessageController implements Controller {

    private BuildLoopQueryService buildLoopQueryService;

    private final DashboardXmlConfigService configService;

    public GetCommitMessageController(BuildLoopQueryService buildLoopQueryService,
            DashboardXmlConfigService configService) {
        this.buildLoopQueryService = buildLoopQueryService;
        this.configService = configService;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String projectName = request.getParameter("project");
        List commitMessages = buildLoopQueryService.getCommitMessages(projectName);
        Map userAndcommitMessage = new HashMap();
        userAndcommitMessage.put(JsonView.RENDER_DIRECT, toJsonHeader(commitMessages, projectName));
        return new ModelAndView(new JsonView(), userAndcommitMessage);
    }

    private List toJsonHeader(List commitMessages, String projectName) {
        List header = new ArrayList();
        if (CollectionUtils.isNotEmpty(commitMessages)) {
            Map storyTrackers = configService.getStoryTrackers();
            StoryTracker storyTracker = (StoryTracker) storyTrackers.get(projectName);
            for (Iterator iter = commitMessages.iterator(); iter.hasNext();) {
                Modification modification = (Modification) iter.next();
                ModificationCommand cmd = new ModificationCommand(modification, storyTracker);
                header.add(cmd.toJsonData());
            }
        }
        return header;
    }
}
