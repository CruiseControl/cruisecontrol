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

package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Log;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * This class allows for starting builds based on the results of another
 * build.  It does this by examining the build's log files.  Only
 * successful builds count as modifications.
 *
 * @author Garrick Olson
 */
@DescriptionFile
public class BuildStatus implements SourceControl {

    private static final Logger LOG = Logger.getLogger(BuildStatus.class);

    /** The location being checked for new log files. */
    public static final String MOST_RECENT_LOGDIR_KEY = "most.recent.logdir";

    /** The name of the newest logfile included in the modification set
     *  (e.g. "log20040120120000L0.1.xml"). */
    public static final String MOST_RECENT_LOGFILE_KEY = "most.recent.logfile";

    /** The timestamp of the newest build included in the modification set
     *  (e.g. "20040120120000"). */
    public static final String MOST_RECENT_LOGTIME_KEY = "most.recent.logtime";

    /**
     * The label of the newest build included in the modification set
     *  (e.g. "0.1").
     */
    public static final String MOST_RECENT_LOGLABEL_KEY = "most.recent.loglabel";

    private final SourceControlProperties properties = new SourceControlProperties();
    private String logDir;

    private boolean vetoIfFailing = false;

    /**
     * This method is used to make certain attributes of the most
     * recent modification available to Ant tasks.
     * @return A Hashtable object containing no properties if there
     *         were no modifications, four properties if there were one
     *         or more modifications (keys are provided as constants on this
     *         class), or five is the property attribute was set.
     *         Never returns null.
     */
    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }
    
    @Description("Will set this property if a modification has occurred. For use in "
            + "conditionally controlling the build later.")
    @Optional
    public void setProperty(String propertyName) {
        properties.assignPropertyName(propertyName);
    }

    /**
     * Indicate where the build to be monitored places its output (log files).
     *
     * @param logDir Absolute path to the log directory.
     */
    @Description("Path to CruiseControl log directory for the project to monitor.")
    @Required
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    /**
     * Make sure any attributes provided by the user are correctly specified.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(logDir, "logdir", this.getClass());

        File logDirectory = new File(logDir);
        ValidationHelper.assertTrue(logDirectory.exists(),
            "Log directory does not exist: " + logDirectory.getAbsolutePath());
        ValidationHelper.assertTrue(logDirectory.isDirectory(),
            "Log directory is not a directory: " + logDirectory.getAbsolutePath());
    }

    /**
     * The modifications reported by this method will be the list of new
     * log files created as the result of successful builds (the log files
     * will include the build label).
     *
     * @param lastBuild Look for successful builds newer than this date
     *        (may not be null).
     * @param unused The timestamp of the current build is passed here
     *        (as per SourceControl interface) but we don't use it.
     */
    public List<Modification> getModifications(final Date lastBuild, final Date unused) {
        properties.put(MOST_RECENT_LOGDIR_KEY, logDir);
        final List<Modification> modifications = new ArrayList<Modification>();
        final File logDirectory = new File(logDir);
        final String filename = Log.formatLogFileName(lastBuild);

        if (!logDirectory.exists()) {
            LOG.error("log directory doesn't exist: " + logDir);
            return modifications;
        }

        if (!logDirectory.isDirectory()) {
            LOG.error("path for log directory exists but isn't a directory: " + logDir);
            return modifications;
        }
        
        if (vetoIfFailing) {
            vetoIfFailing(logDirectory);
        }

        try {
            File[] newLogs = logDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.compareTo(filename) > 0 && Log.wasSuccessfulBuild(name);
                }
            });

            Modification mostRecent = null;

            for (final File newLog : newLogs) {
                final Modification modification = new Modification("buildstatus");
                final String name = newLog.getName();

                modification.modifiedTime = Log.parseDateFromLogFileName(name);
                modification.userName = "cc-" + getProjectFromLog(newLog);
                modification.comment = logDir.substring(logDir.lastIndexOf('/') + 1);
                modification.revision = Log.parseLabelFromLogFileName(name);

                final Modification.ModifiedFile modfile = modification.createModifiedFile(name, null);
                modfile.revision = modification.revision;
                modfile.action = "add";

                if (mostRecent == null || modification.modifiedTime.after(mostRecent.modifiedTime)) {
                    mostRecent = modification;
                }

                modifications.add(modification);
            }

            // This makes information about the most recent modification
            // available to Ant tasks
            if (mostRecent != null) {
                properties.put(MOST_RECENT_LOGFILE_KEY, mostRecent.files.get(0).fileName);
                properties.put(MOST_RECENT_LOGTIME_KEY, DateUtil.getFormattedTime(mostRecent.modifiedTime));
                properties.put(MOST_RECENT_LOGLABEL_KEY, mostRecent.revision);
            }

        } catch (Exception e) {
            LOG.error("Error checking for modifications", e);
        }
        
        if (!modifications.isEmpty()) {
            properties.modificationFound();
        }
        
        return modifications;
    }

    private void vetoIfFailing(File logDirectory) {
        NewestLogfileFilter filter = new NewestLogfileFilter();
        logDirectory.listFiles(filter);
        String mostRecentLogfileName = filter.mostRecent;
        if (mostRecentLogfileName != null && !Log.wasSuccessfulBuild(mostRecentLogfileName)) {
            throw new VetoException("most recent build failed: " + mostRecentLogfileName);
        }
    }
    
    private static class VetoException extends RuntimeException {
        public VetoException(String message) {
            super(message);
        }
    }

    private static class NewestLogfileFilter implements FilenameFilter {
        private String mostRecent;

        public boolean accept(File dir, String name) {
            boolean accept = name.startsWith("log") 
                             && name.endsWith(".xml") 
                             && name.length() >= minimumFilenameLength();
            if (accept) {
                cacheNewestFilename(name);
            }
            return accept;
        }

        private void cacheNewestFilename(String name) {
            try {
                Date logDate = Log.parseDateFromLogFileName(name);
                if (mostRecent == null || logDate.after(Log.parseDateFromLogFileName(mostRecent))) {
                    mostRecent = name;
                }
            } catch (CruiseControlException e) {
                LOG.warn("exception getting date from filename: " + name, e);
            }
        }

        private int minimumFilenameLength() {
            return "log".length() + DateUtil.SIMPLE_DATE_FORMAT.length() + ".xml".length();
        }
    }
    
    private String getProjectFromLog(File f) {
        LOG.info("Getting project from file: " + f.getName());
        try {
            Document doc = readDocFromFile(f);
            LOG.info("Loaded xml document for BuildStatus");
            Element root = doc.getRootElement();
            XMLLogHelper log = new XMLLogHelper(root);
        return log.getProjectName();
        } catch (JDOMException ex) {
            LOG.info("Failed to load BuildStatus xml document" + ex);
        } catch (IOException ex) {
            LOG.info("Failed to load BuildStatus xml document" + ex);
        } catch (CruiseControlException ex) {
            LOG.info("Could load BuildStatus xml log document, but generated exception anyway" + ex);
        }
        return "Unknown";
    }

    private Document readDocFromFile(File f) throws JDOMException, IOException {
        SAXBuilder sxb = new SAXBuilder();
        return sxb.build(f);
    }

    @Description("Will abort the build attempt if the last build of the monitored "
            + "project is failing. Defaults to false.")
    @Optional
    @Default("false")
    public void setVetoIfFailing(boolean b) {
        vetoIfFailing = b;
    }
}

