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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.listeners.ListenerTestNestedPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestOtherNestedPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestSelfConfiguringPlugin;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigTest extends TestCase {

    private CruiseControlConfig config;
    private File configFile;
    private File tempDirectory;
    private File propertiesFile;

    private static final int ONE_SECOND = 1000;

    protected void setUp() throws Exception {
        URL url;
        url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/test.properties");
        propertiesFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));

        // Set up a CruiseControl config file for testing
        url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig.xml");
        configFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));
        tempDirectory = configFile.getParentFile();

        Element ccElement = Util.loadRootElement(configFile);
        Element testpropertiesdir = new Element("property");
        testpropertiesdir.setAttribute("name", "test.properties.dir");
        testpropertiesdir.setAttribute("value", propertiesFile.getParentFile().getAbsolutePath());
        ccElement.addContent(0, testpropertiesdir);
        
        config = new CruiseControlConfig(ccElement);
    }

    protected void tearDown() {
        // The directory "foo" in the system's temporary file location
        // is created by CruiseControl when using the config file below.
        // Specifically because of the line:
        //     <log dir='" + tempDirPath + "/foo' encoding='utf-8' >
        File fooDirectory = new File(tempDirectory, "foo");
        fooDirectory.delete();
    }
    
    public void testUseNonDefaultProjects() throws CruiseControlException {
        Element root = new Element("cruisecontrol");

        Element plugin = new Element("plugin");
        plugin.setAttribute("name", "foo");
        plugin.setAttribute("classname", DummyProject.class.getName());
        root.addContent(0, plugin);
        
        Element dummy = new Element("foo");
        dummy.setAttribute("name", "dummy");
        root.addContent(dummy);
        
        config = new CruiseControlConfig(root);
        assertEquals(1, config.getProjectNames().size());
        assertNotNull(config.getProject("dummy"));
    }
    
    public static class DummyProject implements ProjectInterface {

        private String name;

        public void configureProject() throws CruiseControlException {
        }

        public void execute() {
        }
        
        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void getStateFromOldProject(ProjectInterface project) throws CruiseControlException {
        }

        public void register(MBeanServer server) throws JMException {
        }

        public void setBuildQueue(BuildQueue buildQueue) {
        }

        public void start() {
        }

        public void stop() {
        }

        public void validate() throws CruiseControlException {
        }
        
    }
    
    public void testProjectNamesShouldMatchOrderInFile() {
        Set names = config.getProjectNames();
        Iterator iter = names.iterator();
        assertEquals("project1", (String) iter.next());
        assertEquals("project2", (String) iter.next());
        assertEquals("project3", (String) iter.next());
        assertEquals("project3bis", (String) iter.next());
        assertEquals("project4", (String) iter.next());
    }

    public void testGetProjectNames() {
        assertEquals(17, config.getProjectNames().size());
    }

    public void testGlobalProperty() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("simpleprops");
        Properties props = projConfig.getProperties();
        assertEquals(6, props.size()); // 4 in file, 1 global passed to constructor +
        assertEquals("works!", props.getProperty("global"));
    }

    public void testProjectNameProperty() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project1");
        Properties props = projConfig.getProperties();
        assertEquals(5, props.size());
        assertEquals("project1", props.getProperty("project.name"));
    }    

    public void testProjectNameInGlobalProperty() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project1");
        Properties props = projConfig.getProperties();
        assertEquals(5, props.size());
        assertEquals("works!", props.getProperty("global"));
        assertEquals("project1", props.getProperty("project.name"));
        assertEquals("project=project1", props.getProperty("project.global"));
    }    

    public void testSimpleProperty() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("simpleprops");
        Properties props = projConfig.getProperties();
        assertEquals(6, props.size());
        assertEquals("success!", props.getProperty("simple"));
    }

    public void testMultipleProperties() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("multiprops");
        Properties props = projConfig.getProperties();
        assertEquals(9, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
        assertEquals("three", props.getProperty("third"));
        assertEquals("one.two$three", props.getProperty("multi"));
    }

    public void testNestedProperties() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("nestedprops");
        Properties props = projConfig.getProperties();
        assertEquals(11, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
        assertEquals("three", props.getProperty("third"));
        assertEquals("almost", props.getProperty("one.two.three"));
        assertEquals("threeLevelsDeep", props.getProperty("almost"));
        assertEquals("threeLevelsDeep", props.getProperty("nested"));
    }

    public void testPropertyEclipsing() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("eclipseprop");
        Properties props = projConfig.getProperties();
        assertEquals(5, props.size());
        assertEquals("eclipsed", props.getProperty("global"));
    }
    
    public void testLoadPropertiesFromFile() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("propsfromfile");
        Properties props = projConfig.getProperties();
        assertEquals(10, props.size());
        assertEquals("/home/cruise", props.getProperty("dir1"));
        assertEquals("/home/cruise/logs", props.getProperty("dir2"));
        assertEquals("temp", props.getProperty("tempdir"));
        assertEquals("/home/cruise/logs/temp", props.getProperty("multi"));
    }

    // test that we are capable of resolving properties in all property attributes
    public void testPropertiesInProperties() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("propsinpropsdef");
        Properties props = projConfig.getProperties();
        // these ones where defined normally, shouldn't be any problem
        assertEquals("true", props.getProperty("env.toupper"));
        assertEquals("env", props.getProperty("env.prefix"));

        assertEquals("Resolving property file name attribute worked",
                     "/home/cruise", props.getProperty("dir1"));
        assertEquals("Resolving property name attribute worked",
                      "test1", props.getProperty("test1"));
        int nbEnvPropertiesFound = 0;
        for (Enumeration propertyNames = props.propertyNames(); propertyNames.hasMoreElements(); ) {
           String name = (String) propertyNames.nextElement();
           if (name.startsWith("env.")) {
             nbEnvPropertiesFound++;
           }
        }
        assertTrue("Resolving environment prefix attribute worked",
                       nbEnvPropertiesFound > 0);
        assertNotNull("Resolving environment prefix and touuper attributes worked",
                       props.getProperty("env.PATH"));
    }

    // TODO backport
    /*
    public void testMissingProperty() {
        // there's in fact little need to check for both cases.
        // This will be hardcoded at some point and the default case.
        // Feel free to scrap the first if when checking it in.
        if (ProjectXMLHelper.FAIL_UPON_MISSING_PROPERTY) {
            try {
                createProjectXMLHelper("missingprop");
                fail("A missing property should cause an exception!");
            } catch (CruiseControlException expected) {
            }
        } else {
            try {
                createProjectXMLHelper("missingprop");
            } catch (CruiseControlException unexpected) {
                fail(unexpected.getMessage());
            }
        }
    }
    */

    // TODO this a test of the PluginHelper
    public void testGetPluginConfigNoOverride() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project1");
        PluginRegistry registry = config.getProjectPlugins("project1");

        assertEquals(ListenerTestNestedPlugin.class, registry.getPluginClass("testnested"));

        final ProjectXMLHelper helper
            = new ProjectXMLHelper(projConfig.getProperties(), registry);

        PluginXMLHelper pluginHelper = new PluginXMLHelper(helper);
        Object plugin;

        plugin = helper.getConfiguredPlugin(pluginHelper, "testnested");
        assertEquals(ListenerTestNestedPlugin.class, plugin.getClass());
        ListenerTestNestedPlugin plug1 = (ListenerTestNestedPlugin) plugin;
        assertEquals("default", plug1.getString());
        assertEquals("otherdefault", plug1.getOtherString());

        plugin = helper.getConfiguredPlugin(pluginHelper, "testselfconfiguring");
        assertEquals(null, plugin);

        plugin = helper.getConfiguredPlugin(pluginHelper, "testlistener");
        assertEquals(null, plugin);
    }

    // TODO this a test of the PluginHelper
    public void testGetPluginConfig() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project4");
        PluginRegistry registry = config.getProjectPlugins("project4");

        final ProjectXMLHelper helper
            = new ProjectXMLHelper(projConfig.getProperties(), registry);

        PluginXMLHelper pluginHelper = new PluginXMLHelper(helper);
        Object plugin;

        plugin = helper.getConfiguredPlugin(pluginHelper, "testnested");
        assertEquals(ListenerTestNestedPlugin.class, plugin.getClass());
        ListenerTestNestedPlugin plug1 = (ListenerTestNestedPlugin) plugin;
        assertEquals("overriden", plug1.getString());
        // not overriden
        assertEquals("otherdefault", plug1.getOtherString());

        plugin = helper.getConfiguredPlugin(pluginHelper, "testselfconfiguring");
        assertEquals(ListenerTestSelfConfiguringPlugin.class, plugin.getClass());
        ListenerTestSelfConfiguringPlugin plug2 = (ListenerTestSelfConfiguringPlugin) plugin;
        assertEquals(null, plug2.getString());
        assertEquals(null, plug2.getNested());

        plugin = helper.getConfiguredPlugin(pluginHelper, "testlistener");
        assertEquals(ListenerTestPlugin.class, plugin.getClass());
        ListenerTestPlugin plug3 = (ListenerTestPlugin) plugin;
        assertEquals("project4-0", plug3.getString());
    }

    public void testPluginConfiguration() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project4");
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
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project5");
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

    public void testPropertiesInFullProjectTemplate() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project6");
        assertNotNull(config.getProjectPlugins("project6"));

        Schedule schedule = projConfig.getSchedule();
        assertEquals(20 * ONE_SECOND, schedule.getInterval());

        Properties props = projConfig.getProperties();
        assertEquals(6, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
    }


    // TODO DateFormat management was moved to Project.init()
    /*
    public void testDateFormat() throws Exception {
        final String originalFormat = DateFormatFactory.getFormat();
        createProjectXMLHelper("dateformatfromproperty");
        final String formatFromProperty = DateFormatFactory.getFormat();
        DateFormatFactory.setFormat(DateFormatFactory.DEFAULT_FORMAT);

        assertEquals(DateFormatFactory.DEFAULT_FORMAT, originalFormat);
        assertEquals("MM/dd/yyyy HH:mm:ss a", formatFromProperty);
        assertFalse(originalFormat.equals(formatFromProperty));
    }

    public void testPreconfigureDateFormat() throws Exception {
        final String originalFormat = DateFormatFactory.getFormat();
        createProjectXMLHelper("dateformatpreconfigured");
        final String formatFromProperty = DateFormatFactory.getFormat();
        DateFormatFactory.setFormat(DateFormatFactory.DEFAULT_FORMAT);

        assertEquals(DateFormatFactory.DEFAULT_FORMAT, originalFormat);
        assertEquals("MM/dd/yyyy HH:mm:ss a", formatFromProperty);
        assertFalse(originalFormat.equals(formatFromProperty));
    }
    */

    public void testGetBootstrappers() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project1");

        List bootstrappers = projConfig.getBootstrappers();
        assertEquals(0, bootstrappers.size());

        projConfig = (ProjectConfig) config.getProject("project2");
        bootstrappers = projConfig.getBootstrappers();
        assertEquals(1, bootstrappers.size());
    }

    public void testGetSchedule() {
        ProjectConfig projConfig;
        // TODO
        /*
        projConfig = config.getConfig("project1");
        try {
            projConfig.getSchedule();
            fail("schedule should be a required element");
        } catch (CruiseControlException expected) {
        }
        */

        projConfig = (ProjectConfig) config.getProject("project2");
        Schedule schedule = projConfig.getSchedule();
        assertEquals(20 * ONE_SECOND, schedule.getInterval());
    }

    public void testGetModificationSet() {
        ProjectConfig projConfig;
        // TODO
        /*
        projConfig = config.getConfig("project1");
        try {
            projConfig.getModificationSet();
            fail("modificationset should be a required element");
        } catch (CruiseControlException expected) {
        }
        */

        projConfig = (ProjectConfig) config.getProject("project2");
        ModificationSet modSet = projConfig.getModificationSet();
        assertEquals(10 * ONE_SECOND, modSet.getQuietPeriod());
    }

    public void testGetLabelIncrementer() throws CruiseControlException {
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", CruiseControlConfig.LABEL_INCREMENTER);
        pluginElement.setAttribute("classname", DefaultLabelIncrementer.class.getName());
        PluginRegistry.registerToRoot(pluginElement);
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project2");
        DefaultLabelIncrementer incrementer = (DefaultLabelIncrementer) projConfig.getLabelIncrementer();
        assertTrue(incrementer.isValidLabel("build#9"));

        projConfig = (ProjectConfig) config.getProject("project1");
        incrementer = (DefaultLabelIncrementer) projConfig.getLabelIncrementer();
        assertFalse(incrementer.isValidLabel("build#9"));
    }

    public void testGetLog() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project1");
        assertEquals("logs" + File.separatorChar + "project1", projConfig.getLog().getLogDir());

        projConfig = (ProjectConfig) config.getProject("project2");
        assertEquals(tempDirectory.getAbsolutePath() + "/foo", projConfig.getLog().getLogDir());

        projConfig = (ProjectConfig) config.getProject("project3");
        assertEquals("logs" + File.separatorChar + "project3", projConfig.getLog().getLogDir());

        projConfig = (ProjectConfig) config.getProject("project3bis");
        assertEquals("logs/project3bis", projConfig.getLog().getLogDir());
        assertNull(projConfig.getLog().getLogXmlEncoding());

        projConfig = (ProjectConfig) config.getProject("project2");
        assertEquals("utf-8", projConfig.getLog().getLogXmlEncoding());
    }

    public void testPreconfigureLog() throws Exception {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("logpreconfigured");

        final Log log = projConfig.getLog();
        assertEquals("mylogs/logpreconfigured", log.getLogDir());
        assertEquals("utf128", log.getLogXmlEncoding());
        assertEquals("logpreconfigured", log.getProjectName());

        BuildLogger[] loggers = log.getLoggers();
        assertEquals(2, loggers.length);
    }

    public void testGetListeners() {
        ProjectConfig projConfig = (ProjectConfig) config.getProject("project1");
        List listeners = projConfig.getListeners();
        assertEquals(0, listeners.size());

        projConfig = (ProjectConfig) config.getProject("project2");
        listeners = projConfig.getListeners();
        assertEquals(1, listeners.size());
    }
}
