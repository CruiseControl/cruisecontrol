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

package net.sourceforge.cruisecontrol.dashboard.web.binder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.cruisecontrol.dashboard.web.command.DownLoadArtifactsCommand;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestDataBinder;

public class DownloadArtifactsBinder extends ServletRequestDataBinder {
    private static final int ARTIFACTS = 1;

    private static final int INDEX_BUILD = 2;

    private static final int INDEX_PROJECT_NAME = 3;

    public DownloadArtifactsBinder(Object target) {
        super(target);
    }

    public void bind(ServletRequest request) {
        String[] url = StringUtils.split(((HttpServletRequest) request).getRequestURI(), '/');
        String projectName = url[url.length - INDEX_PROJECT_NAME];
        String build = url[url.length - INDEX_BUILD];
        String fileToBeDownloaded = url[url.length - ARTIFACTS];
        DownLoadArtifactsCommand command = (DownLoadArtifactsCommand) this.getTarget();
        command.setProjectName(projectName);
        command.setBuild(build);
        command.setFileToBeDownloaded(fileToBeDownloaded);
    }
}
