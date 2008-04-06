package net.sourceforge.cruisecontrol.util;

import java.io.File;
import java.util.Date;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;

import junit.framework.TestCase;

public class IOTest extends TestCase {

    private FilesToDelete filesToDelete = new FilesToDelete();
    
    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testWriteWorksWhenParentDirDoesntExist() throws CruiseControlException {
        String nonExistentDirName = getNonExistentDirName();
        File file = new File(nonExistentDirName, "filename");
        filesToDelete.add(file);
        IO.write(file, "Hello World!");
        assertEquals("Hello World!", IO.readLines(file).get(0));
    }

    private String getNonExistentDirName() {
        String name = new Date().getTime() + "";
        while (new File(name).exists()) {
            name = new Date().getTime() + "";
        }
        return name;
    }
    
}
