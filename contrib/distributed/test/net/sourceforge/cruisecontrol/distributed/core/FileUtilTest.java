package net.sourceforge.cruisecontrol.distributed.core;

import junit.framework.TestCase;

import java.io.File;

import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;

/**
 * @author Dan Rollo
 * Date: Jul 30, 2007
 * Time: 3:05:51 PM
 */
public class FileUtilTest extends TestCase {

    public void testGetFileFromResource() throws Exception {
        try {
            FileUtil.getFileFromResource(null);
            fail("Should have failed");
        } catch (IllegalArgumentException e) {
            // expected
        }

        String resourceName = "bogus";
        try {
            FileUtil.getFileFromResource(resourceName);
        } catch (RuntimeException e) {
            assertEquals("Could not find resource: " + resourceName, e.getMessage());
        }

        final File actualResourceFile = FileUtil.getFileFromResource("");
        final File currentDir = new File(DistributedMasterBuilderTest.MAIN_CCDIST_DIR).getCanonicalFile();
        assertEquals(currentDir, actualResourceFile.getParentFile().getParentFile());

        final File classFile = FileUtil
                .getFileFromResource(getClass().getName().replace('.', '/') + ".class");
        assertNotNull(classFile);
    }
}
