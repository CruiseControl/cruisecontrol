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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

import org.jdom.JDOMException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// TODO: Split this up into separate tests
public class SVNTest {
    private SVN svn;
    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        svn = new SVN();
        originalTimeZone = TimeZone.getDefault();
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test(expected = CruiseControlException.class)
    public void failsValidationWhenNoAttributesAreSet() throws CruiseControlException {
        svn.validate();
    }

    @Test
    public void validatesIfAtLeastRepositoryLocationSet() {
        svn.setRepositoryLocation("http://svn.collab.net/repos/svn");
        try {
            svn.validate();
        } catch (CruiseControlException e) {
            fail("should not throw an exception when at least the 'repositoryLocation' attribute is set");
        }
    }

    // TODO: validate the repository location
    //    @Test(expected = CruiseControlException.class)
    //    public void failsValidationForInvalidRepositoryLocation() throws CruiseControlException {
    //        svn.setRepositoryLocation("invalid repository location");
    //        svn.validate();
    //    }

    @Test(expected = CruiseControlException.class)
    public void failsValidationForInvalidLocalWorkingCopy() throws CruiseControlException {
        svn.setLocalWorkingCopy("invalid directory");
        svn.validate();
    }

    @Test
    public void validatesIfAtLeastLocalWorkingCopySet() {
        svn.setLocalWorkingCopy(".");
        try {
            svn.validate();
        } catch (CruiseControlException e) {
            fail("should not throw an exception when at least a valid 'localWorkingCopy' attribute is set");
        }
    }

    @Test(expected = CruiseControlException.class)
    public void failsValidationIfLocalWorkingCopySetToFileInsteadOfDirectory() throws CruiseControlException,
            IOException {

        File tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();

        svn.setLocalWorkingCopy(tempFile.getAbsolutePath());
        svn.validate();
    }

    @Test
    public void buildPropGetCommandWhereOnlyLocalWorkingCopySet() throws CruiseControlException {
        svn.setLocalWorkingCopy(".");

        String[] actualCommand = svn.buildPropgetCommand().getCommandline();
        String[] expectedCommand = new String[] { "svn", "propget", "-R", "--non-interactive", "svn:externals" };

        assertThat(actualCommand, equalTo(expectedCommand));
    }

    @Test
    public void buildPropGetCommandWhereOnlyRepositoryLocationSet() throws CruiseControlException {
        svn.setRepositoryLocation("http://svn.collab.net/repos/svn");

        String[] actualCommand = svn.buildPropgetCommand().getCommandline();
        String[] expectedCommand = new String[] { "svn", "propget", "-R", "--non-interactive", "svn:externals",
                "http://svn.collab.net/repos/svn" };

        assertThat(actualCommand, equalTo(expectedCommand));
    }

