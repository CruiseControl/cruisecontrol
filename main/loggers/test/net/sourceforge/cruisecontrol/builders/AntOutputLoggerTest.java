/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Date: Aug 22, 2007
 * Time: 5:52:24 PM
 */
public class AntOutputLoggerTest extends TestCase {
    private final File outputFile = new File(AntOutputLogger.DEFAULT_OUTFILE_NAME);

    protected void tearDown() throws Exception {
        outputFile.delete();
    }

    public void testShouldCreateNewFileIfItDoesNotAlreadyExist() throws Exception {
        outputFile.delete();
        assertFalse("Output file should not already exist", outputFile.exists());
        final AntOutputLogger logger = new AntOutputLogger();
        logger.printMessage("Message", null, 1);
        assertTrue("Output file should exist now", outputFile.exists());
    }

    public void testShouldWriteOutputToFile() throws Exception {
        AntOutputLogger logger = new AntOutputLogger();
        logger.printMessage("0", null, 1);
        logger.printMessage("1", null, 1);

        final String[] lines = fileLoad();
        assertEquals(2, lines.length);
        assertEquals("0", lines[0]);
        assertEquals("1", lines[1]);
    }

    public void testShouldRecreateOutputfileWhenCreatingNewLogger() throws Exception {
        AntOutputLogger logger = new AntOutputLogger();
        logger.printMessage("0", null, 1);

        String[] lines = fileLoad();
        assertEquals(1, lines.length);
        assertEquals("0", lines[0]);

        logger = new AntOutputLogger();
        logger.printMessage("1", null, 1);

        lines = fileLoad();
        assertEquals(1, lines.length);
        assertEquals("1", lines[0]);
    }

    public void testPrintMessageFileNotFoundExceptionWithNullStream() throws Exception {
        // create output file as a Directory to trigger Exception
        if (outputFile.exists()) {
            assertTrue(outputFile.delete());
        }
        assertFalse(outputFile.exists());

        final File dummyInDir = new File(outputFile.getAbsolutePath(), "dummyInDir");
        assertTrue(dummyInDir.mkdirs());
        assertTrue("delete dummy as dir", dummyInDir.delete());
        new FileOutputStream(dummyInDir).close();
        try {
            assertTrue(outputFile.exists());
            assertTrue(outputFile.isDirectory());

            AntOutputLogger logger = new AntOutputLogger();
            logger.printMessage("0", null, -1);

            String[] lines = fileLoad();
            assertEquals("FileNotFoundException leads to missed message, but does not fail build.", 0, lines.length);
        } finally {
            assertTrue("delete dummy as file", dummyInDir.delete());
            dummyInDir.deleteOnExit();    
        }
    }

    public void testPrintMessageFileNotFoundExceptionWithStream() throws Exception {
        // create output file as a Directory to trigger Exception
        if (outputFile.exists()) {
            assertTrue(outputFile.delete());
        }
        assertFalse(outputFile.exists());

        final File dummyInDir = new File(outputFile.getAbsolutePath(), "dummyInDir");
        assertTrue(dummyInDir.mkdirs());
        assertTrue("delete dummy as dir", dummyInDir.delete());
        new FileOutputStream(dummyInDir).close();
        try {
            assertTrue(outputFile.exists());
            assertTrue(outputFile.isDirectory());

            AntOutputLogger logger = new AntOutputLogger();

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream stream = new PrintStream(baos);
            logger.printMessage("0", stream, -1);

            String[] lines = fileLoad();
            assertEquals("FileNotFoundException leads to missed message, but does not fail build.", 0, lines.length);

            assertTrue(baos.toString().startsWith("Error ("));
        } finally {
            assertTrue("delete dummy as file", dummyInDir.delete());
            dummyInDir.deleteOnExit();
        }
    }


    private String[] fileLoad() {
        List result = new ArrayList();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(outputFile));

            String line = reader.readLine();
            while (line != null) {
                result.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

}
