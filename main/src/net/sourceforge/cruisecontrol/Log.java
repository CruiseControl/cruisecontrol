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
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Handles the Log element, and subelements, of the CruiseControl configuration
 * file.
 */
public class Log {
    private static final Logger LOG = Logger.getLogger(Project.class);

    private transient String logDir;
    private transient String logXmlEncoding;
    private transient File lastLogFile;
    private transient List otherLogFilenames = new ArrayList();
    private final transient String projectName;

    public Log(String projectName) {
        if (projectName == null) {
            throw new NullPointerException("Cannot create a Log instance"
                    + " with a null Project name.");
        }
        this.projectName = projectName;
    }

    /**
     * These are other log files that should be incorporated into the
     * main CruiseControl build file.
     */
    public void addOtherLog(String filename) {
        otherLogFilenames.add(filename);
    }

    public void addOtherLogs(List auxLogs) {
        for (int i = 0; i < auxLogs.size(); i++) {
            String nextFilename = (String) auxLogs.get(i);
            addOtherLog(nextFilename);
        }
    }

    public String[] getOtherLogFilenames() {
        return (String[]) otherLogFilenames.toArray(new String[0]);
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
     * @throws CruiseControlException if directory can't be created or there is a file of the same name
     */
    public void checkLogDirectory() throws CruiseControlException {
        File logDirectory = new File(logDir);
        if (!logDirectory.exists()) {
            LOG.info(
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
     * Creates a File instance appropriate for a build log based on
     * this Log instance.
     */
    public void writeLogFile(Element buildLog, Date now)
            throws CruiseControlException {

        //Merge the other logs into the build log.
        mergeOtherLogContents(buildLog);

        //Figure out what the log filename will be.
        XMLLogHelper helper = new XMLLogHelper(buildLog);

        String logFilename = null;
        if (helper.isBuildSuccessful()) {
            logFilename = "log" + Project.getFormatedTime(now) + "L" + helper.getLabel() + ".xml";
        } else {
            logFilename = "log" + Project.getFormatedTime(now) + ".xml";
        }

        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", File.separator + logFilename);
        buildLog.getChild("info").addContent(logFileElement);


        this.lastLogFile = new File(logDir, logFilename);
        LOG.debug("Project " + projectName + ":  Writing log file ["
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

    /**
     * Merges all of the other auxilliary log
     * files.  If any of the other logs is a directory, all the XML files
     * in that directory will be merged.
     */
    private void mergeOtherLogContents(Element buildLog) {
        for (int i = 0; i < otherLogFilenames.size(); i++) {
            String nextLogFilename = (String) otherLogFilenames.get(i);

            File auxLogFile = new File(nextLogFilename);
            if (auxLogFile.isDirectory()) {
                String[] childFileNames = auxLogFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(".xml");
                    }
                });
                for (int j = 0; j < childFileNames.length; j++) {
                    String nextChildFilename = childFileNames[j];

                    Element auxLogElement =
                            getElementFromAuxLogFile(nextLogFilename + File.separator + nextChildFilename);
                    if (auxLogElement != null) {
                        buildLog.addContent(auxLogElement.detach());
                    }
                }
            } else {
                Element auxLogElement = getElementFromAuxLogFile(nextLogFilename);
                if (auxLogElement != null) {
                    buildLog.addContent(auxLogElement.detach());
                }
            }
        }
    }


    /**
     *  Get a JDOM <code>Element</code> from an XML file.
     *
     *  @param fileName The file name to read.
     *  @return JDOM <code>Element</code> representing that xml file.
     */
    private Element getElementFromAuxLogFile(String fileName) {
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element element = builder.build(fileName).getRootElement();
            if (element.getName().equals("testsuite")) {
                if (element.getChild("properties") != null) {
                    element.getChild("properties").detach();
                }
            }
            return element;
        } catch (JDOMException e) {
            LOG.warn("Could not read aux log: " + fileName + ".  Skipping...", e);
        }

        return null;
    }
}
