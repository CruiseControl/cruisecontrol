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
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.MockCommandline;
import net.sourceforge.cruisecontrol.util.OSEnvironment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;


/**
 * @author Robert Watkins
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class CVSTest extends TestCase {
    private TimeZone originalTimeZone;
    static final String[] CVS_VERSION_COMMANDLINE = new String[]{"cvs", "version"};

    protected void setUp() throws Exception {
        originalTimeZone = TimeZone.getDefault();
    }

    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
        originalTimeZone = null;
    }

    private Date parseCVSDateFormat(String dateString) throws ParseException {
        return CVS.CVSDATE.parse(dateString);
    }

    private Date parseLogDateFormat(String dateString) throws ParseException {
        return CVS.LOGDATE.parse(dateString);
    }

    /**
     * Mocks a CVS class by returning a specific CVS version
     */
    static class MockVersionCVS extends CVS {
        private final Version vers;

        public MockVersionCVS(Version cvsVersion) {
            this.vers = cvsVersion;
        }

        protected Version getCvsServerVersion() {
            return vers;
        }
    }

    private CVS.Version getOfficialCVSVersion(final String cvsVersion) {
        return new CVS.Version(CVS.OFFICIAL_CVS_NAME, cvsVersion);
    }

    /**
     * Overrides the getCommandLine() method by returnning a MockCommandLine whose
     * process input stream will read its contents from the specific input stream.
     */
    static class InputBasedCommandLineMockCVS extends CVS {
        private final InputStream inputStream;
        private final String[] expectedCommandline;
        private final String expectedWorkingDirectory;

        public InputBasedCommandLineMockCVS(final InputStream inputStream,
                                            final String[] expectedCommandLine,
                                            final String expectedWorkingDirectory) {
            this.inputStream = inputStream;
            expectedCommandline = expectedCommandLine;
            this.expectedWorkingDirectory = expectedWorkingDirectory;
        }

        // factory method for mock...
        protected Commandline getCommandline() {
            final MockCommandline mockCommandline = new MockCommandline();
            mockCommandline.setExpectedCommandline(expectedCommandline);
            mockCommandline.setExpectedWorkingDirectory(expectedWorkingDirectory);
            // could System.in and System.out create problems here?
            mockCommandline.setProcessErrorStream(System.in);
            mockCommandline.setProcessInputStream(inputStream);
            mockCommandline.setProcessOutputStream(System.out);
            return mockCommandline;
        }
    };



    public void testValidate() throws IOException {
        CVS cvs = new CVS();

        OSEnvironment env = new OSEnvironment();
        if (env.getVariable("CVSROOT") == null) {
            try {
                cvs.validate();
                fail("CVS should throw exceptions when required fields are not set.");
            } catch (CruiseControlException e) {
            }
        }

        cvs.setCvsRoot("cvsroot");

        try {
            cvs.validate();
            fail("CVS should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        cvs.setModule("module");

        try {
            cvs.validate();
        } catch (CruiseControlException e) {
            fail("CVS should not throw exceptions when required fields are set.");
        }

        cvs.setCvsRoot(null);

        try {
            cvs.validate();
            fail("CVS should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        cvs = new CVS();
        File tempFile = File.createTempFile("temp", ".txt");
        cvs.setLocalWorkingCopy(tempFile.getParent());
        tempFile.delete();

        try {
            cvs.validate();
        } catch (CruiseControlException e) {
            fail("CVS should not throw exceptions when required fields are set.");
        }

        cvs.setModule("module");

        try {
            cvs.validate();
            fail("CVS should not throw exceptions when excluding fields are set.");
        } catch (CruiseControlException e) {
        }

        cvs.setModule(null);

        String badDirName = "z:/foo/foo/foo/bar";
        cvs.setLocalWorkingCopy(badDirName);
        try {
            cvs.validate();
            fail("CVS.validate should throw exception on non-existant directory.");
        } catch (CruiseControlException e) {
        }
    }

    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }

    public void testParseStream() throws IOException, ParseException {
        // ensure CVS version and simulated outputs are in sync
        final String cvsVersion = "1.11.16";
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion(cvsVersion));
        Hashtable emailAliases = new Hashtable();
        emailAliases.put("alden", "alden@users.sourceforge.net");
        emailAliases.put("tim", "tim@tim.net");
        cvs.setMailAliases(emailAliases);

        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("cvslog1-11.txt"));
        List modifications = cvs.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals("Should have returned 5 modifications.",
                5,
                modifications.size());

        Modification mod1 = new Modification("cvs");
        Modification.ModifiedFile mod1file = mod1.createModifiedFile("log4j.properties", null);
        mod1file.action = "modified";
        mod1.revision = "1.2";
        mod1.modifiedTime = parseLogDateFormat("2002/03/13 13:45:50 GMT-6:00");
        mod1.userName = "alden";
        mod1.comment =
                "Shortening ConversionPattern so we don't use up all of the available screen space.";
        mod1.emailAddress = "alden@users.sourceforge.net";

        Modification mod2 = new Modification("cvs");
        Modification.ModifiedFile mod2file = mod2.createModifiedFile("build.xml", null);
        mod2file.action = "modified";
        mod2.revision = "1.41";
        mod2.modifiedTime = parseLogDateFormat("2002/03/13 19:56:34 GMT-6:00");
        mod2.userName = "alden";
        mod2.comment = "Added target to clean up test results.";
        mod2.emailAddress = "alden@users.sourceforge.net";

        Modification mod3 = new Modification("cvs");
        Modification.ModifiedFile mod3file = mod3.createModifiedFile("build.xml", "main");
        mod3file.action = "modified";
        mod3.revision = "1.42";
        mod3.modifiedTime = parseLogDateFormat("2002/03/15 13:20:28 GMT-6:00");
        mod3.userName = "alden";
        mod3.comment = "enabled debug info when compiling tests.";
        mod3.emailAddress = "alden@users.sourceforge.net";

        Modification mod4 = new Modification("cvs");
        Modification.ModifiedFile mod4file = mod4.createModifiedFile("kungfu.xml", "main");
        mod4file.action = "deleted";
        mod4.revision = "1.2";
        mod4.modifiedTime = parseLogDateFormat("2002/03/13 13:45:42 GMT-6:00");
        mod4.userName = "alden";
        mod4.comment = "Hey, look, a deleted file.";
        mod4.emailAddress = "alden@users.sourceforge.net";

        Modification mod5 = new Modification("cvs");
        Modification.ModifiedFile mod5file = mod5.createModifiedFile("stuff.xml", "main");
        mod5file.action = "deleted";
        mod5.revision = "1.4";
        mod5.modifiedTime = parseLogDateFormat("2002/03/13 13:38:42 GMT-6:00");
        mod5.userName = "alden";
        mod5.comment = "Hey, look, another deleted file.";
        mod5.emailAddress = "alden@users.sourceforge.net";

        assertEquals(mod5, modifications.get(0));
        assertEquals(mod4, modifications.get(1));
        assertEquals(mod1, modifications.get(2));
        assertEquals(mod2, modifications.get(3));
        assertEquals(mod3, modifications.get(4));
    }

    public void testParseStreamRemote() throws IOException, ParseException {
        // ensure CVS version and simulated outputs are in sync
        final String cvsVersion = "1.11.16";
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion(cvsVersion));
        cvs.setModule("cruisecontrol/cruisecontrol");
        Hashtable emailAliases = new Hashtable();
        emailAliases.put("alden", "alden@users.sourceforge.net");
        emailAliases.put("tim", "tim@tim.net");
        cvs.setMailAliases(emailAliases);

        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("cvslog1-11-remote.txt"));
        List modifications = cvs.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals("Should have returned 5 modifications.",
                5,
                modifications.size());

        Modification mod1 = new Modification("cvs");
        Modification.ModifiedFile mod1file = mod1.createModifiedFile("log4j.properties", null);
        mod1file.action = "modified";
        mod1.revision = "1.2";
        mod1.modifiedTime = parseLogDateFormat("2002/03/13 13:45:50 GMT-6:00");
        mod1.userName = "alden";
        mod1.comment =
                "Shortening ConversionPattern so we don't use up all of the available screen space.";
        mod1.emailAddress = "alden@users.sourceforge.net";

        Modification mod2 = new Modification("cvs");
        Modification.ModifiedFile mod2file = mod2.createModifiedFile("build.xml", null);
        mod2file.action = "modified";
        mod2.revision = "1.41";
        mod2.modifiedTime = parseLogDateFormat("2002/03/13 19:56:34 GMT-6:00");
        mod2.userName = "alden";
        mod2.comment = "Added target to clean up test results.";
        mod2.emailAddress = "alden@users.sourceforge.net";

        Modification mod3 = new Modification("cvs");
        Modification.ModifiedFile mod3file = mod3.createModifiedFile("build.xml", "main");
        mod3file.action = "modified";
        mod3.revision = "1.42";
        mod3.modifiedTime = parseLogDateFormat("2002/03/15 13:20:28 GMT-6:00");
        mod3.userName = "alden";
        mod3.comment = "enabled debug info when compiling tests.";
        mod3.emailAddress = "alden@users.sourceforge.net";

        Modification mod4 = new Modification("cvs");
        Modification.ModifiedFile mod4file = mod4.createModifiedFile("kungfu.xml", "main");
        mod4file.action = "deleted";
        mod4.revision = "1.2";
        mod4.modifiedTime = parseLogDateFormat("2002/03/13 13:45:42 GMT-6:00");
        mod4.userName = "alden";
        mod4.comment = "Hey, look, a deleted file.";
        mod4.emailAddress = "alden@users.sourceforge.net";

        Modification mod5 = new Modification("cvs");
        Modification.ModifiedFile mod5file = mod5.createModifiedFile("stuff.xml", "main");
        mod5file.action = "deleted";
        mod5.revision = "1.4";
        mod5.modifiedTime = parseLogDateFormat("2002/03/13 13:38:42 GMT-6:00");
        mod5.userName = "alden";
        mod5.comment = "Hey, look, another deleted file.";
        mod5.emailAddress = "alden@users.sourceforge.net";

        assertEquals(mod5, modifications.get(0));
        assertEquals(mod4, modifications.get(1));
        assertEquals(mod1, modifications.get(2));
        assertEquals(mod2, modifications.get(3));
        assertEquals(mod3, modifications.get(4));
    }

    public void testParseStreamNewFormat() throws IOException, ParseException {
        // ensure CVS version and simulated outputs are in sync
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion("1.12.9"));
        Hashtable emailAliases = new Hashtable();
        emailAliases.put("jerome", "jerome@coffeebreaks.org");
        cvs.setMailAliases(emailAliases);

        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("cvslog1-12.txt"));
        List modifications = cvs.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals("Should have returned 1 modification.",
                1,
                modifications.size());

        Modification mod1 = new Modification("cvs");
        Modification.ModifiedFile mod1file = mod1.createModifiedFile("log4j.properties", null);
        mod1file.action = "modified";
        mod1.revision = "1.1";
        mod1.modifiedTime = parseCVSDateFormat("2004-03-25 00:58:49 GMT");
        mod1.userName = "jerome";
        mod1.comment =
                "initial checkin";
        mod1.emailAddress = "jerome@coffeebreaks.org";

        assertEquals(mod1, modifications.get(0));
    }

    public void testParseStreamBranch() throws IOException, ParseException {
        // ensure CVS version and simulated outputs are in sync
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        Hashtable emailAliases = new Hashtable();
        emailAliases.put("alden", "alden@users.sourceforge.net");
        cvs.setMailAliases(emailAliases);

        cvs.setTag("BRANCH_TEST_BUILD");
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("cvslog1-11branch.txt"));
        List modifications = cvs.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals("Should have returned 4 modifications.",
                4,
                modifications.size());

        Modification mod1 = new Modification("cvs");
        mod1.revision = "1.1.2.4";
        mod1.modifiedTime = parseLogDateFormat("2002/10/03 16:05:23 GMT");
        mod1.userName = "tim";
        mod1.comment = "Test commit once more";
        Modification.ModifiedFile mod1file = mod1.createModifiedFile("test.version", null);
        mod1file.action = "modified";
        mod1file.revision = mod1.revision;

        Modification mod2 = new Modification("cvs");
        mod2.revision = "1.1.2.3";
        mod2.modifiedTime = parseLogDateFormat("2002/10/03 14:24:17 GMT");
        mod2.userName = "tim";
        mod2.comment = "Test commit";
        Modification.ModifiedFile mod2file = mod2.createModifiedFile("test.version", null);
        mod2file.action = "modified";
        mod2file.revision = mod2.revision;

        Modification mod3 = new Modification("cvs");
        mod3.revision = "1.1.2.2";
        mod3.modifiedTime = parseLogDateFormat("2002/10/02 21:54:44 GMT");
        mod3.userName = "tim";
        mod3.comment = "Update parameters for test";
        Modification.ModifiedFile mod3file = mod3.createModifiedFile("test.version", null);
        mod3file.action = "modified";
        mod3file.revision = mod3.revision;

        Modification mod4 = new Modification("cvs");
        mod4.revision = "1.1.2.1";
        mod4.modifiedTime = parseLogDateFormat("2002/10/02 21:49:31 GMT");
        mod4.userName = "tim";
        mod4.comment = "Add parameters for test";
        Modification.ModifiedFile mod4file = mod4.createModifiedFile("test.version", null);
        mod4file.action = "modified";
        mod4file.revision = mod4.revision;


        assertEquals(mod4, modifications.get(0));
        assertEquals(mod3, modifications.get(1));
        assertEquals(mod2, modifications.get(2));
        assertEquals(mod1, modifications.get(3));
    }

    public void testGetProperties() throws IOException {
        // ensure CVS version and simulated outputs are in sync
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs.setMailAliases(new Hashtable());
        cvs.setProperty("property");
        cvs.setPropertyOnDelete("propertyOnDelete");

        String logName = "cvslog1-11.txt";
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Should be two properties.", 2, table.size());

        assertTrue("Property was not set.", table.containsKey("property"));
        assertTrue("PropertyOnDelete was not set.",
                table.containsKey("propertyOnDelete"));

        //negative test
        // ensure CVS version and simulated outputs are in sync
        CVS cvs2 = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs2.setMailAliases(new Hashtable());
        input = new BufferedInputStream(loadTestLog(logName));
        cvs2.parseStream(input);
        input.close();

        table = cvs2.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testGetPropertiesNoModifications() throws IOException {
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs.setMailAliases(new Hashtable());
        cvs.setProperty("property");
        cvs.setPropertyOnDelete("propertyOnDelete");
        String logName = "cvslog1-11noMods.txt";
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testGetPropertiesOnlyModifications() throws IOException {
        // ensure CVS version and simulated outputs are in sync
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs.setMailAliases(new Hashtable());
        cvs.setProperty("property");
        cvs.setPropertyOnDelete("propertyOnDelete");
        String logName = "cvslog1-11mods.txt";
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Should be one property.", 1, table.size());
        assertTrue("Property was not set.", table.containsKey("property"));

        //negative test
        // ensure CVS version and simulated outputs are in sync
        CVS cvs2 = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs2.setMailAliases(new Hashtable());
        cvs2.setPropertyOnDelete("propertyOnDelete");
        input = new BufferedInputStream(loadTestLog(logName));
        cvs2.parseStream(input);
        input.close();

        table = cvs2.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testGetPropertiesOnlyDeletions() throws IOException {
        // ensure CVS version and simulated outputs are in sync
        CVS cvs = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs.setMailAliases(new Hashtable());
        cvs.setPropertyOnDelete("propertyOnDelete");
        String logName = "cvslog1-11del.txt";
        BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Should be one property.", 1, table.size());
        assertTrue("PropertyOnDelete was not set.",
                table.containsKey("propertyOnDelete"));

        //negative test
        // ensure CVS version and simulated outputs are in sync
        CVS cvs2 = new MockVersionCVS(getOfficialCVSVersion("1.11.16"));
        cvs2.setMailAliases(new Hashtable());
        input = new BufferedInputStream(loadTestLog(logName));
        cvs2.parseStream(input);
        input.close();

        table = cvs2.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testBuildHistoryCommand() throws CruiseControlException {
        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuildTime = new Date(checkTime.getTime() - tenMinutes);

        CVS element = new CVS();
        element.setCvsRoot("cvsroot");
        element.setLocalWorkingCopy(".");

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-d",
                    "cvsroot",
                    "-q",
                    "log",
                    "-N",
                    "-d" + CVS.formatCVSDate(lastBuildTime) + "<" + CVS.formatCVSDate(checkTime),
                    "-b"};

        String[] noTagCommand = element.buildHistoryCommand(lastBuildTime, checkTime).getCommandline();
        assertCommandsEqual(expectedCommand, noTagCommand);
        
        element.setTag("");
        String[] emptyStringTagCommand = element.buildHistoryCommand(lastBuildTime, checkTime).getCommandline();
        assertCommandsEqual(expectedCommand, emptyStringTagCommand);
        
        element.setTag("HEAD");
        String[] headTagCommand = element.buildHistoryCommand(lastBuildTime, checkTime).getCommandline();
        assertCommandsEqual(expectedCommand, headTagCommand);
    }

    /**
     * @param expectedCommand
     * @param actualCommand
     */
    private void assertCommandsEqual(String[] expectedCommand, String[] actualCommand) {
      assertEquals("Mismatched lengths!", expectedCommand.length, actualCommand.length);
      for (int i = 0; i < expectedCommand.length; i++) {
          assertEquals(expectedCommand[i], actualCommand[i]);
      }
    }

    public void testBuildHistoryCommandWithTag()
            throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot("cvsroot");
        element.setLocalWorkingCopy(".");
        element.setTag("sometag");

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-d",
                    "cvsroot",
                    "-q",
                    "log",
                    "-d" + CVS.formatCVSDate(lastBuildTime) + "<" + CVS.formatCVSDate(lastBuildTime),
                    "-rsometag"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime, lastBuildTime).getCommandline();

        assertCommandsEqual(expectedCommand, actualCommand);
    }

    public void testHistoryCommandNullLocal() throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot("cvsroot");
        element.setModule("module");
        element.setLocalWorkingCopy(null);

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-d",
                    "cvsroot",
                    "-q",
                    "rlog",
                    "-N",
                    "-d" + CVS.formatCVSDate(lastBuildTime) + "<" + CVS.formatCVSDate(lastBuildTime),
                    "-b",
                    "module"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime, lastBuildTime).getCommandline();

        assertCommandsEqual(expectedCommand, actualCommand);
    }

    public void testHistoryCommandNullCVSROOT() throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot(null);
        element.setLocalWorkingCopy(".");

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-q",
                    "log",
                    "-N",
                    "-d" + CVS.formatCVSDate(lastBuildTime) + "<" + CVS.formatCVSDate(lastBuildTime),
                    "-b"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime, lastBuildTime).getCommandline();
        assertCommandsEqual(expectedCommand, actualCommand);
    }

    public void testParseLogDate() throws ParseException {
        TimeZone tz = TimeZone.getDefault();
        Date may18SixPM2001 = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals(may18SixPM2001, CVS.LOGDATE.parse("2001/05/18 18:00:00 "
                + tz.getDisplayName(tz.inDaylightTime(may18SixPM2001), TimeZone.SHORT)));
    }

    public void testFormatCVSDateGMTPlusZero() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0:00"));
        Date mayEighteenSixPM2001 =
                new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals("2001-05-18 18:00:00 GMT",
                CVS.formatCVSDate(mayEighteenSixPM2001));
    }

    public void testFormatCVSDateGMTPlusTen() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));

        Date mayEighteenSixPM2001 =
                new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals("2001-05-18 08:00:00 GMT",
                CVS.formatCVSDate(mayEighteenSixPM2001));
        Date may18EightAM2001 =
                new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();
        assertEquals("2001-05-17 22:00:00 GMT",
                CVS.formatCVSDate(may18EightAM2001));
    }

    public void testFormatCVSDateGMTMinusTen() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));
        Date may18SixPM2001 =
                new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals("2001-05-19 04:00:00 GMT",
                CVS.formatCVSDate(may18SixPM2001));
        Date may18EightAM2001 =
                new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();
        assertEquals("2001-05-18 18:00:00 GMT",
                CVS.formatCVSDate(may18EightAM2001));
    }

    public void testAddAliasToMap() {
        CVS cvs = new CVS();
        Hashtable aliasMap = new Hashtable();
        cvs.setMailAliases(aliasMap);
        String userline = "roberto:'Roberto DaMana <damana@cs.unipr.it>'";
        cvs.addAliasToMap(userline);
        userline = "hill:hill@cs.unipr.it";
        cvs.addAliasToMap(userline);
        userline = "zolo:zolo";
        cvs.addAliasToMap(userline);
        assertEquals("'Roberto DaMana <damana@cs.unipr.it>'", aliasMap.get("roberto"));
        assertEquals("hill@cs.unipr.it", aliasMap.get("hill"));
        assertEquals("zolo", aliasMap.get("zolo"));

        userline = "me";
        cvs.addAliasToMap(userline);
        assertNull(aliasMap.get("me"));
    }

    public void testGetCvsServerVersionDifferingClientServerVersions() throws IOException {
        String logName = "cvslog1-1xversion.txt";
        final BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));

        final CVS cvs = new InputBasedCommandLineMockCVS(input, CVS_VERSION_COMMANDLINE, null);
        assertEquals("differing client & server version",
                getOfficialCVSVersion("1.11.16"), cvs.getCvsServerVersion());
        assertEquals("differing client & server version", false, cvs.isCvsNewOutputFormat());
        input.close();
    }

    public void testGetCvsServerVersionIdenticalClientServerVersions1() throws IOException {
        String logName = "cvslog1-11version.txt";
        final BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));

        final CVS cvs = new InputBasedCommandLineMockCVS(input, CVS_VERSION_COMMANDLINE, null);
        assertEquals("identical client & server version 1.11.16",
                getOfficialCVSVersion("1.11.16"), cvs.getCvsServerVersion());
        assertEquals("old output format", false, cvs.isCvsNewOutputFormat());
        input.close();
    }

    public void testGetCvsServerVersionIdenticalClientServerVersions2() throws IOException {
        String logName = "cvslog1-12version.txt";
        final BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));

        final CVS cvs = new InputBasedCommandLineMockCVS(input, CVS_VERSION_COMMANDLINE, null);
        assertEquals("identical client & server version 1.12.9",
                getOfficialCVSVersion("1.12.9"), cvs.getCvsServerVersion());
        assertEquals("new output format", true, cvs.isCvsNewOutputFormat());
        input.close();
    }


    public void testGetCvsNTServerVersionDifferingClientServerVersions() throws IOException {
        String logName = "cvsntlog2-0xversion.txt";
        final BufferedInputStream input =
                new BufferedInputStream(loadTestLog(logName));

        final CVS cvs = new InputBasedCommandLineMockCVS(input, CVS_VERSION_COMMANDLINE, null);
        assertEquals("differing client & server version",
                new CVS.Version("CVSNT", "2.0.14"), cvs.getCvsServerVersion());
        assertEquals("differing client & server version", false, cvs.isCvsNewOutputFormat());
        input.close();
    }

    public void testIsCVSNewVersion() {

        Object[] array = new Object[]{
            new MockVersionCVS(getOfficialCVSVersion("1.11.16")), Boolean.FALSE,
            new MockVersionCVS(getOfficialCVSVersion("1.12.8")), Boolean.FALSE,
            new MockVersionCVS(getOfficialCVSVersion("1.12.9")), Boolean.TRUE,
            new MockVersionCVS(getOfficialCVSVersion("1.12.81")), Boolean.TRUE,
            new MockVersionCVS(new CVS.Version("cvsnt", "2.0.14")), Boolean.FALSE
        };

        for (int i = 0; i < array.length; i += 2) {
            MockVersionCVS cvs = (MockVersionCVS) array[i];
            Boolean b = (Boolean) array[i + 1];
            assertEquals("output format " + cvs.getCvsServerVersion() + " is new?",
                    b.booleanValue(), cvs.isCvsNewOutputFormat());
        }
    }
}
