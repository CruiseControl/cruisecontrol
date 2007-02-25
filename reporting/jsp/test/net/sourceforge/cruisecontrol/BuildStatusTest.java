package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.IO;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:jeffjensen@upstairstechnology.com">Jeff Jensen </a>
 */
public class BuildStatusTest extends TestCase {
    private static final boolean MULTIPLE_PROJECT_MODE = false;
    private static final boolean SINGLE_PROJECT_MODE = true;
    private static final String PROJECT_NAME = "testProject";
    private static final String STATUS_FILENAME = "buildStatus.txt";
    private static final String TEXT = "the test status";
    private static final String TIME = "12/17/2005 20:11:25";

    private static final String XML_LOGGER_DATA = TIME + " [" + PROJECT_NAME + "]\n";
    private static final String XML_LOGGER_TEXT = "<br>" + XML_LOGGER_DATA;

    private static final String PLAIN_TEXT = TEXT + "\n" + TIME + "\n";
    private static final String PLAIN_WITH_LOGGER = TEXT + "\n" + TIME + XML_LOGGER_TEXT;

    private static final String HTML_TEXT = TEXT + "\n<br/>" + TIME + "\n<br/>";
    private static final String HTML_WITH_LOGGER = HTML_TEXT + XML_LOGGER_DATA + "<br/>";

    private File logDir;

    private TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    protected void setUp() throws Exception {
        // make base log dir
        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
            filesToDelete.add(logDir);
        }

        // make multi project log dir
        File projectLogDir = new File(logDir, PROJECT_NAME + "/");
        if (!projectLogDir.exists()) {
            assertTrue("Failed to create project log dir", projectLogDir.mkdir());
            filesToDelete.add(logDir);
        }

        // for single project
        File file = new File(logDir, STATUS_FILENAME);
        prepareFile(file, PLAIN_WITH_LOGGER);

        // for multi project
        file = new File(projectLogDir, STATUS_FILENAME);
        prepareFile(file, PLAIN_WITH_LOGGER);
    }

    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testStatusFileNotFound() {
        String status = BuildStatus.getStatusHtml(SINGLE_PROJECT_MODE,
            logDir.getAbsolutePath(),
            PROJECT_NAME,
            "badfilename.txt",
            BuildStatus.READ_ALL_LINES);
        assertEquals("(build status file not found)", status);
        status = BuildStatus.getStatusHtml(MULTIPLE_PROJECT_MODE,
            logDir.getAbsolutePath(),
            PROJECT_NAME, "badfilename.txt",
            BuildStatus.READ_ALL_LINES);
        assertEquals("(build status file not found)", status);
    }

    public void testShouldThrowExceptionWithDirectory() {
        try {
            BuildStatus.getStatusHtml(SINGLE_PROJECT_MODE,
                logDir.getAbsolutePath(),
                null,
                PROJECT_NAME,
                BuildStatus.READ_ALL_LINES);
            fail("Expected exception for build status file not found.");
        } catch (CruiseControlWebAppException expected) {
            assertTrue(expected.getMessage().indexOf("is a directory") > 0);
        }
    }

    public void testGetCurrentStatusSingleProject() {
        coreTestPlain("status only: ", SINGLE_PROJECT_MODE, BuildStatus.READ_ONLY_STATUS_LINES, PLAIN_TEXT);
        coreTestHtml("status only: ", SINGLE_PROJECT_MODE, BuildStatus.READ_ONLY_STATUS_LINES, HTML_TEXT);

        coreTestPlain("all lines: ", SINGLE_PROJECT_MODE, BuildStatus.READ_ALL_LINES, PLAIN_TEXT + XML_LOGGER_DATA);
        coreTestHtml("all lines: ", SINGLE_PROJECT_MODE, BuildStatus.READ_ALL_LINES, HTML_WITH_LOGGER);
    }

    public void testGetCurrentStatusMultiProject() {
        coreTestPlain("status only: ", MULTIPLE_PROJECT_MODE, BuildStatus.READ_ONLY_STATUS_LINES, PLAIN_TEXT);
        coreTestHtml("status only: ", MULTIPLE_PROJECT_MODE, BuildStatus.READ_ONLY_STATUS_LINES, HTML_TEXT);

        coreTestPlain("all lines: ", MULTIPLE_PROJECT_MODE, BuildStatus.READ_ALL_LINES, PLAIN_TEXT + XML_LOGGER_DATA);
        coreTestHtml("all lines: ", MULTIPLE_PROJECT_MODE, BuildStatus.READ_ALL_LINES, HTML_WITH_LOGGER);
    }

    private void coreTestPlain(String msg, boolean isSingleProject, int readLines, String expected) {
        String actual = BuildStatus.getStatusPlain(isSingleProject,
                                                   logDir.getAbsolutePath(),
                                                   PROJECT_NAME,
                                                   STATUS_FILENAME,
                                                   readLines);
        assertEquals("plain:" + msg, expected, actual);
    }

    private void coreTestHtml(String msg, boolean isSingleProject, int readLines, String expected) {
        String actual = BuildStatus.getStatusHtml(isSingleProject,
                                                  logDir.getAbsolutePath(),
                                                  PROJECT_NAME,
                                                  STATUS_FILENAME,
                                                  readLines);
        assertEquals("html:" + msg, expected, actual);
    }

    private void prepareFile(File file, String body) throws IOException, CruiseControlException {
        IO.write(file, body);
        filesToDelete.add(file);
    }
}
