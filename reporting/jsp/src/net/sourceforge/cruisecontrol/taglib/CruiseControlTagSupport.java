/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
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

import java.io.File;
import java.util.Enumeration;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * A helper class to consolidate tags that deal with log files.
 * @author <a href="mailto:robertdw@sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlTagSupport extends BodyTagSupport {
    protected void info(String message) {
        System.out.println(message);
    }

    protected void err(String message) {
        System.err.println(message);
    }

    protected void err(Throwable exception) {
        exception.printStackTrace();
    }

    protected File findLogDir() throws JspException {
        String logDirName = pageContext.getServletConfig().getInitParameter("logDir");
        if (logDirName == null) {
            logDirName = pageContext.getServletContext().getInitParameter("logDir");
        }
        File logDir = new File(logDirName);
        if (!logDir.exists() || !logDir.isDirectory()) {
            throw new JspException(logDirName + " either does not exist, or is not a directory");
        }
        return logDir;
    }

    public void setPageContext(PageContext pageContext) {
        this.pageContext = pageContext;
    }

    protected PageContext getPageContext() {
        return pageContext;
    }

    protected String getServletPath() {
        final HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
        return request.getContextPath() + request.getServletPath();
    }

    /**
     * Create a link to the app, including the supplied parameter, but preserving all other parameters.
     * @param paramName  the name of the parameter.
     * @param paramValue the value of the parameter
     * @return
     */
    protected String createUrl(String paramName, String paramValue) {
        StringBuffer url = new StringBuffer(getServletPath());
        StringBuffer queryString = new StringBuffer("?");
        final ServletRequest request = getPageContext().getRequest();
        Enumeration requestParams = request.getParameterNames();
        while (requestParams.hasMoreElements()) {
            String requestParamName = (String) requestParams.nextElement();
            if (!requestParamName.equals(paramName)) {
                String[] requestParamValues = request.getParameterValues(requestParamName);
                for (int i = 0; i < requestParamValues.length; i++) {
                    final String requestParamValue = requestParamValues[i];
                    appendParam(queryString, requestParamName, requestParamValue);
                }
            }
        }
        url.append(queryString);
        if (paramName != null && paramValue != null) {
            appendParam(url, paramName, paramValue);
        }
        url.setLength(url.length() - 1);
        return url.toString();
    }

    private void appendParam(StringBuffer queryString, String name, final String value) {
        queryString.append(name);
        queryString.append("=");
        queryString.append(value);
        queryString.append("&");
    }

    protected String createUrl(String paramToExclude) {
        return createUrl(paramToExclude, null);
    }
}
