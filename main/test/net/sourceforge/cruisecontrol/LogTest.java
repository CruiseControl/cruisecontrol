/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;


public class LogTest extends TestCase {
    private final List filesToClear = new ArrayList();

    public void tearDown() {
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void testCreatingLog() {
        //Cannot create a Log instance with a null project name
        try {
            new Log(null);
            fail("Expected an exception when creating a Log instance with "
                    + "a null Project name.");
        } catch (NullPointerException npe) {
            //Good, expected this exception.
        }
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

    public void testParseDateFromLogFileName() throws ParseException {
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
        String[] encodings = { "UTF-8", "ISO-8859-1", null };

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        //XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        XMLOutputter outputter = new XMLOutputter("  ", true);
        outputter.setTrimAllWhite(true);
        for (int i = 0;  i < encodings.length; i++) {
            Log log = new Log("testXMLEncoding");
            if (encodings[i] != null) {
                log.setLogXmlEncoding(encodings[i]);
            }

            // Add a minimal buildLog
            log.addContent(getBuildLogInfo());
            Element build = new Element("build");
            log.addContent(build);
            log.addContent(new Element("modifications"));

            // Add 8-bit characters 
            build.setText("Something with special characters: \u00c6\u00d8\u00c5");

            // Write and read the file
            log.writeLogFile(new Date());
            File logFile = log.getLastLogFile();
            filesToClear.add(logFile);
            Element actualContent = builder.build(logFile).getRootElement();

            // content.toString() only returns the root element but not the
            // children: [Element: <cruisecontrol/>] 
            // Use an XMLOutputter (that trims whitespace) instead.
            String expected = outputter.outputString(log.getContent());
            String actual = outputter.outputString(actualContent);
            assertEquals(expected, actual);
        }
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
