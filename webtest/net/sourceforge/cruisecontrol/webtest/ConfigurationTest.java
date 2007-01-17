/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.webtest;

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.sourcecontrols.ConcurrentVersionsSystem;
import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.PluginDetail;
import net.sourceforge.cruisecontrol.GenericPluginDetail;
import net.sourceforge.cruisecontrol.PluginConfiguration;
import net.sourceforge.cruisecontrol.CruiseControlException;

import org.jdom.JDOMException;

public class ConfigurationTest extends TestCase {
    private final String contents;
    private Configuration configuration;

    public ConfigurationTest() throws MalformedObjectNameException, IOException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, JDOMException {
        configuration = createConfig();
        contents = configuration.getConfiguration();
    }

    protected void setUp() throws Exception {
        super.setUp();
        configuration = createConfig();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        configuration.setConfiguration(contents);
        configuration.save();
    }

    public void testGetConfiguration() throws Exception {
        String contents = getContents();
        String xmlHdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        assertTrue(contents.indexOf(xmlHdr) == 0);
        assertTrue(contents.indexOf("<cruisecontrol>") != -1);
        assertTrue(contents.indexOf("</cruisecontrol>") != -1);
    }

    public void testSetConfiguration() throws Exception {
        String addContent = "<!-- Hello, world! -->";
        configuration.setConfiguration(getContents() + addContent);
        assertTrue(getContents().indexOf(addContent) != -1);
    }

    public void testCanUpdatePluginConfiguration() throws Exception {
        String addContent = "projects/foobar";
        PluginDetail cvsDetail = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        PluginConfiguration pluginConfiguration = new PluginConfiguration(cvsDetail, configuration);
        pluginConfiguration.setDetail("cvsRoot", "projects/foobar");
        configuration.updatePluginConfiguration(pluginConfiguration);
        assertTrue(getContents().indexOf(addContent) != -1);
    }

    public void testGetPluginDetails() throws Exception {
        PluginDetail[] pluginDetails = configuration.getPluginDetails();
        assertNotNull(pluginDetails);
        assertTrue(0 < pluginDetails.length);
    }

    public void testLoad() throws Exception {
        String addContent = "<!-- Hello, world! -->";
        configuration.setConfiguration(getContents() + addContent);
        configuration.load();
        assertTrue(getContents().indexOf(addContent) == -1);
    }

    public void testSave() throws Exception {
        String addContent = "<!-- Hello, world! -->";
        configuration.setConfiguration(getContents() + addContent);
        configuration.save();
        configuration.load();
        assertTrue(getContents().indexOf(addContent) != -1);
    }

    public void testGetConfiguredBootstrappers() throws CruiseControlException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, JDOMException {
        PluginDetail[] bootstrappers = configuration.getConfiguredBootstrappers("connectfour");
        assertNotNull(bootstrappers);
        assertTrue(1 == bootstrappers.length);
    }

    public void testGetConfiguredListeners() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, CruiseControlException, JDOMException {
        PluginDetail[] listeners = configuration.getConfiguredListeners("connectfour");
        assertNotNull(listeners);
        assertTrue(1 == listeners.length);
    }

    public void testGetConfiguredSourceControls() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, CruiseControlException, JDOMException {
        PluginDetail[] sourceControls = configuration.getConfiguredSourceControls("connectfour");
        assertNotNull(sourceControls);
        assertTrue(1 == sourceControls.length);
    }

    private static Configuration createConfig() throws IOException, MalformedObjectNameException {
        return new Configuration("localhost", 7856);
    }

    private String getContents() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException, IOException, JDOMException {
        return configuration.getConfiguration();
    }
}
