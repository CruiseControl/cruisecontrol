/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Handles the Log element, and subelements, of the CruiseControl configuration file. Also represents the Build Log used
 * by the CruiseControl build process.
 */
public class Log implements Serializable {
    private static final long serialVersionUID = -5727569770074024691L;

    private static final Logger LOG = Logger.getLogger(Log.class);

    public static final int BEFORE_LENGTH = "logYYYYMMDDhhmmssL".length();
    private static final int AFTER_LENGTH = ".xml".length();

    private transient String logDir;
    private transient String logXmlEncoding;
    private transient Element buildLog;
    private final transient List<BuildLogger> loggers = new ArrayList<BuildLogger>();
    private final transient List<Manipulator> manipulators = new ArrayList<Manipulator>();
    private transient String projectName;

    /**
     * Log instances created this way must have their projectName set.
     */
    public Log() {
        reset();
    }

    /**
     * Although this property is required, it is implicitly defined by the project and doesn't map to a config file
     * attribute.
     *
     * @param projectName project name
     * @throws IllegalArgumentException
     *             is projectName is null
     */
    void setProjectName(final String projectName) {
        if (projectName == null) {
            throw new IllegalArgumentException("projectName can't be null");
        }
        this.projectName = projectName;
        if (logDir == null) {
            logDir = "logs" + File.separatorChar + projectName;
        }
    }

    /**
     * Validate the log. Also creates the log directory if it doesn't exist.
     * 
     * @throws CruiseControlException
     *             if projectName wasn't set
     */
    public void validate() throws CruiseControlException {
        if (projectName == null) {
            // Not using ValidationHelper because projectName should be set
            // implictly by the project, not as an attribute.
            throw new CruiseControlException("projectName must be set");
        }
        if (logDir != null) {
            checkLogDirectory(logDir);
        }

        for (final BuildLogger logger : loggers) {
            logger.validate();
        }

        for (final Manipulator manipulator : manipulators) {
            manipulator.validate();
        }
    }

    /**
     * Adds a BuildLogger that will be called to manipulate the project log just prior to writing the log.
     * @param logger a BuildLogger that will be called to manipulate the project log just prior to writing the log
     */
    public void add(final BuildLogger logger) {
        loggers.add(logger);
    }

    /**
     * Adds a Manipulator that will handle old log-files
     * @param manipulator a Manipulator that will handle old log-files
     */
    public void add(final Manipulator manipulator) {
        manipulators.add(manipulator);
    }

    public BuildLogger[] getLoggers() {
        return loggers.toArray(new BuildLogger[loggers.size()]);
    }

    public String getLogXmlEncoding() {
        return logXmlEncoding;
    }

    public String getProjectName() {
        return projectName;
    }

    // @todo Remove throws CruiseControlException?
    public void setDir(final String logDir) throws CruiseControlException {
        this.logDir = logDir;
    }

    public void setEncoding(final String logXmlEncoding) {
        this.logXmlEncoding = logXmlEncoding;
    }

    public String getLogDir() {
        return logDir;
    }

    /**
     * creates log directory if it doesn't already exist
     * @param logDir log directory to create if it doesn't already exist
     * @throws CruiseControlException
     *             if directory can't be created or there is a file of the same name
     */
    private void checkLogDirectory(final String logDir) throws CruiseControlException {
        final File logDirectory = new File(logDir);
        if (!logDirectory.exists()) {
            LOG.info("log directory specified in config file does not exist; creating: "
                    + logDirectory.getAbsolutePath());
            if (!Util.doMkDirs(logDirectory)) {
                throw new CruiseControlException("Can't create log directory specified in config file: "
                        + logDirectory.getAbsolutePath());
            }
        } else if (!logDirectory.isDirectory()) {
            throw new CruiseControlException("Log directory specified in config file is not a directory: "
                    + logDirectory.getAbsolutePath());
        }
    }

