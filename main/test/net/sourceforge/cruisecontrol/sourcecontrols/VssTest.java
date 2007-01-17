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

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

/**
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Arun Aggarwal
 */
public class VssTest extends TestCase {

    private Vss vss;

    private static final String DATE_TIME_STRING = "Date:  6/20/01   Time:  10:36a";
    private static final String ALTERNATE_DATE_TIME_STRING = "Date:  20/6/01   Time:  10:36";

    protected void setUp() throws Exception {
        super.setUp();
        vss = new Vss();
    }

    /**
     * This test is only likely to fail on Windows if the temp file
     * is not deleted. *nix systems are able to delete the file even
     * if input streams remain open on the file
     */
    public void testVssTempFileCleanup() throws Exception {
        vss.setVsspath("vsspath");

        final File tempFile = new File(vss.createFileNameFromVssPath());
        // create the temp file
        assertTrue("Failed to create test temp file: " + tempFile.getAbsolutePath(),
                tempFile.createNewFile());
        tempFile.deleteOnExit();
        try {
            final ArrayList mods = new ArrayList();
            vss.parseTempFile(mods);
            assertFalse("Vss SourceControl failed to delete temp file: "
                    + tempFile.getAbsolutePath()
                    + "\nWindows requires streams be closed before File.delete() will work.",
                    tempFile.exists());
            assertEquals(0, mods.size());
        } finally {
            if (tempFile.exists()) {
                // something went wrong, but clean up anyway
                assertTrue("Failed to delete temp file: " + tempFile.getAbsolutePath(),
                        tempFile.delete());
            }
        }
    }

    public void testValidate() {
        try {
            vss.validate();
            fail("Vss should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        vss.setVsspath("vsspath");
        vss.setLogin("login");

        try {
            vss.validate();
        } catch (CruiseControlException e) {
            fail("Vss should not throw exceptions when required fields are set.");
        }
    }

    public void testParseUserSingleCharName() {
        String testName = "1";
        assertEquals(testName, vss.parseUser(createVSSLine(testName, DATE_TIME_STRING)));
    }

    public void testParseDateSingleCharName() throws ParseException {
        String testName = "1";
        assertEquals(vss.getVssDateTimeFormat().parse(DATE_TIME_STRING.trim() + "m"), vss.parseDate(createVSSLine(
                testName, DATE_TIME_STRING)));
    }

    /**
     * Parse a user supplied date format.
     */
    public void testParseDateAlternate() {
        String testName = "1";
        vss.setDateFormat("dd/MM/yy");
        vss.setTimeFormat("HH:mm");
        try {
            assertEquals(vss.getVssDateTimeFormat().parse(ALTERNATE_DATE_TIME_STRING.trim()), vss
                    .parseDate(createVSSLine(testName, ALTERNATE_DATE_TIME_STRING)));
        } catch (ParseException e) {
            fail("Could not parse date string: " + e.getMessage());
        }
    }

    /**
     * Some people are seeing strange date outputs from their VSS history that looks like this: User: Aaggarwa Date:
     * 6/29/:1 Time: 3:40p Note the ":" rather than a "0"
     *
     * @throws ParseException
     */
    public void testParseDateStrangeDate() throws ParseException {
        String strangeDateLine = "User: Aaggarwa     Date:  6/20/:1   Time: 10:36a";
        assertEquals(vss.getVssDateTimeFormat().parse(DATE_TIME_STRING.trim() + "m"), vss.parseDate(strangeDateLine));
    }

    public void testParseUser10CharName() {
        String testName = "1234567890";
        assertEquals(testName, vss.parseUser(createVSSLine(testName, DATE_TIME_STRING)));
    }

    public void testParseUser20CharName() {
        String testName = "12345678900987654321";
        assertEquals(testName, vss.parseUser(createVSSLine(testName, DATE_TIME_STRING)));
    }

    public void testHandleEntryWithLabel() {

        // need to adjust for cases where Label: line exists
        // and there is also an action.

        List entry = new ArrayList();
        entry.add("*****  DateChooser.java  *****");
        entry.add("Version 8");
        entry.add("Label: \"Completely new version!\"");
        entry.add("User: Arass        Date: 10/21/02   Time: 12:48p");
        entry.add("Checked in $/code/development/src/org/ets/cbtidg/common/gui");
        entry.add("Comment: This is where I add a completely new, but alot nicer version of the date chooser.");

        Modification modification = vss.handleEntry(entry);
        assertEquals("DateChooser.java", modification.getFileName());

        assertEquals("/code/development/src/org/ets/cbtidg/common/gui", modification.getFolderName());
        assertEquals("Comment: This is where I add a completely new, but alot nicer version of the date chooser.",
                modification.comment);
        assertEquals("Arass", modification.userName);

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) modification.files.get(0);
        assertEquals("checkin", modfile.action);
    }

    public void testHandleEntryCheckinWithComment() {
        List entry = new ArrayList();
        entry.add("*****  ttyp_direct.properties  *****");
        entry.add("Version 10");
        entry.add("User: Etucker      Date:  7/03/01   Time:  3:24p");
        entry.add("Checked in $/Eclipse/src/main/com/itxc/eclipse/some/path/here");
        entry.add("Comment: updated country codes for Colombia and Slovokia");

        Modification mod = vss.handleEntry(entry);
        assertEquals(mod.getFileName(), "ttyp_direct.properties");
        assertEquals(mod.getFolderName(), "/Eclipse/src/main/com/itxc/eclipse/some/path/here");
        assertEquals(mod.comment, "Comment: updated country codes for Colombia and Slovokia");
        assertEquals(mod.userName, "Etucker");

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) mod.files.get(0);
        assertEquals(modfile.action, "checkin");
    }

