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
        ProjectXMLHelper helper = null;
        try {
            helper = new ProjectXMLHelper(configFile, "missingprop");
            fail("A missing property should cause an exception!");
        } catch (CruiseControlException expected) {
        }
    }

    public void testDateFormat() throws Exception {
        String originalFormat = DateFormatFactory.getFormat();
        assertEquals("MM/dd/yyyy HH:mm:ss", originalFormat);

        Element projectElement = new Element("project");
        Element dateFormatElement = new Element("dateformat");
        dateFormatElement.setAttribute("format", "yyyy/MM/dd hh:mm:ss a");
        projectElement.addContent(dateFormatElement);

        ProjectXMLHelper helper = new ProjectXMLHelper();
        helper.setDateFormat(projectElement);

        assertEquals("yyyy/MM/dd hh:mm:ss a", DateFormatFactory.getFormat());

        DateFormatFactory.setFormat(originalFormat);
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
        config.append("<cruisecontrol>\n");
        config.append("  <property name='global' value='works!'/>\n");
        config.append("  <property name='project.global' value='project=${project.name}'/>\n");

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
        
        config.append("</cruisecontrol>\n");

        writer = new BufferedWriter(new FileWriter(configFile));
        writer.write(config.toString());
        writer.close();
    }

}