    @Test
    public void parsingTheResultsOfExecutingPropGetForSVNExternals() throws Exception {
        String testPropgetResult = ". - shared/build\tsvn://mybank.org/svnbank/trunk/java/shared/build\n"
                + "shared/tool/jfcunit_2.08\tsvn://mybank.org/svnbank/trunk/java/shared/tool/jfcunit_2.08\n"
                + "shared/lib/jnlp-1_2-dev\tsvn://mybank.org/svnbank/trunk/java/shared/lib/jnlp-1_2-dev\n";

        HashMap directories = new HashMap();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(testPropgetResult
                .getBytes("UTF-8")), "UTF-8"));
        try {
            SVN.parsePropgetReader(reader, directories);
        } finally {
            reader.close();
        }

        assertEquals(1, directories.keySet().size());

        String directory = (String) directories.keySet().iterator().next();
        List externals = (List) directories.get(directory);

        assertEquals("Wrong number of externals", 3, externals.size());

        String[] firstEntry = (String[]) externals.get(0);
        assertEquals("Wrong external: " + Arrays.asList(firstEntry).toString(), 2, firstEntry.length);
        assertEquals("Wrong externalSvnURL", "svn://mybank.org/svnbank/trunk/java/shared/build", firstEntry[1]);
    }

    @Test
    public void formattingSVNDate() {
        GregorianCalendar cal = new GregorianCalendar(2007, Calendar.JULY, 11, 12, 32, 45);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = cal.getTime();

        assertThat("Windows SVN date format does not match", SVN.formatSVNDate(date, true),
                equalTo("\"{2007-07-11T12:32:45Z}\""));
        assertThat("non-Windows SVN date format does not match", SVN.formatSVNDate(date, false),
                equalTo("{2007-07-11T12:32:45Z}"));
    }

    @Test
    public void testBuildHistoryCommand() throws CruiseControlException {
        svn.setLocalWorkingCopy(".");

        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        String[] expectedCmd = new String[] { "svn", "log", "--non-interactive", "--xml", "-v", "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false) };
        String[] actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false),
                SVN.formatSVNDate(checkTime, false)).getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));

        expectedCmd = new String[] { "svn", "log", "--non-interactive", "--xml", "-v", "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false), "external/path" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false), SVN.formatSVNDate(checkTime, false),
                "external/path").getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));

        svn.setRepositoryLocation("http://svn.collab.net/repos/svn");

        expectedCmd = new String[] { "svn", "log", "--non-interactive", "--xml", "-v", "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false), SVN.formatSVNDate(checkTime, false))
                .getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));

        expectedCmd = new String[] { "svn", "log", "--non-interactive", "--xml", "-v", "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false),
                "http://svn.collab.net/repos/external" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false), SVN.formatSVNDate(checkTime, false),
                "http://svn.collab.net/repos/external").getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));

        svn.setUsername("lee");
        svn.setPassword("secret");

        expectedCmd = new String[] { "svn", "log", "--non-interactive", "--xml", "-v", "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false), "--no-auth-cache",
                "--username", "lee", "--password", "secret", "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false), SVN.formatSVNDate(checkTime, false))
                .getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));

        svn.setUsername(null);
        svn.setPassword(null);
        final String testConfDir = "myConfigDir";
        svn.setConfigDir(testConfDir);
        expectedCmd = new String[] { "svn", "log", "--non-interactive", "--xml", "-v", "-r",
                SVN.formatSVNDate(lastBuild, false) + ":" + SVN.formatSVNDate(checkTime, false), "--config-dir",
                testConfDir, "http://svn.collab.net/repos/svn" };
        actualCmd = svn.buildHistoryCommand(SVN.formatSVNDate(lastBuild, false), SVN.formatSVNDate(checkTime, false))
                .getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));
    }

    @Test
    public void testParseModifications() throws JDOMException, ParseException, IOException {
        String svnLog = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + "<log>\n"
                + "  <logentry revision=\"663\">\n" + "    <author>lee</author>\n"
                + "    <date>2003-04-30T10:01:42.349105Z</date>\n" + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/aaa/ccc</path>\n"
                + "      <path action=\"M\">/trunk/playground/aaa/ccc/d.txt</path>\n"
                + "      <path action=\"A\">/trunk/playground/bbb</path>\n" + "    </paths>\n" + "    <msg>bli</msg>\n"
                + "  </logentry>\n" + "  <logentry revision=\"664\">\n" + "    <author>etienne</author>\n"
                + "    <date>2003-04-30T10:03:14.100900Z</date>\n" + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/aaa/f.txt</path>\n" + "    </paths>\n"
                + "    <msg>bla</msg>\n" + "  </logentry>\n" + "  <logentry revision=\"665\">\n"
                + "    <author>martin</author>\n" + "    <date>2003-04-30T10:04:48.050619Z</date>\n" + "    <paths>\n"
                + "      <path action=\"D\">/trunk/playground/bbb</path>\n" + "    </paths>\n" + "    <msg>blo</msg>\n"
                + "  </logentry>\n" + "</log>";

        Modification[] modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog));
        assertEquals(5, modifications.length);

        Modification modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"),
                "lee", "bli", "663", "", "/trunk/playground/aaa/ccc", "added");
        assertThat(modifications[0], equalTo(modification));

        modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"), "lee", "bli",
                "663", "", "/trunk/playground/aaa/ccc/d.txt", "modified");
        assertThat(modifications[1], equalTo(modification));

        modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"), "lee", "bli",
                "663", "", "/trunk/playground/bbb", "added");
        assertThat(modifications[2], equalTo(modification));

        modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:03:14.100"), "etienne", "bla",
                "664", "", "/trunk/playground/aaa/f.txt", "added");
        assertThat(modifications[3], equalTo(modification));

        modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:04:48.050"), "martin", "blo",
                "665", "", "/trunk/playground/bbb", "deleted");
        assertThat(modifications[4], equalTo(modification));

        modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog), "external/path");
        assertEquals(5, modifications.length);

        modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"), "lee", "bli",
                "663", "", "/external/path:/trunk/playground/aaa/ccc", "added");
        assertThat(modifications[0], equalTo(modification));

        modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog), null);
        assertEquals(5, modifications.length);
        modification = createModification(SVN.getOutDateFormatter().parse("2003-04-30T10:01:42.349"), "lee", "bli",
                "663", "", "/trunk/playground/aaa/ccc/d.txt", "modified");
        assertThat(modifications[1], equalTo(modification));
    }

    @Test(expected = ParseException.class)
    public void testConvertDateIllegalArgument() throws ParseException {
        SVN.SVNLogXMLParser.convertDate("2003-04-30T10:01:42.349105");
    }

    @Test
    public void testParseEmptyModifications() throws JDOMException, ParseException, IOException {
        String svnLog = "<?xml version=\"1.0\" encoding = \"ISO-8859-1\"?>\n " + "<log>\n" + "</log>";

        Modification[] modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog));
        assertEquals(0, modifications.length);
    }

    @Test
    public void testChangeWithoutReadAccessToChangedFileShouldResultInNoModificationReported() throws ParseException,
            JDOMException, IOException {
        String svnLog = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<log>\n"
                + "    <logentry revision=\"1234\">\n" + "        <msg></msg>\n" + "    </logentry>\n" + "</log>";
        Modification[] modifications = SVN.SVNLogXMLParser.parse(new StringReader(svnLog));

        assertEquals(0, modifications.length);
    }

    @Test
    public void testParseAndFilter() throws ParseException, JDOMException, IOException {
        String svnLog = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + "<log>\n"
                + "  <logentry revision=\"663\">\n" + "    <author>lee</author>\n"
                + "    <date>2003-08-02T10:01:13.349105Z</date>\n" + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/bbb</path>\n" + "    </paths>\n" + "    <msg>bli</msg>\n"
                + "  </logentry>\n" + "  <logentry revision=\"664\">\n" + "    <author>etienne</author>\n"
                + "    <date>2003-07-29T17:45:12.100900Z</date>\n" + "    <paths>\n"
                + "      <path action=\"A\">/trunk/playground/aaa/f.txt</path>\n" + "    </paths>\n"
                + "    <msg>bla</msg>\n" + "  </logentry>\n" + "  <logentry revision=\"665\">\n"
                + "    <author>martin</author>\n" + "    <date>2003-07-29T18:15:11.100900Z</date>\n" + "    <paths>\n"
                + "      <path action=\"D\">/trunk/playground/ccc</path>\n" + "    </paths>\n" + "    <msg>blo</msg>\n"
                + "  </logentry>\n" + "</log>";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0:00"));
        Date julyTwentynineSixPM2003 = new GregorianCalendar(2003, Calendar.JULY, 29, 18, 0, 0).getTime();

        List modifications = SVN.SVNLogXMLParser.parseAndFilter(new StringReader(svnLog), julyTwentynineSixPM2003);
        assertEquals(2, modifications.size());

        Modification modification = createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"),
                "lee", "bli", "663", "", "/trunk/playground/bbb", "added");
        assertThat((Modification) modifications.get(0), equalTo(modification));

        modification = createModification(SVN.getOutDateFormatter().parse("2003-07-29T18:15:11.100"), "martin", "blo",
                "665", "", "/trunk/playground/ccc", "deleted");
        assertThat((Modification) modifications.get(1), equalTo(modification));

        Date julyTwentyeightZeroPM2003 = new GregorianCalendar(2003, Calendar.JULY, 28, 0, 0, 0).getTime();

        modifications = SVN.SVNLogXMLParser.parseAndFilter(new StringReader(svnLog), julyTwentyeightZeroPM2003);
        assertEquals(3, modifications.size());

        modifications = SVN.SVNLogXMLParser.parseAndFilter(new StringReader(svnLog), julyTwentynineSixPM2003,
                "external/path");
        assertEquals(2, modifications.size());

        modification = createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"), "lee", "bli",
                "663", "", "/external/path:/trunk/playground/bbb", "added");
        assertThat((Modification) modifications.get(0), equalTo(modification));
    }

    @Test
    public void testFormatDatesForSvnLog() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));
        Date maySeventeenSixPM2001 = new GregorianCalendar(2001, Calendar.MAY, 17, 18, 0, 0).getTime();
        assertThat(SVN.formatSVNDate(maySeventeenSixPM2001, false), equalTo("{2001-05-17T08:00:00Z}"));

        Date maySeventeenEightAM2001 = new GregorianCalendar(2001, Calendar.MAY, 17, 8, 0, 0).getTime();
        assertThat(SVN.formatSVNDate(maySeventeenEightAM2001, false), equalTo("{2001-05-16T22:00:00Z}"));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));
        Date marchTwelfFourPM2003 = new GregorianCalendar(2003, Calendar.MARCH, 12, 16, 0, 0).getTime();
        assertThat(SVN.formatSVNDate(marchTwelfFourPM2003, false), equalTo("{2003-03-13T02:00:00Z}"));

        Date marchTwelfTenAM2003 = new GregorianCalendar(2003, Calendar.MARCH, 12, 10, 0, 0).getTime();
        assertThat(SVN.formatSVNDate(marchTwelfTenAM2003, false), equalTo("{2003-03-12T20:00:00Z}"));
    }

    @Test
    public void testSetProperty() throws ParseException {
        svn.setProperty("hasChanges?");

        List noModifications = new ArrayList();
        svn.fillPropertiesIfNeeded(noModifications);
        assertEquals(null, svn.getProperties().get("hasChanges?"));

        List hasModifications = new ArrayList();
        hasModifications.add(createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"), "lee",
                "bli", "333", "", "/trunk/playground/bbb", "deleted"));
        hasModifications.add(createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"), "lee",
                "bli", "663", "", "/trunk/playground/bbb", "added"));
        svn.fillPropertiesIfNeeded(hasModifications);
        Map properties = svn.getProperties();

        assertThat((String) properties.get("hasChanges?"), equalTo("true"));
        assertThat((String) properties.get("svnrevision"), equalTo("663"));
    }

    @Test
    public void testSetPropertyIgnoresPriorState() throws ParseException {
        testSetProperty();
        svn.fillPropertiesIfNeeded(new ArrayList());

        assertFalse(svn.getProperties().containsKey("hasChanges?"));
    }

    @Test
    public void testSetPropertyOnDelete() throws ParseException {
        svn.setPropertyOnDelete("hasDeletions?");

        List noModifications = new ArrayList();
        svn.fillPropertiesIfNeeded(noModifications);
        assertThat(svn.getProperties().get("hasDeletions?"), nullValue());

        List noDeletions = new ArrayList();
        noDeletions.add(createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"), "lee", "bli",
                "663", "", "/trunk/playground/bbb", "added"));
        svn.fillPropertiesIfNeeded(noDeletions);
        assertThat(svn.getProperties().get("hasDeletions?"), nullValue());

        List hasDeletions = new ArrayList();
        hasDeletions.add(createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"), "lee", "bli",
                "663", "", "/trunk/playground/aaa", "added"));
        hasDeletions.add(createModification(SVN.getOutDateFormatter().parse("2003-08-02T10:01:13.349"), "lee", "bli",
                "663", "", "/trunk/playground/bbb", "deleted"));
        svn.fillPropertiesIfNeeded(hasDeletions);

        assertThat((String) svn.getProperties().get("hasDeletions?"), equalTo("true"));
    }

    private static Modification createModification(Date date, String user, String comment, String revision,
            String folder, String file, String type) {
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

    @Test
    public void testParseInfo() throws JDOMException, IOException {
        String svnInfo = "<?xml version=\"1.0\"?>\n" + "<info>\n"
                + "<entry kind=\"dir\" path=\".\" revision=\"12345\">\n"
                + "<url>https://example.org/svn/playground-project</url>\n" + "<repository>\n"
                + "<root>https://example.org/svn</root>\n" + "<uuid>e6710e3c-8f79-4e94-9235-f6793330c154</uuid>\n"
                + "</repository>\n" + "<wc-info>\n" + "<schedule>normal</schedule>\n" + "</wc-info>\n"
                + "<commit revision=\"12345\">\n" + "<author>joebloggs</author>\n"
                + "<date>2007-07-11T08:31:58.089161Z</date>\n" + "</commit>\n" + "</entry>\n" + "</info>";
        String currentRevision = SVN.SVNInfoXMLParser.parse(new StringReader(svnInfo));
        
        assertThat(currentRevision, equalTo("12345"));
    }

    @Test
    public void testBuildInfoCommand() throws CruiseControlException {
        svn.setLocalWorkingCopy(".");
        String[] expectedCmd = { "svn", "info", "--xml" };
        String[] actualCmd = svn.buildInfoCommand(null).getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));

        expectedCmd = new String[] { "svn", "info", "--xml", "foo" };
        actualCmd = svn.buildInfoCommand("foo").getCommandline();
        assertThat(actualCmd, equalTo(expectedCmd));
    }
}
