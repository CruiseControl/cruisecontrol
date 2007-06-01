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
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.cruisecontrol.dashboard.exception.ShouldStopParsingException;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.DurationExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.ModificationExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.SAXBasedExtractor;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class BuildSummary implements Build {
    private String label;

    private String name;

    private File buildLogFile;

    private String projectName = "";

    private ProjectBuildStatus status;

    private DateTime buildingSince;

    private Map propertiesFromLogContent = new HashMap();

    private static final Logger LOGGER = Logger.getLogger(BuildSummary.class);

    public BuildSummary(String projectName, String name, String label, ProjectBuildStatus status,
            String buildLogFilename) {
        this.projectName = projectName;
        this.status = status;
        this.label = label;
        this.name = name;
        this.status = status;
        this.buildLogFile = new File(buildLogFilename);
    }

    public String getName() {
        return name;
    }

    public boolean hasPassed() {
        return status.equals(ProjectBuildStatus.PASSED);
    }

    public String getLabel() {
        return label;
    }

    public String getStatus() {
        return status.getStatus();
    }

    public String getBuildLogFilename() {
        return buildLogFile.getName();
    }

    public String getProjectName() {
        return projectName;
    }

    public DateTime getBuildDate() {
        return CCDateFormatter.format(getName(), "yyyy-MM-dd HH:mm.ss");
    }

    public DateTime getBuildingSince() {
        return buildingSince;
    }

    public void updateStatus(String statusStr) {
        ProjectBuildStatus newStatus = ProjectBuildStatus.getProjectBuildStatus(statusStr);
        if (!newStatus.equals(ProjectBuildStatus.WAITING)) {
            status = newStatus;
            buildingSince = new DateTime(ProjectBuildStatus.getTimestamp(statusStr));
        }
    }

    public String getDuration() {
        try {
            if (!propertiesFromLogContent.containsKey("duration")) {
                parseLogFile(new DurationExtractor());
            }
            return (String) propertiesFromLogContent.get("duration");
        } catch (Exception e) {
            return "0 second";
        }
    }

    private void parseLogFile(SAXBasedExtractor extractor) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(buildLogFile, new CompositeExtractor(new SAXBasedExtractor[] {extractor}));
        } catch (ShouldStopParsingException se) {
            LOGGER.debug("Intentionally throwing exception to stop parsing " + se.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        extractor.report(propertiesFromLogContent);
    }

    public int compareTo(Object other) {
        Build otherSummary = (Build) other;
        String originalOther = StringUtils.defaultString(otherSummary.getProjectName());
        String originalThis = StringUtils.defaultString(this.getProjectName());
        int result = originalThis.toLowerCase().compareTo(originalOther.toLowerCase());
        if (result == 0) {
            return originalThis.compareTo(originalOther);
        }
        return result;
    }

    public String toString() {
        return this.getName();
    }

    public ModificationSet getModificationSet() {
        try {
            if (!propertiesFromLogContent.containsKey("modifications")) {
                parseLogFile(new ModificationExtractor());
            }
            return (ModificationSet) propertiesFromLogContent.get("modifications");
        } catch (Exception e) {
            return null;
        }
    }
}
