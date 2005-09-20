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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestNestedPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestSelfConfiguringPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestOtherNestedPlugin;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

public class ProjectXMLHelperTest extends TestCase {

    private File configFile;
    private File tempDirectory;
    private File propertiesFile;

    private static final int ONE_SECOND = 1000;
    
    public ProjectXMLHelperTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
        Logger.getLogger(this.getClass()).getLoggerRepository().setThreshold(Level.OFF);
    }

    public void testGlobalProperty() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "simpleprops");
        Properties props = helper.getProperties();
        assertEquals(4, props.size());
        assertEquals("works!", props.getProperty("global"));
    }    

    public void testProjectNameProperty() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        Properties props = helper.getProperties();
        assertEquals(3, props.size());
        assertEquals("project1", props.getProperty("project.name"));
    }    

    public void testProjectNameInGlobalProperty() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        Properties props = helper.getProperties();
        assertEquals(3, props.size());
        assertEquals("works!", props.getProperty("global"));
        assertEquals("project1", props.getProperty("project.name"));
        assertEquals("project=project1", props.getProperty("project.global"));
    }    

    public void testSimpleProperty() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "simpleprops");
        Properties props = helper.getProperties();
        assertEquals(4, props.size());
        assertEquals("success!", props.getProperty("simple"));
    }

    public void testMultipleProperties() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "multiprops");
        Properties props = helper.getProperties();
        assertEquals(7, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
        assertEquals("three", props.getProperty("third"));
        assertEquals("one.two$three", props.getProperty("multi"));
    }

    public void testNestedProperties() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "nestedprops");
        Properties props = helper.getProperties();
        assertEquals(9, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
        assertEquals("three", props.getProperty("third"));
        assertEquals("almost", props.getProperty("one.two.three"));
        assertEquals("threeLevelsDeep", props.getProperty("almost"));
        assertEquals("threeLevelsDeep", props.getProperty("nested"));
    }

    public void testPropertyEclipsing() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "eclipseprop");
        Properties props = helper.getProperties();
        assertEquals(3, props.size());
        assertEquals("eclipsed", props.getProperty("global"));
    }
    
    public void testLoadPropertiesFromFile() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "propsfromfile");
        Properties props = helper.getProperties();
        assertEquals(7, props.size());
        assertEquals("/home/cruise", props.getProperty("dir1"));
        assertEquals("/home/cruise/logs", props.getProperty("dir2"));
        assertEquals("temp", props.getProperty("tempdir"));
        assertEquals("/home/cruise/logs/temp", props.getProperty("multi"));
    }
    
    public void testMissingProperty() {
        try {
            new ProjectXMLHelper(configFile, "missingprop");
            fail("A missing property should cause an exception!");
        } catch (CruiseControlException expected) {
        }
    }

    public void testGetPluginConfigNoOverride() throws Exception {
        Element configRoot = Util.loadConfigFile(configFile);
        CruiseControlController.addPluginsToRootRegistry(configRoot, configFile);

        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
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

    public void testGetPluginConfig() throws Exception {
        Element configRoot = Util.loadConfigFile(configFile);
        CruiseControlController.addPluginsToRootRegistry(configRoot, configFile);

        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project4");
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
        Element configRoot = Util.loadConfigFile(configFile);
        CruiseControlController.addPluginsToRootRegistry(configRoot, configFile);

        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project4");

        final PluginRegistry plugins = helper.getPlugins();
        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestNestedPlugin.class, plugins.getPluginClass("testnested"));
        assertEquals(ListenerTestSelfConfiguringPlugin.class, plugins.getPluginClass("testselfconfiguring"));

        List listeners = helper.getListeners();
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
        // note this is in fact undefined behavior!! Because we added twice the stringwrapper (first for the child,
        // then for the parent).
        // this could probably fail depending on a different platform, except if Element.setContent() specifies
        // the order in which children are kept within the element.
        final String wrapper = testListener2.getStringWrapper().getString();
        assertTrue("wrapper2-works!", "wrapper2-works!".equals(wrapper)
                                      || "wrapper1".equals(wrapper));
    }

    public void testPluginConfigurationClassOverride() throws Exception {
        Element configRoot = Util.loadConfigFile(configFile);
        CruiseControlController.addPluginsToRootRegistry(configRoot, configFile);

        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project5");

        final PluginRegistry plugins = helper.getPlugins();
        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestOtherNestedPlugin.class, plugins.getPluginClass("testnested"));

        List listeners = helper.getListeners();
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

    public void testDateFormat() throws Exception {
        final String originalFormat = DateFormatFactory.getFormat();        
        new ProjectXMLHelper(configFile, "dateformatfromproperty");
        final String formatFromProperty = DateFormatFactory.getFormat();
        DateFormatFactory.setFormat(DateFormatFactory.DEFAULT_FORMAT);

        assertEquals(DateFormatFactory.DEFAULT_FORMAT, originalFormat);
        assertEquals("MM/dd/yyyy HH:mm:ss a", formatFromProperty);
        assertFalse(originalFormat.equals(formatFromProperty));
    }

    public void testGetBootstrappers() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        List bootstrappers = helper.getBootstrappers();
        assertEquals(0, bootstrappers.size());

        helper = new ProjectXMLHelper(configFile, "project2");
        bootstrappers = helper.getBootstrappers();
        assertEquals(1, bootstrappers.size());
    }

    public void testGetSchedule() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        try {
            helper.getSchedule();
            fail("schedule should be a required element");
        } catch (CruiseControlException expected) {
        }

        helper = new ProjectXMLHelper(configFile, "project2");
        Schedule schedule = helper.getSchedule();
        assertEquals(20 * ONE_SECOND, schedule.getInterval());
    }

    public void testGetModificationSet() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        try {
            helper.getModificationSet();
            fail("modificationset should be a required element");
        } catch (CruiseControlException expected) {
        }

        helper = new ProjectXMLHelper(configFile, "project2");
        ModificationSet modSet = helper.getModificationSet();
        assertEquals(10 * ONE_SECOND, modSet.getQuietPeriod());
    }

    public void testGetLabelIncrementer() throws CruiseControlException {
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", ProjectXMLHelper.LABEL_INCREMENTER);
        pluginElement.setAttribute("classname", DefaultLabelIncrementer.class.getName());
        PluginRegistry.registerToRoot(pluginElement);
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project2");
        DefaultLabelIncrementer incrementer = (DefaultLabelIncrementer) helper.getLabelIncrementer();
        assertTrue(incrementer.isValidLabel("build#9"));

        helper = new ProjectXMLHelper(configFile, "project1");
        incrementer = (DefaultLabelIncrementer) helper.getLabelIncrementer();
        assertFalse(incrementer.isValidLabel("build#9"));
    }

    public void testGetLog() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        assertEquals("logs" + File.separatorChar + "project1", helper.getLog().getLogDir());
        helper = new ProjectXMLHelper(configFile, "project2");
        assertEquals(tempDirectory.getAbsolutePath() + "/foo", helper.getLog().getLogDir());
        helper = new ProjectXMLHelper(configFile, "project3");
        assertEquals("logs" + File.separatorChar + "project3", helper.getLog().getLogDir());

        assertNull(helper.getLog().getLogXmlEncoding());
        helper = new ProjectXMLHelper(configFile, "project2");
        assertEquals("utf-8", helper.getLog().getLogXmlEncoding());
    }

    public void testGetListeners() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        List listeners = helper.getListeners();
        assertEquals(0, listeners.size());

        helper = new ProjectXMLHelper(configFile, "project2");
        listeners = helper.getListeners();
        assertEquals(1, listeners.size());
    }

    protected void setUp() throws Exception {
        // Set up a properties file to use while testing property expansion
        propertiesFile = File.createTempFile("temp", "properties");
        propertiesFile.deleteOnExit();
        StringBuffer buff = new StringBuffer();
        buff.append("dir1=/home/cruise\n");
        buff.append("  dir2 = ${dir1}/logs   \n");
        buff.append("tempdir= temp\n");
        buff.append("multi= ${dir2}/${tempdir}\n");
        
        Writer writer = new BufferedWriter(new FileWriter(propertiesFile));
        writer.write(buff.toString());
        writer.close();
        
        // Set up a CruiseControl config file for testing
        configFile = File.createTempFile("tempConfig", "xml");
        configFile.deleteOnExit();
        tempDirectory = configFile.getParentFile();
        // Note: the project1 and project3 directories will be created 
        // in <testexecutiondir>/logs/
        StringBuffer config = new StringBuffer();
        addConfigXmlInfo(config);

        writer = new BufferedWriter(new FileWriter(configFile));
        writer.write(config.toString());
        writer.close();
    }

    private void addConfigXmlInfo(StringBuffer config) {
      config.append("<cruisecontrol>\n");
      config.append("  <property name='global' value='works!'/>\n");
      config.append("  <property name='project.global' value='project=${project.name}'/>\n");

      config.append("  <plugin name='testnested' "
          + "classname='net.sourceforge.cruisecontrol.listeners.ListenerTestNestedPlugin' "
          + "string='default' otherstring='otherdefault'/>\n");

      config.append("  <project name='project1' />\n");

      config.append("  <project name='project2' >\n");
      config.append("    <bootstrappers>\n");
      config.append("      <vssbootstrapper vsspath='foo' localdirectory='"
              + tempDirectory.getAbsolutePath() + "' />\n");
      config.append("    </bootstrappers>\n");
      config.append("    <schedule interval='20' >\n");
      config.append("      <ant multiple='1' buildfile='"
              + tempDirectory.getAbsolutePath()
              + "/foo/bar.xml' target='baz' />\n");
      config.append("    </schedule>\n");
      config.append("    <modificationset quietperiod='10' >\n");
      config.append("      <vss vsspath='"
              + tempDirectory.getAbsolutePath()
              + "/foo/bar' login='login' />\n");
      config.append("    </modificationset>\n");
      config.append("    <log dir='"
              + tempDirectory.getAbsolutePath()
              + "/foo' encoding='utf-8' >\n");
      config.append("      <merge file='blah' />\n");
      config.append("    </log>\n");
      config.append("    <labelincrementer separator='#' />\n");
      config.append("    <listeners>\n");
      config.append("      <currentbuildstatuslistener file='status.txt'/>\n");
      config.append("    </listeners>\n");
      config.append("  </project>\n");

      config.append("  <project name='project3' >\n");
      config.append("    <log/>\n");
      config.append("  </project>\n");

      // test plugin configuration inside a project
      config.append("  <project name='project4' >\n");
      // property resolution should still work
      config.append("    <property name='default.testlistener.name' value='${project.name}-0'/>\n");
      // to check overriding plugin & defaults. No need to respecify class
      config.append("    <plugin name='testnested' string='overriden'/>\n");
      // to test self configuring plugins
      config.append("    <plugin name='testselfconfiguring' "
          + "classname='net.sourceforge.cruisecontrol.listeners.ListenerTestSelfConfiguringPlugin'/>\n");
      // to test nested & self-configuring plugins
      config.append("    <plugin name='testlistener' string='${default.testlistener.name}' "
          + "classname='net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin'>\n");
      config.append("      <testnested string='nested'/>");
      config.append("      <testselfconfiguring string='selfconfiguring'>");
      config.append("        <testnested string='nestedagain'/>");
      config.append("      </testselfconfiguring>");
      config.append("      <stringwrapper string='wrapper1'/>");
      config.append("    </plugin>\n");
      // override
      config.append("    <listeners>\n");
      config.append("      <testlistener/>\n");
      config.append("      <testlistener string='listener1'/>\n");
      config.append("      <testlistener string='listener2'>\n");
      config.append("        <stringwrapper string='wrapper2-${global}'/>\n");
      config.append("      </testlistener>\n");
      config.append("    </listeners>\n");
      config.append("  </project>\n");

      // test plugin configuration inside a project
      config.append("  <project name='project5' >\n");
      // to check overriding plugin & defaults. No need to respecify class
      config.append("    <plugin name='testnested' "
          + "classname='net.sourceforge.cruisecontrol.listeners.ListenerTestOtherNestedPlugin' "
          + "string='notshadowing' otherotherstring='otherother'/>\n");
      // to test nested & self-configuring plugins
      config.append("    <plugin name='testlistener' string='default' "
          + "classname='net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin'>\n");
      config.append("      <testnested/>");
      config.append("    </plugin>\n");
      // override
      config.append("    <listeners>\n");
      config.append("      <testlistener/>\n");
      config.append("    </listeners>\n");
      config.append("  </project>\n");

      config.append("  <project name='propsfromfile' >\n");
      config.append("    <property file='"
              + propertiesFile.getAbsolutePath()
              + "' />\n");
      config.append("  </project>\n");

      config.append("  <project name='simpleprops' >\n");
      config.append("    <property name='simple' value='success!'/>\n");
      config.append("  </project>\n");

      config.append("  <project name='multiprops' >\n");
      config.append("    <property name='first' value='one'/>\n");
      config.append("    <property name='second' value='two'/>\n");
      config.append("    <property name='third' value='three'/>\n");
      config.append("    <property name='multi' value='${first}.${second}$${third}'/>\n");
      config.append("  </project>\n");

      config.append("  <project name='nestedprops' >\n");
      config.append("    <property name='first' value='one'/>\n");
      config.append("    <property name='second' value='two'/>\n");
      config.append("    <property name='third' value='three'/>\n");
      config.append("    <property name='one.two.three' value='almost'/>\n");
      config.append("    <property name='almost' value='threeLevelsDeep'/>\n");
      config.append("    <property name='nested' value='${${${first}.${second}.${third}}}'/>\n");
      config.append("  </project>\n");

      config.append("  <project name='missingprop' >\n");
      config.append("    <log dir='${missing}'/>\n");
      config.append("  </project>\n");

      config.append("  <project name='eclipseprop' >\n");
      config.append("    <property name='global' value='eclipsed'/>\n");
      config.append("  </project>\n");
      
      config.append("  <project name='dateformatfromproperty' >\n");
      config.append("    <property name=\"date.format\" value=\"MM/dd/yyyy HH:mm:ss a\"/>\n");
      config.append("    <dateformat format=\"${date.format}\"/>\n");
      config.append("  </project>\n");

      config.append("</cruisecontrol>\n");
    }

}
