/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.sourcecontrols.ConcurrentVersionsSystem;

import org.jdom.JDOMException;

public class ConfigurationTest extends TestCase {
    private Configuration configuration;
    
    private String contents;

    protected void setUp() throws Exception {
        super.setUp();

        configuration = new Configuration("localhost", 7856);
        contents = configuration.getConfiguration();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        
        configuration.setConfiguration(contents);
    }

    public void testGetConfiguration() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        String contents = getContents();
        String xmlHdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        assertTrue(contents.indexOf(xmlHdr) == 0);
        assertTrue(contents.indexOf("<cruisecontrol>") != -1);
        assertTrue(contents.indexOf("</cruisecontrol>") != -1);
    }

    public void testSetConfiguration() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException, InvalidAttributeValueException {
        String addContent = "<!-- Hello, world! -->";
        configuration.setConfiguration(getContents() + addContent);
        assertTrue(getContents().indexOf(addContent) != -1);
    }

    public void testShouldUpdatePluginElement()
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException, JDOMException,
            InvalidAttributeValueException {
        String addContent = "projects/foobar";
        PluginDetail cvsDetail = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        PluginConfiguration pluginConfiguration = new PluginConfiguration(
                cvsDetail, configuration);
        pluginConfiguration.setDetail("cvsRoot", "projects/foobar");
        configuration.updatePlugin(pluginConfiguration);
        assertTrue(getContents().indexOf(addContent) != -1);
    }

    private String getContents() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        return configuration.getConfiguration();
    }
}
