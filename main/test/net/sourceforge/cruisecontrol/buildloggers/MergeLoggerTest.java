/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.buildloggers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Util;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

public class MergeLoggerTest extends TestCase {

    private static final String BASIC_LOG_CONTENT =
            "<cruisecontrol></cruisecontrol>";

    private File directoryToDelete;

    protected void tearDown() throws Exception {
        super.tearDown();

        Util.deleteFile(directoryToDelete);
    }

    public void testMergingFile() throws Exception {
        Element basicLog = getBasicLog();

        //Create a temp file, and write some XML to it so that it will
        //  be merged.
        File tempFile = File.createTempFile(
                MergeLoggerTest.class.getName(), "testfile");
        tempFile.deleteOnExit();

        String content = "<name>John Doe</name>";
        writeFileContents(tempFile, content);

        MergeLogger logger = new MergeLogger();
        logger.setFile(tempFile.getAbsolutePath());
        logger.validate();
        logger.log(basicLog);

        //See if the merge worked...
        String expected = "<cruisecontrol>" + content + "</cruisecontrol>";
        XMLOutputter outputter = new XMLOutputter();
        String actual = outputter.outputString(basicLog);
        assertEquals(expected, actual);
    }

    public void testMergingDirectory() throws Exception {
        Element basicLog = getBasicLog();

        //Find the system temp directory.
        File tempFile = File.createTempFile(
                MergeLoggerTest.class.getName(), "testfile");
        final String tempFileAbsPath = tempFile.getAbsolutePath();
        String systemTempDir = tempFileAbsPath.substring(
                0, tempFileAbsPath.lastIndexOf(File.separator));

        //Create a subdirectory and write some files to it.
        File tempSubdir = new File(systemTempDir,
                "cruisecontroltest" + System.currentTimeMillis());
        tempSubdir.mkdir();
        directoryToDelete = tempSubdir; //Will be deleted in tearDown

        File file1 = new File(tempSubdir, System.currentTimeMillis() + Math.random() + ".xml");
        writeFileContents(file1, "<test1>pass</test1>");

        File file2 = new File(tempSubdir, System.currentTimeMillis() + Math.random() + ".xml");
        writeFileContents(file2, "<test2>pass</test2>");

        //Merge the xml files from the subdirectory.
        MergeLogger logger = new MergeLogger();
        logger.setDir(tempSubdir.getAbsolutePath());
        logger.validate();
        logger.log(basicLog);

        //Since the order isn't guaranteed, the expected value is one of two
        //  things
        String expected1 = "<cruisecontrol><test1>pass</test1><test2>pass</test2></cruisecontrol>";
        String expected2 = "<cruisecontrol><test2>pass</test2><test1>pass</test1></cruisecontrol>";

        XMLOutputter outputter = new XMLOutputter();
        String actual = outputter.outputString(basicLog);
        assertEqualsEither(expected1, expected2, actual);
    }

    /**
     * Asserts that the actual equals either expected1 or expected2.
     */
    private void assertEqualsEither(String expected1, String expected2, String actual) {
        if (!expected1.equals(actual) && !expected2.equals(actual)) {
            fail("Expected either [" + expected1 + "] or [" + expected2
                    + "], but was [" + actual + "].");
        }
    }

    public void testValidation() throws CruiseControlException {
        //Should get an exception if we don't set anything
        MergeLogger logger = new MergeLogger();
        try {
            logger.validate();
            fail("Expected an exception because"
                    + " we didn't set a file or directory.");
        } catch (CruiseControlException expected) {
            //Good, expected this exception
        }

        //Should get an exception if we set both a directory and a file.
        logger.setDir("temp");
        logger.setFile("tempfile.xml");
        try {
            logger.validate();
            fail("Expected an exception because"
                    + " we set a file and a directory.");
        } catch (CruiseControlException expected) {
            //Good, expected this exception
        }

        //With just a file or a directory set...all should be well.
        logger.setDir(null);
        logger.validate();

        logger.setDir("temp");
        logger.setFile(null);
        logger.validate();
    }

    private Element getBasicLog() throws JDOMException {
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(
                new StringReader(BASIC_LOG_CONTENT)).getRootElement();
    }

    private static void writeFileContents(File theFile, String contents)
            throws IOException {

        FileWriter fw = new FileWriter(theFile);
        fw.write(contents);
        fw.close();
    }
}