    /**
     * Writes the current build log to the appropriate directory and filename.
     * @param now current build date
     * @throws CruiseControlException if nextLogger.log throws CruiseControlException
     */
    public void writeLogFile(final Date now) throws CruiseControlException {

        // Call the Loggers to let them do their thing
        for (final BuildLogger nextLogger : loggers) {
            // The buildloggers get the "real" build log, not a clone. Therefore,
            // call getContent() wouldn't be appropriate here.
            nextLogger.log(buildLog);
        }

        final String logFilename = decideLogfileName(now);

        // Add the logDir as an info element
        final Element logDirElement = new Element("property");
        logDirElement.setAttribute("name", "logdir");
        logDirElement.setAttribute("value", new File(logDir).getAbsolutePath());
        buildLog.getChild("info").addContent(logDirElement);

        // Add the logFile as an info element
        final Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", logFilename);
        buildLog.getChild("info").addContent(logFileElement);

        final File logfile = new File(logDir, logFilename);
        LOG.debug("Project " + projectName + ":  Writing log file [" + logfile.getAbsolutePath() + "]");
        writeLogFile(logfile, buildLog);

        callManipulators();
    }

    protected void writeLogFile(final File file, final Element element) throws CruiseControlException {
        // Write the log file out, let jdom care about the encoding by using
        // an OutputStream instead of a Writer.
        OutputStream logStream = null;
        try {
            Format format = Format.getPrettyFormat();
            if (logXmlEncoding != null) {
                format.setEncoding(logXmlEncoding);
            }
            XMLOutputter outputter = new XMLOutputter(format);
            logStream = new BufferedOutputStream(new FileOutputStream(file));
            outputter.output(new Document(element), logStream);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            IO.close(logStream);
        }
    }

    private String decideLogfileName(final Date now) throws CruiseControlException {
        final XMLLogHelper helper = new XMLLogHelper(buildLog);
        if (helper.isBuildSuccessful()) {
            return formatLogFileName(now, helper.getLabel());
        }
            
        return formatLogFileName(now);
    }

    /**
     * Calls all Manipulators to already existing logfiles.
     */
    protected void callManipulators() {
        for (final Manipulator manipulator : manipulators) {
            manipulator.execute(getLogDir());
        }
    }

    public static String formatLogFileName(final Date date) {
        return formatLogFileName(date, null);
    }

    public static String formatLogFileName(final Date date, final String label) {
        final StringBuffer logFileName = new StringBuffer();
        logFileName.append("log");
        logFileName.append(DateUtil.getFormattedTime(date));
        if (label != null) {
            logFileName.append("L");
            logFileName.append(label);
        }
        logFileName.append(".xml");

        return logFileName.toString();
    }

    public void addContent(final Content newContent) {
        buildLog.addContent(newContent);
    }

    public Element getContent() {
        return (Element) buildLog.clone();
    }

    public boolean wasBuildSuccessful() {
        return new XMLLogHelper(buildLog).isBuildSuccessful();
    }

    /**
     * Resets the build log. After calling this method a fresh build log will exist, ready for adding new content.
     */
    public void reset() {
        this.buildLog = new Element("cruisecontrol");
    }

    public static boolean wasSuccessfulBuild(final String filename) {
        if (filename == null) {
            return false;
        }
        final boolean startsWithLog = filename.startsWith("log");
        final boolean hasLabelSeparator = filename.indexOf('L') == BEFORE_LENGTH - 1;
        final boolean isXmlFile = filename.endsWith(".xml");
        return startsWithLog && hasLabelSeparator && isXmlFile;
    }

    public static Date parseDateFromLogFileName(final String filename) throws CruiseControlException {
        return DateUtil.parseFormattedTime(filename.substring(3, BEFORE_LENGTH - 1), "date from logfile name");
    }

    public static String parseLabelFromLogFileName(final String filename) {
        if (!Log.wasSuccessfulBuild(filename)) {
            return "";
        }
        return filename.substring(BEFORE_LENGTH, filename.length() - AFTER_LENGTH);
    }

}
