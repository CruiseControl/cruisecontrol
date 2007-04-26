/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import net.sourceforge.cruisecontrol.LogFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A helper class to consolidate tags that deal with log files.
 * @author <a href="mailto:robertdw@sourceforge.net">Robert Watkins</a>
 * @author <a href="mailto:jeffjensen@upstairstechnology.com">Jeff Jensen </a>
 */
public class CruiseControlTagSupport extends TagSupport {
    protected static final String LOG_PARAMETER = "log";

    public static Log getLog(Class clazz) {
        return (LogFactory.getLog(clazz));
    }

    private static final FilenameFilter DIR_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return (new File(dir, name).isDirectory());
        }
    };

    private String projectName = null;

    protected void debug(String message) {
        getLog(this.getClass()).debug(message);
    }
    protected void debug(String message, Throwable throwable) {
        getLog(this.getClass()).debug(message, throwable);
    }

    protected void info(String message) {
        getLog(this.getClass()).info(message);
    }

    protected void err(String message) {
        getLog(this.getClass()).error(message);
    }

    protected void err(Throwable exception) {
        getLog(this.getClass()).error(exception);
    }

    protected void err(String message, Throwable cause) {
        getLog(this.getClass()).error(message, cause);
    }

    protected String getBaseLogDir() throws JspException {
        String logDirName = getContextParam("logDir");
        if (logDirName == null) {
            throw new JspException("You need to specify a log directory as a context param");
        }
        return logDirName;
    }

    protected File findLogDir() throws JspException {
        String logDirName = getBaseLogDir() + getProject();
        File logDir = new File(logDirName);
        if (!logDir.isDirectory()) {
            throw new JspException(logDirName + " either does not exist, or is not a directory");
        }
        return logDir;
    }

    /**
     *  Gets the correct log file, based on the project and log file requested
     *  in the query string.
     *
     *  @return The specified log file or the latest log, if no log file is
     *          specified in the request.
     * @throws JspException if logDir either does not exist, or is not a directory
     */
    protected LogFile findLogFile() throws JspException {
        String logFile = getPageContext().getRequest().getParameter(LOG_PARAMETER);
        return getXMLFile(findLogDir(), logFile);
    }

    LogFile getXMLFile(File logDir, String logName) {
        LogFile logFile;
        if (logName == null || logName.trim().equals("")) {
            logFile = LogFile.getLatestLogFile(logDir);
            info("Using latest log file: " + logFile.getFile().getAbsolutePath());
        } else {
            logFile = new LogFile(logDir, logName);
            info("Using specified log file: " + logFile.getFile().getAbsolutePath());
        }
        return logFile;
    }

    protected String[] findProjects() throws JspException {
        String logDirName = getBaseLogDir();
        File logDir = new File(logDirName);
        if (!logDir.isDirectory()) {
            throw new JspException(logDirName + " either does not exist, or is not a directory");
        }
        String[] projects = logDir.list(DIR_FILTER);
        Arrays.sort(projects);
        return projects;
    }

    protected String getContextParam(final String name) {
        String value = pageContext.getServletConfig().getInitParameter(name);
        if (value == null) {
            value = pageContext.getServletContext().getInitParameter(name);
        }
        return value;
    }

    public void setProject(String projectName) {
        this.projectName = projectName;
    }

    protected String getProject() {

        if (projectName != null) {
            return "/" + projectName;
        }

        if (isSingleProject()) {
            info("in singleProjectMode");
            return "";
        }
        String pathInfo = getRequest().getPathInfo();
        if (pathInfo == null) {
            info("pathInfo is null");
            return "";
        }
        return pathInfo;
    }

    /**
     * Determine if this is a single project config or not.
     *
     * @return true if this is a single project config.
     */
    protected boolean isSingleProject() {
        String singleProjectMode = getContextParam("singleProject");
        return Boolean.valueOf(singleProjectMode).booleanValue();
    }

    public void setPageContext(PageContext pageContext) {
        this.pageContext = pageContext;
    }

    protected PageContext getPageContext() {
        return pageContext;
    }

    protected String getServletPath() {
        final HttpServletRequest request = getRequest();
        return request.getContextPath() + request.getServletPath();
    }

    protected HttpServletRequest getRequest() {
        return (HttpServletRequest) getPageContext().getRequest();
    }

    /**
     * Create a link to the app, including the supplied parameter, but preserving all other parameters.
     * @param paramName  the name of the parameter.
     * @param paramValue the value of the parameter
     * @return a link to the app, including the supplied parameter, but preserving all other parameters.
     */
    protected String createUrl(String paramName, String paramValue) {
        StringBuffer url = new StringBuffer(getServletPath());
        url.append(getProject());
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
        url.append(queryString.toString());
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

    protected Locale getLocale() {
        return pageContext.getRequest().getLocale();
    }
}
