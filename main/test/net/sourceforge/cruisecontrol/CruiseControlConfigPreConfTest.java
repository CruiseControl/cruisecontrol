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
import net.sourceforge.cruisecontrol.publishers.OnSuccessPublisher;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom2.Element;

public class CruiseControlConfigPreConfTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private CruiseControlConfig config;
    private File configFile;

    @Override
    protected void setUp() throws Exception {
        CruiseControlSettings.getInstance(this);

        URL url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig-preconf.xml");
        configFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));

        Element rootElement = Util.loadRootElement(configFile);

        final File testLogsDir = new File(TestUtil.getTargetDir(), "logs");
        // Pre-create parent "logs" dir to minimize chance of error creating project-log dir on Winz
        Util.doMkDirs(testLogsDir);
        config = new CruiseControlConfig(rootElement);
        filesToDelete.add(testLogsDir);
    }

    @Override
    protected void tearDown() {
        configFile = null;
        config = null;

        filesToDelete.delete();
        CruiseControlSettings.delInstance(this);
    }

    public void testGetProjectNames() {
        assertEquals(11, config.getProjectNames().size());
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
        return getClassInList(list, 0);
    }
    private String getClassInList(List list, int index) {
        return list.get(index).getClass().getName();
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


    public void testPluginInherritnance_leve1() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project9");
        List builders = projConfig.getSchedule().getBuilders();
        List publishers = projConfig.getPublishers();

        assertEquals(2, publishers.size());
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers, 0));
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers, 1));

        System.out.println(getClassInList(builders));
        Foo foo = (Foo) builders.get(0);
        assertNotNull("createProperty wasn't called", foo.property);
        assertEquals("bar", foo.property.getName());
        assertEquals("baz", foo.property.getValue());
        assertEquals("v1",  foo.att1);
        assertEquals("",    foo.att2);
        assertEquals("val1",foo.getEnv("ENV1"));
        assertEquals("val2",foo.getEnv("ENV2"));
    }

    public void testPluginInherritnance_leve2() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project10");
        List builders = projConfig.getSchedule().getBuilders();
        List publishers = projConfig.getPublishers();

        assertEquals(3, publishers.size());
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers, 0));
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers, 1));
        assertEquals(OnSuccessPublisher.class.getName(), getClassInList(publishers, 2));

        System.out.println(getClassInList(builders));
        Foo foo = (Foo) builders.get(0);
        assertNotNull("createProperty wasn't called", foo.property);
        assertEquals("foo", foo.property.getName());
        assertEquals("bar", foo.property.getValue());
        assertEquals("v1",  foo.att1);
        assertEquals("",    foo.att2);
        assertEquals("val1",foo.getEnv("ENV1"));
        assertEquals("val2",foo.getEnv("ENV2"));
        assertEquals("val3",foo.getEnv("ENV3"));
        assertEquals("embedded",foo.getEnv("EMB"));
    }

    public void testPluginInherritnance_leve3() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project11");
        List builders = projConfig.getSchedule().getBuilders();
        List publishers = projConfig.getPublishers();

        assertEquals(3, publishers.size());
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers, 0));
        assertEquals(AntPublisher.class.getName(), getClassInList(publishers, 1));
        assertEquals(OnSuccessPublisher.class.getName(), getClassInList(publishers, 2));

        System.out.println(getClassInList(builders));
        Foo foo = (Foo) builders.get(0);
        assertNotNull("createProperty wasn't called", foo.property);
        assertEquals("foo", foo.property.getName());
        assertEquals("bar", foo.property.getValue());
        assertEquals("v1.3",foo.att1);
        assertEquals("v2.3",foo.att2);
        assertEquals("val1",foo.getEnv("ENV1"));
        assertEquals("val2",foo.getEnv("ENV2"));
        assertEquals("val3.override",foo.getEnv("ENV3"));
        assertEquals("N/A", foo.getEnv("EMB"));
    }


    public static class Foo extends Builder {
        private Property property;
        private String att1 = "";
        private String att2 = "";

        public Property createProperty() {
            property = new Property();
            return property;
        }

        public void setAtt1(String val) {
            att1 = val;
        }
        public void setAtt2(String val) {
            att2 = val;
        }

        public Element build(Map properties, Progress progress) throws CruiseControlException {
            return null;
        }
        public Element buildWithTarget(Map properties, String target, Progress progress) throws CruiseControlException {
            return null;
        }

        public String getEnv(final String name) {
            OSEnvironment env = new OSEnvironment();
            mergeEnv(env);
            return env.getVariable(name, "N/A");
        }
    }
}
