/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Updates replaceable text in a pattern file each time the Project status changes. Can show full project status
 * history. The following items will be replaced with their values each time they occur in the source file:
 * <ul>
 * <li>{Project} - Project Name.</li>
 * <li>{State.Name} - Name of current project state.</li>
 * <li>{State.Description} - Description of current project state.</li>
 * <li>{State.Date} - Date/time the current state happened</li>
 * <li>{State.Duration} - How long since this state was in effect. (Only useful in {History} line.)
 * <li>{History} - Historical states. Must be first on line. This line will be processed and output once for each state
 * the project has previously been in. The {History} tag will be deleted from the line.</li>
 * </ul>
 * <p>
 * {@link net.sourceforge.cruisecontrol.DateFormatFactory} for the dateformat
 * 
 * @author John Lussmyer
 */
public class CurrentBuildStatusPageListener implements Listener {

    private static final long serialVersionUID = -2710491917137221293L;
    private static final Logger LOG = Logger.getLogger(CurrentBuildStatusPageListener.class);

    /** Name of file to write/create */
    private String dstFileName;
    /** File to read pattern text from */
    private File sourceFile = null;
    /** Pattern text to use, contains String objects */
    private List sourceText = new ArrayList();
    /** Default text to use if no source file provided. */
    private static final String DEFAULT_TEXT = "{Project}: {State.Date} - {State.Name}: {State.Description}";
    /** Historical Status changes, contains HistoryItem objects */
    private List history = new ArrayList();

    private static final String KEY_PROJECT = "{project}";
    private static final String KEY_NAME = "{state.name}";
    private static final String KEY_DESC = "{state.description}";
    private static final String KEY_DATE = "{state.date}";
    private static final String KEY_DURATION = "{state.duration}";
    private static final String KEY_HISTORY = "{history}";

    /**
     * Holds info about a project state that has happened.
     * 
     * @author jlussmyer Created on: Jan 12, 2006
     */
    private class HistoryItem implements Serializable {
        private static final long serialVersionUID = -5271600385796774883L;

        public HistoryItem() {
            // needed for serialization
        }

        public HistoryItem(ProjectState projstate) {
            state = projstate.getName();
            desc = projstate.getDescription();
            when = System.currentTimeMillis();
        }

        public String state;
        public String desc;
        public long when;
    }

    /**
     * Default constructor just used for initialization of local members.
     */
    public CurrentBuildStatusPageListener() {
        sourceText.add(DEFAULT_TEXT);
    }

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (!(event instanceof ProjectStateChangedEvent)) {
            // ignore other ProjectEvents
            LOG.debug("ignoring event " + event.getClass().getName() + " for project " + event.getProjectName());
            return;
        }

        final ProjectStateChangedEvent stateChanged = (ProjectStateChangedEvent) event;
        final ProjectState newState = stateChanged.getNewState();
        LOG.debug("updating status to " + newState.getName() + " for project " + stateChanged.getProjectName());

