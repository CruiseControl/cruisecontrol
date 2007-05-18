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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;
import net.sourceforge.cruisecontrol.dashboard.exception.NonSupportedVersionControlException;
import net.sourceforge.cruisecontrol.dashboard.exception.ProjectAlreadyExistException;
import net.sourceforge.cruisecontrol.dashboard.service.VersionControlFactory;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.ConnectionResultContext;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Cvs;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Perforce;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.VCS;
import net.sourceforge.cruisecontrol.dashboard.web.view.JsonView;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class AddProjectFromVersionControlController implements Controller {
    private static final String INVALID_VERSION_CONTROL_SYSTEM = "You have to specify a valid version control system";

    private VersionControlFactory vcsFactory;

    private static final String BUILD_FILE_NOT_FOUND = "You may need to edit cruisecontrol configuration file at "
            + "Administration page to specify the build.xml.";

    public static final String ERROR_WHILE_PARSING_CRUISE_CONFIG_FILE = "Error while parsing Cruise config file.";

    private static final String PROJECT_ALREADY_EXISTS = " already exists, please choose another name.";

    private Configuration configuration;

    public AddProjectFromVersionControlController(VersionControlFactory factory, Configuration configuration) {
        this.vcsFactory = factory;
        this.configuration = configuration;
    }

    public ModelAndView handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {
        String url = httpServletRequest.getParameter("url");
        String projectName = httpServletRequest.getParameter("projectName");
        String vcsType = httpServletRequest.getParameter("vcsType");
        String moduleName = httpServletRequest.getParameter("moduleName");
        if (StringUtils.isBlank(projectName)) {
            return getViewWithMessage("Project name can not be blank", "projectName");
        }
        if (configuration.hasProject(projectName)) {
            return getViewWithMessage(projectName + PROJECT_ALREADY_EXISTS, "projectName");
        }
        VCS vcs;
        try {
            vcs = vcsFactory.getVCSInstance(projectName, url, moduleName, vcsType);
        } catch (NonSupportedVersionControlException e) {
            return getViewWithMessage(INVALID_VERSION_CONTROL_SYSTEM, "vcsType");
        }
        if ((vcs instanceof Cvs) && StringUtils.isBlank(moduleName)) {
            return getViewWithMessage("You must provide a module name for cvs project", "moduleName");
        }
        if ((vcs instanceof Perforce) && StringUtils.isBlank(moduleName)) {
            return getViewWithMessage("You must provide the depot path for perforce project", "moduleName");
        }
        if (!vcs.checkConnection().isValidConnection()) {
            return new ModelAndView(new JsonView(), vcs.checkConnection().getResultMap());
        }
        return addProject(vcs, projectName);
    }

    private ModelAndView getViewWithMessage(String message, String field) {
        ConnectionResultContext connectionResultContext = new ConnectionResultContext(message, field);
        return new ModelAndView(new JsonView(), connectionResultContext.getResultMap());
    }

    private ModelAndView addProject(VCS vcs, String projectName)
            throws ConfigurationException, ProjectAlreadyExistException {
        String flashMessage = "";
        if (!vcs.checkBuildFile()) {
            flashMessage = BUILD_FILE_NOT_FOUND;
        }
        try {
            vcs.checkout(getProjectSourceFolder(projectName));
            configuration.addProject(projectName, vcs);
            String message = getMessage(projectName, flashMessage);
            return getViewWithMessage(message, "");
        } catch (ProjectAlreadyExistException e) {
            return getViewWithMessage(projectName + PROJECT_ALREADY_EXISTS, "projectName");
        }
    }

    private String getProjectSourceFolder(String projectName) {
        return configuration.getCruiseConfigDirLocation() + File.separator + "projects" + File.separator + projectName;
    }

    private String getMessage(String projectName, String flashMessage) {
        String message = "Download launched in " + configuration.getCruiseConfigDirLocation() + "/projects/"
                + projectName + ". " + flashMessage;
        message = StringUtils.replace(message, File.separator, "/");
        return message;
    }

}
