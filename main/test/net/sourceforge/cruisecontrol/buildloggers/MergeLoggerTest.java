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
import net.sourceforge.cruisecontrol.util.IO;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class MergeLoggerTest extends TestCase {
    private MergeLogger logger;
    private File tempSubdir;
    private Element log;
    private XMLOutputter outputter = new XMLOutputter();

    private static final String BASIC_LOG_CONTENT = "<cruisecontrol></cruisecontrol>";

    protected void setUp() throws Exception {
        logger = new MergeLogger();
        log = getBasicLog();

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempSubdir = new File(tempDir, "cruisecontroltest" + System.currentTimeMillis());
        tempSubdir.mkdir();
    }

    protected void tearDown() throws Exception {
        logger = null;
        IO.delete(tempSubdir);
        tempSubdir = null;
        log = null;
    }

    public void testFilePatternValidation() throws Exception {
        logger.setDir("temp");
        logger.setPattern(null);
        try {
            logger.validate();
            fail("Expected an exception because we have not specified a pattern.");
        } catch (CruiseControlException expected) {
            assertEquals("no file pattern was specified", expected.getMessage());
        }

        logger.setPattern("[a-z*");
        try {
            logger.validate();
            fail("Expected an exception because an invalid pattern.");
        } catch (CruiseControlException expected) {
            String expect = "Invalid filename pattern";
            assertEquals(expect, expected.getMessage().substring(0, expect.length()));
        }

        logger.setPattern("*.xml");
        logger.validate();
    }

    public void testMergingFile() throws Exception {
        String content = "<name>John Doe</name>";
        File fileToMerge = createFile(content);

        logger.setFile(fileToMerge.getAbsolutePath());
        logger.validate();
        logger.log(log);

        String expected = "<cruisecontrol>" + content + "</cruisecontrol>";
        String actual = outputter.outputString(log);
        assertEquals(expected, actual);
    }

    public void testMergingDirectory() throws Exception {
        createFile("<test1>pass</test1>");
        createFile("<test2>pass</test2>");

        //Merge the xml files from the subdirectory.
        logger.setDir(tempSubdir.getAbsolutePath());
        logger.validate();
        logger.log(log);

        //Since the order isn't guaranteed, the expected value is one of two things
        String expected1 = "<cruisecontrol><test1>pass</test1><test2>pass</test2></cruisecontrol>";
        String expected2 = "<cruisecontrol><test2>pass</test2><test1>pass</test1></cruisecontrol>";

        String actual = outputter.outputString(log);
        assertEqualsEither(expected1, expected2, actual);
    }

    public void testValidation() throws CruiseControlException {
        try {
            logger.validate();
            fail("Expected an exception because we didn't set a file or directory.");
        } catch (CruiseControlException expected) {
            assertEquals("one of file or dir are required attributes", expected.getMessage());
        }

        logger.setDir("temp");
        logger.setFile("tempfile.xml");
        try {
            logger.validate();
            fail("Expected an exception because we set a file and a directory.");
        } catch (CruiseControlException expected) {
            assertEquals("only one of file or dir may be specified", expected.getMessage());
        }

        logger.setDir(null);
        logger.validate();

        logger.setDir("temp");
        logger.setFile(null);
        logger.validate();
    }

    public void testGetElement() throws IOException, CruiseControlException {
        String withProperties = "<testsuite><properties /></testsuite>";
        String withoutProperties = "<testsuite />";

        File with = createFile(withProperties);
        File without = createFile(withoutProperties);

        Element elementWith = logger.getElement(with);
        Element elementWithout = logger.getElement(without);

        String actualWith = outputter.outputString(elementWith);
        String actualWithout = outputter.outputString(elementWithout);

        assertEquals(withoutProperties, actualWithout);
        assertEquals(withoutProperties, actualWith);

        logger.setRemoveProperties(false);

        elementWith = logger.getElement(with);
        elementWithout = logger.getElement(without);

        actualWith = outputter.outputString(elementWith);
        actualWithout = outputter.outputString(elementWithout);

        assertEquals(withoutProperties, actualWithout);
        assertEquals(withProperties, actualWith);
    }

    private Element getBasicLog() throws JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(BASIC_LOG_CONTENT)).getRootElement();
    }

    private File createFile(String content) throws IOException, CruiseControlException {
        File fileToMerge = File.createTempFile(MergeLoggerTest.class.getName(), ".xml", tempSubdir);
        IO.write(fileToMerge, content);
        return fileToMerge;
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

}
