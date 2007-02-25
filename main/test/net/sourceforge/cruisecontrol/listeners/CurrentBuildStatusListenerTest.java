package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectState;
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
        Date date = new Date();
        listener.handleEvent(new ProjectStateChangedEvent("projName", state));
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        final String dateString = formatter.format(date);
        final String description = state.getDescription();
        String expected = description + " since\n" + dateString;
        assertEquals(expected, Util.readFileToString(fileName));
    }
}
