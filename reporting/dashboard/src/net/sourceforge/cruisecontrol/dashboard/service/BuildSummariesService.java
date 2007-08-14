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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.dashboard.Build;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.ProjectBuildStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.AlphabeticalDescOrderComparator;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.BuildSummariesFilters;
import net.sourceforge.cruisecontrol.dashboard.utils.functors.ReportableFilter;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;

public class BuildSummariesService {
    private static final int LIST_MAXIUM = 25;

    private BuildSummaryService buildSummaryService;

    private static final FilenameFilter CCLOG_FILTER = BuildSummariesFilters.cclogFilter();

    private static final AlphabeticalDescOrderComparator LOG_FILE_DESC_COMPARATOR =
            new AlphabeticalDescOrderComparator();

    private final Configuration configuration;

    public BuildSummariesService(Configuration configuration, BuildSummaryService buildSummaryService) {
        this.configuration = configuration;
        this.buildSummaryService = buildSummaryService;
    }

    public List getLastest25(String projectName) {
        File pjDir = configuration.getLogRoot(projectName);
        return getBuildSummariesObject(getBuildSummariesFile(pjDir, CCLOG_FILTER, LIST_MAXIUM));
    }

    public Build getLatest(String projectName) {
        File pjDir = configuration.getLogRoot(projectName);
        ReportableFilter latest = BuildSummariesFilters.lastFilter();
        getBuildSummariesFile(pjDir, latest, 1);
        return buildSummaryService.createBuildSummary(latest.report());
    }

    public List getAllSucceed(String projectName) {
        File pjDir = configuration.getLogRoot(projectName);
        return getBuildSummariesObject(getBuildSummariesFile(pjDir, BuildSummariesFilters.succeedFilter(),
                Integer.MAX_VALUE));
    }

    public List getAll(String projectName) {
        File pjDir = configuration.getLogRoot(projectName);
        return getBuildSummariesObject(getBuildSummariesFile(pjDir, CCLOG_FILTER, Integer.MAX_VALUE));
    }

    public Build getLastSucceed(String projectName, DateTime datetime) {
        return filterBuildSummaries(projectName, BuildSummariesFilters.lastSucceedFilter(datetime));
    }

    public Build getLastFailed(String projectName, DateTime datetime) {
        return filterBuildSummaries(projectName, BuildSummariesFilters.lastFailedFilter(datetime));
    }

    private Build filterBuildSummaries(String projectName, ReportableFilter filter) {
        File pjDir = configuration.getLogRoot(projectName);
        getBuildSummariesObject(getBuildSummariesFile(pjDir, filter, 1));
        if (filter.report() == null) {
            return null;
        } else {
            return buildSummaryService.createBuildSummary(filter.report());
        }
    }

    public Build getEaliestFailed(String projectName, DateTime datetime) {
        Build lastSucceeded = getLastSucceed(projectName, datetime);
        if (lastSucceeded == null) {
            List summaries = getAll(projectName);
            return summaries.size() == 0 ? null : (Build) summaries.get(summaries.size() - 1);
        }
        return filterBuildSummaries(projectName, BuildSummariesFilters.earliestFailedFilter(lastSucceeded
                .getBuildDate()));
    }

    public Build getEarliestSucceeded(String projectName, DateTime datetime) {
        Build lastFailed = getLastFailed(projectName, datetime);
        if (lastFailed == null) {
            List summaries = getAll(projectName);
            return summaries.size() == 0 ? null : (Build) summaries.get(summaries.size() - 1);
        }
        return filterBuildSummaries(projectName, BuildSummariesFilters.earliestSucceededFilter(lastFailed
                .getBuildDate()));
    }

    public String getDurationFromLastSuccessfulBuild(String projectName, DateTime datetime) {
        Build lastSucessfulBuild = getLastSucceed(projectName, datetime);
        if (lastSucessfulBuild == null) {
            return "N/A";
        }
        long timeSpan = datetime.getMillis() - lastSucessfulBuild.getBuildDate().getMillis();
        return CCDateFormatter.duration(timeSpan);
    }

    public List getLatestOfProjects() {
        File[] osDirs = configuration.getProjectDirectoriesFromFileSystem();
        File[] cfgDirs = configuration.getProjectDirectoriesFromConfigFile();
        Collection inactives = CollectionUtils.disjunction(asList(osDirs), asList(cfgDirs));
        Collection actives = CollectionUtils.intersection(asList(osDirs), asList(cfgDirs));
        List allSummaries = new ArrayList();
        for (Iterator iter = inactives.iterator(); iter.hasNext();) {
            allSummaries.add(buildSummaryService.createInactive((File) iter.next()));
        }
        for (Iterator iter = actives.iterator(); iter.hasNext();) {
            ReportableFilter latest = BuildSummariesFilters.lastFilter();
            File folder = (File) iter.next();
            getBuildSummariesFile(folder, latest, 1);
            allSummaries.add(buildSummaryService.createBuildSummary(latest.report()));
        }
        Collections.sort(allSummaries);
        return allSummaries;
    }

    private Collection asList(File[] ary) {
        if (ary == null) {
            return new ArrayList();
        } else {
            return Arrays.asList(ary);
        }
    }

    private File[] getBuildSummariesFile(File pjDir, FilenameFilter logFilter, int maxium) {
        String[] logFileNames = pjDir.list(logFilter);
        if (logFileNames == null) {
            return new File[] {};
        }
        Arrays.sort(logFileNames, LOG_FILE_DESC_COMPARATOR);
        int size = Math.min(logFileNames.length, maxium);
        File[] logFiles = new File[size];
        for (int i = 0; i < logFiles.length; i++) {
            logFiles[i] = new File(pjDir, logFileNames[i]);
        }
        return logFiles;
    }

    private List getBuildSummariesObject(File[] buildSummariesFiles) {
        List summaries = new ArrayList();
        for (int i = 0; i < buildSummariesFiles.length; i++) {
            summaries.add(buildSummaryService.createBuildSummary(buildSummariesFiles[i]));
        }
        return summaries;
    }

    public ProjectBuildStatus getLastBuildStatus(String projectName) {
        try {
            Build latest = getLatest(projectName);
            return latest == null ? ProjectBuildStatus.PASSED : latest.getStatus();
        } catch (Exception e) {
            return ProjectBuildStatus.PASSED;
        }
    }
}
