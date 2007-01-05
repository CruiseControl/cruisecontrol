/****************************************************************************
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import java.util.Arrays;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;

public class ZipUtilTest extends TestCase {
    
    private static final Logger LOG = Logger.getLogger(ZipUtilTest.class);
    private static final String TEMP_FILE = "data.txt";
    private String rootTempDirPath;
    private File rootTempDir;
    private String filledDirName = "filled";
    private String filledDirPath;
    private File filledDir;
    private String emptyDirName = "empty";
    private String emptyDirPath;
    private File emptyDir;

    public void setUp() {
        try {
            final File dummyTempFile = File.createTempFile("temp", "txt");
            dummyTempFile.delete();
            rootTempDir = new File(new File(dummyTempFile.getParent()), "testRoot");
            rootTempDirPath = rootTempDir.getCanonicalPath();
            rootTempDir.mkdir();
            filledDir = new File(rootTempDirPath, filledDirName);
            filledDir.mkdir();
            filledDirPath = filledDir.getCanonicalPath();
            emptyDir = new File(rootTempDirPath, emptyDirName);
            emptyDir.mkdir();
            emptyDirPath = emptyDir.getCanonicalPath();
        } catch (IOException ioe) {
            String message = "Failed to find temp directory location";
            LOG.error(message);
            System.err.println(message);
        }

        final String tempFile = TEMP_FILE;
        final Writer writer;
        try {
            filledDirPath = rootTempDirPath + File.separator + filledDirName;
            writer = new FileWriter(new File(filledDirPath, tempFile));
            writer.write("The quick brown fox, yadda, yadda, yadda...");
            writer.close();
        } catch (IOException ioe) {
            String message = "Failed to create temp file " + tempFile + " at " + filledDirPath;
            LOG.error(message, ioe);
            System.err.println(message);
        }
    }

    public void tearDown() {
        IO.delete(rootTempDir);
        if (rootTempDir.exists()) {
            final String msg = "Delete file failed: rootTempDir: " + rootTempDir.getAbsolutePath()
                    + "\n\tContents:\n"
                    + (rootTempDir.listFiles() != null ? Arrays.asList(rootTempDir.listFiles()) : null);
            System.out.println(msg);
        }

    }

    public void testSetup() {
        assertTrue(rootTempDir.exists());
        assertTrue(rootTempDir.isDirectory());

        assertTrue(filledDir.exists());
        assertTrue(filledDir.isDirectory());

        assertTrue(emptyDir.exists());
        assertTrue(emptyDir.isDirectory());

        File tempFile = new File(filledDirPath, TEMP_FILE);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.isFile());

        IO.delete(rootTempDir);
        File tempDir = new File(rootTempDirPath);
        assertFalse(tempDir.exists());
    }

    public void testEmptyZip() {
        String emptyZipFilePath = rootTempDir + File.separator + "empty.zip";
        ZipUtil.zipFolderContents(emptyZipFilePath, emptyDirPath);
        File emptyZipFile = new File(emptyZipFilePath);

        assertFalse(emptyZipFile.exists());  // @todo Should empty zips be created?
    }

    public void testZipWithIllegalArguments() {
        try {
            ZipUtil.zipFolderContents(null, filledDirName);
            fail("Should throw an exception since zip filename and/or dir is missing");
        } catch (java.lang.IllegalArgumentException e) {
            assertEquals("Missing output zip file name", e.getMessage());
        }

        try {
            ZipUtil.zipFolderContents("blech.zip", null);
            fail("Should throw an exception since zip filename and/or dir is missing");
        } catch (java.lang.IllegalArgumentException e) {
            assertEquals("Missing folder to zip", e.getMessage());
        }
    }

    public void testUnzip() {
        String zipFilePath = null;
        String dirToZip = null;
        try {
            zipFilePath = new File(rootTempDirPath, "temp.zip").getCanonicalPath();
            dirToZip = new File(filledDirPath).getCanonicalPath();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        ZipUtil.zipFolderContents(zipFilePath, dirToZip);
        final File zipFile = new File(zipFilePath);
        assertTrue(zipFile.exists());
        assertTrue(zipFile.isFile());
        assertTrue(zipFile.length() > 0);


        final File unzipDir = new File(rootTempDirPath, "unzip");
        unzipDir.mkdir();
        assertTrue(unzipDir.exists());
        File unzippedFile = null;
        try {
            ZipUtil.unzipFileToLocation(zipFilePath, unzipDir.getCanonicalPath());
            unzippedFile = new File(unzipDir.getCanonicalPath(), "data.txt");
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        assertTrue(unzippedFile.exists());
        BufferedReader reader;
        String line = "";
        try {
            reader = new BufferedReader(new FileReader(unzippedFile));
            line = reader.readLine();
            reader.close();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        String expectedLine = "The quick brown fox, yadda, yadda, yadda...";
        assertEquals(expectedLine, line);
    }

    /**
     * Test method - zips logs from directory specified by cruise.run.dir
     * property in cruise.properties file to cruise-log.zip file in system
     * default temp file location (i.e. C:\Documents and Settings\ <user>\Local
     * Settings\Temp on Windows XP) then unzips files into same temp directory.
     * 
     * *WARNING* Could be a large bit of data--you might plan to manually delete
     * the results...
     * @param args not used
     * @throws IOException if file IO has problems
     */
    public static void main(String[] args) throws IOException {
        Properties properties = (Properties) PropertiesHelper.loadRequiredProperties("cruise.properties");
        File tempDir = new File(File.createTempFile("temp", "txt").getParent());

        String zipFile = new File(tempDir.getCanonicalPath(), "cruise-logs.zip").getCanonicalPath();
        ZipUtil.zipFolderContents(zipFile, new File(properties.getProperty("cruise.run.dir"), "logs")
                .getCanonicalPath());

        ZipUtil.unzipFileToLocation(zipFile, tempDir.getCanonicalPath());
    }
}
