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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlControllerTest extends TestCase {

    private File configFile = new File("_tempConfigFile");
    private CruiseControlController test;

    protected void setUp() throws Exception {
        test = new CruiseControlController();
    }

    public void tearDown() {
        if (configFile.exists()) {
            configFile.delete();
        }
        test = null;
    }

    public void testSetNoFile() {
        try {
            test.setConfigFile(null);
            fail("Allowed to not set a config file");
        } catch (CruiseControlException expected) {
            assertEquals("No config file", expected.getMessage());
        }
        try {
            test.setConfigFile(configFile);
            fail("Allowed to not set a config file");
        } catch (CruiseControlException expected) {
            assertEquals("Config file not found: " + configFile, expected.getMessage());
        }
    }

    public void testLoadEmptyProjects() throws IOException, CruiseControlException {
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        test.setConfigFile(configFile);
        assertEquals(configFile, test.getConfigFile());
        assertTrue(test.getProjects().isEmpty());
    }

    public void testLoadSomeProjects() throws IOException, CruiseControlException {
        test = new CruiseControlController() {
            protected Project configureProject(String projectName) {
                final Project project = new Project();
                project.setName(projectName);
                return project;
            }
        };
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject2");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        test.setConfigFile(configFile);
        assertEquals(configFile, test.getConfigFile());
        assertEquals(2, test.getProjects().size());
    }

    public void testReadProject() throws IOException {
        File tempFile = File.createTempFile("foo", ".tmp");
        String tempDir = tempFile.getParent();
        tempFile.delete();
        Project project = test.readProject(tempDir);
        assertNotNull(project);
        assertTrue(project.getBuildForced());
    }

    public void testRegisterPlugins() throws Exception {
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        configOut.write("  <plugin name=\"testname\" "
                        + "classname=\"net.sourceforge.cruisecontrol.CruiseControllerTest\"/>\n");
        configOut.write("  <plugin name=\"labelincrementer\" classname=\"my.global.Incrementer\"/>\n");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        test.setConfigFile(configFile);
        PluginRegistry newRegistry = PluginRegistry.createRegistry();
        assertTrue(newRegistry.isPluginRegistered("testname"));
        assertFalse(newRegistry.isPluginRegistered("unknown_plugin"));
        assertEquals(newRegistry.getPluginClassname("labelincrementer"), "my.global.Incrementer");
    }

    private void writeProjectDetails(FileWriter configOut, final String projectName) throws IOException {
        configOut.write("<project name=\"" + projectName + "\" />\n");
    }
}
