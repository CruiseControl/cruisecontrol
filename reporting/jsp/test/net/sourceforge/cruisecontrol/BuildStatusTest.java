package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:jeffjensen@upstairstechnology.com">Jeff Jensen </a>
 */
public class BuildStatusTest extends TestCase {
    private static final String TEST_PROJECT_NAME = "testProject";

    private static final String TEST_STATUS_FILENAME = "buildStatus.txt";

    private static final String TEST_STATUS_TEXT = "the test status";

    private static final String TEST_STATUS_TIME = "12/17/2005 20:11:25";

    private static final String TEST_XML_LOGGER_TEXT = TEST_STATUS_TIME + " [" + TEST_PROJECT_NAME + "]";

    private static final String TEST_STATUS_PLAIN = TEST_STATUS_TEXT + "\n" + TEST_STATUS_TIME + "\n";

    private static final String TEST_STATUS_PLAIN_WITH_LOGGER = TEST_STATUS_PLAIN + TEST_XML_LOGGER_TEXT + "\n";

    private static final String TEST_STATUS_HTML = TEST_STATUS_TEXT + "\n<br/>" + TEST_STATUS_TIME + "\n<br/>";

    private static final String TEST_STATUS_HTML_WITH_LOGGER = TEST_STATUS_HTML + TEST_XML_LOGGER_TEXT + "\n<br/>";

    private File logDir;

    private FilesToDelete files = new FilesToDelete();

    protected void setUp() throws Exception {

        // make base log dir
        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
            files.addFile(logDir);
        }

        // make multi project log dir
        File projectLogDir = new File(logDir, TEST_PROJECT_NAME + "/");
        if (!projectLogDir.exists()) {
            assertTrue("Failed to create project log dir", projectLogDir.mkdir());
            files.addFile(logDir);
        }

        // for single project
        File file = new File(logDir, TEST_STATUS_FILENAME);
        prepareFile(file, TEST_STATUS_PLAIN_WITH_LOGGER);

        // for multi project
        file = new File(projectLogDir, TEST_STATUS_FILENAME);
        prepareFile(file, TEST_STATUS_PLAIN_WITH_LOGGER);
    }

    protected void tearDown() throws Exception {
        files.delete();
    }

    public void testGetCurrentStatusSingleProject() {
        coreStatusTests(true);
    }

    public void testGetCurrentStatusMultiProject() {
        coreStatusTests(false);
    }

    private void coreStatusTests(boolean isSingleProject) {
        String logDirPath = logDir.getAbsolutePath();
        int readLines = BuildStatus.READ_ONLY_STATUS_LINES;

        coreTestPlain("testing getStatusPlain, status only: ", isSingleProject, logDirPath, readLines,
            TEST_STATUS_PLAIN);

        coreTestHtml("testing getStatusHtml, status only: ", isSingleProject, logDirPath, readLines, TEST_STATUS_HTML);

        // note: resetting line count
        readLines = BuildStatus.READ_ALL_LINES;

        coreTestPlain("testing getStatusPlain, all lines: ", isSingleProject, logDirPath, readLines,
            TEST_STATUS_PLAIN_WITH_LOGGER);

        coreTestHtml("testing getStatusHtml, all lines: ", isSingleProject, logDirPath, readLines,
            TEST_STATUS_HTML_WITH_LOGGER);

        try {
            BuildStatus.getStatusHtml(isSingleProject, logDirPath, TEST_PROJECT_NAME, "badfilename.txt", readLines);
            fail("Expected exception for build status file not found.");
        } catch (CruiseControlWebAppException e) {
            // expected, so test passes
        }
    }

    private void coreTestPlain(String msg, boolean isSingleProject, String logDirPath, int readLines, String expected) {
        String actual = BuildStatus.getStatusPlain(isSingleProject, logDirPath, TEST_PROJECT_NAME,
            TEST_STATUS_FILENAME, readLines);

        assertEquals(msg, expected, actual);
    }

    private void coreTestHtml(String msg, boolean isSingleProject, String logDirPath, int readLines, String expected) {
        String actual = BuildStatus.getStatusHtml(isSingleProject, logDirPath, TEST_PROJECT_NAME, TEST_STATUS_FILENAME,
            readLines);

        assertEquals(msg, expected, actual);
    }

    private void prepareFile(File file, String body) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(body);
        writer.close();
        files.addFile(file);
    }

    class FilesToDelete {
        private List files = new Vector();

        void addFile(File file) {
            files.add(file);
        }

        void delete() {
            Iterator fileIterator = files.iterator();
            while (fileIterator.hasNext()) {
                File file = (File) fileIterator.next();
                file.delete();
            }
            files.clear();
        }
    }
}
