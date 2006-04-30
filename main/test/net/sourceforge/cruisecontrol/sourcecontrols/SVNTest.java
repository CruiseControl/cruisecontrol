/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * @see    <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
 * @author <a href="etienne.studer@canoo.com">Etienne Studer</a>
 */
public class SVNTest extends TestCase {
    private SVN svn;
    private TimeZone originalTimeZone;

    protected void setUp() throws Exception {
        svn = new SVN();
        originalTimeZone = TimeZone.getDefault();
    }

    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
        svn = null;
        originalTimeZone = null;
    }

    public void testValidate() throws CruiseControlException, IOException {
        try {
            svn.validate();
            fail("should throw an exception when no attributes are set");
        } catch (CruiseControlException e) {
            // expected
        }

        svn.setRepositoryLocation("http://svn.collab.net/repos/svn");
        try {
            svn.validate();
        } catch (CruiseControlException e) {
            fail(
                "should not throw an exception when at least the 'repositoryLocation' attribute "
                    + "is set");
        }

        svn = new SVN();
        svn.setLocalWorkingCopy("invalid directory");
        try {
            svn.validate();
            fail("should throw an exception when an invalid 'localWorkingCopy' attribute is set");
        } catch (CruiseControlException e) {
            // expected
        }

        File tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();

        svn = new SVN();
        svn.setLocalWorkingCopy(tempFile.getParent());
        try {
            svn.validate();
        } catch (CruiseControlException e) {
            fail(
                "should not throw an exception when at least a valid 'localWorkingCopy' "
                    + "attribute is set");
        }

        svn = new SVN();
        svn.setLocalWorkingCopy(tempFile.getAbsolutePath());
        try {
            svn.validate();
            fail("should throw an exception when 'localWorkingCopy' is file instead of directory.");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testBuildHistoryCommand() throws CruiseControlException {
        svn.setLocalWorkingCopy(".");

        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        String[] expectedCmd =
            new String[] {
                "svn",
                "log",
                "--non-interactive",
                "--xml",
                "-v",
                "-r",
                "{" + SVN.formatSVNDate(lastBuild) + "}:{" + SVN.formatSVNDate(checkTime) + "}"};
        String[] actualCmd = svn.buildHistoryCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        svn.setRepositoryLocation("http://svn.collab.net/repos/svn");

        expectedCmd =
            new String[] {
                "svn",
                "log",
                "--non-interactive",
                "--xml",
                "-v",
                "-r",
                "{" + SVN.formatSVNDate(lastBuild) + "}:{" + SVN.formatSVNDate(checkTime) + "}",
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        svn.setUsername("lee");
        svn.setPassword("secret");

        expectedCmd =
            new String[] {
                "svn",
                "log",
                "--non-interactive",
                "--xml",
                "-v",
                "-r",
                "{" + SVN.formatSVNDate(lastBuild) + "}:{" + SVN.formatSVNDate(checkTime) + "}",
                "--username",
                "lee",
                "--password",
                "secret",
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);
    }

    public void testParseModifications() throws JDOMException, ParseException, IOException {
        String svnLog =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                + "<log>\n"
                + "  <logentry revision=\"663\">\n"
                + "    <author>lee</author>\n"
                + "    <date>2003-04-30T10:01:42.349105Z</date>\n"
                + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/aaa/ccc</path>\n"
                + "      <path action=\"M\">/trunk/playground/aaa/ccc/d.txt</path>\n"
                + "      <path action=\"A\">/trunk/playground/bbb</path>\n"
                + "    </paths>\n"
                + "    <msg>bli</msg>\n"
                + "  </logentry>\n"
                + "  <logentry revision=\"664\">\n"
                + "    <author>etienne</author>\n"
                + "    <date>2003-04-30T10:03:14.100900Z</date>\n"
                + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/aaa/f.txt</path>\n"
                + "    </paths>\n"
                + "    <msg>bla</msg>\n"
                + "  </logentry>\n"
                + "  <logentry revision=\"665\">\n"
                + "    <author>martin</author>\n"
                + "    <date>2003-04-30T10:04:48.050619Z</date>\n"
                + "    <paths>\n"
                + "      <path action=\"D\">/trunk/playground/bbb</path>\n"
                + "    </paths>\n"
                + "    <msg>blo</msg>\n"
                + "  </logentry>\n"
                + "</log>";

        Modification[] modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog));
        assertEquals(5, modifications.length);

        Modification modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/aaa/ccc",
                "added");
        assertEquals(modification, modifications[0]);

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/aaa/ccc/d.txt",
                "modified");
        assertEquals(modification, modifications[1]);

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/bbb",
                "added");
        assertEquals(modification, modifications[2]);

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-04-30T10:03:14.100"),
                "etienne",
                "bla",
                "664",
                "",
                "/trunk/playground/aaa/f.txt",
                "added");
        assertEquals(modification, modifications[3]);

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-04-30T10:04:48.050"),
                "martin",
                "blo",
                "665",
                "",
                "/trunk/playground/bbb",
                "deleted");
        assertEquals(modification, modifications[4]);
    }

    public void testConvertDateIllegalArgument() {
        try {
            Date d = SVN.SVNLogXMLParser.convertDate("2003-04-30T10:01:42.349105");
            fail("expected ParseException for date without Z but got " + d);
        } catch (ParseException e) {
            assertTrue(true);
        }
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

    public void testParseAndFilter() throws ParseException, JDOMException, IOException {
        String svnLog =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                + "<log>\n"
                + "  <logentry revision=\"663\">\n"
                + "    <author>lee</author>\n"
                + "    <date>2003-08-02T10:01:13.349105Z</date>\n"
                + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/bbb</path>\n"
                + "    </paths>\n"
                + "    <msg>bli</msg>\n"
                + "  </logentry>\n"
                + "  <logentry revision=\"664\">\n"
                + "    <author>etienne</author>\n"
                + "    <date>2003-07-29T17:45:12.100900Z</date>\n"
                + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/aaa/f.txt</path>\n"
                + "    </paths>\n"
                + "    <msg>bla</msg>\n"
                + "  </logentry>\n"
                + "  <logentry revision=\"665\">\n"
                + "    <author>martin</author>\n"
                + "    <date>2003-07-29T18:15:11.100900Z</date>\n"
                + "    <paths>\n"
                + "      <path action=\"D\">/trunk/playground/ccc</path>\n"
                + "    </paths>\n"
                + "    <msg>blo</msg>\n"
                + "  </logentry>\n"
                + "</log>";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0:00"));
        Date julyTwentynineSixPM2003 =
            new GregorianCalendar(2003, Calendar.JULY, 29, 18, 0, 0).getTime();

        List modifications = SVN.SVNLogXMLParser.parseAndFilter(new StringReader(svnLog), julyTwentynineSixPM2003);
        assertEquals(2, modifications.size());

        Modification modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/bbb",
                "added");
        assertEquals(modification, modifications.get(0));

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-07-29T18:15:11.100"),
                "martin",
                "blo",
                "665",
                "",
                "/trunk/playground/ccc",
                "deleted");
        assertEquals(modification, modifications.get(1));

        Date julyTwentyeightZeroPM2003 =
                new GregorianCalendar(2003, Calendar.JULY, 28, 0, 0, 0).getTime();

        modifications = SVN.SVNLogXMLParser.parseAndFilter(new StringReader(svnLog), julyTwentyeightZeroPM2003);
        assertEquals(3, modifications.size());
    }

    public void testFormatDatesForSvnLog() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));

        Date maySeventeenSixPM2001 =
            new GregorianCalendar(2001, Calendar.MAY, 17, 18, 0, 0).getTime();
        assertEquals(
            "2001-05-17T08:00:00Z",
            SVN.formatSVNDate(maySeventeenSixPM2001));

        Date maySeventeenEightAM2001 =
            new GregorianCalendar(2001, Calendar.MAY, 17, 8, 0, 0).getTime();
        assertEquals(
            "2001-05-16T22:00:00Z",
            SVN.formatSVNDate(maySeventeenEightAM2001));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));

        Date marchTwelfFourPM2003 =
            new GregorianCalendar(2003, Calendar.MARCH, 12, 16, 0, 0).getTime();
        assertEquals(
            "2003-03-13T02:00:00Z",
            SVN.formatSVNDate(marchTwelfFourPM2003));

        Date marchTwelfTenAM2003 =
            new GregorianCalendar(2003, Calendar.MARCH, 12, 10, 0, 0).getTime();
        assertEquals("2003-03-12T20:00:00Z", SVN.formatSVNDate(marchTwelfTenAM2003));
    }

    public void testSetProperty() throws ParseException {
        svn.setProperty("hasChanges?");

        List noModifications = new ArrayList();
        svn.fillPropertiesIfNeeded(noModifications);
        assertEquals(null, svn.getProperties().get("hasChanges?"));

        List hasModifications = new ArrayList();
        hasModifications.add(createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/bbb",
                "added"));
        svn.fillPropertiesIfNeeded(hasModifications);
        assertEquals("true", svn.getProperties().get("hasChanges?"));
    }

    public void testSetPropertyOnDelete() throws ParseException {
        svn.setPropertyOnDelete("hasDeletions?");

        List noModifications = new ArrayList();
        svn.fillPropertiesIfNeeded(noModifications);
        assertEquals(null, svn.getProperties().get("hasDeletions?"));

        List noDeletions = new ArrayList();
        noDeletions.add(createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/bbb",
                "added"));
        svn.fillPropertiesIfNeeded(noDeletions);
        assertEquals(null, svn.getProperties().get("hasDeletions?"));

        List hasDeletions = new ArrayList();
        hasDeletions.add(createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/aaa",
                "added"));
        hasDeletions.add(createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/bbb",
                "deleted"));
        svn.fillPropertiesIfNeeded(hasDeletions);
        assertEquals("true", svn.getProperties().get("hasDeletions?"));
    }


    private static Modification createModification(
        Date date,
        String user,
        String comment,
        String revision,
        String folder,
        String file,
        String type) {
        Modification modification = new Modification("svn");
        Modification.ModifiedFile modifiedFile = modification.createModifiedFile(file, folder);
        modifiedFile.action = type;
        modifiedFile.revision = revision;

        modification.modifiedTime = date;
        modification.userName = user;
        modification.comment = comment;
        modification.revision = revision;
        return modification;
    }

    private static void assertArraysEquals(Object[] expected, Object[] actual) {
        assertEquals("array lengths mismatch!", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
}
