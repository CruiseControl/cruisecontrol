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
 * @see    <a href="http://smalltalk.cincom.com/">smalltalk.cincom.com</a>
 * @author <a href="rcoulman@gmail.com">Randy Coulman</a>
 */
public class StoreTest extends TestCase {
    private Store store;
    private TimeZone originalTimeZone;

    protected void setUp() throws Exception {
        store = new Store();
        originalTimeZone = TimeZone.getDefault();
    }

    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
    }

    public void testValidate() throws IOException {
        try {
            store.validate();
            fail("should throw an exception when no attributes are set");
        } catch (CruiseControlException e) {
            // expected
        }

        store.setWorkingDirectory(".");
        store.setProfile("local");
        store.setPackages("PackageA,PackageB");

        store.setScript("notThere");
        try {
            store.validate();
            fail("should throw an exception when an invalid 'script' attribute is set");
        } catch (CruiseControlException e) {
            // expected
        }

        File tempFile = File.createTempFile("temp", "sh");
        tempFile.deleteOnExit();

        store.setScript(tempFile.getAbsolutePath());
        try {
            store.validate();
        } catch (CruiseControlException e) {
            fail(
                 "should not throw an exception when a valid 'script' "
                 + "attribute is set");
        }

        store.setWorkingDirectory("notthere");
        try {
            store.validate();
            fail("should throw an exception when an invalid 'workingDirectory' attribute is set");
        } catch (CruiseControlException e) {
            // expected
        }
        
        store.setProfile(null);
        try {
            store.validate();
            fail("should throw an exception when no profile is set");
        } catch (CruiseControlException e) {
            // expected
        }

        store = new Store();
        store.setScript(tempFile.getAbsolutePath());
        store.setProfile("local");

        try {
            store.validate();
            fail("should throw an exception when no packages are set");
        } catch (CruiseControlException e) {
            // expected
        }

        store.setPackages("");
        try {
            store.validate();
            fail("should throw an exception when an empty package list is set");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testBuildHistoryCommand() throws CruiseControlException {
        store.setScript("store");
        store.setProfile("local");
        store.setPackages("PackageA,Package B,Package C");
        
        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        String[] expectedCmd =
            new String[] {
                "store",
                "-profile",
                "local",
                "-packages",
                "PackageA",
                "Package B",
                "Package C",
                "-lastBuild",
                Store.formatDate(lastBuild),
                "-now",
                Store.formatDate(checkTime),
                "-check"
            };
        String[] actualCmd = store.buildCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        store.setVersionRegex("7.5 [0-9]+");

        expectedCmd =
            new String[] {
                "store",
                "-profile",
                "local",
                "-packages",
                "PackageA",
                "Package B",
                "Package C",
                "-versionRegex",
                "7.5 [0-9]+",
                "-lastBuild",
                Store.formatDate(lastBuild),
                "-now",
                Store.formatDate(checkTime),
                "-check"
            };
        actualCmd = store.buildCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        store.setMinimumBlessingLevel("Work In Progress");
        
        expectedCmd =
            new String[] {
                "store",
                "-profile",
                "local",
                "-packages",
                "PackageA",
                "Package B",
                "Package C",
                "-versionRegex",
                "7.5 [0-9]+",
                "-blessedAtLeast",
                "Work In Progress",
                "-lastBuild",
                Store.formatDate(lastBuild),
                "-now",
                Store.formatDate(checkTime),
                "-check"
            };
        actualCmd = store.buildCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);

        store.setParcelBuilderFile("/path/to/parcelsToBuild");
        
        expectedCmd =
            new String[] {
                "store",
                "-profile",
                "local",
                "-packages",
                "PackageA",
                "Package B",
                "Package C",
                "-versionRegex",
                "7.5 [0-9]+",
                "-blessedAtLeast",
                "Work In Progress",
                "-lastBuild",
                Store.formatDate(lastBuild),
                "-now",
                Store.formatDate(checkTime),
                "-parcelBuilderFile",
                "/path/to/parcelsToBuild",
                "-check"
            };
        actualCmd = store.buildCommand(lastBuild, checkTime).getCommandline();
        assertArraysEquals(expectedCmd, actualCmd);
    }

    public void testParseModifications() throws JDOMException, ParseException, IOException {
        String log =
            "<?xml version=\"1.0\"?>\n"
            + "<log>\n"
            + "  <package action=\"modified\" name=\"PackageA\" version=\"44\">\n"
            + "    <blessing timestamp=\"04/27/2007 15:05:52.000\" user=\"fred\">blah</blessing>\n"
            + "  </package>\n"
            + "  <package action=\"modified\" name=\"PackageA\" version=\"43\">\n"
            + "    <blessing timestamp=\"04/27/2007 5:58:47.000\" user=\"fred\">"
            + "Replicated by: fred from: psql_public_cst to: local</blessing>\n"
            + "    <blessing timestamp=\"03/27/2007 5:05:52.000\" user=\"barney\">did something</blessing>\n"
            + "  </package>\n"
            + "  <package action=\"added\" name=\"PackageB\" version=\"7.5 1\">\n"
            + "    <blessing timestamp=\"04/26/2007 17:16:23.000\" user=\"wilma\">initial</blessing>\n"
            + "  </package>\n"
            + "</log>";

        List modifications = Store.StoreLogXMLParser.parse(new StringReader(log));
        assertEquals(4, modifications.size());

        Modification modification =
            createModification(
                               Store.getDateFormatter().parse("04/27/2007 15:05:52.000"),
                               "fred",
                               "blah",
                               "44",
                               "PackageA",
                               "modified");
        assertEquals(modification, modifications.get(0));

        modification =
            createModification(
                               Store.getDateFormatter().parse("04/27/2007 5:58:47.000"),
                               "fred",
                               "Replicated by: fred from: psql_public_cst to: local",
                               "43",
                               "PackageA",
                               "modified");
        assertEquals(modification, modifications.get(1));

        modification =
            createModification(
                               Store.getDateFormatter().parse("03/27/2007 5:05:52.000"),
                               "barney",
                               "did something",
                               "43",
                               "PackageA",
                               "modified");
        assertEquals(modification, modifications.get(2));

        modification =
            createModification(
                               Store.getDateFormatter().parse("04/26/2007 17:16:23.000"),
                               "wilma",
                               "initial",
                               "7.5 1",
                               "PackageB",
                               "added");
        assertEquals(modification, modifications.get(3));
    }

    public void testParseEmptyModifications() throws JDOMException, ParseException, IOException {
        String log =
            "<?xml version=\"1.0\"?>\n " + "<log>\n" + "</log>";

        List modifications = Store.StoreLogXMLParser.parse(new StringReader(log));
        assertEquals(0, modifications.size());
    }

    public void testFormatDatesForSvnLog() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));

        Date maySeventeenSixPM2001 =
            new GregorianCalendar(2001, Calendar.MAY, 17, 18, 0, 0).getTime();
        assertEquals(
                     "05/17/2001 08:00:00.000",
                     Store.formatDate(maySeventeenSixPM2001));

        Date maySeventeenEightAM2001 =
            new GregorianCalendar(2001, Calendar.MAY, 17, 8, 0, 0).getTime();
        assertEquals(
                     "05/16/2001 22:00:00.000",
                     Store.formatDate(maySeventeenEightAM2001));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));

        Date marchTwelfFourPM2003 =
            new GregorianCalendar(2003, Calendar.MARCH, 12, 16, 0, 0).getTime();
        assertEquals(
                     "03/13/2003 02:00:00.000",
                     Store.formatDate(marchTwelfFourPM2003));

        Date marchTwelfTenAM2003 =
            new GregorianCalendar(2003, Calendar.MARCH, 12, 10, 0, 0).getTime();
        assertEquals("03/12/2003 20:00:00.000", Store.formatDate(marchTwelfTenAM2003));
    }

    public void testSetProperty() throws ParseException {
        store.setProperty("hasChanges?");

        final List<Modification> noModifications = new ArrayList<Modification>();
        store.fillPropertiesIfNeeded(noModifications);
        assertEquals(null, store.getProperties().get("hasChanges?"));

        final List<Modification> hasModifications = new ArrayList<Modification>();
        hasModifications.add(createModification(
                                                Store.getDateFormatter().parse("04/27/2007 15:05:52.000"),
                                                "fred",
                                                "blah",
                                                "44",
                                                "PackageA",
                                                "modified"));
        store.fillPropertiesIfNeeded(hasModifications);
        assertEquals("true", store.getProperties().get("hasChanges?"));
    }

    private static Modification createModification(
                                                   Date date,
                                                   String user,
                                                   String comment,
                                                   String revision,
                                                   String file,
                                                   String type) {
        Modification modification = new Modification("store");
        Modification.ModifiedFile modifiedFile = modification.createModifiedFile(file, "");
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
