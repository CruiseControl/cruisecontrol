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
import javax.management.MBeanException;
import javax.management.ReflectionException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.interceptor.PluginLocator;

public class PluginLocatorTest extends TestCase {
    private PluginLocator locator;

    protected void setUp() throws Exception {
        super.setUp();

        Configuration configuration = new Configuration("localhost", 7856);
        locator = new PluginLocator(configuration);
    }

    public void testShouldGetBootstrappers() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        PluginDetail[] bootstrappers = locator.getPlugins("bootstrappers");
        assertEquals(12, bootstrappers.length);
        assertEquals("accurevbootstrapper", bootstrappers[0].getPluginName());
        assertEquals("bootstrappers", bootstrappers[0].getPluginType());
        assertEquals("cvsbootstrapper", bootstrappers[7].getPluginName());
        assertEquals("bootstrappers", bootstrappers[7].getPluginType());
        assertEquals("svnbootstrapper", bootstrappers[10].getPluginName());
        assertEquals("bootstrappers", bootstrappers[10].getPluginType());
    }

    public void testShouldGetPublishers() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        PluginDetail[] publishers = locator.getPlugins("publishers");
        assertEquals(17, publishers.length);
        assertEquals("antpublisher", publishers[0].getPluginName());
        assertEquals("publishers", publishers[0].getPluginType());
        assertEquals("ftppublisher", publishers[7].getPluginName());
        assertEquals("publishers", publishers[7].getPluginType());
        assertEquals("xsltlogpublisher", publishers[16].getPluginName());
        assertEquals("publishers", publishers[16].getPluginType());
    }

    public void testShouldGetSourceControls()
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        PluginDetail[] srcControls = locator.getPlugins("modificationset");
        assertEquals(18, srcControls.length);
        assertEquals("accurev", srcControls[0].getPluginName());
        assertEquals("modificationset", srcControls[0].getPluginType());
        assertEquals("cvs", srcControls[7].getPluginName());
        assertEquals("modificationset", srcControls[7].getPluginType());
        assertEquals("svn", srcControls[15].getPluginName());
        assertEquals("modificationset", srcControls[15].getPluginType());
    }

    public void testShouldGetCVSDetail() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        PluginDetail cvsDetail = locator.getPluginDetail("cvs",
                "modificationset");
        assertEquals("cvs", cvsDetail.getPluginName());
        assertEquals("modificationset", cvsDetail.getPluginType());
        assertEquals(6, cvsDetail.getRequiredAttributes().length);
    }

    public void testShouldGetSVNDetail() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        PluginDetail svnDetail = locator.getPluginDetail("svn",
                "modificationset");
        assertEquals("svn", svnDetail.getPluginName());
        assertEquals("modificationset", svnDetail.getPluginType());
        assertEquals(6, svnDetail.getRequiredAttributes().length);
    }
}
