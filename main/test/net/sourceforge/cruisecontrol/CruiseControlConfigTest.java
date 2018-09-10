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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.ExecBuilder;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.listeners.ListenerTestNestedPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestOtherNestedPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom2.Element;

public class CruiseControlConfigTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private CruiseControlConfig config;
    private File configFile;
    private File classpathDirectory;
    private File propertiesFile;

    private static final int ONE_SECOND = 1000;

    @Override
    protected void setUp() throws Exception {
        CruiseControlSettings.getInstance(this);

        URL url;
        url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/test.properties");
        propertiesFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));

        // Set up a CruiseControl config file for testing
        url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig.xml");
        configFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));
        classpathDirectory = configFile.getParentFile();

        Element ccElement = Util.loadRootElement(configFile);
        Element testpropertiesdir = new Element("property");
        testpropertiesdir.setAttribute("name", "test.properties.dir");
        testpropertiesdir.setAttribute("value", propertiesFile.getParentFile().getAbsolutePath());
        ccElement.addContent(0, testpropertiesdir);

        // The directory "foo" in the classpath is
        // created by CruiseControl by the log element
        // in testconfig.xml.
        filesToDelete.add(new File(classpathDirectory, "foo"));

        // The directories "${missing}", "mylogs", and "logs"
        // created by CruiseControl by the log element
        // in testconfig.xml.
        filesToDelete.add(new File(TestUtil.getTargetDir(), "${missing}"));
        filesToDelete.add(new File(TestUtil.getTargetDir(), "mylogs"));
        filesToDelete.add(new File(TestUtil.getTargetDir(), "logs"));

        config = new CruiseControlConfig(ccElement);
    }

    @Override
    protected void tearDown() {
        filesToDelete.delete();

        propertiesFile = null;
        configFile = null;
        classpathDirectory = null;
        config = null;

        CruiseControlSettings.delInstance(this);
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

    public void testUseDefaultProjects() throws CruiseControlException {
        Element root = new Element("cruisecontrol");

        Element plugin = new Element("plugin");
        plugin.setAttribute("name", "foo");
        plugin.setAttribute("from", "project"); // pluin from in-build CC project class
        root.addContent(0, plugin);

        Element dummy = new Element("foo");
        dummy.setAttribute("name", "dummy");
        root.addContent(dummy);

        Element content = new Element("schedule");
        content.addContent(new Element("ant"));
        dummy.addContent(content);

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

        public Map<String, String> getProperties() {
            return null;
        }

        public List<Modification> modificationsSinceLastBuild() {
            return null;
        }

        public Date successLastBuild() {
            return null;
        }

        public List<Modification> modificationsSince(Date since) {
            return null;
        }

        public String getLogDir() {
            return null;
        }

        public String successLastLabel() {
            return null;
        }

        public String successLastLog() {
            return null;
        }
    }

    public void testProjectNamesShouldMatchOrderInFile() {
        Set names = config.getProjectNames();
        Iterator iter = names.iterator();
        assertEquals("project1", (String) iter.next());
        assertEquals("preconfigured.project", (String) iter.next());
        assertEquals("project2", (String) iter.next());
        assertEquals("project.global", (String) iter.next());
        assertEquals("project4", (String) iter.next());
    }

    public void testGetProjectNames() {
        assertEquals(24, config.getProjectNames().size());
    }

    public void testGlobalProperty() throws Exception {
        String targetProject = "simple.global";
        String expectedPropertyValue = "works!";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    private void assertPropertyValue(String targetProject, String expectedPropertyValue) {
        MockProjectInterface projConfig = (MockProjectInterface) config.getProject(targetProject);
        MockProjectInterface.Foo foo = projConfig.getFoo();
        assertEquals(expectedPropertyValue, foo.getName());
    }

    public void testProjectNameProperty() throws Exception {
        String targetProject = "project1";
        String expectedPropertyValue = "project1";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    public void testProjectNameInGlobalProperty() throws Exception {
        String targetProject = "project.global";
        String expectedPropertyValue = "project=project.global";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    public void testSimpleProperty() throws Exception {
        String targetProject = "simpleprops";
        String expectedPropertyValue = "success!";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    public void testMultipleProperties() throws Exception {
        String targetProject = "multiprops";
        String expectedPropertyValue = "one.two$three";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    public void testNestedProperties() throws Exception {
        String targetProject = "nestedprops";
        String expectedPropertyValue = "threeLevelsDeep";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    public void testPropertyEclipsing() throws Exception {
        String targetProject = "eclipseprop";
        String expectedPropertyValue = "eclipsed";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    public void testLoadPropertiesFromFile() throws Exception {
        String targetProject = "propsfromfile";
        String expectedPropertyValue = "/home/cruise/logs/temp";
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    // test that we are capable of resolving properties in all property attributes
    public void testPropertiesInProperties() throws Exception {
        String targetProject = "propsinpropsdef";
        String expectedPropertyValue = new OSEnvironment().getVariableIgnoreCase("PATH");
        assertPropertyValue(targetProject, expectedPropertyValue);
    }

    // test that we are capable of resolving properties redefined in various ways
    public void testPropertiesRedefine() throws Exception {
        ListenerTestPlugin listener;
        ProjectConfig projConfig = (ProjectConfig) config.getProject("inherit1");
        List<Listener> listeners = projConfig.getListeners();

        listener = (ListenerTestPlugin) listeners.get(0);
        assertEquals("override", listener.getString());

        listener = (ListenerTestPlugin) listeners.get(1);
        assertEquals("test", listener.getString());

        listener = (ListenerTestPlugin) listeners.get(2);
        assertEquals("filled_test", listener.getString());

        listener = (ListenerTestPlugin) listeners.get(3);
        assertEquals("value", listener.getString());


        projConfig = (ProjectConfig) config.getProject("inherit2");
        listeners = projConfig.getListeners();

        listener = (ListenerTestPlugin) listeners.get(0);
        assertEquals("works!", listener.getString());

        listener = (ListenerTestPlugin) listeners.get(1);
        assertEquals("empty", listener.getString());

        listener = (ListenerTestPlugin) listeners.get(2);
        assertEquals("filled_empty", listener.getString());

        listener = (ListenerTestPlugin) listeners.get(3);
        assertEquals("temp", listener.getString());
    }

    // test that we are capable of resolving properties redefined in various ways
    public void testCustomProperties() throws Exception {
        MockProjectInterface projConfig;
        MockProjectInterface.Foo foo;

        projConfig = (MockProjectInterface) config.getProject("customprops1");
        foo = projConfig.getFoo();
        assertEquals("mockval_value", foo.getName());

        projConfig = (MockProjectInterface) config.getProject("customprops2");
        foo = projConfig.getFoo();
        assertEquals("mockval_customprops2", foo.getName());

        projConfig = (MockProjectInterface) config.getProject("customprops3");
        foo = projConfig.getFoo();
        assertEquals("mockval_temp", foo.getName());

        projConfig = (MockProjectInterface) config.getProject("customprops4");
        foo = projConfig.getFoo();
        assertEquals("mockval_justval", foo.getName());

        projConfig = (MockProjectInterface) config.getProject("customprops5");
        foo = projConfig.getFoo();
        assertEquals("local-in-customprops5_filled", foo.getName());

        projConfig = (MockProjectInterface) config.getProject("customprops6");
        foo = projConfig.getFoo();
        assertEquals("local-in-customprops6_filled_works!", foo.getName());
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

    public void testPluginConfiguration() throws Exception {
        final ProjectConfig projConfig = (ProjectConfig) config.getProject("project4");
        final PluginRegistry plugins = config.getProjectPlugins("project4");

        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestNestedPlugin.class, plugins.getPluginClass("testnested"));

        final List listeners = projConfig.getListeners();
        assertEquals(3, listeners.size());

        final Listener listener0 = (Listener) listeners.get(0);
        assertEquals(ListenerTestPlugin.class, listener0.getClass());
        final ListenerTestPlugin testListener0 = (ListenerTestPlugin) listener0;
        assertEquals("project4-0", testListener0.getString());

        final Listener listener1 = (Listener) listeners.get(1);
        assertEquals(ListenerTestPlugin.class, listener1.getClass());
        final ListenerTestPlugin testListener1 = (ListenerTestPlugin) listener1;
        assertEquals("listener1", testListener1.getString());
        assertEquals("wrapper1", testListener1.getStringWrapper().getString());

        final Listener listener2 = (Listener) listeners.get(2);
        assertEquals(ListenerTestPlugin.class, listener2.getClass());
        final ListenerTestPlugin testListener2 = (ListenerTestPlugin) listener2;
        assertEquals("listener2", testListener2.getString());
        // note this is in fact undefined behavior!! Because we added twice the stringwrapper
        // (first for the child, then for the parent).
        // this could probably fail depending on a different platform, except if Element.setContent()
        // specifies the order in which children are kept within the element.
        final String wrapper = testListener2.getStringWrapper().getString();
        assertTrue("wrapper2-works!", "wrapper2-works!".equals(wrapper) || "wrapper1".equals(wrapper));
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
        assertEquals("notshadowing", nested.getString());
        assertEquals(null, nested.getOtherString());
        assertEquals("otherother", ((ListenerTestOtherNestedPlugin) nested).getOtherOtherString());
    }

    public void testPluginConfigurationInherit() throws Exception {
        ExecBuilder builder = new ExecBuilder();
        // Get the working directory when not explicitly set
        builder.setCommand("foo");
        builder.validate();
        final String wdir = builder.getWorkingDir();

        // project override1
        ProjectConfig projConfig = (ProjectConfig) config.getProject("inherit1");
        List builders = projConfig.getSchedule().getBuilders();
        assertEquals(5, builders.size());

        builder = (ExecBuilder)builders.get(0);
        assertEquals("cX", builder.getCommand());
        assertEquals("dA", builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(1);
        assertEquals("cB", builder.getCommand());
        assertEquals("dB", builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(2);
        assertEquals("cZ", builder.getCommand());
        assertEquals(wdir, builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(3);
        assertEquals("c+", builder.getCommand());
        assertEquals(wdir, builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(4);
        assertEquals("c*", builder.getCommand());
        assertEquals("dE", builder.getWorkingDir());

        // project override2
        projConfig = (ProjectConfig) config.getProject("inherit2");
        builders = projConfig.getSchedule().getBuilders();
        assertEquals(5, builders.size());

        builder = (ExecBuilder)builders.get(0);
        assertEquals("cX", builder.getCommand());
        assertEquals("dA", builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(1);
        assertEquals("cB", builder.getCommand());
        assertEquals("dB", builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(2);
        assertEquals("cX", builder.getCommand());
        assertEquals("dA", builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(3);
        assertEquals("c+", builder.getCommand());
        assertEquals(wdir, builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(4);
        assertEquals("c*", builder.getCommand());
        assertEquals("dE", builder.getWorkingDir());

        // project override1
        projConfig = (ProjectConfig) config.getProject("inherit3");
        builders = projConfig.getSchedule().getBuilders();
        assertEquals(5, builders.size());

        builder = (ExecBuilder)builders.get(0);
        assertEquals("foo", builder.getCommand());
        assertEquals(wdir, builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(1);
        assertEquals("cB", builder.getCommand());
        assertEquals("dB", builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(2);
        assertEquals("cZ", builder.getCommand());
        assertEquals(wdir, builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(3);
        assertEquals("c+", builder.getCommand());
        assertEquals(wdir, builder.getWorkingDir());
        builder = (ExecBuilder)builders.get(4);
        assertEquals("c*", builder.getCommand());
        assertEquals("dE", builder.getWorkingDir());
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
        ProjectConfig projConfig = (ProjectConfig) config.getProject("preconfigured.project");

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

        projConfig = (ProjectConfig) config.getProject("preconfigured.project");
        incrementer = (DefaultLabelIncrementer) projConfig.getLabelIncrementer();
        assertFalse(incrementer.isValidLabel("build#9"));
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

}
