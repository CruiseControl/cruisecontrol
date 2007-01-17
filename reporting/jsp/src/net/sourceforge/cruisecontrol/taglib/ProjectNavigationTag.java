/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.taglib;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

/**
 *
 */
public class ProjectNavigationTag extends CruiseControlBodyTagSupport {
    static final String STATUS_PAGE_TEXT = "-- STATUS PAGE --";
    static final String SELECTED_ATTR_VALUE = "selected=\"selected\"";
    public static final String SELECTED_ATTR = "selected";
    public static final String LINK_TEXT_ATTR = "linktext";
    public static final String URL_ATTR = "projecturl";

    private int count;  // the current item counter of the project list
    private int endPoint;  // How many times around the loop have we gone
    private String[] projects; // array of project names

    public int doStartTag() throws JspException {
        projects = findProjects();
        Arrays.sort(projects);
        count = -1;
        endPoint = projects.length;
        if (count < endPoint) {
            return EVAL_BODY_TAG;
        } else {
            return SKIP_BODY;
        }
    }


    public void doInitBody() throws JspException {
       setupLinkVariables();
    }

    void setupLinkVariables() {
        if (count == -1) {
            getPageContext().setAttribute(LINK_TEXT_ATTR, STATUS_PAGE_TEXT);
            getPageContext().setAttribute(URL_ATTR, getRequest().getContextPath() + "/");
            getPageContext().setAttribute(SELECTED_ATTR, "");
        } else {
            final String projectName = projects[count];
            getPageContext().setAttribute(LINK_TEXT_ATTR, projectName);
            getPageContext().setAttribute(URL_ATTR, getURL(projectName));
            getPageContext().setAttribute(SELECTED_ATTR, getSelected(projectName));
           }
        count++;
    }

    private String getSelected(final String projectName) {
        if (getProject().length() > 0 && getProject().substring(1).compareTo(projectName) == 0) {
            return SELECTED_ATTR_VALUE;
        }
        return "";
    }

    private String getURL(String projectName) {
       return getServletPath() + "/" + projectName;
    }


    public int doAfterBody() throws JspException {
        if (count < endPoint) {
            setupLinkVariables();
            return EVAL_BODY_TAG;
        } else {
            try {
                BodyContent out = getBodyContent();
                out.writeOut(out.getEnclosingWriter());
            } catch (IOException e) {
                err(e);
            }
            return SKIP_BODY;
        }
    }
}
