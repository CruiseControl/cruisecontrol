/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BuildOutputBufferManagerTest extends TestCase {
    private BuildOutputLoggerManager loggerManager;
    private File tempFile;

    protected void setUp() throws Exception {
        loggerManager = new BuildOutputLoggerManager();
        tempFile = tempFile();
    }

    public void testShouldCreateLogger() throws Exception {
        BuildOutputLogger logger = loggerManager.lookupOrCreate("project1", tempFile);
        assertEquals(0, logger.retrieveLines(0).length);
        logger.consumeLine("1");
        logger.consumeLine("2");
        assertEquals(2, logger.retrieveLines(0).length);
        assertSame(logger,  loggerManager.lookup("project1"));
        assertSame(logger,  loggerManager.lookupOrCreate("project1", tempFile));
    }

    public void testShouldCreateTemporaryLoggerWhenLookingUpMissingLogger() throws Exception {
        BuildOutputLogger temporaryLogger = loggerManager.lookup("project2");
        BuildOutputLogger logger = loggerManager.lookupOrCreate("project2", tempFile);
        assertNotSame(temporaryLogger, logger);
        assertSame(logger, loggerManager.lookup("project2"));
        assertSame(logger, loggerManager.lookupOrCreate("project2", tempFile));
        assertEquals(0, temporaryLogger.retrieveLines(0).length);
    }
    
    public void testLoggersWithSameProjectSameFileShouldBeSame() throws IOException {
        File file = tempFile();
        File same = new File(file.getAbsolutePath());
        assertEquals(file, same);
        BuildOutputLogger logger = loggerManager.lookupOrCreate("project3", file);
        assertSame(logger, loggerManager.lookupOrCreate("project3", same));
    }
    
    public void testLoggersWithSameProjectDifferentFilesShouldBeSame() throws IOException {
        File file = tempFile();
        File different = tempFile();
        assertFalse(file.equals(different));
        BuildOutputLogger logger = loggerManager.lookupOrCreate("project4", file);
        assertSame(logger, loggerManager.lookupOrCreate("project4", different));
    }

    public void testLoggersWithDifferentProjectDifferentFilesShouldBeDifferent() throws IOException {
        File file = tempFile();
        File different = tempFile();
        assertFalse(file.equals(different));
        BuildOutputLogger logger = loggerManager.lookupOrCreate("project5", file);
        assertNotSame(logger, loggerManager.lookupOrCreate("project6", different));
    }

    public void testLookupOrCreateNull() throws Exception {
        final BuildOutputLogger lookupNull = loggerManager.lookup(null);
        assertFalse(lookupNull.isDataFileSet());

        final BuildOutputLogger lookupOrCreateNull = loggerManager.lookupOrCreate(null, null);
        assertFalse(lookupOrCreateNull.isDataFileSet());

        assertNotSame(lookupNull, lookupOrCreateNull);
    }

    public void testLoggersWithDifferentFilesConcurrentAccess() throws Exception {
        final File file = tempFile();
        final File different = tempFile();
        assertFalse(file.equals(different));
//        final BuildOutputLogger logger = loggerManager.lookupOrCreate(file);
//        final BuildOutputLogger logger2 = loggerManager.lookupOrCreate(different);

        final String suffixLookOrCreate = " line";
        final String suffixLook = " line lookup";

        final String tName1 = "T1";
        final String expectedLookOrCreateLineT1 = tName1 + suffixLookOrCreate;
        final String expectedLookLineT1 = tName1 + suffixLook;
        final Thread t = new Thread(tName1) {
            public void run() {
                while (true) {
                    loggerManager.lookupOrCreate(tName1, file).consumeLine(expectedLookOrCreateLineT1);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                    loggerManager.lookup(tName1).consumeLine(expectedLookLineT1);
                }
            }
        };

        final String tName2 = "T2";
        final String expectedLookOrCreateLineT2 = tName2 + suffixLookOrCreate;
        final String expectedLookLineT2 = tName2 + suffixLook;
        final Thread t2 = new Thread(tName2) {
            public void run() {
                while (true) {
                    loggerManager.lookupOrCreate(tName2, different).consumeLine(expectedLookOrCreateLineT2);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                    loggerManager.lookup(tName2).consumeLine(expectedLookLineT2);
                }
            }
        };

        t.start();
        t2.start();

        Thread.sleep(3000);

        t.interrupt();
        t2.interrupt();


        // read all lines into map to collection unique lines
        final String msgMissing = "BuildOutputLoggerManager missing expected output (likely a test timing issue).";
        final String msgMixed = "BuildOutputLoggerManager mixed up outputs from different builds into output file.";
        final BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            final Set<String> uniqueLinesFile = new HashSet<String>();
            String line;
            while ((line  = br.readLine()) != null) {
                uniqueLinesFile.add(line);
            }

            assertTrue(msgMissing, uniqueLinesFile.contains(expectedLookOrCreateLineT1));
            assertTrue(msgMissing, uniqueLinesFile.contains(expectedLookLineT1));

            assertFalse(msgMixed, uniqueLinesFile.contains(expectedLookOrCreateLineT2));
            assertFalse(msgMixed, uniqueLinesFile.contains(expectedLookLineT2));
        } finally {
            br.close();
        }

        final BufferedReader br2 = new BufferedReader(new FileReader(different));
        try {
            final Set<String> uniqueLinesFile2 = new HashSet<String>();
            String line;
            while ((line  = br2.readLine()) != null) {
                uniqueLinesFile2.add(line);
            }

            assertFalse(msgMixed, uniqueLinesFile2.contains(expectedLookOrCreateLineT1));
            assertFalse(msgMixed, uniqueLinesFile2.contains(expectedLookLineT1));

            assertTrue(msgMissing, uniqueLinesFile2.contains(expectedLookOrCreateLineT2));
            assertTrue(msgMissing, uniqueLinesFile2.contains(expectedLookLineT2));
        } finally {
            br.close();
        }
    }


    private File tempFile() throws IOException {
        final File file = File.createTempFile("tempOutputlogger", ".tmp");
        file.deleteOnExit();
        return file;
    }
}
