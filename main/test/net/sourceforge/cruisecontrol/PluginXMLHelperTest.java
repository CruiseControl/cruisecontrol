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
import org.jdom.Element;
import net.sourceforge.cruisecontrol.publishers.MockPublisher;

public class PluginXMLHelperTest extends TestCase {

    public PluginXMLHelperTest(String name) {
        super(name);
    }

    public void testConfigure() {
        Element testElement = new Element("test");
        testElement.setAttribute("somestring", "expectedString");
        testElement.setAttribute("someint", "15");
        Element childElement = new Element("mockpluginchild");
        childElement.setAttribute("somestring", "childString");
        childElement.setAttribute("someint", "16");
        testElement.addContent(childElement);

        PluginXMLHelper helper = new PluginXMLHelper();
        MockPublisher plugin = null;
        try {
            plugin = (MockPublisher) helper.configure(testElement, "net.sourceforge.cruisecontrol.publishers.MockPublisher");
        } catch (CruiseControlException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertEquals(plugin.getSomeString(), "expectedString");
        assertEquals(plugin.getSomeInt(), 15);
        assertEquals(plugin.getMockPluginChild().getSomeString(), "childString");
        assertEquals(plugin.getMockPluginChild().getSomeInt(), 16);

        try {
            plugin = (MockPublisher) helper.configure(testElement, "noclass");
            assertTrue(false);
        } catch(Exception e) {
            e.printStackTrace();
            assertEquals("Could not find class: noclass", e.getMessage());
        }
    }
}
