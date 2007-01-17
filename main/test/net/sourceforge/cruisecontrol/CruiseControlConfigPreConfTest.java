/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.Property;
import net.sourceforge.cruisecontrol.listeners.CurrentBuildStatusListener;
import net.sourceforge.cruisecontrol.publishers.AntPublisher;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigPreConfTest extends TestCase {

    private CruiseControlConfig config;
    private File configFile;

    protected void setUp() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig-preconf.xml");
        configFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));

        Element rootElement = Util.loadRootElement(configFile);
        config = new CruiseControlConfig(rootElement);
    }

    protected void tearDown() {
        configFile = null;
        config = null;
    }

    public void testGetProjectNames() {
        assertEquals(7, config.getProjectNames().size());
    }

    public void testProjectPreConfiguration() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project3");

        List bootstrappers = projConfig.getBootstrappers();
        assertEquals(1, bootstrappers.size());
        assertEquals(VssBootstrapper.class.getName(), getClassInList(bootstrappers));

        Schedule schedule = projConfig.getSchedule();
        assertEquals(20 * 1000, schedule.getInterval());
        List builders = schedule.getBuilders();
        assertEquals(1, builders.size());
        assertEquals(AntBuilder.class.getName(), getClassInList(builders));

        List listeners = projConfig.getListeners();
        assertEquals(1, listeners.size());
        assertEquals(CurrentBuildStatusListener.class.getName(), getClassInList(listeners));
    }

    private String getClassInList(List list) {
        return list.get(0).getClass().getName();
    }

    public void testPreConfiguredPluginInPreconfiguredProject() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project2");
        List publishers = projConfig.getPublishers();
        assertEquals(1, publishers.size());
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers));
    }


    // TODO
    /*
    public void testPreConfiguredProjectSubtypes() throws Exception {
    }
    */

    public void testPreConfiguredNestedProperties() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project7");
        Schedule schedule = projConfig.getSchedule();
        List builders = schedule.getBuilders();
        System.out.println(getClassInList(builders));
        Foo foo = (Foo) builders.get(0);
        assertNotNull("createProperty wasn't called", foo.property);
        assertEquals("bar", foo.property.getName());
        assertEquals("baz", foo.property.getValue());
    }

    public static class Foo extends Builder {
        private Property property;

        public Property createProperty() {
            property = new Property();
            return property;
        }

        public Element build(Map properties) throws CruiseControlException {
            return null;
        }
        public Element buildWithTarget(Map properties, String target) throws CruiseControlException {
            return null;
        }
    }

}
