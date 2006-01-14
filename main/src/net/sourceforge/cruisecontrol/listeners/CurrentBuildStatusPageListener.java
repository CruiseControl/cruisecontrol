package net.sourceforge.cruisecontrol.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.DateFormatFactory;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Updates replaceable text in a pattern file each time the Project status changes. Can show full
 * project status history. The following items will be replaced with their values each time they
 * occur in the source file:
 * <ul>
 * <li>{Project} - Project Name.</li>
 * <li>{State.Name} - Name of current project state.</li>
 * <li>{State.Description} - Description of current project state.</li>
 * <li>{State.Date} - Date/time the current state happened</li>
 * <li>{State.Duration} - How long since this state was in effect. (Only useful in {History} line.)
 * <li>{History} - Historical states. Must be first on line. This line will be processed and output
 * once for each state the project has previously been in. The {History} tag will be deleted from
 * the line.</li>
 * </ul>
 * <p>
 * {@link net.sourceforge.cruisecontrol.DateFormatFactory} for the dateformat
 *
 * @author John Lussmyer
 */
public class CurrentBuildStatusPageListener
    implements Listener {
    private static final Logger LOG          = Logger.getLogger(CurrentBuildStatusPageListener.class);
    /** Name of file to write/create */
    private String              dstFileName;
    /** File to read pattern text from */
    private File                sourceFile   = null;
    /** Pattern text to use, contains String objects */
    private List                sourceText   = new ArrayList();
    /** Default text to use if no source file provided. */
    private static final String DEFAULT_TEXT = "{Project}: {State.Date} - {State.Name}: {State.Description}";
    /** Historical Status changes, contains HistoryItem objects */
    private List                history      = new ArrayList();

    private static final String KEY_PROJECT  = "{project}";
    private static final String KEY_NAME     = "{state.name}";
    private static final String KEY_DESC     = "{state.description}";
    private static final String KEY_DATE     = "{state.date}";
    private static final String KEY_DURATION = "{state.duration}";
    private static final String KEY_HISTORY  = "{history}";

    /**
     * Holds info about a project state that has happened.
     *
     * @author jlussmyer Created on: Jan 12, 2006
     */
    private class HistoryItem {
        public HistoryItem(ProjectState projstate) {
            state = projstate.getName();
            desc = projstate.getDescription();
            when = System.currentTimeMillis();
        }

        public String state;
        public String desc;
        public long   when;
    }


    /**
     * Default constructor just used for initialization of local members.
     */
    public CurrentBuildStatusPageListener() {
        sourceText.add(DEFAULT_TEXT);
        return;
    }


    public void handleEvent(ProjectEvent event)
        throws CruiseControlException {
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
        writeFile(result);

        return;
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
     * @param src Source line to have substitutions made
     * @param projectName Name of project being processed
     * @param current current project state information
     */
    private String substituteItems(String src, String projectName, HistoryItem current, long prevtime) {
        int idx;
        StringBuffer result = new StringBuffer();
        DateFormat dateFmt = DateFormatFactory.getDateFormat();

        // Find and substitute entries in this line
        while ((idx = src.indexOf('{')) != -1) {
            if (idx > 0) {
                result.append(src.substring(0, idx));
            }

            src = src.substring(idx); // Trim off preceding text

            int skiplen = 0;

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
                result.append(dateFmt.format(new Date(current.when)));
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

        return (result.toString());
    }


    /**
     * formats the given number of milliseconds as HH:MM:SS.sss
     *
     * @param msecs number of milliseconds
     * @return String of the form HH:MM:SS.sss representing the given milliseconds
     */
    private String formatDuration(long msecs) {
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


    public void validate()
        throws CruiseControlException {

        ValidationHelper.assertIsSet(dstFileName, "file", this.getClass());
        CurrentBuildFileWriter.validate(dstFileName);

        if (sourceFile != null) {
            ValidationHelper.assertTrue(sourceFile.exists(), dstFileName + " does not exist: "
                                                             + sourceFile.getAbsolutePath());
            ValidationHelper.assertTrue(sourceFile.isFile(), dstFileName + " is not a file: "
                                                             + sourceFile.getAbsolutePath());

            sourceText = readSource();
        }

        return;
    }


    /**
     * Write the resulting substituted text to the output file.
     *
     * @param content New content for the output file.
     * @throws CruiseControlException
     */
    private void writeFile(String content)
        throws CruiseControlException {

        FileWriter fw = null;
        try {
            fw = new FileWriter(dstFileName);
            fw.write(content);
        } catch (IOException ioe) {
            throw new CruiseControlException("Error writing file: " + dstFileName, ioe);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignore) {

                }
            }
        }
    }


    /**
     * Read the source text file
     *
     * @return List of lines of text (String objects)
     * @throws CruiseControlException
     */
    private List readSource()
        throws CruiseControlException {

        List result = new ArrayList();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(sourceFile));
            String line = null;

            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException ioe) {
            throw new CruiseControlException("Error reading file: " + sourceFile, ioe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {

                }
            }
        }

        return result;
    }


    public void setFile(String fileName) {
        this.dstFileName = fileName.trim();
        LOG.debug("set fileName = " + fileName);
    }


    public void setSourceFile(String fileName) {
        sourceFile = new File(fileName);
        LOG.debug("set sourceFile = " + fileName);
        return;
    }
}
