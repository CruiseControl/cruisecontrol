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

import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    private transient String logDir;
    private transient String logXmlEncoding;
    private transient File lastLogFile;
    private transient Element buildLog;
    private transient List loggers = new ArrayList();
    private final transient String projectName;

    public Log(String projectName) {
        if (projectName == null) {
            throw new NullPointerException("Cannot create a Log instance"
                    + " with a null Project name.");
        }
        this.projectName = projectName;
        reset();
    }

    /**
     * Adds a BuildLogger that will be called to manipulate the project log
     * just prior to writing the log.
     */
    public void addLogger(BuildLogger logger) {
        loggers.add(logger);
    }

    public BuildLogger[] getLoggers() {
        return (BuildLogger[]) loggers.toArray(new BuildLogger[0]);
    }

    public String getLogXmlEncoding() {
        return logXmlEncoding;
    }

    public void setLogDir(String logDir) throws CruiseControlException {
        this.logDir = logDir;
        checkLogDirectory();
    }

    public void setLogXmlEncoding(String logXmlEncoding) {
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
    public void checkLogDirectory() throws CruiseControlException {
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
    public void writeLogFile(Date now)
            throws CruiseControlException {

        //Call the Loggers to let them do their thing
        for (int i = 0; i < loggers.size(); i++) {
            BuildLogger nextLogger = (BuildLogger) loggers.get(i);
            //The buildloggers get the "real" build log, not a clone. Therefore,
            //  call getContent() wouldn't be appropriate here.
            nextLogger.log(buildLog);
        }

        //Figure out what the log filename will be.
        XMLLogHelper helper = new XMLLogHelper(buildLog);

        String logFilename = null;
        if (helper.isBuildSuccessful()) {
            logFilename = formatLogFileName(now, helper.getLabel());
        } else {
            logFilename = formatLogFileName(now);
        }

        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", File.separator + logFilename);
        buildLog.getChild("info").addContent(logFileElement);


        this.lastLogFile = new File(logDir, logFilename);
        LOG4J.debug("Project " + projectName + ":  Writing log file ["
                + lastLogFile.getAbsolutePath() + "]");

        //Write the log file out using the proper encoding.
        BufferedWriter logWriter = null;
        try {
            XMLOutputter outputter = null;
            if (logXmlEncoding == null) {
                outputter = new XMLOutputter("   ", true);
                logWriter =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lastLogFile)));
            } else {
                outputter = new XMLOutputter("   ", true, logXmlEncoding);
                logWriter =
                    new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(lastLogFile), logXmlEncoding));
            }
            outputter.output(new Document(buildLog), logWriter);
            logWriter = null;
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            logWriter = null;
        }
    }

    public static String formatLogFileName(Date date) {
        return formatLogFileName(date, null);
    }

    public static String formatLogFileName(Date date, String label) {
        StringBuffer logFileName = new StringBuffer();
        logFileName.append("log");
        logFileName.append(Project.getFormatedTime(date));
        if (label != null) {
            logFileName.append("L");
            logFileName.append(label);
        }
        logFileName.append(".xml");

        return logFileName.toString();
    }

    public void addContent(Element newContent) {
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
        return filename.matches("log\\d{14}L.*\\.xml");
    }

    public static Date parseDateFromLogFileName(String filename) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.parse(filename.substring(3, 17));
    }

    public static String parseLabelFromLogFileName(String filename) {
        if (!Log.wasSuccessfulBuild(filename)) {
            return "";
        }
        String beforeLabel = "log??????????????L";
        String afterLabel = ".xml";
        String label = filename.substring(beforeLabel.length());
        label = label.substring(0, label.length() - afterLabel.length());
        return label;
    }
}
