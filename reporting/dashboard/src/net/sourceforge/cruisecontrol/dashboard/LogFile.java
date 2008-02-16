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
package net.sourceforge.cruisecontrol.dashboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Understands parsing a log file.
 */
public class LogFile extends File {
    private static final String LOG_COMPRESSED_SUFFIX = ".xml.gz";

    private String dateTime;
    private String label;

    public LogFile(String pathname) {
        super(pathname);
        initializeInternalState();
    }

    public LogFile(File parent, String child) {
        super(parent, child);
        initializeInternalState();
    }

    private void initializeInternalState() {
        Matcher matcher = matcher();
        validateLogFileName(matcher);
        dateTime = extractDateTime(matcher);
        label = extractLabel(matcher);
    }

    private String extractLabel(Matcher matcher) {
        return isSuccessful() ? matcher.group(LABEL_GROUP) : "";
    }

    private void validateLogFileName(Matcher matcher) {
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid logfile name: " + getName());
        }
    }

    public InputStream getInputStream() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(this);
        if (isZippedLogFile()) {
            return new GZIPInputStream(fileInputStream);
        } else {
            return fileInputStream;
        }
    }

    public String getDateTime() {
        return dateTime;
    }

    private boolean isZippedLogFile() {
        return getName().endsWith(LOG_COMPRESSED_SUFFIX);
    }

    private static final int YEAR_GROUP = 1;
    private static final int MONTH_GROUP = 2;
    private static final int DAY_GROUP = 3;
    private static final int HOUR_GROUP = 4;
    private static final int MINUTE_GROUP = 5;
    private static final int SECOND_GROUP = 6;

    private static final int LABEL_GROUP = 7;

    private String extractDateTime(Matcher matcher) {
        return matcher.group(YEAR_GROUP) + "-" + matcher.group(MONTH_GROUP) + "-" + matcher.group(DAY_GROUP)
                + " " + matcher.group(HOUR_GROUP) + ":" + matcher.group(MINUTE_GROUP) + "."
                + matcher.group(SECOND_GROUP);
    }

    private static final Pattern SUCCESSFUL_BUILD_PATTERN =
                Pattern.compile("^log(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})L(.*)\\.xml(\\.gz)?$");

    private static final Pattern FAILED_BUILD_PATTERN =
            Pattern.compile("^log(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})\\.xml(\\.gz)?$");

    private Matcher matcher() {
        Pattern pattern = isSuccessful() ? SUCCESSFUL_BUILD_PATTERN : FAILED_BUILD_PATTERN;
        return pattern.matcher(getName());
    }

    private boolean isSuccessful() {
        return getName().indexOf("L") > 0;
    }

    public String getLabel() {
        return label;
    }
}
