/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Handles the Log element, and subelements, of the CruiseControl configuration
 * file. Also represents the Build Log used by the CruiseControl build process.
 */
public class Log {
    private static final org.apache.log4j.Logger LOG4J =
            org.apache.log4j.Logger.getLogger(Log.class);

    private static final int BEFORE_LENGTH = "logYYYYMMDDhhmmssL".length();
    private static final int AFTER_LENGTH  = ".xml".length();
    
    private transient String logDir;
    private transient String logXmlEncoding;
    private transient File lastLogFile;
    private transient Element buildLog;
    private transient List loggers = new ArrayList();
    private transient String projectName;

    /**
     * @param projectName
     * @throws NullPointerException is projectName is null
     */
    public Log(String projectName) {
        this();
        setProjectName(projectName);
    }

    /**
     * Log instances created that way must have their {@link #setProjectName(String) projectName set}.
     */
    public Log() {
        reset();
    }

    /**
     * Althought this property is required, it is implicitly defined by the project and doesn't map to
     * a config file attribute.
     * @param projectName
     * @throws NullPointerException is projectName is null
     */
    void setProjectName(String projectName) {
        if (projectName == null) {
            throw new NullPointerException("null projectName.");
        }
        this.projectName = projectName;
    }

    /**
     * Validate the log. Also creates the log directory if it doesn't exist.
     * @throws IllegalStateException if projectName wasn't set
     */
    public void validate() throws CruiseControlException {
        if (projectName == null)  {
            // not a real validation. Not using ValidationHelper
            throw new IllegalStateException("projectName unset.");
        }
        if (logDir != null) {
            checkLogDirectory(logDir);
        }
    }

    /**
     * Adds a BuildLogger that will be called to manipulate the project log
     * just prior to writing the log.
     */
    public void addLogger(BuildLogger logger) {
        loggers.add(logger);
    }

    public BuildLogger[] getLoggers() {
        return (BuildLogger[]) loggers.toArray(new BuildLogger[loggers.size()]);
    }

    public String getLogXmlEncoding() {
        return logXmlEncoding;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * @param logDir
     * @throws CruiseControlException
     * @deprecated use {@link #setDir(String)}
     */
    public void setLogDir(String logDir) throws CruiseControlException {
        setDir(logDir);
    }

    public void setDir(String logDir) throws CruiseControlException {
        this.logDir = logDir;
    }

    /**
     * @param logXmlEncoding
     * @deprecated use {@link #setEncoding(String)}
     */
    public void setLogXmlEncoding(String logXmlEncoding) {
        setEncoding(logXmlEncoding);
    }

    public void setEncoding(String logXmlEncoding) {
        this.logXmlEncoding = logXmlEncoding;
    }

    public String getLogDir() {
        return logDir;
    }

    /**
     * @return The last log file that was written; null if none written yet.
     */
    public File getLastLogFile() {
        return this.lastLogFile;
    }

    /**
     * creates log directory if it doesn't already exist
     * @throws CruiseControlException if directory can't be created or there is
     * a file of the same name
     */
    private void checkLogDirectory(String logDir) throws CruiseControlException {
        File logDirectory = new File(logDir);
        if (!logDirectory.exists()) {
            LOG4J.info(
                "log directory specified in config file does not exist; creating: "
                    + logDirectory.getAbsolutePath());
            if (!logDirectory.mkdirs()) {
                throw new CruiseControlException(
                    "Can't create log directory specified in config file: "
                        + logDirectory.getAbsolutePath());
            }
        } else if (!logDirectory.isDirectory()) {
            throw new CruiseControlException(
                "Log directory specified in config file is not a directory: "
                    + logDirectory.getAbsolutePath());
        }
    }

    /**
     * Writes the current build log to the appropriate directory and filename.
     */
    public void writeLogFile(Date now) throws CruiseControlException {

        //Call the Loggers to let them do their thing
        for (int i = 0; i < loggers.size(); i++) {
            BuildLogger nextLogger = (BuildLogger) loggers.get(i);
            //The buildloggers get the "real" build log, not a clone. Therefore,
            //  call getContent() wouldn't be appropriate here.
            nextLogger.log(buildLog);
        }

        //Figure out what the log filename will be.
        XMLLogHelper helper = new XMLLogHelper(buildLog);

        String logFilename;
        if (helper.isBuildSuccessful()) {
            logFilename = formatLogFileName(now, helper.getLabel());
        } else {
            logFilename = formatLogFileName(now);
        }

        this.lastLogFile = new File(logDir, logFilename);
        if (LOG4J.isDebugEnabled()) {
            LOG4J.debug("Project " + projectName + ":  Writing log file ["
                + lastLogFile.getAbsolutePath() + "]");
        }

        // Add the logDir as an info element
        Element logDirElement = new Element("property");
        logDirElement.setAttribute("name", "logdir");
        logDirElement.setAttribute("value", new File(logDir).getAbsolutePath());
        buildLog.getChild("info").addContent(logDirElement);

        // Add the logFile as an info element
        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", logFilename);
        buildLog.getChild("info").addContent(logFileElement);

        //Write the log file out, let jdom care about the encoding by using
        //an OutputStream instead of a Writer.
        OutputStream logStream = null;
        try {
            Format format = Format.getPrettyFormat();
            if (logXmlEncoding != null) {
                format.setEncoding(logXmlEncoding);
            }
            XMLOutputter outputter = new XMLOutputter(format);
            logStream = new BufferedOutputStream(new FileOutputStream(lastLogFile));
            outputter.output(new Document(buildLog), logStream);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            if (logStream != null) {
                try {
                    logStream.close();
                } catch (IOException e) {
                    // nevermind, then
                }
            }
        }
    }

    public static String formatLogFileName(Date date) {
        return formatLogFileName(date, null);
    }

    public static String formatLogFileName(Date date, String label) {
        StringBuffer logFileName = new StringBuffer();
        logFileName.append("log");
        logFileName.append(DateUtil.getFormattedTime(date));
        if (label != null) {
            logFileName.append("L");
            logFileName.append(label);
        }
        logFileName.append(".xml");

        return logFileName.toString();
    }

    public void addContent(Content newContent) {
        buildLog.addContent(newContent);
    }

    public Element getContent() {
        return (Element) buildLog.clone();
    }

    public boolean wasBuildSuccessful() {
        return new XMLLogHelper(buildLog).isBuildSuccessful();
    }

    /**
     * Resets the build log. After calling this method a fresh build log
     * will exist, ready for adding new content.
     */
    public void reset() {
        this.buildLog = new Element("cruisecontrol");
    }

    public static boolean wasSuccessfulBuild(String filename) {
        if (filename == null) {
            return false;
        }
        boolean startsWithLog = filename.startsWith("log");
        boolean hasLabelSeparator = filename.indexOf('L') == BEFORE_LENGTH - 1;
        boolean isXmlFile = filename.endsWith(".xml");
        return startsWithLog && hasLabelSeparator && isXmlFile;
    }

    public static Date parseDateFromLogFileName(String filename) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.parse(filename.substring(3, BEFORE_LENGTH - 1));
    }

    public static String parseLabelFromLogFileName(String filename) {
        if (!Log.wasSuccessfulBuild(filename)) {
            return "";
        }
        return filename.substring(BEFORE_LENGTH, filename.length() - AFTER_LENGTH);
    }
}

