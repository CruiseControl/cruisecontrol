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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.jdom.JDOMException;


/**
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @see <a href="http://www.selenic.com/mercurial">Mercurial web site</a>
 */
public class MercurialTest extends TestCase {
    private Mercurial mercurial;
    private TimeZone originalTimeZone;
    private File tempFile;

    protected void setUp() throws Exception {
        mercurial = new Mercurial();
        mercurial.setLocalWorkingCopy(".");
        originalTimeZone = TimeZone.getDefault();
        tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();
    }

    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
    }

    public void testValidateNoAttributesSet() throws IOException {
        try {
            new Mercurial().validate();
        } catch (CruiseControlException e) {
            fail("should not throw an exception when no attributes are set " + e.getMessage());
        }
    }

    public void testValidatInvalidLocalWorkingCopy() throws IOException {
        mercurial.setLocalWorkingCopy("invalid directory");
        try {
            mercurial.validate();
            fail("should throw an exception when an invalid 'localWorkingCopy' attribute is set");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testValidateValidLocalWorkingCopy() throws IOException {
        mercurial.setLocalWorkingCopy(tempFile.getParent());
        try {
            mercurial.validate();
        } catch (CruiseControlException e) {
            fail(
                "should not throw an exception when at least a valid 'localWorkingCopy' "
                    + "attribute is set");
        }
    }

    public void testValidateFailWhenLocalWorkingCopyIsAFile() throws IOException {
        mercurial.setLocalWorkingCopy(tempFile.getAbsolutePath());
        try {
            mercurial.validate();
            fail("should throw an exception when 'localWorkingCopy' is file instead of directory.");
        } catch (CruiseControlException e) {
            // expected
        }
    }
    
    public void testValidatePassIfHgCommandIsIncomingOrLog() throws Exception {
        try {
            mercurial.setHgCommand("incoming");
            mercurial.validate();
            mercurial.setHgCommand("log");
            mercurial.validate();
        } catch (CruiseControlException e) {
            fail("should not throw an exception when hgcommand is either incoming or log");
        }
    }
    
    public void testValidateFailIfHgCommandIsNeitherIncomingNorLog() throws Exception {
        try {
            mercurial.setHgCommand("in");
            mercurial.validate();
            fail("should throw an exception when hgcommand is neither incoming nor log");
        } catch (CruiseControlException e) {
            // expected, even the command is in, which's the alias of incoming
        }
    }
    
    public void testShouldUseLogToGetModificationsIfHgCommandIsLog() throws Exception {
        Date from = Mercurial.HG_DATE_PARSER.parse("1978-03-06 17:33:55 +0000");
        Date to = Mercurial.HG_DATE_PARSER.parse("2978-03-06 17:33:55 +0000");

        String[] expectedCmd = new String[] { 
            "hg", 
            "log", 
            "--debug", 
            "--date",
            Mercurial.HG_DATE_PARSER.format(from) + " to " + Mercurial.HG_DATE_PARSER.format(to), 
            "--template",
            Mercurial.MODIFICATION_XML_TEMPLATE, new File(".").getAbsolutePath() 
        };

        mercurial.setHgCommand("log");
        String[] actualCmd = mercurial.buildHistoryCommand(from, to).getCommandline();
        TestUtil.assertArray("", expectedCmd, actualCmd);
    }
    
    public void testShouldUseIncomingToGetModificationsByDefaultSoThatItWontBreakExistingConfig()
            throws Exception {
        Date from = Mercurial.HG_DATE_PARSER.parse("1978-03-06 17:33:55 +0000");
        Date to = Mercurial.HG_DATE_PARSER.parse("2978-03-06 17:33:55 +0000");

        String[] expectedCmd =
            new String[] {
                "hg",
                "incoming",
                "--debug",
                "--template",
                Mercurial.MODIFICATION_XML_TEMPLATE
            };
        String[] actualCmd = mercurial.buildHistoryCommand(from, to).getCommandline();
        TestUtil.assertArray("", expectedCmd, actualCmd);
    }

    public void testParseModifications() throws JDOMException, ParseException, IOException {
        BufferedInputStream input = new BufferedInputStream(loadTestLog("mercurial_incoming_xml_debug.txt"));
        List modifications = Mercurial.parseStream(input);
        input.close();

        assertEquals("Should have returned 7 modifications.", 7, modifications.size());

        Modification modification =
            createModification(
                parseIso8601Format("2007-08-27 16:11:19 +0200"),
                "ET4642@localhost",
                "Test of a fourth commit",
                "3:bb0a5f00315f4f6dddb7362a69bc0910b07c4faa",
                "",
                "file1.txt",
                "modified");
        assertEquals(modification, modifications.get(0));

        modification =
            createModification(
                parseIso8601Format("2007-08-29 21:38:18 +0200"),
                "ET4642@edbwp000856.edb.local",
                "Changed 2 files",
                "4:1da89ee88532fddb21235b2d21e4a46424adbe39",
                "",
                "file1.txt",
                "modified");
        assertEquals(modification, modifications.get(1));

        modification =
            createModification(
                parseIso8601Format("2007-08-29 21:38:18 +0200"),
                "ET4642@edbwp000856.edb.local",
                "Changed 2 files",
                "4:1da89ee88532fddb21235b2d21e4a46424adbe39",
                "",
                "file2.txt",
                "modified");
        assertEquals(modification, modifications.get(2));

        modification =
            createModification(
                parseIso8601Format("2007-08-29 21:44:37 +0200"),
                "ET4642@edbwp000856.edb.local",
                "new change, with one directory depth",
                "5:93981bd125719a98d7c11028560b2b736b3f12ec",
                "",
                "file2.txt",
                "modified");
        assertEquals(modification, modifications.get(3));


        modification =
            createModification(
                parseIso8601Format("2007-08-29 21:44:37 +0200"),
                "ET4642@edbwp000856.edb.local",
                "new change, with one directory depth",
                "5:93981bd125719a98d7c11028560b2b736b3f12ec",
                "",
                "mydir/newfile.txt",
                "added");
        assertEquals(modification, modifications.get(4));

        modification =
            createModification(
                parseIso8601Format("2007-08-30 09:09:08 +0200"),
                "ET4642@localhost",
                "removed one file, changed one",
                "6:e3cefb520ddc6c7de8bad83f17df4b3d4194fe08",
                "",
                "file2.txt",
                "removed");
        assertEquals(modification, modifications.get(5));

        modification =
            createModification(
                parseIso8601Format("2007-08-30 09:09:08 +0200"),
                "ET4642@localhost",
                "removed one file, changed one",
                "6:e3cefb520ddc6c7de8bad83f17df4b3d4194fe08",
                "",
                "file1.txt",
                "modified");
        assertEquals(modification, modifications.get(6));

    }

    private Date parseIso8601Format(String iso8601Date) throws ParseException {
        return Mercurial.HG_DATE_PARSER.parse(iso8601Date);
    }

    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }

    public void testSetPropertyNoChanges() throws ParseException {
        mercurial.setProperty("hasChanges?");

        List noModifications = new ArrayList();
        mercurial.fillPropertiesIfNeeded(noModifications);

        assertFalse(mercurial.getProperties().containsKey("hasChanges?"));
        assertEquals(null, mercurial.getProperties().get("hgrevision"));
    }

    public void testSetPropertyHasChanges() throws ParseException {
        mercurial.setProperty("hasChanges?");

        final List<Modification> hasModifications = new ArrayList<Modification>();
        hasModifications.add(createModification(
                parseIso8601Format("2007-08-27 16:11:19 +0200"),
                "ET4642@localhost",
                "Test of a fourth commit",
                "3:bb0a5f00315f4f6dddb7362a69bc0910b07c4faa",
                "",
                "file1.txt",
                "modified"));

        hasModifications.add(createModification(
                parseIso8601Format("2007-08-29 21:38:18 +0200"),
                "ET4642@edbwp000856.edb.local",
                "Changed 2 files",
                "4:1da89ee88532fddb21235b2d21e4a46424adbe39",
                "",
                "file1.txt",
                "modified"));

        mercurial.fillPropertiesIfNeeded(hasModifications);
        final Map<String, String> properties = mercurial.getProperties();
        assertEquals("true", properties.get("hasChanges?"));
        assertEquals("4:1da89ee88532fddb21235b2d21e4a46424adbe39", properties.get("hgrevision"));
    }

    public void testSetPropertyIgnoresPriorState() throws ParseException {
        testSetPropertyHasChanges();
        mercurial.fillPropertiesIfNeeded(new ArrayList());
        
        assertFalse(mercurial.getProperties().containsKey("hasChanges?"));
    }

    public void testSetPropertyOnDeleteEmptyModifications() throws ParseException {
        mercurial.setPropertyOnDelete("hasDeletions?");

        List noModifications = new ArrayList();
        mercurial.fillPropertiesIfNeeded(noModifications);

        assertEquals(null, mercurial.getProperties().get("hasDeletions?"));
    }

    public void testSetPropertyOnDeleteNoDeletion() throws ParseException {
        mercurial.setPropertyOnDelete("hasDeletions?");

        final List<Modification> noDeletions = new ArrayList<Modification>();
        noDeletions.add(createModification(
                parseIso8601Format("2007-08-27 16:11:19 +0200"),
                "ET4642@localhost",
                "Test of a fourth commit",
                "3:bb0a5f00315f4f6dddb7362a69bc0910b07c4faa",
                "",
                "file1.txt",
                "modified"));

        mercurial.fillPropertiesIfNeeded(noDeletions);

        assertEquals(null, mercurial.getProperties().get("hasDeletions?"));
    }

    public void testSetPropertyOnDeleteHasDeletion() throws ParseException {
        mercurial.setPropertyOnDelete("hasDeletions?");

        final List<Modification> hasDeletions = new ArrayList<Modification>();
        hasDeletions.add(createModification(
                parseIso8601Format("2007-08-27 16:11:19 +0200"),
                "ET4642@localhost",
                "Test of a fourth commit",
                "3:bb0a5f00315f4f6dddb7362a69bc0910b07c4faa",
                "",
                "file1.txt",
                "modified"));

        hasDeletions.add(createModification(
                parseIso8601Format("2007-08-29 21:38:18 +0200"),
                "ET4642@edbwp000856.edb.local",
                "Changed 2 files",
                "4:1da89ee88532fddb21235b2d21e4a46424adbe39",
                "",
                "file1.txt",
                "deleted"));
        mercurial.fillPropertiesIfNeeded(hasDeletions);
        assertEquals("true", mercurial.getProperties().get("hasDeletions?"));
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

    public void testParseVersion() throws JDOMException, IOException, ParseException {
        BufferedInputStream input = new BufferedInputStream(loadTestLog("mercurial_version.txt"));
        String version = Mercurial.parseVersionStream(input);
        input.close();

        assertEquals("version 0.9.4", version);
    }

    public void testBuildVersionCommand() throws CruiseControlException {
        mercurial.setLocalWorkingCopy(".");
        String[] expectedCmd = { "hg", "version" };
        String[] actualCmd = mercurial.buildVersionCommand().getCommandline();
        TestUtil.assertArray("", expectedCmd, actualCmd);
    }
}
