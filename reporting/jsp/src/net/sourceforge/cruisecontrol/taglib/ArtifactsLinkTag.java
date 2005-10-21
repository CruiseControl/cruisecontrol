/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;

import net.sourceforge.cruisecontrol.LogFile;
import net.sourceforge.cruisecontrol.util.CCTagException;

/**
 * @author jfredrick
 */
public class ArtifactsLinkTag extends CruiseControlBodyTagSupport {

    static final String URL_ATTRIBUTE = "artifacts_url";
    static final String LOG_PARAMETER = "log";
    static final String ARTIFACTS_SERVLET_CONTEXT = "artifacts";

    private static final String NO_LOG_PARAM = "";

    public void doInitBody() throws JspException {
        String url = getArtifactURL();
        info("artifactURL is " + url);
        getPageContext().setAttribute(URL_ATTRIBUTE, url);
    }
    
    public int doAfterBody() throws JspTagException {
        try {
            BodyContent out = getBodyContent();
            out.writeOut(out.getEnclosingWriter());
        } catch (IOException e) {
            err(e);
            throw new CCTagException("IO Error: " + e.getMessage(), e);

        }
        return SKIP_BODY;
    }

    private String getArtifactURL() throws JspException {    
        StringBuffer urlBuffer = new StringBuffer();
        urlBuffer.append(ARTIFACTS_SERVLET_CONTEXT);
        String project = getProject();
        if (project.length() > 0) {
            info("project is " + project);
            urlBuffer.append(project);
        } else {
            info("getProject().length() is " + project.length());
        }
        String timeString = getTimeString();
        if (timeString != null && timeString.length() > 0) {
            urlBuffer.append("/" + timeString);
        }
        return urlBuffer.toString();
    }

    String getTimeString() throws JspException {
        String timeString = getTimeFromLogParam();
        if (timeString == NO_LOG_PARAM) {
            info("no log param, trying log file name");
            timeString = getTimeFromLatestLogFile();
        }
        info("timeString is " + timeString);
        return timeString;
    }

    String getTimeFromLogParam() {
        String timeString;
        final String logPrefix = "log";
        final char labelSeparator = 'L';

        String logParameter = getPageContext().getRequest().getParameter("log");
        if (logParameter == null) {
            return NO_LOG_PARAM;
        }

        int labelIndex = logParameter.indexOf(labelSeparator);
        if (labelIndex != -1) {
            timeString = logParameter.substring(logPrefix.length(), labelIndex);
        } else {
            timeString = logParameter.substring(logPrefix.length());
        }

        return timeString;
    }

    String getTimeFromLatestLogFile() throws JspException {
        final String logPrefix = "log";
        final char labelSeparator = 'L';
        
        File logDir = findLogDir();
        File latestFile = LogFile.getLatestLogFile(logDir).getFile();
        String logName = latestFile.getName();
        
        int startIndex = logPrefix.length();
        int endIndex = logName.indexOf('.');
        int labelSeparatorIndex = logName.indexOf(labelSeparator);
        if (labelSeparatorIndex > startIndex && labelSeparatorIndex < endIndex) {
            endIndex = labelSeparatorIndex;
        }
        String timeString = logName.substring(startIndex, endIndex);
        return timeString;
    }

}
