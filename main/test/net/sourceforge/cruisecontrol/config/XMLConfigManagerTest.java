/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.config;

import java.io.File;

import org.jdom2.Element;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.IO;

public class XMLConfigManagerTest extends TestCase {
    private File configurationFile;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    protected void setUp() throws Exception {
        configurationFile = File.createTempFile("config", "xml");
        filesToDelete.add(configurationFile);
        
        filesToDelete.add(new File(TestUtil.getTargetDir(), "logs"));
        writeConfigurationFile(
                "<cruisecontrol><include.projects file='foo.xml'/><project name=\"DOESNTMATTER\"><schedule>"
                        + "<ant/></schedule></project></cruisecontrol>\n");
    }

    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testShouldStoreUpdatedMD5HashWhenItChanges() throws CruiseControlException {
        XMLConfigManager configManager = new XMLConfigManager(configurationFile);

        assertFalse(configManager.reloadIfNecessary());

        writeConfigurationFile(
                "<cruisecontrol><project name=\"DOESNTMATTER\"><schedule interval=\"30\">"
                        + "<ant/></schedule></project></cruisecontrol>");
        assertTrue(configManager.reloadIfNecessary());

        assertFalse(configManager.reloadIfNecessary());
    }
    
    public void testShouldDetectChangesToIncludedFiles() throws CruiseControlException {
        File includedFile = new File(configurationFile.getParentFile(), "foo.xml");
        filesToDelete.add(includedFile);
        IO.write(includedFile, "<cruisecontrol></cruisecontrol>");
        
        XMLConfigManager configManager = new XMLConfigManager(configurationFile);
        assertFalse(configManager.reloadIfNecessary());
        
        IO.write(includedFile, "<cruisecontrol><property name='foo' value='bar'/></cruisecontrol>");
        
        assertTrue(configManager.reloadIfNecessary());
        assertFalse(configManager.reloadIfNecessary());
    }

    /**
     * Tests the automatic reaload of project (as detected by {@link XMLConfigManager#reloadIfNecessary()}
     * when a properties file is changed. In general, change of any file registered through {@link FileResolver}
     * shoild lead to propject reload; however, we use {@link DefaultPropertiesPlugin} as the example class
     * implementing {@link net.sourceforge.cruisecontrol.ResolverUser} which provides access
     * to {@link FileResolver} framework.
     * @throws CruiseControlException if the test fails.
     */
    public void testShouldDetectChangesToPropertyFiles() throws CruiseControlException {
        // properties file
        File propertyFile = new File(configurationFile.getParentFile(), "foo.props");
        filesToDelete.add(propertyFile);
        IO.write(propertyFile, "prop.item1 = val1");
        // project config
        File includedFile = new File(configurationFile.getParentFile(), "foo.xml");
        filesToDelete.add(includedFile);
        IO.write(includedFile, "<cruisecontrol><property file='" + propertyFile.getName() + "'/></cruisecontrol>");

        XMLConfigManager configManager = new XMLConfigManager(configurationFile);
        assertFalse(configManager.reloadIfNecessary());

        // Change the properties file
        IO.write(propertyFile, "prop.item1 = value1");

        assertTrue(configManager.reloadIfNecessary());
        assertFalse(configManager.reloadIfNecessary());
    }

    public void testResolverShouldReturnCorrectElement() throws Exception {
        XMLConfigManager configManager = new XMLConfigManager(configurationFile);
        File file = File.createTempFile("XmlConfigManagerTest", ".xml", configurationFile.getParentFile());
        filesToDelete.add(file);
        IO.write(file, "<foo><bar/></foo>");
        XmlResolver resolver = configManager.new Resolver();
        Element element = resolver.getElement(file.getName());
        assertEquals("foo", element.getName());
    }

    // Tests the inclusion of XML using <xi:include >
    public void testReadXincludeConfig() throws Exception {
        File schedFile = new File(configurationFile.getParentFile(), "foo.xml");
        filesToDelete.add(schedFile);
        IO.write(schedFile, "<?xml version=\"1.0\"?><!DOCTYPE data [" +
                "<!ELEMENT data (schedule)><!ATTLIST schedule id ID #REQUIRED>]>" +
                "<data><property name=\"foo\" value=\"bar\" id=\"includ\"/>" + 
                "<schedule interval=\"30\" id=\"includ\"><ant/></schedule></data>");

        IO.write(configurationFile, "<cruisecontrol><project name=\"SCHED_XINCLUDED\">"+
                "<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='" + schedFile.getPath() + 
                "' xpointer='element(includ)'/></project></cruisecontrol>");

        // Project must be OK
        XMLConfigManager configManager = new XMLConfigManager(configurationFile);
        CruiseControlConfig conf = configManager.getCruiseControlConfig();

        assertNotNull(conf.getProject("SCHED_XINCLUDED"));
    }

    private void writeConfigurationFile(final String contents) throws CruiseControlException {
        final StringBuilder configuration = new StringBuilder();
        configuration.append(contents);
        IO.write(configurationFile, configuration.toString());
    }
}
