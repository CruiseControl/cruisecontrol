/******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

import java.io.File;

public class MainTest extends TestCase {

    private Main main;

    public MainTest(String name) {
        super(name);
    }

    protected void setUp() {
        main = new Main();
    }

    public void testConfigureProject() throws Exception {

        String[] correctArgs = new String[]{"-lastbuild", "20020310120000",
                                            "-label", "1.2.2", "-projectname",
                                            "myproject", "-configfile",
                                            "config.xml"};

        File myProjFile = new File("myproject");
        if (myProjFile.exists()) {
            myProjFile.delete();
        }

        {
            Project project = main.configureProject(correctArgs);
            assertEquals(project.getConfigFileName(), "config.xml");
            assertEquals(project.getLabel(), "1.2.2");
            assertEquals(project.getLastBuild(), "20020310120000");
            assertEquals(project.getName(), "myproject");
        }

        {
            Project project = new Project();
            project.setConfigFileName("config.xml");
            project.setLabel("1.2.2");
            project.setLastBuild("20020310120000");
            project.setName("myproject");
            project.serializeProject();

            Project newProject = main.configureProject(
                    new String[]{"-projectname", "myproject"});
            assertEquals(newProject.getConfigFileName(), "config.xml");
            assertEquals(newProject.getLabel(), "1.2.2");
            assertEquals(newProject.getLastBuild(), "20020310120000");
            assertEquals(newProject.getName(), "myproject");
        }

        try {
            Project project = new Project();
            project.setConfigFileName("config.xml");
            project.setLastBuild("20020310120000");
            project.setName("myproject");
            project.serializeProject();

            Project newProject = main.configureProject(
                    new String[]{"-projectname", "myproject"});
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

    }

    public void testParseLastBuild() throws Exception {
        String[] correctArgs = new String[]{"-lastbuild", "20020310120000"};
        String[] missingArgs = new String[]{""};
        String[] incorrectArgs = new String[]{"-lastbuild"};

        assertEquals(main.parseLastBuild(correctArgs, null), "20020310120000");

        assertEquals(main.parseLastBuild(missingArgs, "20020310000000"),
                "20020310000000");

        try {
            main.parseLastBuild(incorrectArgs, null);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

        try {
            main.parseLastBuild(missingArgs, null);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testParseLabelCorrect() throws CruiseControlException {
        String correctLabel = "1.2.3";
        String[] correctArgs = new String[]{"-label", correctLabel};

        assertEquals(
                main.parseLabel(correctArgs, null),
                correctLabel);
    }

    public void testParseLabelNoArgs() throws CruiseControlException {
        String[] noArgs = new String[]{""};
        String previousLabel = "1.2.2";

        assertEquals(main.parseLabel(noArgs, previousLabel),
                previousLabel);
    }

    public void testParseLabelMissingLabelValue() {
        String[] incorrectArgs = new String[]{"-label"};

        try {
            main.parseLabel(incorrectArgs, null);
            fail("Expected exception due to missing label value");
        } catch (CruiseControlException expected) {
        }
    }

    public void testParseLabelNoArgsNoPreviousLabel() {
        String[] noArgs = new String[]{""};

        try {
            main.parseLabel(noArgs, null);
            fail("Expected exception due to label not being set");
        } catch (CruiseControlException expected) {
        }
    }

    public void testParseConfigurationFileName() throws Exception {
        String[] correctArgs = new String[]{"-configfile", "config.xml"};
        String[] missingArgs = new String[]{""};
        String[] incorrectArgs = new String[]{"-configfile"};

        assertEquals(main.parseConfigFileName(correctArgs, null), "config.xml");

        assertEquals(main.parseConfigFileName(missingArgs, "config.xml"),
                "config.xml");

        try {
            main.parseConfigFileName(incorrectArgs, null);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

        try {
            main.parseConfigFileName(missingArgs, null);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testParseProjectName() throws Exception {
        String[] correctArgs = new String[]{"-projectname", "myproject"};
        String[] missingArgs = new String[]{""};
        String[] incorrectArgs = new String[]{"-projectname"};

        assertEquals(main.parseProjectName(correctArgs), "myproject");

        try {
            main.parseProjectName(missingArgs);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

        try {
            main.parseProjectName(incorrectArgs);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }
    }

}