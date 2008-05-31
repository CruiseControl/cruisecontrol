package net.sourceforge.cruisecontrol.logmanipulators;

import junit.framework.TestCase;

import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.DateUtil;

/**
 * @author Dan Rollo
 * Date: May 30, 2008
 * Time: 1:36:27 AM
 */
public class DeleteArtifactsManipulatorTest extends TestCase {

    public DeleteArtifactsManipulatorTest(String testName) {
        super(testName);
    }

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(DateUtil.SIMPLE_DATE_FORMAT);
    private static final int UNIT = 1;
    private static final Date YESTERDAY;
    static {
        final Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_MONTH, -UNIT);
        YESTERDAY = cal.getTime();
    }
    private static final String TIMESTAMP_YESTERDAY = FORMATTER.format(YESTERDAY);

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private File tempProjectLogDir;
    private File tempProjectLog;
    private File tempArtifactsDir;
    private DeleteArtifactsManipulator instance;

    protected void setUp() throws Exception {
        final File tempDir = new File(System.getProperty("java.io.tmpdir"));
        final File tempSubdir = new File(tempDir, "cruisecontroltest" + System.currentTimeMillis());
        assertTrue(tempSubdir.mkdir());
        filesToDelete.add(tempSubdir);

        tempProjectLogDir = new File(tempSubdir, "dummyProjectLogDir");
        assertTrue(tempProjectLogDir.mkdir());

        tempProjectLog = File.createTempFile("log" + TIMESTAMP_YESTERDAY + "Dummy", ".xml", tempProjectLogDir);
        
        tempArtifactsDir = new File(tempProjectLogDir, FORMATTER.format(YESTERDAY));
        assertTrue(tempArtifactsDir.mkdir());

        File.createTempFile("tempArtifact", ".tmp", tempArtifactsDir);

        instance = new DeleteArtifactsManipulator();
        instance.setUnit("DAY");
        instance.setEvery(UNIT);
    }

    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }


    public void testGetFilenameFilter() throws Exception {

        final File[] artifactDirToDelete = instance.getRelevantFiles(tempProjectLogDir.getAbsolutePath(),
                // 2nd param is ignored
                true);
        TestUtil.assertArray("Wrong artifacts to delete list",
                new File[] { tempArtifactsDir },
                artifactDirToDelete);

        instance.setUnit("WEEK");
        assertEquals("Wrong artifacts to delete list", 0,
                                                                            // 2nd param is ignored
                instance.getRelevantFiles(tempProjectLogDir.getAbsolutePath(), false).length);
    }

    public void testDelete() throws Exception {

        instance.execute(tempProjectLogDir.getAbsolutePath());
        assertFalse("Failed to delete artifacts dir: " + tempArtifactsDir.getAbsolutePath(),
                tempArtifactsDir.exists());
        assertTrue("Should not have deleted log file: " + tempProjectLog.getAbsolutePath(),
                tempProjectLog.exists());
    }

}
