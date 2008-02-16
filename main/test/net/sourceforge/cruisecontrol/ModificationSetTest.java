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

package net.sourceforge.cruisecontrol;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.sourcecontrols.MockSourceControl;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class ModificationSetTest extends TestCase {

    private static class MockProgress implements Progress {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final MockProgress mockProgress = new MockProgress();

    private ModificationSet modSet;

    protected void setUp() throws Exception {
        modSet = new ModificationSet();
        modSet.setQuietPeriod(0);
    }

    public void testIsLastModificationInQuietPeriod() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Modification mod1 = new Modification();
        mod1.modifiedTime = formatter.parse("20020621140000");
        Modification mod2 = new Modification();
        mod2.modifiedTime = formatter.parse("20020621140100");

        // When a change is put into source control with a bad date in the
        // future, we should still build
        Modification modInFuture = new Modification();
        modInFuture.modifiedTime = formatter.parse("30020731150000");

        List mods1 = new ArrayList();
        mods1.add(mod1);
        mods1.add(mod2);

        List mods2 = new ArrayList();
        mods2.add(mod1);

        List hasModInFuture = new ArrayList();
        hasModInFuture.add(mod1);
        hasModInFuture.add(mod2);
        hasModInFuture.add(modInFuture);

        Date now = formatter.parse("20020621140103");

        modSet.setQuietPeriod(5);

        assertEquals(true, modSet.isLastModificationInQuietPeriod(now, mods1));
        assertEquals(false, modSet.isLastModificationInQuietPeriod(now, mods2));
        assertEquals(false, modSet.isLastModificationInQuietPeriod(now, hasModInFuture));
    }

    public void testGetLastModificationMillis() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Modification mod1 = new Modification();
        mod1.modifiedTime = formatter.parse("20020621140000");
        Modification mod2 = new Modification();
        mod2.modifiedTime = formatter.parse("20020621140100");

        List mods1 = new ArrayList();
        mods1.add(mod2);
        mods1.add(mod1);

        assertEquals(mod2.modifiedTime.getTime(), modSet.getLastModificationMillis(mods1));
    }

    public void testGetQuietPeriodDifference() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date now = formatter.parse("20020621140103");
        Modification mod1 = new Modification();
        mod1.modifiedTime = formatter.parse("20020621140000");
        Modification mod2 = new Modification();
        mod2.modifiedTime = formatter.parse("20020621140100");

        List mods1 = new ArrayList();
        mods1.add(mod1);

        List mods2 = new ArrayList();
        mods2.add(mod2);

        modSet.setQuietPeriod(5);

        assertEquals(0, modSet.getQuietPeriodDifference(now, mods1));
        assertEquals(2000, modSet.getQuietPeriodDifference(now, mods2));
    }

    /**
     * @deprecated Tests deprecated method, remove when method is removed.
     */
    public void testProgressNull() throws Exception {
        // @todo Tests deprecated method, remove when method is removed.

        final Date now = new Date();

        final MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        mock1.setModifiedDate(now);

        modSet.add(mock1);

        final int quietPeriod = 1;
        modSet.setQuietPeriod(quietPeriod);

        // Wait to ensure "now" used in getMods will detect mod in quiet period
        Thread.sleep((long) ((quietPeriod * 1000) * .5));

        assertNull(mockProgress.getValue());
        modSet.retrieveModificationsAsElement(new Date(), null);
        assertNull(mockProgress.getValue());
    }

    public void testProgressInQuietPeriod() throws Exception {
        final Date now = new Date();

        final MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        mock1.setModifiedDate(now);

        modSet.add(mock1);

        final int quietPeriod = 1;
        modSet.setQuietPeriod(quietPeriod);

        // Wait to ensure "now" used in getMods will detect mod in quiet period
        Thread.sleep((long) ((quietPeriod * 1000) * .5));

        assertNull(mockProgress.getValue());
        modSet.retrieveModificationsAsElement(now, mockProgress);
        final String progressMsg = mockProgress.getValue();
        assertNotNull("Modset progress msg should not be null", progressMsg);
        assertTrue(progressMsg.indexOf(ModificationSet.MSG_PROGRESS_PREFIX_QUIETPERIOD_MODIFICATION_SLEEP) > -1);
    }

    public void testGetModifications() throws Exception {
        MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        MockSourceControl mock2 = new MockSourceControl();
        mock2.setType(2);

        modSet.add(mock1);
        modSet.add(mock2);

        // mock source controls don't care about the date
        final Element modSetResults = modSet.retrieveModificationsAsElement(new Date(), mockProgress);

        DateFormat formatter = DateFormatFactory.getDateFormat();
        Element modificationsElement = new Element("modifications");
        Iterator mock1ModificationsIterator = mock1.getModifications(new Date(), new Date()).iterator();
        while (mock1ModificationsIterator.hasNext()) {
            Modification modification = (Modification) mock1ModificationsIterator.next();
            modificationsElement.addContent(modification.toElement(formatter));
        }
        Iterator mock2ModificationsIterator = mock2.getModifications(new Date(), new Date()).iterator();
        while (mock2ModificationsIterator.hasNext()) {
            Modification modification = (Modification) mock2ModificationsIterator.next();
            modificationsElement.addContent(modification.toElement(formatter));
        }

        XMLOutputter outputter = new XMLOutputter();
        assertEquals("XML data differ", outputter.outputString(modificationsElement), outputter
                .outputString(modSetResults));
    }

    /**
     * This test will give modificationset two different types of modifications.
     * One regular, based on the object, and one with Element data. Uses inline
     * sourcecontrol implementation instead of mock.
     */
    public void testGetMixedModifications() throws ParseException {
        DateFormat formatter = DateFormatFactory.getDateFormat();

        Modification mod1 = new Modification();
        mod1.userName = "user3";
        mod1.modifiedTime = formatter.parse("04/04/2004 17:23:50");
        mod1.comment = "comment3";

        Modification.ModifiedFile mod1file = mod1.createModifiedFile("file3", "dir3");
        mod1file.action = "Checkin";

        Modification mod2 = new Modification();
        mod2.userName = "user4";
        mod2.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod2.comment = "comment4";

        Modification.ModifiedFile mod2file = mod1.createModifiedFile("file4", "dir4");
        mod2file.action = "Checkin";

        final List result = new ArrayList();
        result.add(mod1.toElement(formatter));
        result.add(mod2);

        assertEquals(mod1.modifiedTime.getTime(), modSet.getLastModificationMillis(result));

        modSet.add(new MockSourceControl() {
            public List getModifications(Date lastBuild, Date now) {
                return result;
            }
        });

        final Element actual = modSet.retrieveModificationsAsElement(new Date(), mockProgress);

        Element expected = new Element("modifications");
        expected.addContent(mod1.toElement(formatter));
        expected.addContent(mod2.toElement(formatter));

        XMLOutputter outputter = new XMLOutputter();
        assertEquals("XML data differ", outputter.outputString(expected), outputter.outputString(actual));

    }

    public void testGetProperties() throws Exception {
        MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        MockSourceControl mock2 = new MockSourceControl();
        mock2.setType(2);

        modSet.add(mock1);
        modSet.add(mock2);

        modSet.retrieveModificationsAsElement(new Date(), mockProgress); // mock source
        // controls don't
        // care about the
        // date

        Hashtable table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should be empty.", 0, table.size());

        modSet = new ModificationSet();
        modSet.setQuietPeriod(0);
        mock1 = new MockSourceControl();
        mock2 = new MockSourceControl();
        mock1.setType(1);
        mock2.setType(2);
        mock1.setProperty("property");
        mock2.setPropertyOnDelete("propertyOnDelete");

        modSet.add(mock1);
        modSet.add(mock2);

        modSet.retrieveModificationsAsElement(new Date(), mockProgress); // mock source
        // controls don't
        // care about the
        // date

        table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should should have 2 entries.", 2, table.size());
        assertTrue("Property not found.", table.containsKey("property"));
        assertTrue("PropertyOnDelete not found.", table.containsKey("propertyOnDelete"));

        modSet = new ModificationSet();
        modSet.setQuietPeriod(0);
        mock1 = new MockSourceControl();
        mock1.setType(1);
        mock1.setProperty("property");

        modSet.add(mock1);
        modSet.retrieveModificationsAsElement(new Date(), mockProgress); // mock source
        // controls don't
        // care about the
        // date
        table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should should have 1 entry.", 1, table.size());
        assertTrue("Property not found.", table.containsKey("property"));
    }

    public void testValidate() throws CruiseControlException {
        try {
            modSet.validate();
            fail("modificationset should require at least one sourcecontrol");
        } catch (CruiseControlException expected) {
        }

        SourceControl invalidSC = new MockSourceControl() {
            public void validate() throws CruiseControlException {
                throw new CruiseControlException("validation was called");
            }
        };
        modSet.add(invalidSC);
        try {
            modSet.validate();
            fail("validate should be called on child source controls");
        } catch (CruiseControlException expected) {
            assertEquals("validation was called", expected.getMessage());
        }
    }

    public void testSetIgnoreFiles() {

        final String correctPattern = "*.txt,dir1/*/file*.txt";
        try {
            modSet.setIgnoreFiles(correctPattern);
        } catch (CruiseControlException e) {
            fail("Exception while setting pattern");
        }

        final List globPatterns = modSet.getIgnoreFiles();
        assertEquals("The number of parsed patterns is not correct", 2, globPatterns.size());

    }

    public void testFilterIgnoredFiles() throws CruiseControlException, ParseException {
        final DateFormat formatter = DateFormatFactory.getDateFormat();
        final List modifications = new ArrayList();

        final Modification mod1 = new Modification();
        mod1.type = "Checkin";
        mod1.userName = "user1";
        mod1.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod1.comment = "comment1";
        mod1.createModifiedFile("file1", "dir1");
        modifications.add(mod1);

        final Modification mod2 = new Modification();
        mod2.type = "Checkin";
        mod2.userName = "user2";
        mod2.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod2.comment = "comment2";
        mod2.createModifiedFile("file1", "dir2");
        modifications.add(mod2);

        final Modification mod3 = new Modification();
        mod3.type = "Checkin";
        mod3.userName = "user3";
        mod3.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod3.comment = "comment1";
        mod3.createModifiedFile("file3", "dir1");
        modifications.add(mod3);

        modSet.filterIgnoredModifications(modifications);
        assertEquals("No modification should have been filtered out", 3, modifications.size());

        // Now set a filter
        modSet.setIgnoreFiles("dir2/file3,di?1/f*3");
        modSet.filterIgnoredModifications(modifications);
        assertEquals("No modification have been filtered out", 2, modifications.size());

        final List expectedModifications = new ArrayList();
        expectedModifications.add(mod1);
        expectedModifications.add(mod2);

        assertEquals("The wrong modification has been filtered out", expectedModifications, modifications);
    }

    public void testFilterIgnoredFilesInMultipleSubdirectories() throws CruiseControlException, ParseException {
        final DateFormat formatter = DateFormatFactory.getDateFormat();
        final List modifications = new ArrayList();

        final Modification mod1 = new Modification();
        mod1.type = "Checkin";
        mod1.userName = "user1";
        mod1.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod1.comment = "comment1";
        mod1.createModifiedFile("file1", "dir1");
        modifications.add(mod1);

        final Modification mod2 = new Modification();
        mod2.type = "Checkin";
        mod2.userName = "user2";
        mod2.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod2.comment = "comment2";
        mod2.createModifiedFile("file1", "dir2");
        modifications.add(mod2);

        final Modification mod3 = new Modification();
        mod3.type = "Checkin";
        mod3.userName = "user3";
        mod3.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod3.comment = "comment1";
        mod3.createModifiedFile("file3", "dir1");
        modifications.add(mod3);

        // Now set a filter
        modSet.setIgnoreFiles("*/file1");
        modSet.filterIgnoredModifications(modifications);
        assertEquals(1, modifications.size());

        assertEquals(mod3, modifications.get(0));
    }

    /**
     * Tests ignoring files when multiple files exist in the modification sets.
     */
    public void testFilterIgnoredFilesMultipleFiles() throws CruiseControlException, ParseException {
        final DateFormat formatter = DateFormatFactory.getDateFormat();
        final List modifications = new ArrayList();

        final Modification mod1 = new Modification();
        mod1.type = "Checkin";
        mod1.userName = "user1";
        mod1.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod1.comment = "comment1";
        mod1.createModifiedFile("ignore1", "dir1");
        mod1.createModifiedFile("ignore2", "dir1");
        modifications.add(mod1);

        final Modification mod2 = new Modification();
        mod2.type = "Checkin";
        mod2.userName = "user2";
        mod2.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod2.comment = "comment2";
        mod2.createModifiedFile("ignore2", "dir1");
        mod2.createModifiedFile("keep2", "dir1");
        modifications.add(mod2);

        final Modification mod3 = new Modification();
        mod3.type = "Checkin";
        mod3.userName = "user3";
        mod3.modifiedTime = formatter.parse("02/02/2002 17:23:50");
        mod3.comment = "comment1";
        mod3.createModifiedFile("file3", "dir1");
        modifications.add(mod3);

        modSet.setIgnoreFiles("dir1/ignore*");
        modSet.filterIgnoredModifications(modifications);
        assertEquals("Incorrect number of modifications were filtered out", 2, modifications.size());

        final List expectedModifications = new ArrayList();
        expectedModifications.add(mod2);
        expectedModifications.add(mod3);

        assertEquals("The wrong modification has been filtered out", expectedModifications, modifications);
    }
}
