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

import java.text.SimpleDateFormat;
import java.util.*;

public class ModificationSetTest extends TestCase {

    public ModificationSetTest(String name) {
        super(name);
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

        Element modificationsElement = new Element("modifications");
        Iterator mock1ModificationsIterator = mock1.getModifications(new Date(), new Date()).iterator();
        while (mock1ModificationsIterator.hasNext()) {
            Modification modification = (Modification) mock1ModificationsIterator.next();
            modificationsElement.addContent(modification.toElement(modSet._formatter));
        }
        Iterator mock2ModificationsIterator = mock2.getModifications(new Date(), new Date()).iterator();
        while (mock2ModificationsIterator.hasNext()) {
            Modification modification = (Modification) mock2ModificationsIterator.next();
            modificationsElement.addContent(modification.toElement(modSet._formatter));
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
        result.add(mod1.toElement(modSet._formatter));
        result.add(mod2);

        modSet.addSourceControl(new SourceControl() {
            public List getModifications(Date lastBuild, Date now) {
                return result;
            }

            // None of the below is used
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
        expectedModificationsElement.addContent(mod1.toElement(modSet._formatter));
        expectedModificationsElement.addContent(mod2.toElement(modSet._formatter));

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

        Element modSetResults = modSet.getModifications(new Date()); //mock source controls don't care about the date

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

        modSetResults = modSet.getModifications(new Date()); //mock source controls don't care about the date

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
        modSetResults = modSet.getModifications(new Date()); //mock source controls don't care about the date
        table = modSet.getProperties();
        assertNotNull("Properties shouldn't be null.", table);
        assertEquals("Properties should should have 1 entry.", 1, table.size());
        assertTrue("Property not found.", table.containsKey("property"));
    }

}
