/*
 * Created on Apr 8, 2005
 */
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

/**
 * @author Jeffrey Fredrick
 */
public class ProjectStateTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private static final File QUEUED_OBJECT_FILE = new File(TestUtil.getTargetDir(), "target/queued.object");

    protected void setUp() throws Exception {
        QUEUED_OBJECT_FILE.getParentFile().mkdir();
        filesToDelete.add(new File(TestUtil.getTargetDir(), "target"));
    }
    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testSerialization() throws Exception {
        FileOutputStream fos = new FileOutputStream(QUEUED_OBJECT_FILE);
        final ObjectOutputStream oos = new ObjectOutputStream(fos);
        try {
            oos.writeObject(ProjectState.QUEUED);
        } finally {
            oos.close();
        }

        FileInputStream fis = new FileInputStream(QUEUED_OBJECT_FILE);
        final ObjectInputStream ois = new ObjectInputStream(fis);
        final ProjectState queued;
        try {
            queued = (ProjectState) ois.readObject();
        } finally {
            ois.close();
        }
        assertTrue(ProjectState.QUEUED == queued);
    }

}