package net.sourceforge.cruisecontrol.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
        final String nonExistentDirName = getNonExistentDirName();
        filesToDelete.add(new File(nonExistentDirName));

        final File file = new File(nonExistentDirName, "filename");
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
    
    public void testReadText() throws IOException {
        File file = File.createTempFile("testReadText", "txt");
        filesToDelete.add(file);
        
        // Populate the file.
        PrintWriter writer = new PrintWriter(file);
        try {
            for (int i = 0; i < 500; i++) {
                writer.write("abcdef");
            }
        } finally {
            writer.close();
        }
        
        // Read it back in.
        assertEquals(500 * 6, IO.readText(new FileInputStream(file)).length());
    }

}
