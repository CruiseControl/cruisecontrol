/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.publishers.MockPublisher;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.Map;

public class PluginXMLHelperTest extends TestCase {
    private static final Logger LOG = Logger.getLogger(PluginXMLHelperTest.class);
    private PluginXMLHelper helper;
    private ProjectXMLHelper projectXmlHelper;

    private static final int SOME_INT = 15;
    private static final int SOME_OTHER_INT = 16;

    public PluginXMLHelperTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
    }

    protected void setUp() throws CruiseControlException {
        projectXmlHelper = new ProjectXMLHelper();
        helper = new PluginXMLHelper(projectXmlHelper);
        LOG.getLoggerRepository().setThreshold(Level.OFF);
    }

    protected void tearDown() throws Exception {
        LOG.getLoggerRepository().setThreshold(Level.ALL);
    }

    public void testConfigure() throws Exception {
        Element testElement = new Element("test");
        testElement.setAttribute("somestring", "expectedString");
        testElement.setAttribute("someint", Integer.toString(SOME_INT));
        testElement.setAttribute("someboolean", "true");
        Element childElement = new Element("mockpluginchild");
        childElement.setAttribute("somestring", "childString");
        childElement.setAttribute("someint", Integer.toString(SOME_OTHER_INT));
        testElement.addContent(childElement);
        Element childMapper = new Element("mockMapper");
        childMapper.setAttribute("address", "foo");
        testElement.addContent(childMapper);

        PluginRegistry registry = projectXmlHelper.getPlugins();
        registry.register("mockMapper", "net.sourceforge.cruisecontrol.publishers.email.MockMapping");

        MockPublisher plugin = (MockPublisher) helper.configure(testElement,
                Class.forName("net.sourceforge.cruisecontrol.publishers.MockPublisher"), false);

        assertEquals("expectedString", plugin.getSomeString());
        assertEquals(SOME_INT, plugin.getSomeInt());
        assertEquals(true, plugin.getSomeBoolean());
        assertEquals("childString", plugin.getMockPluginChild().getSomeString());
        assertEquals(SOME_OTHER_INT, plugin.getMockPluginChild().getSomeInt());
        assertEquals("foo", plugin.getEmailMapping().getAddress());
    }

    public void testConfigureNoClass() {
        try {
            helper.configure(new Element("irrelevant"), MockBadBuilder.class, false);
            fail("Expected an exception because noclass shouldn't exist");
        } catch (CruiseControlException expected) {
        }
    }
}

/**
 * Used to test an exception that gets thrown while instantiating a builder.
 */
class MockBadBuilder extends Builder {

    /**
     * A constructor that violates the rules for a Builder.
     */
    public MockBadBuilder(String ruleBreakingArg) {

    }

    //should return log from build
    public Element build(Map properties)
            throws CruiseControlException {
        return null;
    }

}
