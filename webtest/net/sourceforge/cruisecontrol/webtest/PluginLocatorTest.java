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
import javax.management.ReflectionException;

import org.jdom.JDOMException;

import junit.framework.TestCase;
import junit.framework.Assert;
import net.sourceforge.cruisecontrol.interceptor.PluginLocator;
import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.PluginDetail;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginType;

public class PluginLocatorTest extends TestCase {
    private PluginLocator locator;

    protected void setUp() throws Exception {
        super.setUp();
        Configuration configuration = new Configuration("localhost", 7856);
        locator = new PluginLocator(configuration);
    }

    public void testGetAvailableBootstrappers() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        PluginDetail[] bootstrappers = locator.getAvailablePlugins("bootstrappers");
        assertNotNull(bootstrappers);
        assertTrue(0 < bootstrappers.length);
    }

    public void testGetAvailablePublishers() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        PluginDetail[] publishers = locator.getAvailablePlugins("publishers");
        assertNotNull(publishers);
        assertTrue(0 < publishers.length);
    }

    public void testGetAvailableSourceControls() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        PluginDetail[] sourceControls = locator.getAvailablePlugins("modificationset");
        assertNotNull(sourceControls);
        assertTrue(0 < sourceControls.length);
    }

    public void testGetConfiguredListeners() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, CruiseControlException, JDOMException {
        PluginDetail[] listeners = locator.getConfiguredPlugins("connectfour", "listener");
        assertNotNull(listeners);
        assertTrue(1 == listeners.length);
    }

    public void testGetConfiguredBootstrappers() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, CruiseControlException, JDOMException {
        PluginDetail[] bootstrappers = locator.getConfiguredPlugins("connectfour", "bootstrapper");
        assertNotNull(bootstrappers);
        assertTrue(1 == bootstrappers.length);
    }

    public void testGetConfiguredSourceControls() throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, CruiseControlException, JDOMException {
        PluginDetail[] sourceControls = locator.getConfiguredPlugins("connectfour", "sourcecontrol");
        assertNotNull(sourceControls);
        assertTrue(1 == sourceControls.length);
    }

    public void testGetCVSDetail() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException, IOException {
        PluginDetail cvsDetail = locator.getPluginDetail("cvs", "sourcecontrol");
        assertEquals("cvs", cvsDetail.getName());
        assertEquals("sourcecontrol", cvsDetail.getType().getName());
        Assert.assertEquals(PluginType.SOURCE_CONTROL, cvsDetail.getType());
        assertEquals(6, cvsDetail.getRequiredAttributes().length);
    }

    public void testGetSVNDetail() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException, IOException {
        PluginDetail svnDetail = locator.getPluginDetail("svn", "sourcecontrol");
        assertEquals("svn", svnDetail.getName());
        assertEquals("sourcecontrol", svnDetail.getType().getName());
        assertEquals(PluginType.SOURCE_CONTROL, svnDetail.getType());
        assertEquals(6, svnDetail.getRequiredAttributes().length);
    }
}
