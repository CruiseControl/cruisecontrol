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

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashMap;

import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import org.apache.oro.text.MalformedCachePatternException;
import org.jdom2.Element;

/**
 * Set of modifications collected from included SourceControls
 *
 * @see SourceControl
 */
public class ModificationSet implements Serializable {

    private static final long serialVersionUID = 7834545928469764690L;

    private static final Logger LOG = Logger.getLogger(ModificationSet.class);
    private static final int ONE_SECOND = 1000;

    private List<Modification> modifications = new ArrayList<Modification>();
    private final List<SourceControl> sourceControls = new ArrayList<SourceControl>();
    private int quietPeriod = 60 * ONE_SECOND;
    private Date timeOfCheck;

    /**
     * File-Patterns (as org.apache.oro.io.GlobFilenameFilter) to be ignored
     */
    private List<GlobFilenameFilter> ignoreFiles;

    static final String MSG_PROGRESS_PREFIX_QUIETPERIOD_MODIFICATION_SLEEP = "quiet period modification, sleep ";

    /**
     * Set the amount of time in which there is no source control activity after which it is assumed that it is safe to
     * update from the source control system and initiate a build.
     * @param seconds quite period in seconds
     */
    public void setQuietPeriod(int seconds) {
        quietPeriod = seconds * ONE_SECOND;
    }

    /**
     * Set the list of Glob-File-Patterns to be ignored
     *
     * @param filePatterns
     *            a comma separated list of glob patterns. "*" and "?" are valid wildcards example: "?razy-*-.txt,*.jsp"
     * @throws CruiseControlException
     *             if at least one of the patterns is malformed
     */
    public void setIgnoreFiles(String filePatterns) throws CruiseControlException {
        if (filePatterns != null) {
            StringTokenizer st = new StringTokenizer(filePatterns, ",");
            ignoreFiles = new ArrayList<GlobFilenameFilter>();
            while (st.hasMoreTokens()) {
                String pattern = st.nextToken();
                // Compile the pattern
                try {
                    ignoreFiles.add(new GlobFilenameFilter(pattern));
                } catch (MalformedCachePatternException e) {
                    throw new CruiseControlException("Invalid filename pattern '" + pattern + "'", e);
                }
            }
        }
    }

    public void add(SourceControl sourceControl) {
        sourceControls.add(sourceControl);
    }

    public List<SourceControl> getSourceControls() {
        return sourceControls;
    }

    protected boolean isLastModificationInQuietPeriod(final Date timeOfCheck,
                                                      final List<Modification> modificationList) {

        final long lastModificationTime = getLastModificationMillis(modificationList);
        final long quietPeriodStart = timeOfCheck.getTime() - quietPeriod;
        final boolean modificationInFuture = new Date().getTime() < lastModificationTime;
        if (modificationInFuture) {
            LOG.warn("A modification has been detected in the future.  Building anyway.");
        }
        return (quietPeriodStart <= lastModificationTime) && !modificationInFuture;
    }

    protected long getLastModificationMillis(final List<Modification> modificationList) {
        Date timeOfLastModification = new Date(0);
        for (final Modification modification : modificationList) {
            final Date modificationDate = modification.modifiedTime;
            if (modificationDate.after(timeOfLastModification)) {
                timeOfLastModification = modificationDate;
            }
        }
        if (modificationList.size() > 0) {
            LOG.debug("Last modification: " + DateUtil.formatIso8601(timeOfLastModification));
        } else {
            LOG.debug("list has no modifications; returning new Date(0).getTime()");
        }
        return timeOfLastModification.getTime();
    }

    protected long getQuietPeriodDifference(final Date now, final List<Modification> modificationList) {
        long diff = quietPeriod - (now.getTime() - getLastModificationMillis(modificationList));
        return Math.max(0, diff);
    }

    /**
     * Returns a Map of name-value pairs representing any properties set by the SourceControl.
     *
     * @return Map of properties.
     */
    public Map<String, String> getProperties() {
        final Map<String, String> table = new HashMap<String, String>();
        for (final SourceControl control : sourceControls) {
            mergeProperties(table, control);
        }
        return table;
    }

    private void mergeProperties(final Map<String, String> properties, final SourceControl control) {
        final Map<String, String> newProperties = control.getProperties();
        final Set<String> existingKeys = properties.keySet();
        final Set<String> newKeys = newProperties.keySet();
        if (Collections.disjoint(existingKeys, newKeys)) {
            properties.putAll(newProperties);
            return;
        }

        final Set<String> disjointKeys = new HashSet<String>(newKeys);
        final Set<String> unionKeys = new HashSet<String>(newKeys);
       
        disjointKeys.removeAll(existingKeys);
        unionKeys.retainAll(existingKeys);

        for (final String key : disjointKeys) {
            properties.put(key, newProperties.get(key));
        }

        for (final String key : unionKeys) {
            final String oldValue = properties.get(key);
            final String newValue = newProperties.get(key);
            final String value = chooseValue(oldValue, newValue);
            properties.put(key, value);
        }        
    }

