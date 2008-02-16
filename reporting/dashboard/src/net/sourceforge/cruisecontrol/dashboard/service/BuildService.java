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

import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildLiveDetail;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.LogFile;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.exception.ShouldStopParsingException;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.BasicInfoExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.DurationExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.ModificationExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.StackTraceExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.TestSuiteExtractor;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.BuildSummariesFilters;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.SpecificLogFileFilter;
import org.apache.log4j.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildService {
    private static final Logger LOGGER = Logger.getLogger(BuildService.class);

    private final ConfigurationService configuration;
    private final BuildLoopQueryService queryService;

    public BuildService(ConfigurationService configuration, BuildLoopQueryService queryService) {
        this.configuration = configuration;
        this.queryService = queryService;
    }

    public BuildLiveDetail getActiveBuild(final String projectName, PreviousResult previousResult) {
        return new BuildLiveDetail(projectName, previousResult);
    }

    public BuildDetail getBuild(String projectName, String yyyyMMddssHHmmss) {
        BuildDetail build = createBuildFromFile(getBuildFile(projectName, yyyyMMddssHHmmss));
        if (queryService.isDiscontinued(projectName)) {
            build.updateStatus(CurrentStatus.DISCONTINUED);
        } else if (isPaused(projectName)) {
            build.updateStatus(CurrentStatus.PAUSED);
        }
        return build;
    }

    private boolean isPaused(String projectName) {
        String status = queryService.getProjectStatus(projectName);
        return CurrentStatus.PAUSED.equals(CurrentStatus.getProjectBuildStatus(status));
    }

    private LogFile getBuildFile(String projectName, final String yyyyMMddssHHmmss) {
        File logRoot = configuration.getLogRoot(projectName);
        File[] files = logRoot.listFiles(new SpecificLogFileFilter(yyyyMMddssHHmmss));
        return new LogFile(logRoot, files[0].getName());
    }

    BuildDetail createBuildFromFile(LogFile logFile) {
        try {
            Map properties = new HashMap();
            parseLogFile(logFile, properties);
            properties.put("artifactfolder", getArtifactsRootDir((String) properties.get("projectname")));
            return new BuildDetail(logFile, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Can not parse the log file: " + logFile.getAbsolutePath(), e);
            return null;
        }
    }

    private void parseLogFile(LogFile buildLogFile, Map props) throws Exception {
        CompositeExtractor compositeExtractor = compositeExtractor(buildLogFile);
        parse(buildLogFile.getInputStream(), compositeExtractor);
        compositeExtractor.report(props);
    }

    private void parse(InputStream logFileInputStream, CompositeExtractor compositeExtractor)
            throws Exception {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        try {
            saxParser.parse(logFileInputStream, compositeExtractor);
        } catch (ShouldStopParsingException e) {
            LOGGER.debug("Intentionally throwing exception to stop parsing.");
        }
    }

    private CompositeExtractor compositeExtractor(File buildLogFile) {
        List handlers = defaultExtractors();
        if (!succeeded(buildLogFile)) {
            handlers.add(new StackTraceExtractor());
        }
        return new CompositeExtractor(handlers);
    }

    private List defaultExtractors() {
        List extractors = new ArrayList();
        extractors.add(new DurationExtractor());
        extractors.add(new ModificationExtractor());
        extractors.add(new BasicInfoExtractor());
        extractors.add(new TestSuiteExtractor());
        return extractors;
    }

    private boolean succeeded(File buildLogFile) {
        return BuildSummariesFilters.succeedFilter().accept(buildLogFile.getParentFile(),
                buildLogFile.getName());
    }

    private File getArtifactsRootDir(String projectName) {
        File artifactRoot = configuration.getArtifactRoot(projectName);
        if (artifactRoot == null) {
            return new File("could/not/find/artifactsfolder/for/" + projectName);
        }
        return artifactRoot;
    }
}
