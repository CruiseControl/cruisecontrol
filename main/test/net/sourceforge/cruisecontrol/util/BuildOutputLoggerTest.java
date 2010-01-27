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
package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class BuildOutputLoggerTest extends TestCase {

    public void testShouldReturnEmptyArrayWhenFileIsEmpty() throws Exception {

        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(0));

        assertEquals(0, logger.retrieveLines(0).length);

    }

    public void testShouldReturnAllLinesFromFirstLine() throws Exception {

        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(3));

        final String[] lines = logger.retrieveLines(0);

        assertEquals(3, lines.length);
        assertEquals("1", lines[0]);
        assertEquals("2", lines[1]);
        assertEquals("3", lines[2]);
    }

    public void testShouldReturnAllLinesFromStartLine() throws Exception {

        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(7));

        final String[] lines = logger.retrieveLines(4);

        assertEquals(3, lines.length);
        assertEquals("5", lines[0]);
        assertEquals("7", lines[2]);
    }

    public void testShouldReturnAllLinesAcrossWrap() throws Exception {

        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(13));

        final String[] lines = logger.retrieveLines(9);

        assertEquals(4, lines.length);
        assertEquals("10", lines[0]);
        assertEquals("11", lines[1]);
        assertEquals("12", lines[2]);
        assertEquals("13", lines[3]);

    }

    public void testShouldRetrieveNothingAfterClearingBuffer() throws Exception {
        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(6));


        assertEquals(6, logger.retrieveLines(0).length);
        logger.clear();
        assertEquals(0, logger.retrieveLines(0).length);
    }

    public void testShouldLoadBufferFromFileWhenFilePresentAndLinesRetrieved() throws Exception {
        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(6));

        final String[] lines = logger.retrieveLines(0);
        assertEquals(6, lines.length);
        assertEquals("1", lines[0]);
        assertEquals("2", lines[1]);
        assertEquals("3", lines[2]);
    }

    public void testShouldOnlyLoadNewLinesFromFile() throws Exception {
        final File tempFile = prepareBufferFile(6);
        final BuildOutputLogger logger = new BuildOutputLogger(tempFile);

        assertEquals(6, logger.retrieveLines(0).length);
        assertEquals(6, logger.retrieveLines(0).length);
        addLineToFile(tempFile);
        assertEquals(7, logger.retrieveLines(0).length);
    }

    public void testShouldNotFailIfFileDoesNotExist() throws Exception {
        final BuildOutputLogger logger = new BuildOutputLogger(new File("notexists.tmp"));
        assertEquals(0, logger.retrieveLines(0).length);
    }

    public void testShouldThrowExceptionIfOutfileDoesNotExistWhenConsuming() throws Exception {
        final BuildOutputLogger logger = new BuildOutputLogger(null);
        try {
            logger.consumeLine("should fail");
            fail("Should not be able to consume a line when no log file specified");
        } catch (Exception expected) {
            assertEquals("No log file specified", expected.getMessage());
        }
    }

    public void testShouldWriteToOutfileWhenConsumingLine() throws Exception {
        final BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(0));
        logger.consumeLine("one");
        final String[] lines = logger.retrieveLines(0);
        assertEquals(1, lines.length);
        assertEquals("one", lines[0]);
    }

    private void addLineToFile(final File file) throws FileNotFoundException {
        final PrintStream out = new PrintStream(new FileOutputStream(file, true));
        try {
            out.println("1");
        } finally {
            out.close();
        }
    }

    private File prepareBufferFile(final int count) throws IOException {
        final File tempFile = File.createTempFile("bufferload-test", ".tmp");
        tempFile.deleteOnExit();

        final PrintStream out = new PrintStream(new FileOutputStream(tempFile));
        try {
            for (int i = 0; i < count; i++) {
                out.println(1 + i);
            }
        } finally {
            out.close();
        }
        return tempFile;
    }
}
