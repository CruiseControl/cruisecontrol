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

import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.util.CCTagException;
import net.sourceforge.cruisecontrol.util.DateHelper;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class NavigationTag extends CruiseControlBodyTagSupport {
    public static final String LINK_TEXT_ATTR = "linktext";
    public static final String URL_ATTR = "url";
    public static final String LOG_FILE_ATTR = "logfile";
    public static final String BUILD_INFO_ATTR = "buildinfo";

    private BuildInfo[] buildInfo; // the log files in the log directory.
    private int count;  // How many times around the loop have we gone.

    private int startingBuildNumber = 0;
    private int finalBuildNumber = Integer.MAX_VALUE;
    private int endPoint;
    private DateFormat dateFormat = null;

    protected String getLinkText(BuildInfo info) {
        String label = "";
        if (info.getLabel() != null) {
            label = " (" + info.getLabel() + ")";
        }

        return getDateFormat().format(info.getBuildDate()) + label;
    }

    public int doStartTag() throws JspException {
        BuildInfo [] logFileNames = findLogFiles();
        //sort links...
        Arrays.sort(logFileNames, new ReversedComparator());
        buildInfo = logFileNames;
        count = Math.max(0, startingBuildNumber);
        endPoint = Math.min(finalBuildNumber, buildInfo.length - 1) + 1;
        if (count < endPoint) {
            return EVAL_BODY_TAG;
        } else {
            return SKIP_BODY;
        }
    }

    private BuildInfo[] findLogFiles() throws JspException {
        File logDir = findLogDir();
        return BuildInfo.loadFromDir(logDir).asArray();
    }

    public void doInitBody() throws JspException {
        setupLinkVariables();
    }

    void setupLinkVariables() throws JspTagException {
        final BuildInfo info = buildInfo[count];
        String logName = info.getLogName();
        getPageContext().setAttribute(URL_ATTR, createUrl(LOG_PARAMETER, logName));
        getPageContext().setAttribute(LINK_TEXT_ATTR, getLinkText(info));
        getPageContext().setAttribute(LOG_FILE_ATTR, logName);
        getPageContext().setAttribute(BUILD_INFO_ATTR, info);
        count++;
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
                throw new CCTagException("IO Error: " + e.getMessage(), e);
            }
            return SKIP_BODY;
        }
    }

    public int getStartingBuildNumber() {
        return startingBuildNumber;
    }

    public void setStartingBuildNumber(int startingBuildNumber) {
        this.startingBuildNumber = startingBuildNumber;
    }

    public int getFinalBuildNumber() {
        return finalBuildNumber;
    }

    public void setFinalBuildNumber(int finalBuildNumber) {
        this.finalBuildNumber = finalBuildNumber;
    }

    /**
     * Set the DateFormat to use.
     * The default is based on the client's locale with a 24 hour time. For a
     * client with a US locale that would be MM/dd/yyyy HH:mm:ss.
     *
     * @param dateFormatString The date format to use. Any format appropriate for the
     *                         {@link SimpleDateFormat} is okay to use.
     * @see SimpleDateFormat
     */
    public void setDateFormat(String dateFormatString) {
        dateFormat = new SimpleDateFormat(dateFormatString);
    }

    private DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = DateHelper.createDateFormat(getLocale());
        }
        return dateFormat;
    }

}
