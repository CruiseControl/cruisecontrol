/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.taglib.CruiseControlLogFileFilter;

/**
 * TODO: TYpe comment.
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class BuildInfo implements Comparable {
    private static final String LOG_PREFIX = "log";
    private static final String LOG_SUFFIX = ".xml";
    private static final String LABEL_SEPARATOR = "L";
    private static final String LOG_DATE_PATTERN = "yyyyMMddHHmmSS";
    private static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat(LOG_DATE_PATTERN);
    private final Date buildDate;
    private final String label;
    private final String fileName;
    
    
    public BuildInfo(String infoText) throws ParseException {
        buildDate = deriveDate(infoText);
        label = deriveLabel(infoText);
        fileName = infoText;
        
    }
    
    /**
     * @param infoText
     * @return
     */
    private String deriveLabel(String infoText) {
        boolean buildSuccessful = (infoText.length() > 21);
        String theLabel;
        if (buildSuccessful) {
            int labelStartIndex = (LOG_PREFIX + LOG_DATE_PATTERN + LABEL_SEPARATOR).length();
            int labelEndIndex = infoText.length() - LOG_SUFFIX.length();
            theLabel = infoText.substring(labelStartIndex, labelEndIndex);
        } else {
            theLabel = null;
        }
        return theLabel;
    }

    /**
     * @param infoText
     * @return
     * @throws ParseException
     */
    private Date deriveDate(String infoText) throws ParseException {
        String dateStamp = infoText.substring(LOG_PREFIX.length(), LOG_PREFIX.length() + LOG_DATE_PATTERN.length());
        Date theDate;
        try {
            theDate = LOG_DATE_FORMAT.parse(dateStamp);
        } catch (ParseException e) {
            throw new ParseException("Invalid format: " + infoText + ". Format must be logyyyyMMddHHmmSS.xml or "
                                     + "logyyyyMMddHHmmSSLlabel.xml", e.getErrorOffset());
        }
        return theDate;
    }

    /**
     * @return Returns the buildDate.
     */
    public Date getBuildDate() {
        return buildDate;
    }
    /**
     * @return Returns the label.
     */
    public String getLabel() {
        return label;
    }
    
    public boolean isSuccessful() {
        return getLabel() != null;
    }

    /**
     * @return Returns the fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param file
     * @return
     */
    public static Summary loadFromDir(File logDir) throws CruiseControlException {
        String [] logFileNames = logDir.list(new CruiseControlLogFileFilter());
        if (logFileNames == null) {
            throw new CruiseControlException("Could not access the directory " + logDir.getAbsolutePath());
        } else if (logFileNames.length == 0) {
            throw new CruiseControlException("Configuration problem? No logs found in logDir: "
                                             + logDir.getAbsolutePath());
        }
        List buildInfoList = new ArrayList(logFileNames.length);
        for (int i = 0; i < logFileNames.length; i++) {
            String logFileName = logFileNames[i];
            try {
                buildInfoList.add(new BuildInfo(logFileName));
            } catch (ParseException e) {
                throw new RuntimeException("Could not parse log file name " + logFileName
                                           + ". Is the filter broken?", e);
            }
        }
        Collections.sort(buildInfoList);
        return new Summary(buildInfoList);
    }

    /**
     * Return a comparision based on the build time.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        BuildInfo other = (BuildInfo) arg0;
        return this.buildDate.compareTo(other.buildDate);
    }
    
    public static final class Summary {
        private final List buildInfoList;
        private final int numBrokenBuilds;
        private final int numSuccessfulBuilds;

        private Summary(List buildInfoList) {
            this.buildInfoList = Collections.unmodifiableList(buildInfoList);
            int brokenBuildsCounter = 0;
            int successfulBuildsCounter = 0;
            for (Iterator i = buildInfoList.iterator(); i.hasNext();) {
                BuildInfo buildInfo = (BuildInfo) i.next();
                if (buildInfo.isSuccessful()) {
                    successfulBuildsCounter++;
                } else {
                    brokenBuildsCounter++;
                }
            }
            numBrokenBuilds = brokenBuildsCounter;
            numSuccessfulBuilds = successfulBuildsCounter;
        }
        
        
        
        /**
         * @return Returns the buildInfoList.
         */
        public List getBuildInfoList() {
            return buildInfoList;
        }
        /**
         * @return Returns the numBrokenBuilds.
         */
        public int getNumBrokenBuilds() {
            return numBrokenBuilds;
        }
        /**
         * @return Returns the numSuccessfulBuilds.
         */
        public int getNumSuccessfulBuilds() {
            return numSuccessfulBuilds;
        }
        
        /**
         * @return
         */
        public Iterator iterator() {
            return buildInfoList.iterator();
        }
        
        /**
         * @return
         */
        public int size() {
            return buildInfoList.size();
        }



        /**
         * @return
         */
        public BuildInfo[] asArray() {
            return (BuildInfo[]) buildInfoList.toArray(new BuildInfo[buildInfoList.size()]);
        }
    }
}
