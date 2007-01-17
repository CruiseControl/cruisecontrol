/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.CDATA;
import org.jdom.Element;

public class ClearCaseBaselinePublisherTest extends TestCase {

    public Element mockbuildLog() {
        // create a fake cruisecontrol log
        Element logElement = new Element("cruisecontrol");
        // add a single modification
        Element mods = new Element("modifications");
        logElement.addContent(mods);
        Element mod = new Element("modification");
        mod.setAttribute("type", "activity");
        mods.addContent(mod);
        Element rev = new Element("revision");
        rev.addContent(new CDATA("Some activitiy"));
        mod.addContent(rev);
        // and a build label
        Element info = new Element("info");
        logElement.addContent(info);
        Element prop = new Element("property");
        prop.setAttribute("name", "label");
        prop.setAttribute("value", "1_TST");
        info.addContent(prop);
        return logElement;
    }

    public void testValidate() {
        ClearCaseBaselinePublisher cbp = new ClearCaseBaselinePublisher();

        try {
            cbp.validate();
            fail("ClearCaseBaselinePublisher should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set",
                    "'viewtag' is required for ClearCaseBaselinePublisher", e.getMessage());
        }
        cbp.setViewtag("someviewtag");
        try {
            cbp.validate();
        } catch (CruiseControlException e) {
            fail("ClearCaseBaselinePublisher should not throw an exception when the required attributes are set.");
        }
    }

    public void testGetActivities() {
        ClearCaseBaselinePublisher cbp = new ClearCaseBaselinePublisher();
        cbp.setViewtag("someviewtag");
        assertEquals(0, cbp.getActivities(new Element("cruisecontrol")).size());
        Element logElement = mockbuildLog();
        assertEquals(1, cbp.getActivities(logElement).size());
    }

    public void testShouldPublish() {
        ClearCaseBaselinePublisher cbp = new ClearCaseBaselinePublisher();
        cbp.setViewtag("someviewtag");
        assertFalse(cbp.shouldPublish(new Element("cruisecontrol")));
        Element logElement = mockbuildLog();
        assertTrue(cbp.shouldPublish(logElement));
    }
}
