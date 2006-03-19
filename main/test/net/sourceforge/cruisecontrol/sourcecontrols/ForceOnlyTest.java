/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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

import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class ForceOnlyTest extends TestCase {

    public void testGettingModifications() {
        ForceOnly forceOnly = new ForceOnly();

        List mods = forceOnly.getModifications(null, null);
        assertNotNull(mods);
        assertEquals("ForceOnly source control should never return any mods.", 0, mods.size());

        mods = forceOnly.getModifications(new Date(), new Date());
        assertNotNull(mods);
        assertEquals("ForceOnly source control should never return any mods.", 0, mods.size());

        mods = forceOnly.getModifications(new Date(0), null);
        assertNotNull(mods);
        assertEquals("ForceOnly source control should never return any mods.", 0, mods.size());

        mods = forceOnly.getModifications(null, new Date(0));
        assertNotNull(mods);
        assertEquals("ForceOnly source control should never return any mods.", 0, mods.size());

        mods = forceOnly.getModifications(new Date(0), new Date());
        assertNotNull(mods);
        assertEquals("ForceOnly source control should never return any mods.", 0, mods.size());
    }

    public void testSettingProperties() {
        ForceOnly forceOnly = new ForceOnly();

        Map props = forceOnly.getProperties();
        assertNotNull(props);
        assertEquals("Expected the properties to be empty", 0, props.size());
    }

    public void testValidate() {
        ForceOnly forceOnly = new ForceOnly();

        //validating should never throw an exception.
        try {
            forceOnly.validate();
        } catch (CruiseControlException e) {
            fail("validate should never throw an exception");
        }
    }
}
