/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class FileSystemTest extends TestCase {

    public FileSystemTest(String name) {
        super(name);
    }

    public void testValidate() {
        FileSystem fs = new FileSystem();

        try {
            fs.validate();
            fail("FileSystem should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'folder' is a required attribute for FileSystem", e.getMessage());
        }

        fs.setFolder("folder");

        try {
            fs.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("FileSystem should not throw exceptions when required attributes are set.");
        }
    }

    public void testGettingModifications() throws Exception {
        //Figure out where the temp directory is...
        File tempFile = File.createTempFile("CruiseControl", "TEST");
        tempFile.deleteOnExit();
        File tempDirectory = getDirectory(tempFile);

        //Create a subdirectory in the temp directory for us to use.
        tempDirectory = new File(tempDirectory,
                "filesystemtest" + System.currentTimeMillis());
        assertTrue(tempDirectory.mkdir());

        //Setup a filesystem element that points at our test subdirectory...
        FileSystem fsystem = new FileSystem();
        fsystem.setFolder(tempDirectory.getAbsolutePath());

        //Check for modifications...there shouldn't be any
        Date lastBuild = new GregorianCalendar(1900, 0, 1).getTime();
        Date now = new Date();
        List mods = fsystem.getModifications(lastBuild, now);
        assertNotNull(mods);
        assertEquals(0, mods.size());

        //Write some files...
        tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        tempFile.deleteOnExit();
        writeContent(tempFile, "testing");
        tempFile.setLastModified(now.getTime() + 1);

        tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        tempFile.deleteOnExit();
        writeContent(tempFile, "testing 2");
        tempFile.setLastModified(now.getTime() + 1);

        //Check for mods...there should be some, one for each file written.
        lastBuild = now;
        Thread.sleep(100); //slight delay
        now = new Date();
        mods = fsystem.getModifications(lastBuild, now);
        assertNotNull(mods);
        assertEquals(2, mods.size());

        //Write some new files...
        tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        tempFile.deleteOnExit();
        writeContent(tempFile, "testing 3");
        tempFile.setLastModified(now.getTime() + 1);

        tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        tempFile.deleteOnExit();
        writeContent(tempFile, "testing 4");
        tempFile.setLastModified(now.getTime() + 1);

        tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        tempFile.deleteOnExit();
        writeContent(tempFile, "testing 5");
        tempFile.setLastModified(now.getTime() + 1);

        //Checking for mods again should turn up only the new files.
        lastBuild = now;
        Thread.sleep(100); //slight delay
        now = new Date();
        mods = fsystem.getModifications(lastBuild, now);
        assertNotNull(mods);
        assertEquals(3, mods.size());

        //Create one modified file.
        tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        tempFile.deleteOnExit();
        writeContent(tempFile, "testing 6");
        tempFile.setLastModified(now.getTime() + 1);

        //Checking for mods again should turn up only the one file
        lastBuild = now;
        Thread.sleep(100); //slight delay
        now = new Date();
        mods = fsystem.getModifications(lastBuild, now);
        assertNotNull(mods);
        assertEquals(1, mods.size());

        //Using this one mod, check the modification information for correctness.
        Modification modification = (Modification) mods.get(0);
        assertEquals(tempFile.getName(), modification.fileName);
        assertEquals(getDirectory(tempFile).getPath(), modification.folderName);
        assertEquals(tempFile.lastModified(), modification.modifiedTime.getTime());
    }

    private static File getDirectory(File file) {
        String absPath = file.getAbsolutePath();
        String dirPath = absPath.substring(0, absPath.lastIndexOf(File.separator));
        return new File(dirPath);
    }

    private static void writeContent(File file, String content)
            throws IOException {
        PrintWriter writer =
                new PrintWriter(new BufferedWriter(new FileWriter(file)));
        writer.print(content);
        writer.flush();
        writer.close();
    }
}