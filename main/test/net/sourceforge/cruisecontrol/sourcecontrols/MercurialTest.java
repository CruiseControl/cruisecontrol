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
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import org.jdom.JDOMException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class MercurialTest extends TestCase {
    private Mercurial mercurial;
    private TimeZone originalTimeZone;

    protected void setUp() throws Exception {
        mercurial = new Mercurial();
        originalTimeZone = TimeZone.getDefault();
    }

    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
    }

    public void testBuildHistoryCommand() throws CruiseControlException {
        mercurial.setLocalWorkingCopy(".");
        String[] expectedCmd =
            new String[] {
                "hg",
                "incoming",
                "--debug",
                "--template",
                Mercurial.INCOMING_XML_TEMPLATE
            };
        String[] actualCmd = mercurial.buildHistoryCommand().getCommandline();
        TestUtil.assertArray("", expectedCmd, actualCmd);
    }

    public void testParseModifications() throws JDOMException, ParseException, IOException {
        BufferedInputStream input = new BufferedInputStream(loadTestLog("mercurial_incoming_xml_debug.txt"));
        List modifications = Mercurial.parseStream(input);
        input.close();

        assertEquals("Should have returned 7 modifications.", 7, modifications.size());

        Modification modification =
            createModification(
                getOutDateFormatter().parse("2007-08-27 16:11:19 +0200"),
                "ET4642@localhost",
                "Test of a fourth commit",
                "3:bb0a5f00315f4f6dddb7362a69bc0910b07c4faa",
                "",
                "file1.txt",
                "modified");
        assertEquals(modification, modifications.get(0));

        modification =
            createModification(
                getOutDateFormatter().parse("2007-08-29 21:38:18 +0200"),
                "ET4642@edbwp000856.edb.local",
                "Changed 2 files",
                "4:1da89ee88532fddb21235b2d21e4a46424adbe39",
                "",
                "file1.txt",
                "modified");
        assertEquals(modification, modifications.get(1));

        modification =
            createModification(
                getOutDateFormatter().parse("2007-08-29 21:38:18 +0200"),
                "ET4642@edbwp000856.edb.local",
                "Changed 2 files",
                "4:1da89ee88532fddb21235b2d21e4a46424adbe39",
                "",
                "file2.txt",
                "modified");
        assertEquals(modification, modifications.get(2));

        modification =
            createModification(
                getOutDateFormatter().parse("2007-08-29 21:44:37 +0200"),
                "ET4642@edbwp000856.edb.local",
                "new change, with one directory depth",
                "5:93981bd125719a98d7c11028560b2b736b3f12ec",
                "",
                "file2.txt",
                "modified");
        assertEquals(modification, modifications.get(3));


        modification =
            createModification(
                getOutDateFormatter().parse("2007-08-29 21:44:37 +0200"),
                "ET4642@edbwp000856.edb.local",
                "new change, with one directory depth",
                "5:93981bd125719a98d7c11028560b2b736b3f12ec",
                "",
                "mydir/newfile.txt",
                "added");
        assertEquals(modification, modifications.get(4));

        modification =
            createModification(
                getOutDateFormatter().parse("2007-08-30 09:09:08 +0200"),
                "ET4642@localhost",
                "removed one file, changed one",
                "6:e3cefb520ddc6c7de8bad83f17df4b3d4194fe08",
                "",
                "file2.txt",
                "removed");
        assertEquals(modification, modifications.get(5));

        modification =
            createModification(
                getOutDateFormatter().parse("2007-08-30 09:09:08 +0200"),
                "ET4642@localhost",
                "removed one file, changed one",
                "6:e3cefb520ddc6c7de8bad83f17df4b3d4194fe08",
                "",
                "file1.txt",
                "modified");
        assertEquals(modification, modifications.get(6));

    }

    // 2007-08-29 21:44:19 +0200
    private static final class Iso8601DateParser {
        private Iso8601DateParser() { }
        public static final SimpleDateFormat ISO8601_DATE_PARSER = new SimpleDateFormat("yyyy-MM-d HH:mm:ss Z");

        private static Date parse(String date) throws ParseException {
            return ISO8601_DATE_PARSER.parse(date);
        }
    }

    public static DateFormat getOutDateFormatter() {
        return Iso8601DateParser.ISO8601_DATE_PARSER;
    }


    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }

    public void testParseEmptyModifications() throws JDOMException, ParseException, IOException {
        String svnLog =
            "<?xml version=\"1.0\" encoding = \"ISO-8859-1\"?>\n " + "<log>\n" + "</log>";

        Modification[] modifications =  SVN.SVNLogXMLParser.parse(new StringReader(svnLog));
        assertEquals(0, modifications.length);
    }

    public void testChangeWithoutReadAccessToChangedFileShouldResultInNoModificationReported()
          throws ParseException, JDOMException, IOException {
        String svnLog = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                           + "<log>\n"
                           + "    <logentry revision=\"1234\">\n"
                           + "        <msg></msg>\n"
                           + "    </logentry>\n"
                           + "</log>";
        Modification[] modifications =  SVN.SVNLogXMLParser.parse(new StringReader(svnLog));
        assertEquals(0, modifications.length);
    }

    public void testFormatDatesForSvnLog() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));

        Date maySeventeenSixPM2001 =
            new GregorianCalendar(2001, Calendar.MAY, 17, 18, 0, 0).getTime();
        assertEquals(
            "{2001-05-17T08:00:00Z}",
            SVN.formatSVNDate(maySeventeenSixPM2001, false));

        Date maySeventeenEightAM2001 =
            new GregorianCalendar(2001, Calendar.MAY, 17, 8, 0, 0).getTime();
        assertEquals(
            "{2001-05-16T22:00:00Z}",
            SVN.formatSVNDate(maySeventeenEightAM2001, false));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));

        Date marchTwelfFourPM2003 =
            new GregorianCalendar(2003, Calendar.MARCH, 12, 16, 0, 0).getTime();
        assertEquals(
            "{2003-03-13T02:00:00Z}",
            SVN.formatSVNDate(marchTwelfFourPM2003, false));

        Date marchTwelfTenAM2003 =
            new GregorianCalendar(2003, Calendar.MARCH, 12, 10, 0, 0).getTime();
        assertEquals("{2003-03-12T20:00:00Z}", SVN.formatSVNDate(marchTwelfTenAM2003, false));
    }

    private static Modification createModification(
        Date date,
        String user,
        String comment,
        String revision,
        String folder,
        String file,
        String type) {
        Modification modification = new Modification("mercurial");
        Modification.ModifiedFile modifiedFile = modification.createModifiedFile(file, folder);
        modifiedFile.action = type;
        modifiedFile.revision = revision;

        modification.modifiedTime = date;
        modification.userName = user;
        modification.comment = comment;
        modification.revision = revision;
        return modification;
    }
}