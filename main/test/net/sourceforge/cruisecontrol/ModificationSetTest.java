/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.sourcecontrols.MockSourceControl;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

public class ModificationSetTest extends TestCase {

    private static Logger log = Logger.getLogger(ModificationSetTest.class);

    public ModificationSetTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
        log.getLoggerRepository().setThreshold(Level.OFF);
    }

    public void testIsLastModificationInQuietPeriod() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Modification mod1 = new Modification();
        mod1.modifiedTime = formatter.parse("20020621140000");
        Modification mod2 = new Modification();
        mod2.modifiedTime = formatter.parse("20020621140100");

        List mods1 = new ArrayList();
        mods1.add(mod1);
        mods1.add(mod2);

        List mods2 = new ArrayList();
        mods2.add(mod1);

        Date now = formatter.parse("20020621140103");

        ModificationSet modSet = new ModificationSet();
        modSet.setQuietPeriod(5);

        assertEquals(true, modSet.isLastModificationInQuietPeriod(now, mods1));
        assertEquals(false, modSet.isLastModificationInQuietPeriod(now, mods2));
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

        ModificationSet modSet = new ModificationSet();

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

        ModificationSet modSet = new ModificationSet();
        modSet.setQuietPeriod(5);

        assertEquals(0, modSet.getQuietPeriodDifference(now, mods1));
        assertEquals(2000, modSet.getQuietPeriodDifference(now, mods2));
    }

    public void testGetModifications() throws Exception {
        ModificationSet modSet = new ModificationSet();
        MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        MockSourceControl mock2 = new MockSourceControl();
        mock2.setType(2);

        modSet.addSourceControl(mock1);
        modSet.addSourceControl(mock2);

        Element modSetResults = modSet.getModifications(new Date()); //mock source controls don't care about the date

        SimpleDateFormat formatter = new SimpleDateFormat(DateFormatFactory.getFormat());
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
        assertEquals("XML data differ", 
                outputter.outputString(modificationsElement), 
                outputter.outputString(modSetResults));
    }

    /**
     * This test will give modificationset two different types of 
     * modifications. One regular, based on the object, and one with Element data.
     * Uses inline sourcecontrol implementation instead of mock.
     */
    public void testGetMixedModifications() {
        ModificationSet modSet = new ModificationSet();
        SimpleDateFormat formatter = new SimpleDateFormat(DateFormatFactory.getFormat());

        Modification mod1 = new Modification();
        mod1.type = "Checkin";
        mod1.fileName = "file3";
        mod1.folderName = "dir3";
        mod1.userName = "user3";
        mod1.modifiedTime = new Date();
        mod1.comment = "comment3";

        Modification mod2 = new Modification();
        mod2.type = "Checkin";
        mod2.fileName = "file4";
        mod2.folderName = "dir4";
        mod2.userName = "user4";
        mod2.modifiedTime = new Date();
        mod2.comment = "comment4";
        final List result = new ArrayList();
        result.add(mod1.toElement(formatter));
        result.add(mod2);

        modSet.addSourceControl(new SourceControl() {
            public List getModifications(Date lastBuild, Date now) {
                return result;
            }

            // None of the below is used
            public void validate() throws CruiseControlException {
            }

            public Hashtable getProperties() {
                return null;
            }

            public void setProperty(String property) {
            }

            public void setPropertyOnDelete(String property) {
            }
        });

        Element modSetResults = modSet.getModifications(new Date()); //mock source controls don't care about the date

        Element expectedModificationsElement = new Element("modifications");
        expectedModificationsElement.addContent(mod1.toElement(formatter));
        expectedModificationsElement.addContent(mod2.toElement(formatter));

        XMLOutputter outputter = new XMLOutputter();
        assertEquals("XML data differ", 
                outputter.outputString(expectedModificationsElement), 
                outputter.outputString(modSetResults));
    }

    public void testGetProperties() throws Exception {
        ModificationSet modSet = new ModificationSet();
        MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        MockSourceControl mock2 = new MockSourceControl();
        mock2.setType(2);

        modSet.addSourceControl(mock1);
        modSet.addSourceControl(mock2);

        modSet.getModifications(new Date()); //mock source controls don't care about the date

        Hashtable table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should be empty.", 0, table.size());

        modSet = new ModificationSet();
        mock1 = new MockSourceControl();
        mock2 = new MockSourceControl();
        mock1.setType(1);
        mock2.setType(2);
        mock1.setProperty("property");
        mock2.setPropertyOnDelete("propertyOnDelete");

        modSet.addSourceControl(mock1);
        modSet.addSourceControl(mock2);

        modSet.getModifications(new Date()); //mock source controls don't care about the date

        table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should should have 2 entries.", 2, table.size());
        assertTrue("Property not found.", table.containsKey("property"));
        assertTrue("PropertyOnDelete not found.", table.containsKey("propertyOnDelete"));

        modSet = new ModificationSet();
        mock1 = new MockSourceControl();
        mock1.setType(1);
        mock1.setProperty("property");

        modSet.addSourceControl(mock1);
        modSet.getModifications(new Date()); //mock source controls don't care about the date
        table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should should have 1 entry.", 1, table.size());
        assertTrue("Property not found.", table.containsKey("property"));
    }

}
