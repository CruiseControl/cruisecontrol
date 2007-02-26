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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class CompoundTest extends TestCase {

    public void testGetModifications() {
        List modList = null;

        Compound compound = new Compound();
        Triggers trigger = (Triggers) compound.createTriggers();
        Targets target = (Targets) compound.createTargets();

        MockSourceControl msc1 = new MockSourceControl();
        msc1.setType(1);
        trigger.add(msc1);

        MockSourceControl msc2 = new MockSourceControl();
        msc2.setType(2);
        target.add(msc2);

        List triggerModsList = msc1.getModifications(new Date(0), new Date());
        List targetModsList = msc2.getModifications(new Date(0), new Date());
        List allModsList = new ArrayList();
        allModsList.addAll(targetModsList);
        allModsList.addAll(triggerModsList);

        // test retrieving mods without including trigger changes
        compound.setIncludeTriggerChanges("false");

        modList = compound.getModifications(new Date(0), new Date());
        assertEquals("modification lists should match", modList, targetModsList);
        assertEquals(0, compound.getProperties().size());
        
        // test retrieving mods without including trigger changes
        compound.setIncludeTriggerChanges("true");
        compound.setProperty("property");
        
        modList = compound.getModifications(new Date(0), new Date());
        assertEquals("modification lists should match", modList, allModsList);
        Map properties = compound.getProperties();
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("property"));
    }

    public void testValidate() {
        Compound compound = null;

        // test compound with no triggers
        compound = new Compound();
        compound.createTargets();

        try {
            compound.validate();
            fail("Compound should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("Error: there must be exactly one \"triggers\" block in a compound block.",
                    e.getMessage());
        }

        // test compound with no targets
        compound = new Compound();
        compound.createTriggers();

        try {
            compound.validate();
            fail("Compound should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("Error: there must be exactly one \"targets\" block in a compound block.",
                    e.getMessage());
        }

        // test compound with all attributes properly set
        compound.createTargets();

        try {
            compound.validate();
        } catch (CruiseControlException e) {
            fail("Compound should not throw exceptions when required attributes are set.");
        }
    }
}
