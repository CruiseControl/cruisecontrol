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

/**
 * @author Jeffrey Fredrick
 */
public class ProjectStateTest extends TestCase {

    private static final String QUEUED_OBJECT_FILE = "target/queued.object";
    
    protected void tearDown() throws Exception {
        File file = new File(QUEUED_OBJECT_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    public void testSerialization() throws Exception {
        FileOutputStream fos = new FileOutputStream(QUEUED_OBJECT_FILE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(ProjectState.QUEUED);
        oos.close();

        FileInputStream fis = new FileInputStream(QUEUED_OBJECT_FILE);
        ObjectInputStream ois = new ObjectInputStream(fis);
        ProjectState queued = (ProjectState) ois.readObject();
        ois.close();

        assertTrue(ProjectState.QUEUED == queued);
    }

}