    private String chooseValue(final String oldValue, final String newValue) {
        if (oldValue.equals(newValue)) {
            return newValue;
        }
        
        if (!(newValue != null)) {
            return newValue;
        }

        final Integer oldInt = getInteger(oldValue);
        final Integer newInt = getInteger(newValue);
        
        Date oldDate = getDate(oldValue);
        Date newDate = getDate(newValue);
        
        final boolean oldBigger;
        
        if (oldInt != null && newInt != null) {
            oldBigger = oldInt.compareTo(newInt) > 0;
        } else if (oldDate != null && newDate != null) {
            oldBigger = oldDate.compareTo(newDate) > 0;            
        } else {
            oldBigger = oldValue.compareTo(newValue) > 0;
        }

        if (oldBigger) {
            return oldValue;
        }
        
        return newValue;
    }

    private Date getDate(final String string) {
        try {
            return DateFormat.getDateInstance().parse(string);
        } catch (ParseException e) {
            return null;
        }
    }

    private Integer getInteger(final String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Modification> getCurrentModifications() {
        return this.modifications;
    }

    /**
     * Returns the modifications as of lastBuild as an XML element.
     * @param lastBuild date of last build
     * @param progress ModificationSet progress message callback object
     * @return modifications element
     */
    // @todo Make this non-public? (package visible only)
    public Element retrieveModificationsAsElement(final Date lastBuild, final Progress progress) {
        Element modificationsElement;
        do {
            timeOfCheck = new Date();
            modifications = new ArrayList<Modification>();
            for (final SourceControl sourceControl : sourceControls) {
                modifications.addAll(sourceControl.getModifications(lastBuild, timeOfCheck));
            }

            // Postfilter all modifications of ignored files
            filterIgnoredModifications(modifications);

            if (modifications.size() > 0) {
                LOG.info(modifications.size()
                        + ((modifications.size() > 1) ? " modifications have been detected."
                                : " modification has been detected."));
            }
            modificationsElement = new Element("modifications");
            for (final Modification modification : modifications) {
                final Element modificationElement = modification.toElement();
                modification.log();
                modificationsElement.addContent(modificationElement);
            }

            if (isLastModificationInQuietPeriod(timeOfCheck, modifications)) {
                LOG.info("A modification has been detected in the quiet period.  ");
                if (LOG.isDebugEnabled()) {
                    final Date quietPeriodStart = new Date(timeOfCheck.getTime() - quietPeriod);
                    LOG.debug(DateUtil.formatIso8601(quietPeriodStart) + " <= Quiet Period <= "
                            + DateUtil.formatIso8601(timeOfCheck));
                }
                final Date now = new Date();
                final long timeToSleep = getQuietPeriodDifference(now, modifications);
                LOG.info("Sleeping for " + (timeToSleep / 1000) + " seconds before retrying.");

                if (progress == null) {
                    throw new IllegalStateException(
                            "retrieveModificationsAsElement(): 'progress' parameter must not be null.");
                }
                progress.setValue(MSG_PROGRESS_PREFIX_QUIETPERIOD_MODIFICATION_SLEEP
                        + (timeToSleep / 1000) + " secs");

                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        } while (isLastModificationInQuietPeriod(timeOfCheck, modifications));

        return modificationsElement;
    }

    /**
     * Remove all Modifications that match any of the ignoreFiles-patterns
     * @param modifications the list of modifications to be filtered (altered).
     */
    protected void filterIgnoredModifications(final List<Modification> modifications) {
        if (ignoreFiles != null) {
            for (Iterator<Modification> iterator = modifications.iterator(); iterator.hasNext();) {
                final Modification modification = iterator.next();
                if (isIgnoredModification(modification)) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean isIgnoredModification(final Modification modification) {
        boolean foundAny = false;

        // Go through all the files in the modification. If all are ignored, ignore this modification.
        for (final Modification.ModifiedFile modFile : modification.getModifiedFiles()) {
            final File file;
            if (modFile.folderName == null) {
                if (modification.getFileName() == null) {
                    continue;
                } else {
                    file = new File(modFile.fileName);
                }
            } else {
                file = new File(modFile.folderName, modFile.fileName);
            }
            String path = file.toString();
            foundAny = true;

            // On systems with a '\' as pathseparator convert it to a forward slash '/'
            // That makes patterns platform independent
            if (File.separatorChar == '\\') {
                path = path.replace('\\', '/');
            }

            boolean useThisFile = true;
            for (Iterator<GlobFilenameFilter> iterator = ignoreFiles.iterator(); iterator.hasNext() && useThisFile;) {
                final GlobFilenameFilter pattern = iterator.next();

                // We have to use a little tweak here, since GlobFilenameFilter only matches the filename, but not
                // the path, so we use the complete path as the 'filename'-argument.
                if (pattern.accept(file, path)) {
                    useThisFile = false;
                }
            }
            if (useThisFile) {
                return false;
            }
        }
        return foundAny;
    }

    public Date getTimeOfCheck() {
        return timeOfCheck;
    }

    public boolean isModified() {
        return !modifications.isEmpty();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(sourceControls.isEmpty(),
                "modificationset element requires at least one nested source control element");

        for (final SourceControl sc : sourceControls) {
            sc.validate();
        }
    }

    int getQuietPeriod() {
        return quietPeriod;
    }

}
