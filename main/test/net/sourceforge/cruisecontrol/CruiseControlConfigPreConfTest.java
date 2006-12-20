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
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.Property;
import net.sourceforge.cruisecontrol.publishers.AntPublisher;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigPreConfTest extends TestCase {

    private CruiseControlConfig config;
    private File configFile;
    private File tempDirectory;

    protected void setUp() throws Exception {
        // Set up a CruiseControl config file for testing
        URL url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig-preconf.xml");
        configFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));
        tempDirectory = configFile.getParentFile();

        Element rootElement = Util.loadRootElement(configFile);
        config = new CruiseControlConfig(rootElement);
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

    public void testProjectPreConfiguration() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project3");
        
        List bootstrappers = projConfig.getBootstrappers();
        assertEquals(1, bootstrappers.size());
        
        VssBootstrapper vss = (VssBootstrapper) bootstrappers.get(0);
        Field vssPath = VssBootstrapper.class.getDeclaredField("vssPath");
        vssPath.setAccessible(true);
        assertEquals("foo", vssPath.get(vss));

        Schedule schedule = projConfig.getSchedule();
        assertEquals(20 * 1000, schedule.getInterval());
        List builders = schedule.getBuilders();
        assertEquals(1, builders.size());
        
        AntBuilder ant = (AntBuilder) builders.get(0);
        Field buildFile = AntBuilder.class.getDeclaredField("buildFile");
        buildFile.setAccessible(true);
        assertEquals("checkout/project3/build.xml", buildFile.get(ant));
        
        Field properties = AntBuilder.class.getDeclaredField("properties");
        properties.setAccessible(true);
        List antProperties = (List) properties.get(ant);
        assertEquals(1, antProperties.size());
        Property property = (Property) antProperties.get(0);
        assertEquals("project.name", property.getName());
        assertEquals("project3", property.getValue());
        
        List listeners = projConfig.getListeners();
        assertEquals(1, listeners.size());
        String listenerClassName = listeners.get(0).getClass().getName();
        assertEquals("net.sourceforge.cruisecontrol.listeners.CurrentBuildStatusListener", listenerClassName);
    }
    
    public void testPreConfiguredPluginInPreconfiguredProject() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project2");
        List publishers = projConfig.getPublishers();
        assertEquals(1, publishers.size());
        
        AntPublisher antPublisher = (AntPublisher) publishers.get(0);
        Field delegate = AntPublisher.class.getDeclaredField("delegate");
        delegate.setAccessible(true);
        AntBuilder ant = (AntBuilder) delegate.get(antPublisher);
        Field properties = AntBuilder.class.getDeclaredField("properties");
        properties.setAccessible(true);
        List publisherProperties = (List) properties.get(ant);
        assertEquals(1, publisherProperties.size());
    }
    

    // TODO
    /*
    public void testPreConfiguredProjectSubtypes() throws Exception {
    }
    */

}