    public void testHandleEntryShared() {
        List entry = new ArrayList();
        vss.setVsspath("\\vsspath");
        entry.add("*****  a  *****");
        entry.add("Version 19");
        entry.add("User: Etucker      Date:  7/03/01   Time: 11:16a");
        entry.add("$/Users/jfredrick/test1/b/move.file.2 shared");

        Modification mod = vss.handleEntry(entry);
        assertEquals(mod.getFileName(), "$/Users/jfredrick/test1/b/move.file.2");
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.getFolderName(), "$\\vsspath\\a");

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) mod.files.get(0);
        assertEquals(modfile.action, "share");
    }

    public void testHandleEntryDirBranched() {

        List entry = new ArrayList();
        vss.setVsspath("\\vsspath");
        entry.add("*****  core  *****");
        entry.add("Version 19");
        entry.add("User: Etucker      Date:  7/03/01   Time: 11:16a");
        entry.add("SessionIdGenerator.java branched");

        Modification mod = vss.handleEntry(entry);
        assertEquals(mod.getFileName(), "SessionIdGenerator.java");
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.getFolderName(), "$\\vsspath\\core");
        assertEquals(mod.type, "vss");

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) mod.files.get(0);
        assertEquals(modfile.action, "branch");
    }

    public void testHandleEntryFileBranched() {

        List entry = new ArrayList();
        vss.setVsspath("\\vsspath");
        entry.add("*****  branch.file.1  *****");
        entry.add("Version 19");
        entry.add("User: Etucker      Date:  7/03/01   Time: 11:16a");
        entry.add("Branched");

        Modification mod = vss.handleEntry(entry);
        assertNull(mod);

    }

    public void testHandleEntryAdded() {
        List entry = new ArrayList();
        vss.setVsspath("\\vsspath");
        entry.add("*****  core  *****");
        entry.add("Version 19");
        entry.add("User: Etucker      Date:  7/03/01   Time: 11:16a");
        entry.add("SessionIdGenerator.java added");

        Modification mod = vss.handleEntry(entry);
        assertEquals(mod.getFileName(), "SessionIdGenerator.java");
        assertEquals(mod.userName, "Etucker");
        assertEquals(mod.getFolderName(), "$\\vsspath\\core");

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) mod.files.get(0);
        assertEquals(modfile.action, "add");
    }

    public void testHandleEntryAddedInRoot() {
        //*****************  Version 9   *****************
        //User: Jfredrick     Date:  7/09/02   Time: 12:55p
        //branch.file.2 added

        List entry = new ArrayList();
        vss.setVsspath("\\vsspath");
        entry.add("*****************  Version 19  *****************");
        entry.add("User: MStave      Date:  7/03/01   Time: 11:16a");
        entry.add("SessionIdGenerator.java added");

        Modification mod = vss.handleEntry(entry);
        assertEquals(mod.getFileName(), "SessionIdGenerator.java");
        assertEquals(mod.userName, "MStave");
        assertEquals(mod.getFolderName(), "$\\vsspath");

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) mod.files.get(0);
        assertEquals(modfile.action, "add");
    }

    public void testHandleEntryRename() {
        vss.setPropertyOnDelete("setThis");
        List entry = new ArrayList();
        entry.add("*****  core  *****");
        entry.add("Version 19");
        entry.add("User: Etucker      Date:  7/03/01   Time: 11:16a");
        entry.add("SessionIdGenerator.java renamed to SessionId.java");

        Modification modification = vss.handleEntry(entry);
        assertEquals("SessionIdGenerator.java renamed to SessionId.java", modification.getFileName());
        assertEquals("Etucker", modification.userName);

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) modification.files.get(0);
        assertEquals("rename", modfile.action);

        Map properties = vss.getProperties();
        String setThisValue = (String) properties.get("setThis");
        assertEquals("true", setThisValue);
    }

    public void testHandleEntryDestroyed() {
        vss.setPropertyOnDelete("setThis");
        List entry = new ArrayList();
        entry.add("*****  installer_vms  *****");
        entry.add("Version 42");
        entry.add("User: Sfrolich      Date:  11/19/02   Time: 1:40p");
        entry.add("IBMJDK130AIX_with_xerces.jar.vm destroyed");

        Modification modification = vss.handleEntry(entry);
        assertEquals("IBMJDK130AIX_with_xerces.jar.vm", modification.getFileName());
        assertEquals("Sfrolich", modification.userName);

        Modification.ModifiedFile modfile = (Modification.ModifiedFile) modification.files.get(0);
        assertEquals("destroy", modfile.action);

        Map properties = vss.getProperties();
        String setThisValue = (String) properties.get("setThis");
        assertEquals("true", setThisValue);
    }

    public void testHandleEntryLabel() {
        List entry = new ArrayList();
        entry.add("**********************");
        entry.add("Label: \"KonaBuild_452\"");
        entry.add("User: Cm           Date:  4/29/03   Time: 12:03a");
        entry.add("Labeled");
        entry.add("Label comment: AutoBuild KonaBuild_452");

        Modification modification = vss.handleEntry(entry);
        assertNull(modification);

        // Labeled root directory
        entry = new ArrayList();
        entry.add("*****************  Version 222  *****************");
        entry.add("Label: \"build.83\"");
        entry.add("User: Fabricator     Date:  7/16/03   Time: 10:29a");
        entry.add("Labeled");
        entry.add(" ");

        modification = vss.handleEntry(entry);
        assertNull(modification);

        // Labled subdirectory
        entry = new ArrayList();
        entry.add("*****  built  *****");
        entry.add("Version 4");
        entry.add("Label: \"autobuild_test\"");
        entry.add("User: Etucker      Date:  6/26/01   Time: 11:53a");
        entry.add("Labeled");

        modification = vss.handleEntry(entry);
        assertNull(modification);
    }

    // entry.add("***** ttyp_direct.properties *****");
    // entry.add("Version 10");
    // entry.add("User: Etucker Date: 7/03/01 Time: 3:24p");
    // entry.add("Checked in $/Eclipse/src/main/com/itxc/eclipse/some/path/here");
    // entry.add("Comment: updated country codes for Colombia and Slovokia");

    public void testCommentsContainingAsterisksAreNotMistakenAsEntryHeaders() throws Exception {
        String entry = "Comment: blah blah blah\n*****\n*****  test.file  *****\nVersion 1\nUser: Jyip      "
                + "Date:  7/8/06   Time:  12:24p\nChecked in $/some/folder/path\n" + "Comment: some normal comment\n";
        List modifications = new ArrayList();
        BufferedReader reader = new BufferedReader(new StringReader(entry));

        vss.parseHistoryEntries(modifications, reader);

        assertEquals(1, modifications.size());
        Modification modification = (Modification) modifications.get(0);
        assertEquals("test.file", modification.getFileName());
        assertEquals("/some/folder/path", modification.getFolderName());
    }

    public void testGetCommandLine() throws Exception {
        vss.setVsspath("vsspath");
        vss.setLogin("login,password");

        Date lastBuild = createDate("2002/08/03 09:30:00"); // Set date to Aug 3, 2002 9:30am
        Date now = createDate("2002/08/04 13:15:00"); // Set date to Aug 4, 2002 1:15pm
        String[] expectedCommand = { "ss.exe", "history", "$vsspath", "-R", "-Vd08/04/02;01:15P~08/03/02;09:30A",
                "-Ylogin,password", "-I-N", "-Ovsspath.tmp" };
        String[] actualCommand = vss.getCommandLine(lastBuild, now);
        for (int i = 0; i < expectedCommand.length; i++) {
            assertEquals(expectedCommand[i], actualCommand[i]);
        }

        vss.setDateFormat("dd/MM/yy");
        String[] expectedCommandWithDate = { "ss.exe", "history", "$vsspath", "-R",
                "-Vd04/08/02;01:15P~03/08/02;09:30A", "-Ylogin,password", "-I-N", "-Ovsspath.tmp" };
        String[] actualCommandWithDate = vss.getCommandLine(lastBuild, now);
        for (int i = 0; i < expectedCommandWithDate.length; i++) {
            assertEquals(expectedCommandWithDate[i], actualCommandWithDate[i]);
        }

        File execFile = new File("..", "ss.exe");
        vss.setSsDir("..");
        System.out.println(execFile.getCanonicalPath());
        String[] expectedCommandWithSsdir = { execFile.getCanonicalPath(), "history", "$vsspath", "-R",
                "-Vd04/08/02;01:15P~03/08/02;09:30A", "-Ylogin,password", "-I-N", "-Ovsspath.tmp" };
        String[] actualCommandWithSsdir = vss.getCommandLine(lastBuild, now);
        for (int i = 0; i < expectedCommandWithSsdir.length; i++) {
            assertEquals(expectedCommandWithSsdir[i], actualCommandWithSsdir[i]);
        }

        vss.setTimeFormat("HH:mm");
        String[] expectedCommandWithTimeFormat = { execFile.getCanonicalPath(), "history", "$vsspath", "-R",
                "-Vd04/08/02;13:15~03/08/02;09:30", "-Ylogin,password", "-I-N", "-Ovsspath.tmp" };
        String[] actualCommandWithTimeFormat = vss.getCommandLine(lastBuild, now);

        for (int i = 0; i < expectedCommandWithTimeFormat.length; i++) {
            assertEquals(expectedCommandWithTimeFormat[i], actualCommandWithTimeFormat[i]);
        }
    }

    public void testCreateFileNameFromVssPath() {
        vss.setVsspath("//foo/bar");
        assertEquals("foo_bar.tmp", vss.createFileNameFromVssPath());
    }

    /**
     * Utility method to easily create a given date.
     */
    private Date createDate(String dateString) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.parse(dateString);
    }

    /**
     * Produces a VSS line that looks something like this: User: Username Date: 6/14/01 Time: 6:39p
     *
     * @param testName
     *            Replaces the Username in above example
     */
    private String createVSSLine(String testName, String dateTimeString) {
        return "User: " + testName + " " + dateTimeString;
    }

}
