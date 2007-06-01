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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.exception.ShouldStopParsingException;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.BasicInfoExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.DurationExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.ModificationExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.SAXBasedExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.StackTraceExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.TestSuiteExtractor;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.BuildSummariesFilters;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class BuildService {
    private static final Logger LOGGER = Logger.getLogger(BuildService.class);

    private net.sourceforge.cruisecontrol.dashboard.Configuration configuration;

    public BuildService(net.sourceforge.cruisecontrol.dashboard.Configuration configuration) {
        this.configuration = configuration;
    }

    public Build getActiveBuild(final String projectName) {
        return new BuildDetail(new HashMap()) {
            public String getStatus() {
                return ProjectBuildStatus.BUILDING.getStatus();
            }

            public String getProjectName() {
                return projectName;
            }

            public String getBuildLogFilename() {
                String now = CCDateFormatter.yyyyMMddHHmmss(new DateTime());
                return "log" + now + ".xml";
            }

            public DateTime getBuildDate() {
                return new DateTime();
            }
        };
    }

    public Build getBuild(String projectName, String buildLogFilename) {
        return createBuildFromFile(getBuildFile(projectName, buildLogFilename));
    }

    public File getBuildFile(String projectName, String buildLogFileName) {
        return new File(configuration.getCruiseLogfileLocation() + File.separator + projectName
                + File.separator + buildLogFileName);
    }

    public BuildDetail createBuildFromFile(File logFile) {
        try {
            Map properties = new HashMap();
            parseLogFile(logFile, properties);
            properties.put("logfile", logFile);
            properties.put("artifactfolder", getArtifactsRootDir((String) properties
                    .get("projectname")));
            return new BuildDetail(properties);
        } catch (Exception e) {
            LOGGER.error("Can not parse the log file: " + logFile.getAbsolutePath(), e);
            return null;
        }
    }

    private void parseLogFile(File buildLogFile, Map props) throws Exception {
        // TODO injection
        SAXBasedExtractor[] handlers;
        if (BuildSummariesFilters.succeedFilter().accept(buildLogFile.getParentFile(),
                buildLogFile.getName())) {
            handlers =
                    new SAXBasedExtractor[] {new DurationExtractor(), new ModificationExtractor(),
                            new BasicInfoExtractor()};
        } else {
            handlers =
                    new SAXBasedExtractor[] {new DurationExtractor(), new ModificationExtractor(),
                            new BasicInfoExtractor(), new StackTraceExtractor(),
                            new TestSuiteExtractor()};
        }
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        CompositeExtractor compositeExtractor = new CompositeExtractor(handlers);
        try {
            saxParser.parse(buildLogFile, compositeExtractor);
        } catch (ShouldStopParsingException e) {
            LOGGER.debug("Intentionally throwing exception to stop parsing.");
        }
        compositeExtractor.report(props);
    }

    private File getArtifactsRootDir(String projectName) {
        File artifactRoot = configuration.getArtifactRoot(projectName);
        if (artifactRoot == null) {
            return new File("could/not/find/artifactsfolder/for/" + projectName);
        }
        return artifactRoot;
    }
}
