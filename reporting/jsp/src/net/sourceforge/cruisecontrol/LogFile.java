/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.taglib.CruiseControlLogFileFilter;
import net.sourceforge.cruisecontrol.taglib.CruiseControlSuccessfulLogFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.GZIPInputStream;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * Represents a XML log file.
 * Build information that is based on the log name is in the <code>BuildInfo</code>
 * class
 *
 * @see BuildInfo
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class LogFile implements Serializable {
    // NOTE: LogFile must be Serializable for Metrics tab (charts) to work

    public static final String LOG_SUFFIX = ".xml";
    public static final String LOG_COMPRESSED_SUFFIX = LOG_SUFFIX + ".gz";
    private static final FilenameFilter LOG_FILTER = new CruiseControlLogFileFilter();
    private static final FilenameFilter SUCCESSFUL_FILTER = new CruiseControlSuccessfulLogFileFilter();

    private File xmlFile;

    /**
     * Creates a new instance of LogFile
     * @param logDir directory with the XML log file
     * @param logName name of the XML log file
     */
    public LogFile(File logDir, String logName) {
        this.xmlFile = new File(logDir, logName + LOG_SUFFIX);
        if (!xmlFile.exists()) {
            xmlFile = new File(logDir, logName + LOG_COMPRESSED_SUFFIX);
        }
    }

    /**
     * Creates a new instance of LogFile
     * @param xmlFile the XML log file
     */
    public LogFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
     *  Gets the latest log file in a given directory.  Since all of our logs contain a date/time string, this method
     *  is actually getting the log file that comes last alphabetically.
     *
     *  @return The latest log file or <code>null</code> if there are no log
     *          files in the given directory.
     */
    public static LogFile getLatestLogFile(File logDir) {
        File[] logs = logDir.listFiles(LOG_FILTER);
        if (logs != null && logs.length > 0) {
            return new LogFile((File) Collections.max(Arrays.asList(logs)));
        } else {
            return null;
        }
    }

    /**
     *  Gets the latest successful log file in a given directory.
     *  Since all of our logs contain a date/time string, this method
     *  is actually getting the log file that comes last alphabetically.
     *
     *  @return The latest log file or <code>null</code> if there are no
     *          successful log files in the given directory
     */
    public static LogFile getLatestSuccessfulLogFile(File logDir) {
        File[] logs = logDir.listFiles(SUCCESSFUL_FILTER);
        if (logs != null && logs.length > 0) {
            return new LogFile((File) Collections.max(Arrays.asList(logs)));
        } else {
            return null;
        }
    }

    /**
     * Gets the build information for this log file like the label and build
     * date.
     * @return the log file's build information
     */
    public BuildInfo getBuildInfo() throws ParseException {
        return new BuildInfo(this);
    }

    /**
     * Gets the file object.
     * @return the log file
     */
    public File getFile() {
        return xmlFile;
    }

    /**
     * Whether the log file is compressed or not.
     * @return <code>true</code> if the file is compressed
     */
    public boolean isCompressed() {
        return getFile().getName().endsWith(LOG_COMPRESSED_SUFFIX);
    }

    /**
     * Gets the log's name.
     * This is the file name without a file extension like <code>.xml<code> or
     * <code>.xml.gz</code>. Use <code>getFile().getName()</code> to get the
     * file name with extension.
     *
     * @return the name of the log
     */
    public String getName() {
        return extractLogNameFromFileName(getFile().getName());
    }
    private String extractLogNameFromFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf(LOG_SUFFIX));
    }

    /**
     * Gets the log file's directory.
     * @return the parent directory of the log file
     */
    public File getLogDirectory() {
        return getFile().getParentFile();
    }

    /**
     * Gets a stream with the log file's content.
     *
     * @throws java.io.IOException if there is an error reading the file
     * @return the file content as a stream
     */
    public InputStream getInputStream() throws IOException {
        InputStream in = new FileInputStream(xmlFile);
        if (isCompressed()) {
            in = new GZIPInputStream(in);
        }
        return in;
    }

    public Document asDocument() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        InputStream in = getInputStream();
        Document log = null;
        try {
            log = builder.build(in, getFile().getAbsolutePath());
        } finally {
            in.close();
        }
        return log;
    }
}
