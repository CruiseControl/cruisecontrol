/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.logmanipulators.DeleteManipulator;
import net.sourceforge.cruisecontrol.logmanipulators.GZIPManipulator;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


public class LogTest extends TestCase {
    private final FilesToDelete filesToDelete = new FilesToDelete();
    private static final String LOG_DIR = "LogTest";

    protected void setUp() {
        filesToDelete.add(new File(TestUtil.getTargetDir(), LOG_DIR));
    }

    protected void tearDown() {
        filesToDelete.delete();
    }

    public void testSetProjectNameShouldThrowIAEIfPassedNull() {
        Log log = new Log();
        try {
            log.setProjectName(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("projectName can't be null", expected.getMessage());
        }
    }

    public void testValidateShouldFailWhenProjectNameNotSet() {
        Log log = new Log();
        try {
            log.validate();
            fail();
        } catch (CruiseControlException expected) {
            assertEquals("projectName must be set", expected.getMessage());
        }
    }

    public void testDefaultLogLocation() {
        Log log = new Log();
        log.setProjectName("foo");
        assertEquals("logs" + File.separatorChar + "foo", log.getLogDir());
    }

    public void testFormatLogFileName() {
        Calendar augTweleveCalendar = Calendar.getInstance();
        augTweleveCalendar.set(2004, 7, 12, 1, 1, 1);
        Date augTweleve = augTweleveCalendar.getTime();

        String expected = "log20040812010101.xml";
        String actual = Log.formatLogFileName(augTweleve);
        assertEquals(
            expected + "--" + actual,
            expected, actual);
        assertEquals("log20040812010101Lbuild.1.xml", Log.formatLogFileName(augTweleve, "build.1"));
    }

    public void testWasSuccessfulBuild() {
        assertTrue(Log.wasSuccessfulBuild("log20040812010101Lbuild.1.xml"));
        assertFalse(Log.wasSuccessfulBuild("log20040812010101.xml"));
        assertFalse(Log.wasSuccessfulBuild(null));
    }

    public void testParseDateFromLogFileName() throws CruiseControlException {
        Calendar augTweleveCalendar = Calendar.getInstance();
        augTweleveCalendar.set(2004, 7, 12, 1, 1, 1);
        Date augTweleve = augTweleveCalendar.getTime();

        assertEquals(augTweleve.toString(), Log.parseDateFromLogFileName("log20040812010101Lbuild.1.xml").toString());
        assertEquals(augTweleve.toString(), Log.parseDateFromLogFileName("log20040812010101.xml").toString());
    }

    public void testParseLabelFromLogFileName() {
        assertEquals("build.1", Log.parseLabelFromLogFileName("log20040812010101Lbuild.1.xml"));
        assertEquals("", Log.parseLabelFromLogFileName("log20040812010101.xml"));
    }

    public void testXMLEncoding()
            throws CruiseControlException, IOException, JDOMException {

        final String[] encodings = { "UTF-8", "ISO-8859-1", null };

        final SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        for (final String encoding : encodings) {
            final Log log = new Log();
            log.setProjectName("testXMLEncoding");
            log.setDir(LOG_DIR);
            if (encoding != null) {
                log.setEncoding(encoding);
            }
            log.validate();
            // log.validate() creates a new dir, so make sure we leave no side effects and delete this dir
            filesToDelete.add(new File(log.getLogDir()));

            // Add a minimal buildLog
            log.addContent(getBuildLogInfo());
            final Element build = new Element("build");
            log.addContent(build);
            log.addContent(new Element("modifications"));

            // Add 8-bit characters
            build.setText("Something with special characters: \u00c6\u00d8\u00c5");

            final Date now = new Date();
            // Write and read the file
            log.writeLogFile(now);

            final String expectFilename = "log" + DateUtil.getFormattedTime(now) + "L.xml";
            final File logFile = new File(LOG_DIR, expectFilename);
            assertTrue(logFile.isFile());
            filesToDelete.add(logFile);

            final Element actualContent = builder.build(logFile).getRootElement();

            // content.toString() only returns the root element but not the
            // children: [Element: <cruisecontrol/>]
            // Use an XMLOutputter (that trims whitespace) instead.
            final String expected = outputter.outputString(log.getContent());
            final String actual = outputter.outputString(actualContent);
            assertEquals(expected, actual);
        }
    }

    /**
     * Asserts that leading and trailing whitespace in CDATA elements is preserved.
     *
     * @throws Exception if test fails
     */
    public void testXMLWhitespacePreservation() throws Exception {
        final String testString = "   la dee da\t";

        // test default
        assertXMLWhiteSpacePreservation(testString, testString, new Log());

        final Log logTrimmed = new Log();
        logTrimmed.setTrimWhitespace(true);
        assertXMLWhiteSpacePreservation(testString.trim(), testString, logTrimmed);

        final Log logWhitespacePreserved = new Log();
        logWhitespacePreserved.setTrimWhitespace(false);
        assertXMLWhiteSpacePreservation(testString, testString, logWhitespacePreserved);
    }

    private void assertXMLWhiteSpacePreservation(final String expectedLogText, final String testString, final Log log)
            throws Exception {

        log.setProjectName(getName());
        log.setDir(LOG_DIR);
        log.addContent(getBuildLogInfo());

        final Element buildElem = new Element("build");
        log.addContent(buildElem);
        final Element msgElem = new Element("message");
        msgElem.setAttribute("priority", "info");
        msgElem.addContent(new CDATA(testString));
        buildElem.addContent(msgElem);

        log.validate();
        // log.validate() creates a new dir, so make sure we leave no side effects and delete this dir
        filesToDelete.add(new File(log.getLogDir()));

        final Date now = new Date();
        log.writeLogFile(now);
        final String expectFilename = "log" + DateUtil.getFormattedTime(now) + "L.xml";
        final File logFile = new File(LOG_DIR, expectFilename);
        assertTrue(logFile.isFile());

        Element elem = new SAXBuilder("org.apache.xerces.parsers.SAXParser").build(logFile).getRootElement();
        elem = elem.getChild("build");
        elem = elem.getChild("message");
        final CDATA cdata = (CDATA) elem.getContent(0);

        assertEquals(expectedLogText, cdata.getText());
    }

    public void testManipulateLog() throws Exception {
        final String testProjectName = "testBackupLog";

        // Test backup of 12 Months
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, date.get(Calendar.YEAR) - 1);
        date.set(Calendar.MONTH, date.get(Calendar.MONTH) - 13);
        GZIPManipulator gzip = new GZIPManipulator();
        gzip.setEvery(12);
        gzip.setUnit("month");
        // create old log
        getWrittenTestLog(testProjectName, date.getTime());
        // create new log
        Log log = getWrittenTestLog(testProjectName, new Date());
        log.add(gzip);
        log.validate();
        assertBackupsHelper(log, 2, 1, 1);

        // Test Backup of 2 days
        date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH) - 2);
        gzip = new GZIPManipulator();
        gzip.setEvery(2);
        gzip.setUnit("day");
        log = getWrittenTestLog(testProjectName, date.getTime());
        log.add(gzip);
        log.validate();
        assertBackupsHelper(log, 3, 1, 2);

        // Test delete of logfile
        date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH) - 2);
        DeleteManipulator deleteManipulator = new DeleteManipulator();
        deleteManipulator.setEvery(2);
        deleteManipulator.setUnit("day");
        log = getWrittenTestLog(testProjectName, date.getTime());
        log.add(deleteManipulator);
        log.validate();
        assertBackupsHelper(log, 3, 1, 2);

        date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH) - 2);
        deleteManipulator = new DeleteManipulator();
        deleteManipulator.setEvery(2);
        deleteManipulator.setUnit("day");
        // This should delete the gz-files too
        deleteManipulator.setIgnoreSuffix(true);
        log = getWrittenTestLog(testProjectName, date.getTime());
        log.add(deleteManipulator);
        log.validate();
        assertBackupsHelper(log, 1, 1, 0);

        //Validation Error
        gzip = new GZIPManipulator();
        gzip.setUnit("day");
        log = getWrittenTestLog(testProjectName, date.getTime());
        log.add(gzip);
        try {
            log.validate();
            fail("Validation should fail!");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }


    }

    private void assertBackupsHelper(final Log log,
                                     final int expectedLength, final int expectedXML, final int expectedGZIP) {
        log.callManipulators();
        final File[] logfiles = new File(log.getLogDir()).listFiles(new FilenameFilter() {

            public boolean accept(final File file, final String fileName) {
                return fileName.startsWith("log20")
                        && (fileName.endsWith(".xml") || fileName
                                .endsWith(".gz"));
            }

        });
        int countGzip = 0;
        int countXML = 0;
        for (final File file : logfiles) {
            if (file.getName().endsWith(".gz")) {
                filesToDelete.add(file);
                countGzip++;
            } else if (file.getName().endsWith(".xml")) {
                countXML++;
            } else {
                fail("Other log files exists");
            }
        }
        assertEquals("Wrong number of gzip log files after manipulation", expectedGZIP, countGzip);
        assertEquals("Wrong number of xml log files after manipulation", expectedXML, countXML);
        assertEquals("Wrong total number of log files after manipulation", expectedLength, logfiles.length);
    }

    private Log getWrittenTestLog(final String projectName, final Date date)
            throws CruiseControlException, JDOMException, IOException {


        final Log log = new Log();
        log.setProjectName(projectName);
        log.setDir(LOG_DIR);

        log.validate();
        // log.validate() creates a new dir, so make sure we leave no side effects and delete this dir
        filesToDelete.add(new File(log.getLogDir()));

        log.addContent(getBuildLogInfo());
        final Element build = new Element("build");
        log.addContent(build);
        log.addContent(new Element("modifications"));
        log.writeLogFile(date);

        final String expectFilename = "log" + DateUtil.getFormattedTime(date) + "L.xml";
        final File logFile = new File(LOG_DIR, expectFilename);
        filesToDelete.add(logFile);

        assertTrue(logFile.isFile());

        return log;
    }

    // Get a minimal info element for the buildLog
    private Element getBuildLogInfo() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        String infoXML = "<info><property name=\"label\" value=\"\"/>"
                + "<property name=\"lastbuildtime\" value=\"\"/>"
                + "<property name=\"lastgoodbuildtime\" value=\"\"/>"
                + "<property name=\"lastbuildsuccessful\" value=\"\"/>"
                + "<property name=\"buildfile\" value=\"\"/>"
                + "<property name=\"buildtarget\" value=\"\"/>"
                + "</info>";
        Element info = builder.build(new StringReader(infoXML)).getRootElement();
        return (Element) info.clone();
    }
}
