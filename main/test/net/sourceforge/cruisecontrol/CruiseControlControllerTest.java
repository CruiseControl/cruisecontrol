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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlControllerTest extends TestCase {

    private File dir = new File("target");
    private File configFile = new File(dir, "_tempConfigFile");
    private CruiseControlController ccController;

    protected void setUp() {
        dir.mkdirs();
        ccController = new CruiseControlController();
    }

    public void tearDown() {
        if (configFile.exists()) {
            configFile.delete();
        }
        ccController = null;
    }

    public void testSetNoFile() {
        try {
            ccController.setConfigFile(null);
            fail("Allowed to not set a config file");
        } catch (CruiseControlException expected) {
            assertEquals("No config file", expected.getMessage());
        }
        try {
            ccController.setConfigFile(configFile);
            fail("Config file must exist");
        } catch (CruiseControlException expected) {
            assertEquals("Config file not found: " + configFile.getAbsolutePath(), expected.getMessage());
        }
    }

    public void testLoadEmptyProjects() throws IOException, CruiseControlException {
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        ccController.setConfigFile(configFile);
        assertEquals(configFile, ccController.getConfigFile());
        assertTrue(ccController.getProjects().isEmpty());
    }

    public void testLoadSomeProjects() throws IOException, CruiseControlException {
        ccController = new CruiseControlController() {
            protected Project configureProject(String projectName) {
                final Project project = new Project();
                project.setSchedule(new Schedule());
                project.setName(projectName);
                project.setConfigFile(configFile); 
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

        ccController.setConfigFile(configFile);
        assertEquals(configFile, ccController.getConfigFile());
        assertEquals(2, ccController.getProjects().size());
    }

    public void testLoadSomeProjectsWithDuplicates() throws IOException, CruiseControlException {
        ccController = new CruiseControlController() {
            protected Project configureProject(String projectName) {
                final Project project = new Project();
                project.setSchedule(new Schedule());
                project.setName(projectName);
                project.setConfigFile(configFile); 
                return project;
           }
        };
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject1");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        try {
            ccController.setConfigFile(configFile);
            fail("duplicate project names should fail");
        } catch (CruiseControlException expected) {
            assertEquals("Duplicate entries in config file for project name testProject1", expected.getMessage());
        }
    }

    public void testConfigReloading() throws IOException, CruiseControlException {
        MyListener listener = new MyListener();

        ccController = new CruiseControlController() {
            protected Project configureProject(String projectName) {
                final Project project = new Project();
                project.setSchedule(new Schedule());
                project.setName(projectName);
                project.setConfigFile(configFile);
                return project;
            }
        };
        ccController.addListener(listener);
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject2");
        configOut.write("</cruisecontrol>\n");
        configOut.close();
        ccController.setConfigFile(configFile);

        assertEquals(configFile, ccController.getConfigFile());
        assertEquals(2, ccController.getProjects().size());
        assertEquals(2, listener.added.size());
        assertEquals(0, listener.removed.size());

        listener.clear();

        // no change - no reload
        ccController.parseConfigFileIfNecessary();
        // nothing happened
        assertEquals(0, listener.added.size());
        assertEquals(0, listener.removed.size());


        // add a project:

        listener.clear();

        sleep(1200);
        configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject2");
        writeProjectDetails(configOut, "testProject3");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        ccController.parseConfigFileIfNecessary();

        assertEquals(3, ccController.getProjects().size());
        assertEquals(1, listener.added.size());
        assertEquals(0, listener.removed.size());

        // remove 2 projects

        listener.clear();

        sleep(1200);
        configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        writeProjectDetails(configOut, "testProject3");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        ccController.reloadConfigFile();

        assertEquals(1, ccController.getProjects().size());
        assertEquals(0, listener.added.size());
        assertEquals(2, listener.removed.size());

    }

    private void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException dontCare) {
            System.out.println("dontCare happened");
        }
    }

    public void testReadProject() throws IOException {
        File tempFile = File.createTempFile("foo", ".tmp");
        String tempDir = tempFile.getParent();
        tempFile.delete();
        Project project = ccController.readProject(tempDir);
        assertNotNull(project);
        assertTrue(project.getBuildForced());
    }

    public void testRegisterPlugins() throws IOException, CruiseControlException {
        FileWriter configOut = new FileWriter(configFile);
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
        configOut.write("  <plugin name='testname' "
                        + "classname='net.sourceforge.cruisecontrol.CruiseControllerTest'/>\n");
        configOut.write("  <plugin name='labelincrementer' classname='my.global.Incrementer'/>\n");
        configOut.write("</cruisecontrol>\n");
        configOut.close();

        ccController.setConfigFile(configFile);
        PluginRegistry newRegistry = PluginRegistry.createRegistry();
        assertTrue(newRegistry.isPluginRegistered("testname"));
        assertFalse(newRegistry.isPluginRegistered("unknown_plugin"));
        assertEquals(newRegistry.getPluginClassname("labelincrementer"), "my.global.Incrementer");
    }

    private void writeProjectDetails(FileWriter configOut, final String projectName) throws IOException {
        configOut.write("<project name='" + projectName + "'>\n");
        configOut.write("  <modificationset><alwaysbuild/></modificationset>\n");
        configOut.write("  <schedule><ant/></schedule>\n");
        configOut.write("</project>\n");
    }

    class MyListener implements CruiseControlController.Listener {
        private List added = new ArrayList();
        private List removed = new ArrayList();
        public void clear() {
            added.clear();
            removed.clear();
        }
        public void projectAdded(Project project) {
            added.add(project);
        }
        public void projectRemoved(Project project) {
            removed.add(project);
        }
    }
}
