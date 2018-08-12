/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.publishers.MockPublisher;
import net.sourceforge.cruisecontrol.publishers.email.MockMapping;

import org.jdom2.Element;

public class PluginXMLHelperTest extends TestCase {
    private PluginXMLHelper helper;
    private ProjectXMLHelper projectXmlHelper;
    private PluginRegistry registry;
    private Locale originalLocale;

    private static final int SOME_INT = 15;
    private static final int SOME_OTHER_INT = 16;

    protected void setUp() throws CruiseControlException {
        registry = PluginRegistry.loadDefaultPluginRegistry();
        projectXmlHelper = new ProjectXMLHelper(new HashMap<String, String>(), registry, null);
        helper = new PluginXMLHelper(projectXmlHelper);
        
        // Using Turkish because of how I (as in setInterval) is mapped to lower case (CC-871)
        originalLocale = Locale.getDefault();
        final Locale turkishLocale = new Locale("tr", "TU");
        Locale.setDefault(turkishLocale);
    }

    protected void tearDown() throws Exception {
        registry = null;
        projectXmlHelper = null;
        helper = null;
        Locale.setDefault(originalLocale);
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

        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", "mockMapper");
        pluginElement.setAttribute("classname", "net.sourceforge.cruisecontrol.publishers.email.MockMapping");
        // set a default value for the 'mockProperty' property of the MockMapping
        pluginElement.setAttribute("mockProperty", "bar");

        registry.register(pluginElement);

        MockPublisher plugin = (MockPublisher) helper.configure(testElement,
                Class.forName("net.sourceforge.cruisecontrol.publishers.MockPublisher"), false);

        assertEquals("expectedString", plugin.getSomeString());
        assertEquals(SOME_INT, plugin.getSomeInt());
        assertEquals(true, plugin.getSomeBoolean());
        assertEquals("childString", plugin.getMockPluginChild().getSomeString());
        assertEquals(SOME_OTHER_INT, plugin.getMockPluginChild().getSomeInt());
        assertEquals("foo", plugin.getEmailMapping().getAddress());
        assertEquals("bar", ((MockMapping) plugin.getEmailMapping()).getMockProperty());
    }

    public void testConfigureNoClass() {
        try {
            helper.configure(new Element("irrelevant"), MockBadBuilder.class, false);
            fail("Expected an exception because noclass shouldn't exist");
        } catch (CruiseControlException expected) {
        }
    }

    public void testConfigureDefaultsForKnownPlugin() throws CruiseControlException {
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", "ant");
        final String loggerClassName = "net.sourceforge.cruisecontrol.util.XmlLoggerWithStatus";
        pluginElement.setAttribute("loggerClassName", loggerClassName);

        try {
            registry.register(pluginElement);
        } catch (CruiseControlException e) {
            fail("Shouldn't get exception on missing classname for known plugin");
        }

        AntBuilder antBuilder = (AntBuilder) projectXmlHelper.configurePlugin(new Element("ant"), false);
        assertEquals(loggerClassName, antBuilder.getLoggerClassName());
    }
    
    public void testShouldWorkInTurkishLocale() throws CruiseControlException {
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", "test_In_turkish");
        String className = TestNamesInTurkishLocale.class.getCanonicalName();
        pluginElement.setAttribute("classname", className.toLowerCase(Locale.US));
        registry.register(pluginElement);
        
        Element testElement = new Element("test_in_turkIsh");
        testElement.setAttribute("Interval", "60");
        Element childElement = new Element("Insanity");
        testElement.addContent(childElement);

        helper.configure(testElement, TestNamesInTurkishLocale.class, false);
    }

}

class TestNamesInTurkishLocale {
    public TestNamesInTurkishLocale() { }
    
    public void setInterval(long interval) {
    }
    
    public Object createInsanity() {
        return new Object();
    }
}

/**
 * Used to test an exception that gets thrown while instantiating a builder.
 */
class MockBadBuilder extends Builder {

    /**
     * A constructor that violates the rules for a Builder.
     * @param ruleBreakingArg a bad arg
     */
    public MockBadBuilder(String ruleBreakingArg) {

    }

    public Element build(Map properties, Progress progress) throws CruiseControlException {
        return null;
    }

    public Element buildWithTarget(Map properties, String target, Progress progress) throws CruiseControlException {
        return null;
    }

}
