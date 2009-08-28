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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.utils.TimeConverter;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class BuildDetail implements Comparable, Build {

    private DateTime date;

    private String timeStamp;

    private Map pluginOutpus = new LinkedHashMap();

    private final Map props;

    private final LogFile logFile;

    private CurrentStatus currentStatus = CurrentStatus.WAITING;

    private final TimeConverter timeConverter;

    public BuildDetail(LogFile logFile) {
        this(logFile, new HashMap(), new TimeConverter());
    }

    public BuildDetail(LogFile logFile, Map props) {
        this(logFile, props, new TimeConverter());
    }

    public BuildDetail(LogFile logFile, Map props, TimeConverter timeConverter) {
        this.logFile = logFile;
        this.timeConverter = timeConverter;
        this.props = Collections.unmodifiableMap(props);
    }

    public String getProjectName() {
        return (String) props.get("projectname");
    }

    public String getLabel() {
        return (String) props.get("label");
    }

    public String getDuration() {
        return (String) props.get("duration");
    }

    public LogFile getLogFile() {
        return logFile;
    }

    public String getLogFileName() {
        return this.getBuildLogFilename();
    }

    public File getLogFolder() {
        return getLogFile().getParentFile();
    }

    public List getTestSuites() {
        return (List) props.get("testsuites");
    }

    public String getTimeStamp() {
        if (timeStamp == null) {
            timeStamp = CCDateFormatter.yyyyMMddHHmmss(this.getBuildDate());
        }
        return timeStamp;
    }

    public int compareTo(Object o) {
        return this.getBuildDate().compareTo(((BuildDetail) o).getBuildDate());
    }

    public List<Modification> getModifications() {
        return (List<Modification>) props.get("modifications");
    }

    public int getNumberOfTests() {
        int totalNumberOfTests = 0;

        List testSuites = getTestSuites();
        for (int i = 0; i < testSuites.size(); i++) {
            BuildTestSuite suite = (BuildTestSuite) testSuites.get(i);
            totalNumberOfTests += suite.getNumberOfTests();
        }

        return totalNumberOfTests;
    }

    public int getNumberOfFailures() {
        int numberOfFailures = 0;

        List testSuites = getTestSuites();
        for (int i = 0; i < testSuites.size(); i++) {
            BuildTestSuite suite = (BuildTestSuite) testSuites.get(i);
            numberOfFailures += suite.getNumberOfFailures();
        }

        return numberOfFailures;
    }

    public int getNumberOfErrors() {
        int numberOfErrors = 0;

        List testSuites = getTestSuites();
        for (int i = 0; i < testSuites.size(); i++) {
            BuildTestSuite suite = (BuildTestSuite) testSuites.get(i);
            numberOfErrors += suite.getNumberOfErrors();
        }

        return numberOfErrors;
    }

    public boolean hasPassed() {
        return StringUtils.contains(getLogFileName(), "L");
    }

    public File getArtifactFolder() {
        File folder = (File) props.get("artifactfolder");
        return new File(folder, getTimeStamp());
    }

    public List getArtifactFiles() {
        List result = new ArrayList();
        File[] files = getArtifactFolder().listFiles();
        if (files == null) {
            return result;
        }
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isHidden()) {
                result.add(files[i]);
            }
        }
        return result;
    }

    public Map getPluginOutputs() {
        return pluginOutpus;
    }

    public void addPluginOutput(String category, Object output) {
        pluginOutpus.put(category, output);
    }

    public DateTime getBuildDate() {
        if (date == null) {
            date = CCDateFormatter.formatLogName(getLogFileName());
        }
        return date;
    }

    public String getBuildLogFilename() {
        return getLogFile().getName();
    }

    public DateTime getBuildingSince() {
        return getBuildDate();
    }

    public CurrentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void updateStatus(CurrentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public PreviousResult getPreviousBuildResult() {
        return hasPassed() ? PreviousResult.PASSED : PreviousResult.FAILED;
    }

    public String getConvertedTime() {
        return this.timeConverter.getConvertedTime(getBuildDate().toDate());
    }
}
