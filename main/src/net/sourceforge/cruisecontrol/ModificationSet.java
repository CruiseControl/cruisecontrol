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
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import org.apache.oro.text.MalformedCachePatternException;
import org.jdom.Element;

/**
 * Set of modifications collected from included SourceControls
 *
 * @see SourceControl
 */
public class ModificationSet implements Serializable {

    private static final long serialVersionUID = 7834545928469764690L;

    private boolean lieOnIsModified = false;
    private static final Logger LOG = Logger.getLogger(ModificationSet.class);
    private static final int ONE_SECOND = 1000;

    private List modifications = new ArrayList();
    private final List sourceControls = new ArrayList();
    private int quietPeriod = 60 * ONE_SECOND;
    private Date timeOfCheck;
    private final DateFormat formatter = DateFormatFactory.getDateFormat();

    /**
     * File-Patterns (as org.apache.oro.io.GlobFilenameFilter) to be ignored
     */
    private List ignoreFiles;

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
            ignoreFiles = new ArrayList();
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

    protected List getIgnoreFiles() {
        return this.ignoreFiles;
    }

    /** @deprecated * */
    public void addSourceControl(SourceControl sourceControl) {
        add(sourceControl);
    }

    public void add(SourceControl sourceControl) {
        sourceControls.add(sourceControl);
    }

    public List getSourceControls() {
        return sourceControls;
    }

    protected boolean isLastModificationInQuietPeriod(Date timeOfCheck, List modificationList) {
        long lastModificationTime = getLastModificationMillis(modificationList);
        final long quietPeriodStart = timeOfCheck.getTime() - quietPeriod;
        final boolean modificationInFuture = new Date().getTime() < lastModificationTime;
        if (modificationInFuture) {
            LOG.warn("A modification has been detected in the future.  Building anyway.");
        }
        return (quietPeriodStart <= lastModificationTime) && !modificationInFuture;
    }

    protected long getLastModificationMillis(List modificationList) {
        Date timeOfLastModification = new Date(0);
        Iterator iterator = modificationList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            Modification modification = null;
            if (object instanceof Modification) {
                modification = (Modification) object;
            }
            if (object instanceof Element) {
                Element element = (Element) object;
                modification = new Modification("unknown");
                modification.fromElement(element, formatter);
            }
            if (modification != null) {
                Date modificationDate = modification.modifiedTime;
                if (modificationDate.after(timeOfLastModification)) {
                    timeOfLastModification = modificationDate;
                }
            }
        }
        if (modificationList.size() > 0) {
            LOG.debug("Last modification: " + formatter.format(timeOfLastModification));
        } else {
            LOG.debug("list has no modifications; returning new Date(0).getTime()");
        }
        return timeOfLastModification.getTime();
    }

    protected long getQuietPeriodDifference(Date now, List modificationList) {
        long diff = quietPeriod - (now.getTime() - getLastModificationMillis(modificationList));
        return Math.max(0, diff);
    }

    /**
     * Returns a Hashtable of name-value pairs representing any properties set by the SourceControl.
     *
     * @return Hashtable of properties.
     */
    public Hashtable getProperties() {
        Hashtable table = new Hashtable();
        for (Iterator iter = sourceControls.iterator(); iter.hasNext();) {
            SourceControl control = (SourceControl) iter.next();
            table.putAll(control.getProperties());
        }
        return table;
    }

    /**
     * @deprecated use {@link #getModifications(java.util.Date, Progress)} instead.
     */
    public Element getModifications(final Date lastBuild) {
        return getModifications(lastBuild, null);
    }
    /**
     * @param lastBuild date of last build
     * @param progress ModificationSet progress message callback object
     * @return modifications element
     */
    public Element getModifications(final Date lastBuild, final Progress progress) {
        Element modificationsElement;
        do {
            timeOfCheck = new Date();
            modifications = new ArrayList();
            Iterator sourceControlIterator = sourceControls.iterator();
            while (sourceControlIterator.hasNext()) {
                SourceControl sourceControl = (SourceControl) sourceControlIterator.next();
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
            Iterator modificationIterator = modifications.iterator();
            while (modificationIterator.hasNext()) {
                Object object = modificationIterator.next();
                if (object instanceof Element) {
                    modificationsElement.addContent(((Element) object).detach());
                } else {
                    Modification modification = (Modification) object;
                    Element modificationElement = (modification).toElement(formatter);
                    modification.log(formatter);
                    modificationsElement.addContent(modificationElement);
                }
            }

            if (isLastModificationInQuietPeriod(timeOfCheck, modifications)) {
                LOG.info("A modification has been detected in the quiet period.  ");
                if (LOG.isDebugEnabled()) {
                    final Date quietPeriodStart = new Date(timeOfCheck.getTime() - quietPeriod);
                    LOG.debug(formatter.format(quietPeriodStart) + " <= Quiet Period <= "
                            + formatter.format(timeOfCheck));
                }
                Date now = new Date();
                long timeToSleep = getQuietPeriodDifference(now, modifications);
                LOG.info("Sleeping for " + (timeToSleep / 1000) + " seconds before retrying.");

                // @todo Remove "if (progress != null)" when deprecated getModifications(Date lastBuild) is removed
                if (progress != null) {
                    progress.setValue(MSG_PROGRESS_PREFIX_QUIETPERIOD_MODIFICATION_SLEEP
                            + (timeToSleep / 1000) + " secs");
                }

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
     */
    protected void filterIgnoredModifications(List modifications) {
        if (this.ignoreFiles != null) {
            for (Iterator iterator = modifications.iterator(); iterator.hasNext();) {
                Object object = iterator.next();
                Modification modification = null;
                if (object instanceof Modification) {
                    modification = (Modification) object;
                } else if (object instanceof Element) {
                    Element element = (Element) object;
                    modification = new Modification();
                    modification.fromElement(element, formatter);
                }

                if (isIgnoredModification(modification)) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean isIgnoredModification(Modification modification) {
        boolean foundAny = false;

        // Go through all the files in the modification. If all are ignored, ignore this modification.
        for (final Iterator modFileIter = modification.getModifiedFiles().iterator(); modFileIter.hasNext();) {
            final Modification.ModifiedFile modFile = (Modification.ModifiedFile) modFileIter.next();

            File file;
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
            for (Iterator iterator = ignoreFiles.iterator(); iterator.hasNext() && useThisFile;) {
                GlobFilenameFilter pattern = (GlobFilenameFilter) iterator.next();

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
        return (!modifications.isEmpty()) || lieOnIsModified;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(sourceControls.isEmpty(),
                "modificationset element requires at least one nested source control element");

        for (Iterator i = sourceControls.iterator(); i.hasNext();) {
            SourceControl sc = (SourceControl) i.next();
            sc.validate();
        }
    }

    int getQuietPeriod() {
        return quietPeriod;
    }

    /**
     * @param isModifiedAccurate
     * @deprecated
     */
    public void setRequireModification(boolean isModifiedAccurate) {
        LOG.warn("<modificationset requiremodification=\"true|false\" is deprecated. "
                + "Use <project requiremodification=\"true|false\".");
        lieOnIsModified = !isModifiedAccurate;
    }
}
