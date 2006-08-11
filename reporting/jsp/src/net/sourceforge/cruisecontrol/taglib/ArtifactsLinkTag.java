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

import java.io.IOException;

import java.text.ParseException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;

import net.sourceforge.cruisecontrol.LogFile;
import net.sourceforge.cruisecontrol.util.CCTagException;

/**
 * Creates an <code>artifacts_url</code> variable with the link to the artifacts
 * for this project and log file.
 * @author jfredrick
 */
public class ArtifactsLinkTag extends CruiseControlBodyTagSupport {

    static final String URL_ATTRIBUTE = "artifacts_url";
    static final String ARTIFACTS_SERVLET_CONTEXT = "artifacts";

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
        assert timeString != null && timeString.length() > 0;
        urlBuffer.append("/");
        urlBuffer.append(timeString);
        return urlBuffer.toString();
    }

    String getTimeString() throws JspException {
        String timeString = null;
        LogFile latestFile = findLogFile();

        try {
            timeString = latestFile.getBuildInfo().getDateStamp();
        } catch (ParseException pex) {
            throw new CCTagException(pex.getMessage(), pex);
        }
        return timeString;
    }
}
