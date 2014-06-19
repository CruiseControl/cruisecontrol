/********************************************************************************
 *
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
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.testutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;


/**
 * The extension of junit.framework.TestCase providing some more useful assert and
 * tool methods.
 * 
 * @author dtihelka
 *
 */
public class TestCase  extends junit.framework.TestCase {

    /**
     * Method reading two files, comparing one against the another.
     *
     * @param  refrFile reference file.
     * @param  testFile tested file.
     * @throws IOException if files cannot be handled.
     */
    public static void assertFiles(File refrFile, File testFile)
        throws IOException {

        /* Both files must exist */
        assertTrue("Reference file " + refrFile, refrFile.exists());
        assertTrue("Tested file " + testFile, refrFile.exists());
        /* Test streams */
        assertStreams(new FileInputStream(refrFile), new FileInputStream(testFile));
    }

    /**
     * Method reading two streams, comparing one against the another. As text files are expected
     * under the streams, {@link BufferedReader} class is used to read from the streams and the
     * lines are compared, actually.
     *
     * @param  refrStream reference stream.
     * @param  testStream tested stream.
     * @throws IOException if streams cannot be handled.
     */
    public static void assertStreams(InputStream refrStream, InputStream testStream)
        throws IOException {
        assertReaders(new InputStreamReader(refrStream), new InputStreamReader(testStream));
    }

    /**
     * Method reading two readers, comparing one against the another. As text files are expected
     * under the streams, the individual lines are compared, actually.
     *
     * @param  refr reference reader.
     * @param  test tested reader.
     * @throws IOException if streams cannot be handled.
     */
    public static void assertReaders(Reader refr, Reader test)
        throws IOException {

        /* Create readers */
        final BufferedReader refrReader = new BufferedReader(refr);
        final BufferedReader testReader = new BufferedReader(test);
        int numLinesRead  = 0;
        /* Read and compare line by line */
        while (true) {
            String refrLine = refrReader.readLine();
            String testLine = testReader.readLine();

            /* Leave if one of them is Null */
            if (refrLine == null && testLine == null) {
                break;
            }
            /* Compare lines */
            assertEquals("Line " + ++numLinesRead, refrLine, testLine);
        }
        /* Close them */
        refrReader.close();
        testReader.close();
    }
    
    /**
     * Method comparing actual string with the required string represented as regular
     * expression. It is almost equal to {@link #assertEquals(String, String, String)} with the
     * difference that required string can be set as regular expression.
     *
     * @param message what is printed in case of failure
     * @param expected the expected format of the message (as regular expression)
     * @param actual actual message to check.
     */
    public static void assertRegex(String message, String expected, String actual)
    {
        if (Pattern.matches(expected, actual)) {
            return;
        }
        /* Not passed - how to print expected/actual message? */
        assertEquals(message, "regex[" + expected + "]", actual);
    }
    
}
