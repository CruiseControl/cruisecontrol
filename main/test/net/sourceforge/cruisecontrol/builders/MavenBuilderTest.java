/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import java.util.List;
import java.util.Hashtable;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;

public class MavenBuilderTest extends TestCase {

    /**
     * void validate()
     */
    public void testvalidate() {
        final String testScriptName = "_testmaven.bat";
        final String testProjectName = "_testproject.xml";
        MavenBuilder mb = new MavenBuilder();
        try {
            mb.validate();
            fail("MavenBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        try {
            // these files must also exist for MavenBuilder to be happy.
            makeTestFile(testScriptName,
                         "@echo This is a fake maven.bat\n");
            makeTestFile(testProjectName,
                         "<project><!-- This is a fake Maven project file --></project>\n");
            mb.setMultiple(1);
            mb.setMavenScript(testScriptName);
            mb.setProjectFile(testProjectName);

            try {
                mb.validate();
                assertTrue(true);
            } catch (CruiseControlException e) {
                fail("MavenBuilder should not throw exceptions when required fields are set.");
            }
        } finally {
            (new File(testScriptName)).delete();
            (new File(testProjectName)).delete();
        }
    }

    /**
     * String[] getCommandLineArgs(Map, boolean, boolean, boolean, String)
     */
    public void testgetCommandLineArgs() {
        MavenBuilder builder = new MavenBuilder();
        // none should exist for this test
        builder.setMavenScript("testmaven.sh");
        builder.setProjectFile("testproject.xml");
        Hashtable properties = new Hashtable();
        properties.put("label", "200.1.23");

        Logger.getRoot().setLevel(Level.INFO);
        compareArrays("NoDebug:",
                      new String[]{
                          "testmaven.sh",
                          "-Dlabel=200.1.23",
                          "-b",
                          "-p",
                          "testproject.xml"},
                      builder.getCommandLineArgs(properties, false, null));

        Logger.getRoot().setLevel(Level.DEBUG);
        compareArrays("WithDebug:",
                      new String[]{
                          "testmaven.sh",
                          "-Dlabel=200.1.23",
                          "-X",
                          "-b",
                          "-p",
                          "testproject.xml"},
                      builder.getCommandLineArgs(properties, false, null));

        Logger.getRoot().setLevel(Level.INFO);
        compareArrays("Windows:",
                      new String[]{
                          "cmd.exe",
                          "/C",
                          "testmaven.sh",
                          "-Dlabel=200.1.23",
                          "-b",
                          "-p",
                          "testproject.xml"},
                      builder.getCommandLineArgs(properties, true, null));

        compareArrays("WithTarget:",
                      new String[]{
                          "testmaven.sh",
                          "-Dlabel=200.1.23",
                          "-b",
                          "-p",
                          "testproject.xml",
                          "clean",
                          "jar"},
                      // notice the spaces in goalSet
                      builder.getCommandLineArgs(properties, false, " clean jar"));
    }

    /**
     * Element build(Map). Mock a success.
     */
    public void testbuild() {
        internaltestbuild(false);  // mocking the success
        internaltestbuild(true);   // mocking the failure
    }

    /**
     * Element build(Map). mockFailure == (Mock a failure?).
     */
    private void internaltestbuild(boolean mockFailure) {
        MavenBuilder mb = new MavenBuilder();
        String testScriptName = null;
        String msgHead = mockFailure ? "MockFailure" : "MockSuccess";
        try {
            // Prepare mock files.
            if (mb.isWindows()) {
                testScriptName = "_testmaven.bat";
                makeTestFile(testScriptName,
                             "@rem This is a fake maven.bat\n"
                             + "@echo java:compile:\n"
                             + "@echo Bla-bla-compile\n"
                             + "@echo test:test:\n"
                             + "@echo " + (mockFailure ? "BUILD FAILED" : "BUILD SUCCESSFUL") + "\n");
            } else {
                testScriptName = "_testmaven.sh";
                fail("Some kind person, please write the Linux version of the test"
                     + " or comment this test out alltogether");
            }
            mb.setMavenScript(testScriptName);
            mb.setProjectFile("don-t-care.xml");

            try {
                Element we;
                List goalTags;

                // some fake goal is still needed to start working (no '|' here!)
                mb.setGoal("fakegoal");
                // this should "succeed"
                Element logElement = mb.build(new Hashtable());
                assertNotNull(msgHead, logElement);
                goalTags = logElement.getChildren("mavengoal");
                assertNotNull(msgHead, goalTags);
                assertEquals(msgHead, 2, goalTags.size());
                we = (Element) goalTags.get(0);
                assertEquals(msgHead, "java:compile", we.getAttribute("name").getValue());
                we = (Element) goalTags.get(1);
                assertEquals(msgHead, "test:test", we.getAttribute("name").getValue());
                if (mockFailure) {
                    assertNotNull(msgHead, logElement.getAttribute("error"));
                } else {
                    assertNull(msgHead, logElement.getAttribute("error"));
                }

                // this time let's test multiple runs
                mb.setGoal("fakegoal|otherfakegoal");
                // this should "double succeed"
                logElement = mb.build(new Hashtable());
                assertNotNull(msgHead, logElement);
                goalTags = logElement.getChildren("mavengoal");
                assertNotNull(msgHead, goalTags);
                // if we mocked a failure, the second run should never happen
                assertEquals(msgHead, mockFailure ? 2 : 4, goalTags.size());
                we = (Element) goalTags.get(0);
                assertEquals(msgHead, "java:compile", we.getAttribute("name").getValue());
                we = (Element) goalTags.get(1);
                assertEquals(msgHead, "test:test", we.getAttribute("name").getValue());
                if (mockFailure) {
                    assertNotNull(msgHead, logElement.getAttribute("error"));
                } else {
                    we = (Element) goalTags.get(2);
                    assertEquals(msgHead, "java:compile", we.getAttribute("name").getValue());
                    we = (Element) goalTags.get(3);
                    assertEquals(msgHead, "test:test", we.getAttribute("name").getValue());
                    assertNull(logElement.getAttribute("error"));
                }

            } catch (CruiseControlException e) {
                fail("MavenBuilder should not throw exceptions when build()-ing.");
            }
        } finally {
            if (testScriptName != null) {
                (new File(testScriptName)).delete();
            }
        }
    }

    /**
     * List getGoalSets()
     */
    public void testgetGoalSets() {
        MavenBuilder mb = new MavenBuilder();
        List gsList;
        mb.setGoal(null);
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("No goal produces non-empty list", 0, gsList.size());

        mb.setGoal("clean ");   // I want the space there..
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("One goal should produce one item", 1, gsList.size());
        // but notice, no space below
        assertEquals("One goal produces bad list content", "clean", (String) gsList.get(0));

        mb.setGoal(" clean|update ");    // Notice the spaces here
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("Two goals should produce two items", 2, gsList.size());
        // but not here
        assertEquals("First run produces bad goal", "clean", (String) gsList.get(0));
        assertEquals("Second run produces bad goal", "update", (String) gsList.get(1));

        // full-featured test
        mb.setGoal("clean update|\ttest||");    // Notice the spaces here
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("Complex goal should produce two goalsets", 2, gsList.size());
        // but not here
        assertEquals("First cplx run produces bad goal", "clean update", (String) gsList.get(0));
        assertEquals("Second cplx run produces bad goal", "test", (String) gsList.get(1));
    }

    /**
     * Make a test file with specified content. Assumes the file does not exist.
     */
    private void makeTestFile(String fname, String content) {
        File tFile = new File(fname);
        try {
            BufferedWriter bwr = new BufferedWriter(new FileWriter(tFile));
            bwr.write(content);
            bwr.flush();
            bwr.close();
        } catch (IOException ioex) {
            fail("Unexpected IOException while preparing "
                 + fname + " test file");
        }
    }

    /**
     * Return true when same.
     */
    private boolean compareArrays(String msg, String[] refarr, String[] testarr) {
        if (refarr == null && testarr == null) {
            return true;
        }

        if (refarr == null) {
            fail(msg + " Reference array is null and test not");
        }
        if (testarr == null) {
            fail(msg + " Test array is null and reference not");
        }
        if (refarr.length != testarr.length) {
            fail(msg + " Arrays have different lengths");
        }

        for (int i = 0; i < refarr.length; i++) {
            assertEquals(msg + " Element " + i + " mismatch.",
                         refarr[i], testarr[i]);
        }
        return true;
    }
}