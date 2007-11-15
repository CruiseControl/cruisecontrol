package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.MockProject;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.DateFormatFactory;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
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

        checkResultForState(fileName, ProjectState.WAITING);
        checkResultForState(fileName, ProjectState.IDLE);
        checkResultForState(fileName, ProjectState.QUEUED);
        checkResultForState(fileName, ProjectState.BOOTSTRAPPING);
        checkResultForState(fileName, ProjectState.MODIFICATIONSET);
        checkResultForState(fileName, ProjectState.BUILDING);
        checkResultForState(fileName, ProjectState.MERGING_LOGS);
        checkResultForState(fileName, ProjectState.PUBLISHING);
        checkResultForState(fileName, ProjectState.PAUSED);
        checkResultForState(fileName, ProjectState.STOPPED);
    }

    private void checkResultForState(final String fileName, ProjectState state)
            throws CruiseControlException, IOException {
        // This should be equivalent to the date used in listener at seconds precision
        final Date date = new Date();
        listener.handleEvent(new ProjectStateChangedEvent("projName", state));
        final String expected = getExpectedStateText(date, state);
        assertEquals(expected, Util.readFileToString(fileName));
    }

    private static String getExpectedStateText(Date date, ProjectState state) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        final String dateString = formatter.format(date);
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

        final String expectedProgressTime = DateFormatFactory.getTimeFormat().format(new Date()) + " ";
        final String expectedProgressMsgPrefix = CurrentBuildStatusListener.MSG_PREFIX_PROGRESS + expectedProgressTime;

        String testMsg = "test msg1";
        checkResultForProgress(fileName, testMsg, expectedProgressMsgPrefix + testMsg, project, progress);

        testMsg = null;
        checkResultForProgress(fileName, testMsg, expectedProgressMsgPrefix + testMsg, project, progress);

        // This should be equivalent to the date used in listener at seconds precision
        final Date date = new Date();
        // add a ProjectState string to the status text file
        checkResultForState(fileName, ProjectState.BUILDING);
        final String expectedPrefix = getExpectedStateText(date, ProjectState.BUILDING)
                + "\n" + expectedProgressMsgPrefix;

        testMsg = "test msg2";
        checkResultForProgress(fileName, testMsg, expectedPrefix + testMsg, project, progress);

        testMsg = "";
        checkResultForProgress(fileName, testMsg, expectedPrefix + testMsg, project, progress);

        testMsg = null;
        checkResultForProgress(fileName, testMsg, expectedPrefix + testMsg, project, progress);
    }

    private void checkResultForProgress(final String fileName, final String msgProgress,
                                        final String expectedStatusText,
                                        final MockProject project, final Progress progress)
            throws CruiseControlException, IOException {


        progress.setValue(msgProgress);
        listener.handleEvent(new ProgressChangedEvent(project.getName(), progress));

        assertEquals(expectedStatusText, Util.readFileToString(fileName));
    }
}
