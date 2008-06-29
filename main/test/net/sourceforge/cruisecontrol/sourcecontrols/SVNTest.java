/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Calendar;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


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
    }

    public void testValidate() throws IOException {
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

    public void testBuildPropgetCommand() throws CruiseControlException {
        svn.setLocalWorkingCopy(".");

        String[] expectedCmd =
            new String[] {
                "svn",
                "propget",
                "-R",
                "--non-interactive",
                "svn:externals" };
        String[] actualCmd = svn.buildPropgetCommand().getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        svn.setRepositoryLocation("http://svn.collab.net/repos/svn");

        expectedCmd =
            new String[] {
                "svn",
                "propget",
                "-R",
                "--non-interactive",
                "svn:externals",
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildPropgetCommand().getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);
    }

    public void testParsePropgetReader() throws Exception {
        final String testPropgetResult = ". - shared/build\tsvn://mybank.org/svnbank/trunk/java/shared/build\n"
                + "shared/tool/jfcunit_2.08\tsvn://mybank.org/svnbank/trunk/java/shared/tool/jfcunit_2.08\n"
                + "shared/lib/jnlp-1_2-dev\tsvn://mybank.org/svnbank/trunk/java/shared/lib/jnlp-1_2-dev\n";

        final HashMap directories = new HashMap();
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(testPropgetResult.getBytes("UTF-8")), "UTF-8"));
        try {
            SVN.parsePropgetReader(reader, directories);
        } finally {
            reader.close();
        }
        assertEquals(1, directories.keySet().size());
        final String directory = (String) directories.keySet().iterator().next();
        final ArrayList externals = (ArrayList) directories.get(directory);
        assertEquals("Wrong number of externals", 3, externals.size());

        assertEquals("Wrong external: " + Arrays.asList(((String[]) externals.get(0))).toString(),
                2, ((String[]) externals.get(0)).length);

        assertEquals("Wrong externalSvnURL",
                "svn://mybank.org/svnbank/trunk/java/shared/build",
                ((String[]) externals.get(0))[1]);
    }

    public void testFormatSVNDateForWindows() {
        GregorianCalendar cal = new GregorianCalendar(2007, Calendar.JULY, 11, 12, 32, 45);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = cal.getTime();
        
        assertEquals("\"{2007-07-11T12:32:45Z}\"", SVN.formatSVNDate(date, true));
    }

    public void testFormatSVNDateForNonWindows() {
        GregorianCalendar cal = new GregorianCalendar(2007, Calendar.JULY, 11, 12, 32, 45);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = cal.getTime();
        
        assertEquals("{2007-07-11T12:32:45Z}", SVN.formatSVNDate(date, false));
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
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false)};
        String[] actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false)).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        expectedCmd =
            new String[] {
                "svn",
                "log",
                "--non-interactive",
                "--xml",
                "-v",
                "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "external/path"};
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false), "external/path").getCommandline();
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
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false)).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        expectedCmd =
            new String[] {
                "svn",
                "log",
                "--non-interactive",
                "--xml",
                "-v",
                "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "http://svn.collab.net/repos/external"};
        actualCmd = svn.buildHistoryCommand(
                SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false), "http://svn.collab.net/repos/external").getCommandline();
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
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "--no-auth-cache",
                "--username",
                "lee",
                "--password",
                "secret",
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false)).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);


        svn.setUsername(null);
        svn.setPassword(null);
        final String testConfDir = "myConfigDir";
        svn.setConfigDir(testConfDir);
        expectedCmd =
            new String[] {
                "svn",
                "log",
                "--non-interactive",
                "--xml",
                "-v",
                "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "--config-dir",
                testConfDir,
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false)).getCommandline();
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

        modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog), "external/path");
        assertEquals(5, modifications.length);

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"),
                "lee",
                "bli",
                "663",
                "",
                "/external/path:/trunk/playground/aaa/ccc",
                "added");
        assertEquals(modification, modifications[0]);

        modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog), null);
        assertEquals(5, modifications.length);
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

        modifications = SVN.SVNLogXMLParser.parseAndFilter(
            new StringReader(svnLog), julyTwentynineSixPM2003, "external/path");
        assertEquals(2, modifications.size());

        modification =
            createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/external/path:/trunk/playground/bbb",
                "added");
        assertEquals(modification, modifications.get(0));
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
                "333",
                "",
                "/trunk/playground/bbb",
                "deleted"));
        hasModifications.add(createModification(
                SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee",
                "bli",
                "663",
                "",
                "/trunk/playground/bbb",
                "added"));
        svn.fillPropertiesIfNeeded(hasModifications);
        Map properties = svn.getProperties();
        assertEquals("true", properties.get("hasChanges?"));
        assertEquals("663", properties.get("svnrevision"));
    }

    public void testSetPropertyIgnoresPriorState() throws ParseException {
        testSetProperty();
        svn.fillPropertiesIfNeeded(new ArrayList());
        assertFalse(svn.getProperties().containsKey("hasChanges?"));

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
        assertEquals("array lengths mismatch! was " + Arrays.asList(actual), expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
    
    public void testParseInfo() throws JDOMException, IOException {
        String svnInfo = "<?xml version=\"1.0\"?>\n"
            + "<info>\n"
            + "<entry kind=\"dir\" path=\".\" revision=\"12345\">\n"
            + "<url>https://example.org/svn/playground-project</url>\n"
            + "<repository>\n"
            + "<root>https://example.org/svn</root>\n"
            + "<uuid>e6710e3c-8f79-4e94-9235-f6793330c154</uuid>\n"
            + "</repository>\n"
            + "<wc-info>\n"
            + "<schedule>normal</schedule>\n"
            + "</wc-info>\n"
            + "<commit revision=\"12345\">\n"
            + "<author>joebloggs</author>\n"
            + "<date>2007-07-11T08:31:58.089161Z</date>\n"
            + "</commit>\n"
            + "</entry>\n"
            + "</info>";
        String currentRevision = SVN.SVNInfoXMLParser.parse(new StringReader(svnInfo));
        assertEquals("12345", currentRevision);
    }

    public void testBuildInfoCommand() throws CruiseControlException {
        svn.setLocalWorkingCopy(".");
        String[] expectedCmd = { "svn", "info", "--xml" };
        String[] actualCmd = svn.buildInfoCommand(null).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        expectedCmd = new String[] { "svn", "info", "--xml", "foo" };
        actualCmd = svn.buildInfoCommand("foo").getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);
    }
}
