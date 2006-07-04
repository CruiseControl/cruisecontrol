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
import java.util.Date;
import java.util.Locale;

import net.sourceforge.cruisecontrol.util.DateHelper;

/**
 * @author Jared Richardson User: jfredrick Adapted from StatusPage.java,
 *         submitted by Jared to the cruisecontrol-devel mailing list.
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn </a>
 * @author <a href="mailto:jeffjensen@upstairstechnology.com">Jeff Jensen </a>
 */
public class StatusHelper {
    private BuildInfo newestBuild;
    private BuildInfo newestSuccessfulBuild;

    private static final String PASSED = "passed";
    private static final String FAILED = "failed";

    public void setProjectDirectory(File directory) {
        LogFile newestLogfile = LogFile.getLatestLogFile(directory);
        if (newestLogfile == null) {
            newestBuild = null;
        } else {
            try {
                newestBuild = new BuildInfo(newestLogfile);
            } catch (ParseException pe) {
                newestBuild = null;
            }
        }
        LogFile newestSuccessfulLogfile = LogFile.getLatestSuccessfulLogFile(directory);
        if (newestSuccessfulLogfile == null) {
            newestSuccessfulBuild = null;
        } else {
            try {
                newestSuccessfulBuild = new BuildInfo(newestSuccessfulLogfile);
            } catch (ParseException pe) {
                newestBuild = null;
            }
        }
    }

    public String getLastBuildResult() {
        if (newestBuild == null) {
            return null;
        }

        return newestBuild.isSuccessful() ? PASSED : FAILED;
    }

    public String getLastBuildTimeString(Locale locale) {
        if (newestBuild == null) {
            return null;
        }
        return getBuildTimeString(newestBuild, locale);
    }

public Date getLastBuildTime() {
if (newestBuild == null) {
return null;
}
return newestBuild.getBuildDate();
}

    public String getLastSuccessfulBuildLabel() {
        if (newestSuccessfulBuild == null) {
            return null;
        }

        return newestSuccessfulBuild.getLabel();
    }

    public String getLastSuccessfulBuildTimeString(Locale locale) {
        if (newestSuccessfulBuild == null) {
            return null;
        }
        return getBuildTimeString(newestSuccessfulBuild, locale);
    }

    private String getBuildTimeString(BuildInfo logInfo, Locale locale) {
        Date date = logInfo.getBuildDate();
        return DateHelper.createDateFormat(locale).format(date);
    }

    public String getCurrentStatus(String singleProject, String logDirPath, String projectName, String statusFile) {
        boolean isSingleProject = Boolean.getBoolean(singleProject);

        return getCurrentStatus(isSingleProject, logDirPath, projectName, statusFile);
    }

    public String getCurrentStatus(boolean isSingleProject, String logDirPath, String projectName, String statusFile) {
        String status = null;

        try {
            status = BuildStatus.getStatusHtml(isSingleProject, logDirPath, projectName, statusFile,
                BuildStatus.READ_ONLY_STATUS_LINES);
        } catch (CruiseControlWebAppException e) {
            status = '(' + e.getMessage() + ')';
        }

        return status;
    }
}
