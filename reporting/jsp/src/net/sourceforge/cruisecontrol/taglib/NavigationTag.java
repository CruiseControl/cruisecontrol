/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

/**
 *
 */
public class NavigationTag extends CruiseTagSupport {
    public static final String LABEL_SEPARATOR = "L";
    public static final SimpleDateFormat US_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    public static final String LINK_TEXT_ATTR = "linktext";
    public static final String URL_ATTR = "url";
    public static final String LOG_FILE_ATTR = "logfile";

    private File logDir;
    private String[] fileNames;
    private int count;
    private DateFormat dateFormat = US_DATE_FORMAT;

    private int startingBuildNumber = 0;
    private int finalBuildNumber = Integer.MAX_VALUE;
    private int endPoint;
    private static final SimpleDateFormat LOG_TIME_FORMAT_SECONDS = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

    private String extractLogNameFromFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf(".xml"));
    }

    protected String getLinkText(String fileName) {
        String dateString = "";
        String label = "";
        if (fileName.lastIndexOf(LABEL_SEPARATOR) > -1) {
            dateString = fileName.substring(3, fileName.indexOf(LABEL_SEPARATOR));
            label = " (" + fileName.substring(fileName.indexOf(LABEL_SEPARATOR) + 1, fileName.length())
                    + ")";
        } else {
            dateString = fileName.substring(3, fileName.length());
        }
        DateFormat inputDate = null;
        if (dateString.length() == 14) {
            inputDate = LOG_TIME_FORMAT_SECONDS;
        } else {
            inputDate = LOG_TIME_FORMAT;
        }

        Date date = null;
        try {
            date = inputDate.parse(dateString);
        } catch (ParseException e) {
            err(e);
        }

        return dateFormat.format(date) + label;
    }

    public int doStartTag() throws JspException {
        String [] logFileNames = findLogFiles();
        //sort links...
        Arrays.sort(logFileNames, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((String) o2).compareTo((String) o1);
            }
        });
        setFileNames(logFileNames);
        count = Math.max(0, startingBuildNumber);
        endPoint = Math.min(finalBuildNumber, fileNames.length - 1) + 1;
        if (count < endPoint) {
            return EVAL_BODY_TAG;
        } else {
            return SKIP_BODY;
        }
    }

    private String[] findLogFiles() throws JspException {
        logDir = findLogDir();
        String logDirPath = logDir.getAbsolutePath();
        info("Scanning directory: " + logDirPath + " for log files.");
        String [] logFileNames = logDir.list(new CruiseLogFileNameFilter());
        if (logFileNames == null) {
            throw new JspException("Could not access the directory " + logDirPath);
        } else if (logFileNames.length == 0) {
            throw new JspException("Configuration problem? No logs found in logDir: " + logDirPath);
        }
        return logFileNames;
    }

    void setFileNames(String [] logFileNames) {
        fileNames = logFileNames;
    }

    public void doInitBody() throws JspException {
       setupLinkVariables();
    }

    void setupLinkVariables() {
        final String fileName = fileNames[count];
        String logName = extractLogNameFromFileName(fileName);
        getPageContext().setAttribute(URL_ATTR, createUrl("log", logName));
        getPageContext().setAttribute(LINK_TEXT_ATTR, getLinkText(logName));
        getPageContext().setAttribute(LOG_FILE_ATTR, logName);
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
     * Set the DateFormat to use. The default is for US-Style (MM/dd/yyyy HH:mm:ss).
     * @param dateFormatString  the date format to use. Any format appropriate for the java.text.SimpleDataFormat is
     *                          okay to use.
     */
    public void setDateFormat(String dateFormatString) {
        dateFormat = new SimpleDateFormat(dateFormatString);
    }

    private static class CruiseLogFileNameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            if (!name.startsWith("log")) {
                return false;
            } else if (!name.endsWith(".xml")) {
                return false;
            } else if (new File(dir, name).isDirectory()) {
                return false;
            }
            return true;
        }
    }
}
