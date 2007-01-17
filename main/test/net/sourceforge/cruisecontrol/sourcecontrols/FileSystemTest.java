/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
import net.sourceforge.cruisecontrol.util.IO;

import java.io.File;
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
            assertEquals("'folder' is required for FileSystem", e.getMessage());
        }

        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        //Create a subdirectory in the temp directory for us to use.
        tempDirectory = new File(tempDirectory, "filesystemtest2" + System.currentTimeMillis());
        fs.setFolder(tempDirectory.getAbsolutePath());

         try {
             fs.validate();
            fail("FileSystem should throw exceptions when folder doesn't exist.");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().indexOf("must exist") > -1);
        }

        assertTrue(tempDirectory.mkdir());

        try {
            fs.validate();
        } catch (CruiseControlException e) {
            fail("FileSystem should not throw exceptions when required attributes are set.");
        } finally {
            IO.delete(tempDirectory);
        }
    }

    public void testGettingModifications() throws Exception {
        //Figure out where the temp directory is...
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));

        //Create a subdirectory in the temp directory for us to use.
        tempDirectory = new File(tempDirectory,
                "filesystemtest" + System.currentTimeMillis());
        assertTrue(tempDirectory.mkdir());

        try {
            //Setup a filesystem element that points at our test subdirectory...
            FileSystem fsystem = new FileSystem();
            fsystem.setFolder(tempDirectory.getAbsolutePath());

            //Check for modifications...there shouldn't be any
            Date startTime = new GregorianCalendar(2000, 0, 1).getTime();
            Date timeOne = new Date(startTime.getTime() + 2000);
            Date timeTwo = new Date(timeOne.getTime() + 2000);
            Date timeThree = new Date(timeTwo.getTime() + 2000);
            List mods = fsystem.getModifications(startTime, timeOne);
            assertNotNull(mods);
            assertEquals(0, mods.size());

            //Write some files...
            File tempFile;

            tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
            IO.write(tempFile, "testing");
            tempFile.setLastModified(timeOne.getTime());

            tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
            IO.write(tempFile, "testing 2");
            tempFile.setLastModified(timeOne.getTime());

            //Check for mods...there should be some, one for each file written.
            mods = fsystem.getModifications(startTime, timeOne);
            assertNotNull(mods);
            assertEquals(2, mods.size());

            //Write some new files...
            tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
            IO.write(tempFile, "testing 3");
            tempFile.setLastModified(timeTwo.getTime());

            tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
            IO.write(tempFile, "testing 4");
            tempFile.setLastModified(timeTwo.getTime());

            tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
            IO.write(tempFile, "testing 5");
            tempFile.setLastModified(timeTwo.getTime());

            //Checking for mods again should turn up only the new files.
            mods = fsystem.getModifications(timeOne, timeTwo);
            assertNotNull(mods);
            assertEquals(3, mods.size());

            //Create one modified file.
            tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
            IO.write(tempFile, "testing 6");
            tempFile.setLastModified(timeThree.getTime());

            //Checking for mods again should turn up only the one file
            mods = fsystem.getModifications(timeTwo, timeThree);
            assertNotNull(mods);
            assertEquals(1, mods.size());

            //Using this one mod, check the modification information for correctness.
            Modification modification = (Modification) mods.get(0);
            assertEquals(tempFile.getName(), modification.getFileName());
            assertEquals(tempFile.getParent(), modification.getFolderName());
            assertEquals(tempFile.lastModified(), modification.modifiedTime.getTime());
        } finally {
            IO.delete(tempDirectory);
        }
    }
}