        HistoryItem hist = new HistoryItem(newState);
        String result = substituteText(hist, stateChanged.getProjectName());
        history.add(0, hist);
        IO.write(dstFileName, result);
    }

    /**
     * Perform all the needed text substitutions.
     * 
     * @return String resulting form substituting entries from sourceText.
     */
    private String substituteText(HistoryItem current, String projectName) {
        StringBuffer result = new StringBuffer();
        Iterator lineIter = sourceText.iterator();

        while (lineIter.hasNext()) {
            String src = (String) lineIter.next();

            // See if we need to output this line once for each historical state
            if (src.toLowerCase().startsWith(KEY_HISTORY)) {
                src = src.substring(KEY_HISTORY.length());
                Iterator histIter = history.iterator();
                long prevtime = current.when;

                while (histIter.hasNext()) {
                    HistoryItem hist = (HistoryItem) histIter.next();
                    result.append(substituteItems(src, projectName, hist, prevtime));
                    result.append('\n');
                    prevtime = hist.when;
                }
            } else {
                // Just do this line once
                result.append(substituteItems(src, projectName, current, 0));
                result.append('\n');
            }

        }

        return result.toString();
    }

    /**
     * Substitute values for any and all key items in a line of text.
     * 
     * @param src
     *            Source line to have substitutions made
     * @param projectName
     *            Name of project being processed
     * @param current
     *            current project state information
     */
    private String substituteItems(String src, String projectName, HistoryItem current, long prevtime) {
        int idx;
        StringBuffer result = new StringBuffer();

        // Find and substitute entries in this line
        while ((idx = src.indexOf('{')) != -1) {
            if (idx > 0) {
                result.append(src.substring(0, idx));
            }

            src = src.substring(idx); // Trim off preceding text

            int skiplen;

            if (src.toLowerCase().startsWith(KEY_PROJECT)) {
                result.append(projectName);
                skiplen = KEY_PROJECT.length();
            } else if (src.toLowerCase().startsWith(KEY_NAME)) {
                result.append(current.state);
                skiplen = KEY_NAME.length();
            } else if (src.toLowerCase().startsWith(KEY_DESC)) {
                result.append(current.desc);
                skiplen = KEY_DESC.length();
            } else if (src.toLowerCase().startsWith(KEY_DATE)) {
                result.append(DateUtil.formatIso8601(new Date(current.when)));
                skiplen = KEY_DATE.length();
            } else if (src.toLowerCase().startsWith(KEY_DURATION)) {
                result.append(formatDuration(prevtime - current.when));
                skiplen = KEY_DURATION.length();
            } else {
                result.append('{');
                skiplen = 1;
            }

            if (skiplen > 0) {
                src = src.substring(skiplen); // remove KEY text
            }
        }

        result.append(src); // Pick up any leftover text

        return result.toString();
    }

    /**
     * formats the given number of milliseconds as HH:MM:SS.sss
     * 
     * @param msecs
     *            number of milliseconds
     * @return String of the form HH:MM:SS.sss representing the given milliseconds
     */
    public static String formatDuration(long msecs) {
        StringBuffer buf = new StringBuffer();
        long hours = msecs / (60 * 60 * 1000);
        msecs %= (60 * 60 * 1000);
        long mins = msecs / (60 * 1000);
        msecs %= (60 * 1000);
        long secs = msecs / 1000;
        msecs %= 1000;

        if (hours > 0) {
            buf.append(hours);
            buf.append(':');
        }

        if ((mins > 0) || (hours > 0)) {
            if (mins < 10) {
                buf.append('0');
            }
            buf.append(mins);
            buf.append(':');
        }

        if ((secs < 10) && ((mins > 0) || (hours > 0))) {
            buf.append('0');
        }

        buf.append(secs);
        buf.append('.');

        if (msecs < 100) {
            buf.append('0');
        }

        if (msecs < 10) {
            buf.append('0');
        }

        buf.append(msecs);

        return buf.toString();
    }

    public void validate() throws CruiseControlException {

        ValidationHelper.assertIsSet(dstFileName, "file", this.getClass());
        CurrentBuildFileWriter.validate(dstFileName);

        if (sourceFile != null) {
            ValidationHelper.assertTrue(sourceFile.exists(), "'sourceFile' does not exist: "
                    + sourceFile.getAbsolutePath());
            ValidationHelper.assertTrue(sourceFile.isFile(), "'sourceFile' must be a file: "
                    + sourceFile.getAbsolutePath());
            sourceText = IO.readLines(sourceFile);
        }
    }

    public void setFile(String fileName) {
        this.dstFileName = fileName.trim();
        LOG.debug("set fileName = " + fileName);
    }

    public void setSourceFile(String fileName) {
        sourceFile = new File(fileName);
        LOG.debug("set sourceFile = " + fileName);
    }
}
