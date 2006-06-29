/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigPreConfTest extends TestCase {

    private CruiseControlConfig config;
    private File configFile;
    private File tempDirectory;

    protected void setUp() throws Exception {
        // Set up a CruiseControl config file for testing
        URL url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig-preconf.xml");
        configFile = new File(URLDecoder.decode(url.getPath()));
        tempDirectory = configFile.getParentFile();

        Element rootElement = Util.loadConfigFile(configFile);
        config = new CruiseControlConfig();
        config.configure(rootElement);
    }

    protected void tearDown() {
        // The directory "foo" in the system's temporary file location
        // is created by CruiseControl when using the config file below.
        // Specifically because of the line:
        //     <log dir='" + tempDirPath + "/foo' encoding='utf-8' >
        File fooDirectory = new File(tempDirectory, "foo");
        fooDirectory.delete();
    }

    public void testGetProjectNames() {
        assertEquals(6, config.getProjectNames().size());
    }

    /*
    public void testPluginConfiguration() throws Exception {
        ProjectConfig projConfig = config.getConfig("project4");
        PluginRegistry plugins = config.getProjectPlugins("project4");

        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestNestedPlugin.class, plugins.getPluginClass("testnested"));
        assertEquals(ListenerTestSelfConfiguringPlugin.class, plugins.getPluginClass("testselfconfiguring"));

        List listeners = projConfig.getListeners();
        assertEquals(3, listeners.size());

        Listener listener0 = (Listener) listeners.get(0);
        assertEquals(ListenerTestPlugin.class, listener0.getClass());
        ListenerTestPlugin testListener0 = (ListenerTestPlugin) listener0;
        assertEquals("project4-0", testListener0.getString());

        Listener listener1 = (Listener) listeners.get(1);
        assertEquals(ListenerTestPlugin.class, listener1.getClass());
        ListenerTestPlugin testListener1 = (ListenerTestPlugin) listener1;
        assertEquals("listener1", testListener1.getString());
        assertEquals("wrapper1", testListener1.getStringWrapper().getString());

        Listener listener2 = (Listener) listeners.get(2);
        assertEquals(ListenerTestPlugin.class, listener2.getClass());
        ListenerTestPlugin testListener2 = (ListenerTestPlugin) listener2;
        assertEquals("listener2", testListener2.getString());
        // note this is in fact undefined behavior!! Because we added twice the stringwrapper
        // (first for the child, then for the parent).
        // this could probably fail depending on a different platform, except if Element.setContent()
        // specifies the order in which children are kept within the element.
        final String wrapper = testListener2.getStringWrapper().getString();
        assertTrue("wrapper2-works!", "wrapper2-works!".equals(wrapper)
                                      || "wrapper1".equals(wrapper));
    }

    public void testPluginConfigurationClassOverride() throws Exception {
        ProjectConfig projConfig = config.getConfig("project5");
        PluginRegistry plugins = config.getProjectPlugins("project5");

        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestOtherNestedPlugin.class, plugins.getPluginClass("testnested"));

        List listeners = projConfig.getListeners();
        assertEquals(1, listeners.size());

        Listener listener0 = (Listener) listeners.get(0);
        assertEquals(ListenerTestPlugin.class, listener0.getClass());
        ListenerTestPlugin testListener0 = (ListenerTestPlugin) listener0;
        assertEquals("default", testListener0.getString());
        ListenerTestNestedPlugin nested = testListener0.getNested();
        assertTrue(nested instanceof ListenerTestOtherNestedPlugin);
        assertEquals("notshadowing", ((ListenerTestOtherNestedPlugin) nested).getString());
        assertEquals(null, ((ListenerTestOtherNestedPlugin) nested).getOtherString());
        assertEquals("otherother", ((ListenerTestOtherNestedPlugin) nested).getOtherOtherString());
    }
    */

}
