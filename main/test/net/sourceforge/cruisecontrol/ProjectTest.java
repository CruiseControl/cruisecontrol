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
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

import java.io.*;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;

public class ProjectTest extends TestCase {
    private static Logger log = Logger.getLogger(ProjectTest.class);

    private Project project;

    public ProjectTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
        log.getLoggerRepository().setThreshold(Level.OFF);
    }

    protected void setUp() {
        project = new Project();
    }

    public void testBuild() throws Exception {
        assertEquals("Default value of config file doesn't match", "config.xml",
                project.getConfigFileName());

        MockSchedule sched = new MockSchedule();
        project.setLabel("1.2.2");
        project.setName("myproject");
        project.setSchedule(sched);
        project.setLogDir("test-results");
		project.setLogXmlEncoding("ISO-8859-1");
        project.addAuxiliaryLogFile("_auxLog1.xml");
        project.addAuxiliaryLogFile("_auxLogs");
        project.setLabelIncrementer(new DefaultLabelIncrementer());
        project.setModificationSet(new MockModificationSet());
        writeFile("_auxLog1.xml", "<one/>");
        File auxLogsDirectory = new File("_auxLogs");
        auxLogsDirectory.mkdir();
        writeFile("_auxLogs/_auxLog2.xml", "<testsuite><properties><property/></properties><testcase/></testsuite>");
        writeFile("_auxLogs/_auxLog3.xml", "<testsuite/>");

        project.build();

        assertTrue(project.isLastBuildSuccessful());

        String expected = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><cruisecontrol><modifications /><info><property name=\"projectname\" value=\"myproject\" /><property name=\"lastbuild\" value=\"" + project.getBuildTime() + "\" /><property name=\"builddate\" value=\"" + new SimpleDateFormat(DateFormatFactory.getFormat()).format(project.getNow()) + "\" /><property name=\"label\" value=\"1.2.2\" /><property name=\"interval\" value=\"0\" /><property name=\"lastbuildsuccessful\" value=\"false\" /><property name=\"logfile\" value=\"" + File.separator + "log" + project.getBuildTime() + "L1.2.2.xml\" /></info><build /><one /><testsuite><testcase /></testsuite><testsuite /></cruisecontrol>";
        assertEquals(expected, readFileToString(project.getLogFileName()));
        assertEquals("Didn't increment the label", "1.2.3",
                project.getLabel().intern());

        //look for sourcecontrol properties
        java.util.Map props = sched.getBuildProperties();
        assertNotNull("Build properties were null.", props);
        assertEquals("Should be 4 build properties.", 4, props.size());
        assertTrue("filemodified not found.", props.containsKey(
                "filemodified"));
        assertTrue("fileremoved not found.", props.containsKey("fileremoved"));
    }

    public void testBadLabel() {
        try {
            project.validateLabel("build_0", new DefaultLabelIncrementer());
            fail("Expected exception due to bad label");
        } catch (CruiseControlException expected) {

        }
    }

    public void testSetLastBuild() throws CruiseControlException {
        String lastBuild = "20000101120000";

        project.setLastBuild(lastBuild);

        assertEquals(lastBuild, project.getLastBuild());
    }

    public void testNullLastBuild() throws CruiseControlException {
        try {
            project.setLastBuild(null);
            fail("Expected an IllegalArgumentException for a null last build");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testBadLastBuild() {
        try {
            project.setLastBuild("af32455432");
            fail("Expected a CruiseControlException for a bad last build");
        } catch (CruiseControlException e) {
        }
    }

    private String readFileToString(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuffer result = new StringBuffer();

        String s = reader.readLine();
        while (s != null) {
            result.append(s.trim());
            s = reader.readLine();
        }
        reader.close();

        return result.toString();
    }

    private void writeFile(String fileName, String contents)
            throws IOException {

        FileWriter fw = new FileWriter(fileName);
        fw.write(contents);
        fw.close();
    }

}