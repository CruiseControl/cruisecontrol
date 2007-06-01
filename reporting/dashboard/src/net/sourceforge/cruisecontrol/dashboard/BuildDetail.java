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
import java.util.Iterator;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class BuildDetail implements Comparable, Build {

    private static Logger logger = Logger.getLogger(BuildDetail.class);

    private DateTime date;
    private String timeStamp;
    private Map pluginOutpus = new LinkedHashMap();
    private List artifacts;
    private List artifactNames;
    private Map props;

    public BuildDetail(Map props) {
        if (props == null) {
            this.props = new HashMap();
        } else {
            this.props = Collections.unmodifiableMap(props);
        }
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

    public File getLogFile() {
        return (File) props.get("logfile");
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

    public ModificationSet getModificationSet() {
        return (ModificationSet) props.get("modifications");
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

    public String getStackTrace() {
        return (String) props.get("stacktrace");
    }

    public String getStatus() {
        return this.hasPassed() ? "Passed" : "Failed";
    }

    public boolean hasPassed() {
        return StringUtils.contains(getLogFileName(), "L");
    }

    public File getArtifactFolder() {
        File folder = (File) props.get("artifactfolder");
        return new File(folder, getTimeStamp());
    }

    public List getArtifacts() {
        if (artifacts == null) {
            artifacts = getArtifactsPaths(getArtifactFolder().listFiles());
        }
        return artifacts;
    }

    public List getArtifactNames() {
        if (artifactNames == null) {
            artifactNames = new ArrayList();
            try {
                String artifactsRootDir = getArtifactFolder().getPath();
                Iterator iter = getArtifacts().iterator();
                while (iter.hasNext()) {
                    String artifactPath = (String) iter.next();
                    artifactNames.add(StringUtils.remove(artifactPath, artifactsRootDir + File.separator));
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
        return this.artifactNames;
    }

    public Map getPluginOutputs() {
        return pluginOutpus;
    }

    public void addPluginOutput(String category, Object output) {
        pluginOutpus.put(category, output);
    }

    private static List getArtifactsPaths(File[] artifactDirs) {
        List result = new ArrayList();
        if (artifactDirs == null) {
            return result;
        }
        for (int i = 0; i < artifactDirs.length; i++) {
            File file = artifactDirs[i];
            String filePath = file.getName();
            if (filePath.indexOf(".") == 0) {
                continue;
            }
            if (file.isDirectory()) {
                result.addAll(getArtifactsPaths(file.listFiles()));
            } else {
                result.add(file.getPath());
            }
        }
        return result;
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

    public String getName() {
        return null;
    }

    public void updateStatus(String statusStr) {
    }
}
