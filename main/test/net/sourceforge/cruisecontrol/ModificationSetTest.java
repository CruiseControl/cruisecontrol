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

import java.util.Date;
import java.util.Iterator;
import java.text.SimpleDateFormat;

public class ModificationSetTest extends TestCase {

    public ModificationSetTest(String name) {
        super(name);
    }

    public void testGetModifications() {
        ModificationSet modSet = new ModificationSet();
        MockSourceControl mock1 = new MockSourceControl();
        mock1.setType(1);
        MockSourceControl mock2 = new MockSourceControl();
        mock2.setType(2);

        modSet.addSourceControl(mock1);
        modSet.addSourceControl(mock2);

        Element modSetResults = modSet.getModifications(new Date()); //mock source controls don't care about the date

        Element modificationsElement = new Element("modifications");
        Iterator mock1ModificationsIterator = mock1.getModifications(new Date(), new Date(), 0).iterator();
        while (mock1ModificationsIterator.hasNext()) {
            Modification modification = (Modification) mock1ModificationsIterator.next();
            modificationsElement.addContent(modification.toElement(new SimpleDateFormat("")));
        }
        Iterator mock2ModificationsIterator = mock2.getModifications(new Date(), new Date(), 0).iterator();
        while (mock2ModificationsIterator.hasNext()) {
            Modification modification = (Modification) mock2ModificationsIterator.next();
            modificationsElement.addContent(modification.toElement(new SimpleDateFormat("")));
        }

        assertEquals(modSetResults.toString(), modificationsElement.toString());
    }
}
