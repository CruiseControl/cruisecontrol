package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.MockProject;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.Util;

/**
 * .
 * User: jfredrick
 * Date: Sep 6, 2004
 * Time: 10:58:41 PM
 */
public class CurrentBuildStatusListenerTest extends TestCase {
    private static final String TEST_DIR = "tmp";
    private final FilesToDelete filesToDelete = new FilesToDelete();
    private CurrentBuildStatusListener listener;

    protected void setUp() throws Exception {
        listener = new CurrentBuildStatusListener();

        // listener.handleEvent() will create a file in the test tmp dir, so clean it up
        filesToDelete.add(new File(TEST_DIR));
    }

    protected void tearDown() {
        listener = null;
        filesToDelete.delete();
    }

    public void testValidate() throws CruiseControlException {
        try {
            listener.validate();
            fail("'file' should be a required attribute");
        } catch (CruiseControlException cce) {
        }

        listener.setFile("somefile");
        listener.validate();

        listener.setFile(System.getProperty("java.io.tmpdir") + File.separator + "filename");
        listener.validate();
    }

    public void testWritingStatus() throws CruiseControlException, IOException {
        final String fileName = TEST_DIR + File.separator + "_testCurrentBuildStatus.txt";
        listener.setFile(fileName);
        filesToDelete.add(new File(fileName));

        // This should be equivalent to the date used in listener at seconds precision
        final Date date = new Date();

        checkResultForState(fileName, date, ProjectState.WAITING);
        checkResultForState(fileName, date, ProjectState.IDLE);
        checkResultForState(fileName, date, ProjectState.QUEUED);
        checkResultForState(fileName, date, ProjectState.BOOTSTRAPPING);
        checkResultForState(fileName, date, ProjectState.MODIFICATIONSET);
        checkResultForState(fileName, date, ProjectState.BUILDING);
        checkResultForState(fileName, date, ProjectState.MERGING_LOGS);
        checkResultForState(fileName, date, ProjectState.PUBLISHING);
        checkResultForState(fileName, date, ProjectState.PAUSED);
        checkResultForState(fileName, date, ProjectState.STOPPED);
    }

    private void checkResultForState(final String fileName, final Date date, final ProjectState state)
            throws CruiseControlException, IOException {
        listener.handleEvent(new ProjectStateChangedEvent("projName", state));
        final String expected = getExpectedStateText(date, state);
        assertEquals(expected, Util.readFileToString(fileName));
    }

    private static String getExpectedStateText(Date date, ProjectState state) {
        final String dateString = DateUtil.formatIso8601(date);
        final String description = state.getDescription();
        return description + " since\n" + dateString;
    }

    public void testWritingProgress() throws CruiseControlException, IOException {
        final String fileName = TEST_DIR + File.separator + "_testCurrentBuildStatus.txt";
        listener.setFile(fileName);
        filesToDelete.add(new File(fileName));

        final ProjectConfig projectConfig = new ProjectConfig();
        projectConfig.add(new DefaultLabelIncrementer());

        final MockProject project = new MockProject();
        final Progress progress = project.getProgress();
        project.setProjectConfig(projectConfig);

        // This should be equivalent to the date used in listener at seconds precision
        final Date date = new Date();
        final String expectedProgressMsgPrefix = CurrentBuildStatusListener.MSG_PREFIX_PROGRESS
                + DateUtil.formatIso8601(date) + " ";

        checkResultForProgress(fileName, "test msg1", expectedProgressMsgPrefix, project, progress);

        checkResultForProgress(fileName, null, expectedProgressMsgPrefix, project, progress);

        // add a ProjectState string to the status text file
        checkResultForState(fileName, date, ProjectState.BUILDING);
        final String expectedPrefix = getExpectedStateText(date, ProjectState.BUILDING)
                + "\n" + expectedProgressMsgPrefix;

        checkResultForProgress(fileName, "test msg2", expectedPrefix, project, progress);

        checkResultForProgress(fileName, "", expectedPrefix, project, progress);

        checkResultForProgress(fileName, null, expectedPrefix, project, progress);
    }

    private void checkResultForProgress(final String fileName, final String msgProgress,
                                        final String expectedStatusTextPrefix,
                                        final MockProject project, final Progress progress)
            throws CruiseControlException, IOException {


        progress.setValue(msgProgress);
        listener.handleEvent(new ProgressChangedEvent(project.getName(), progress));

        assertEquals(expectedStatusTextPrefix + msgProgress, Util.readFileToString(fileName));
    }
}
