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
package net.sourceforge.cruisecontrol.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.LogFileReader;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.StatusHelper;

/**
 * Servlet to generate an XML file in the format required by cctray
 * see http://confluence.public.thoughtworks.org/display/CCNET/CCTray
 * 
 */
public class XmlServlet extends HttpServlet {

    private static final int MILLISECONDS_IN_SECOND = 1000;
    public static final String STATUS_UNKNOWN = "Unknown";
    public static final String STATUS_SUCCESS = "Success";
    public static final String STATUS_FAILURE = "Failure";
    public static final String ACTIVITY_CHECKING_MODIFICATIONS = "CheckingModifications";
    public static final String ACTIVITY_BUILDING = "Building";
    public static final String ACTIVITY_SLEEPING = "Sleeping";
    public static final String LOG_DIR = "logDir";
    public static final String SINGLE_PROJECT = "singleProject";
    public static final String CURRENT_BUILD_STATUS_FILE = "currentBuildStatusFile";
    private static final SimpleDateFormat XML_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    private String statusFile;
    private String singleProject;
    private String logDirPath;
    private File logDir;


    /** 
     * Set up the servlet, mainly getting the required parameters.
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig servletconfig) throws ServletException {
        super.init(servletconfig);
        this.statusFile = getInitParameter(servletconfig, CURRENT_BUILD_STATUS_FILE);
        this.singleProject = getInitParameter(servletconfig, SINGLE_PROJECT);
        this.logDirPath = getInitParameter(servletconfig, LOG_DIR);
        this.logDir = new File(logDirPath);
    }

    /** 
     * Produce XML in the format required by CCTray
     * @see javax.servlet.http.HttpServlet
     */
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] projectDirs = getProjectDirs();
        response.setContentType("text/xml");
        PrintWriter writer = response.getWriter();
        Element projectsElement = new Element("Projects");
        
        
        for (int i = 0; i < projectDirs.length; i++) {
            String project = projectDirs[i];
            File projectDir = new File(this.logDir, project);
            if (!projectDir.isDirectory()) {
                throw new ServletException("Project directory must be a directory "
                                            + this.logDirPath + File.separator + project);
            }
            // initialise status Helper for this project
            StatusHelper statusHelper = new StatusHelper();
            statusHelper.setProjectDirectory(projectDir);
            
            Element projectElement = new Element("Project");
            projectElement.setAttribute("name", project);
            projectElement.setAttribute("activity", getActivity(project, statusHelper));
            projectElement.setAttribute("lastBuildStatus",  getFormattedStatus(statusHelper));
            projectElement.setAttribute("lastBuildLabel", statusHelper.getLastSuccessfulBuildLabel());
            projectElement.setAttribute("lastBuildTime", getFormattedDate(statusHelper));
            projectElement.setAttribute("nextBuildTime", getNextBuildDate(statusHelper));
            projectElement.setAttribute("webUrl", getBaseUrl(request) + project);
            
            projectsElement.addContent(projectElement);
        }
        Document document = new Document(projectsElement);
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        outputter.output(document, writer);
        writer.flush();
        writer.close();
    }

    /**
     * Get the next build date for the project.
     * This is the date/time cruisecontrol will next check for modifications.
     * This assumes that the next build will be a whole number of intervals
     * from the las build date.
     * This is returned as a string in the format yyyy-MM-dd'T'HH:mm:ss
     * 
     * @return the next build date
     */
    private String getNextBuildDate(StatusHelper statusHelper) {
        int buildInterval = getBuildInterval(statusHelper);
        Date lastBuildDate = statusHelper.getLastBuildTime();
        Calendar nextBuild = Calendar.getInstance();
        if (buildInterval != 0) {
            final Date now = new Date();
            int secondsFromLastBuildCheck = (int) (((now.getTime() - lastBuildDate.getTime())
                                                / MILLISECONDS_IN_SECOND) % buildInterval);
            int secondsFromNow = buildInterval - secondsFromLastBuildCheck;
            nextBuild.roll(Calendar.SECOND,  secondsFromNow);
        }
        Date nextBuildDate = nextBuild.getTime();
        return XML_DATE_FORMAT.format(nextBuildDate);
    }

    /**
     * Get the build interval from the log file.
     * 
     * @return the build interval in Seconds
     */
    private int getBuildInterval(StatusHelper statusHelper) {
        int buildInterval = 0;
        try {
            BuildInfo lastBuild = statusHelper.getLastBuild();
            if (lastBuild != null) {
                LogFileReader reader = lastBuild.getLogFile().getReader();
                buildInterval = reader.getBuildInterval();
            }
        } catch (JDOMException e) {
            // TODO log this error - non-fatal, we just can't calculate the next build time
        } catch (IOException e) {
            // TODO log this error - non-fatal, we just can't calculate the next build time
        }
        return buildInterval;
    }
    
    /**
     * Get an init parameter, first trying servletConfig, then trying servletContext.
     * A ServletException exception is thrown if the paramter is not found.
     * @param servletconfig the config to use
     * @param name the name of the parameter to return
     * @return the value of the parameter
     * @throws ServletException exception thrown if parameter not found
     */
    private String getInitParameter(ServletConfig servletconfig, String name) throws ServletException {
        String value = servletconfig.getInitParameter(name);
        if (value == null) {
            value = servletconfig.getServletContext().getInitParameter(name);
        }
        getInitParameter("test");
        if (value == null) {
            throw new ServletException("Context parameter " + name + " needs to be set.");
        }
        return value;
    }

    /**
     * Get the time of the last build
     * @return the time of the last build
     */
    private String getFormattedDate(StatusHelper statusHelper) {
        Date date = statusHelper.getLastBuildTime();
        return XML_DATE_FORMAT.format(date);
    }

    /**
     * Get the current activity.
     */
    private String getActivity(String project, StatusHelper statusHelper) {
        String htmlFormattedActivity = statusHelper.getCurrentStatus(
                singleProject, logDirPath, project, statusFile);
        return getXmlActivity(htmlFormattedActivity);
    }


    /**
     * Get the current build status.
     * @return the build status
     */
    private String getFormattedStatus(StatusHelper statusHelper) {
        final String result = statusHelper.getLastBuildResult();
        return getXmlStatus(result);
    }

    /**
     * Get a list of project names
     * @return an array containing the full paths to the project directories
     * @throws ServletException
     */
    private String[] getProjectDirs() throws ServletException {
        if (this.logDir == null) {
            throw new ServletException("Log file directory not found");
        }
        if (!this.logDir.isDirectory()) {
            throw new ServletException("Could not access log directory");
        }

        final String[] projectDirs = logDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (new File(dir, name).isDirectory());
            }
        });

        Arrays.sort(projectDirs);
        return projectDirs;
    }

    /**
     * Get the base URL for this cruise control instance
     * @param request the request
     * @return the base URL
     */
    private String getBaseUrl(HttpServletRequest request) {
        String baseURL = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath()
                + "/buildresults/";
        return baseURL;
    }
    

    /**
     * Get the activity in the format required by cctray.
     * This will be one of
     * <ul>
     * <li>Sleeping</li>
     * <li>Building</li>
     * <li>CheckingModifications</li>
     * </ul>
     * @param project the project name
     * @return the build status
     * @param activity the activity from {@link ProjectState}
     * @return the activity
     */
    String getXmlActivity(String activity) {
        String xmlActivity = ACTIVITY_SLEEPING;
        if (activity.startsWith(ProjectState.BUILDING.getDescription())) {
            xmlActivity = ACTIVITY_BUILDING;
        } else if (activity.startsWith(ProjectState.BOOTSTRAPPING.getDescription())
                || activity.startsWith(ProjectState.MODIFICATIONSET.getDescription())
                || activity.startsWith(ProjectState.MERGING_LOGS.getDescription())
                || activity.startsWith(ProjectState.PUBLISHING.getDescription())) {
            xmlActivity = ACTIVITY_CHECKING_MODIFICATIONS;
        }
        return xmlActivity;
    }
    
    /**
     * Get the status required by cctray
     * This will be one of
     * <ul>
     * <li>Unknown</li>
     * <li>Success</li>
     * <li>Failure</li>
     * @param status the status from {@link StatusHelper}
     * @return the status
     */
    String getXmlStatus(final String status) {
        String xmlStatus = STATUS_UNKNOWN;
        if (StatusHelper.PASSED.equalsIgnoreCase(status)) {
            xmlStatus = STATUS_SUCCESS;
        }
        if (StatusHelper.FAILED.equalsIgnoreCase(status)) {
            xmlStatus = STATUS_FAILURE;
        }
        return xmlStatus;
    }
    
    public String getLogDirPath() {
        return logDirPath;
    }

    public String getSingleProject() {
        return singleProject;
    }

    public String getStatusFile() {
        return statusFile;
    }

}
