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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.IO;
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
    private transient List loggers = new ArrayList();
    private transient List manipulators = new ArrayList();
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
     * @throws IllegalArgumentException
     *             is projectName is null
     */
    void setProjectName(String projectName) {
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
     * @throws IllegalStateException
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

        for (Iterator i = loggers.iterator(); i.hasNext();) {
            BuildLogger logger = (BuildLogger) i.next();
            logger.validate();
        }

        for (Iterator i = manipulators.iterator(); i.hasNext();) {
            Manipulator manipulator = (Manipulator) i.next();
            manipulator.validate();
        }
    }

    /**
     * Adds a BuildLogger that will be called to manipulate the project log just prior to writing the log.
     */
    public void add(BuildLogger logger) {
        loggers.add(logger);
    }

    /**
     * Adds a Manipulator that will handle old log-files
     */
    public void add(Manipulator manipulator) {
        manipulators.add(manipulator);
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
     * creates log directory if it doesn't already exist
     * 
     * @throws CruiseControlException
     *             if directory can't be created or there is a file of the same name
     */
    private void checkLogDirectory(String logDir) throws CruiseControlException {
        File logDirectory = new File(logDir);
        if (!logDirectory.exists()) {
            LOG.info("log directory specified in config file does not exist; creating: "
                    + logDirectory.getAbsolutePath());
            if (!logDirectory.mkdirs()) {
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
     */
    public void writeLogFile(Date now) throws CruiseControlException {

        // Call the Loggers to let them do their thing
        for (int i = 0; i < loggers.size(); i++) {
            BuildLogger nextLogger = (BuildLogger) loggers.get(i);
            // The buildloggers get the "real" build log, not a clone. Therefore,
            // call getContent() wouldn't be appropriate here.
            nextLogger.log(buildLog);
        }

        String logFilename = decideLogfileName(now);

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

        File logfile = new File(logDir, logFilename);
        LOG.debug("Project " + projectName + ":  Writing log file [" + logfile.getAbsolutePath() + "]");
        writeLogFile(logfile, buildLog);

        callManipulators();
    }

    protected void writeLogFile(File file, Element element) throws CruiseControlException {
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

    private String decideLogfileName(Date now) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(buildLog);
        if (helper.isBuildSuccessful()) {
            return formatLogFileName(now, helper.getLabel());
        }
            
        return formatLogFileName(now);
    }

    /**
     * Calls all Manipulators to already existing logfiles.
     */
    protected void callManipulators() {
        for (Iterator i = manipulators.iterator(); i.hasNext();) {
            Manipulator manipulator = (Manipulator) i.next();
            manipulator.execute(getLogDir());
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

    public void updateLabel(String newLabel) throws CruiseControlException {
        Element infoElement = buildLog.getChild("info");
        List infoPropertyElements = infoElement.getChildren();
        Iterator iterator = infoPropertyElements.iterator();
        while (iterator.hasNext()) {
            Element infoPropertyElement = (Element) iterator.next();
            String infoPropertyName = infoPropertyElement.getAttributeValue("name");
            if (infoPropertyName.equals("label")) {
                infoPropertyElement.setAttribute("value", newLabel);
                return;
            }
        }
        throw new CruiseControlException("Could not find label property in log file when attempting to update label");
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
