/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.sourceforge.cruisecontrol.taglib.CruiseControlTagSupport;
import net.sourceforge.cruisecontrol.util.DateUtil;

/**
 * @author Jared Richardson
 * User: jfredrick
 * Adapted from StatusPage.java, submitted by Jared to the cruisecontrol-devel mailing list.
 */
public class StatusHelper {
    private File newestLogfile;
    private File newestSuccessfulLogfile;

    private static final String PASSED = "passed";
    private static final String FAILED = "failed";

    private static final SimpleDateFormat LOG_TIME_FORMAT_SECONDS = new SimpleDateFormat("yyyyMMddHHmmss");

    public void setProjectDirectory(File directory) {
        newestLogfile = CruiseControlTagSupport.getLatestLogFile(directory);
        newestSuccessfulLogfile = CruiseControlTagSupport.getLatestSuccessfulLogFile(directory);
    }

    public String getLastBuildResult() {
        if (newestLogfile == null) {
            return null;
        }

        if (newestLogfile.equals(newestSuccessfulLogfile)) {
            return PASSED;
        }

        return FAILED;
    }

    public String getLastBuildTimeString(Locale locale) {
        if (newestLogfile == null) {
            return null;
        }
        String filename = newestLogfile.getName();
        return getBuildTimeString(filename, locale);
    }

    public String getLastSuccessfulBuildLabel() {
        if (newestSuccessfulLogfile == null) {
            return null;
        }

        String filename = newestSuccessfulLogfile.getName();

        // passing log file name is of form log20020102030405L.*.xml
        // look for L
        return filename.substring(18, (filename.length() - 4));
    }

    public String getLastSuccessfulBuildTimeString(Locale locale) {
        if (newestSuccessfulLogfile == null) {
            return null;
        }
        String filename = newestSuccessfulLogfile.getName();
        return getBuildTimeString(filename, locale);
    }

    private String getBuildTimeString(String filename, Locale locale) {
        String dateFromFilename = filename.substring(3, 17);
        String dateString = "error";
        try {
            Date date = LOG_TIME_FORMAT_SECONDS.parse(dateFromFilename);
            dateString = DateUtil.createDateFormat(locale).format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateString;
    }    

}
