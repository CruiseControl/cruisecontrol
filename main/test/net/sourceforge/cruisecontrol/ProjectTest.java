/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

import java.io.*;

public class ProjectTest extends TestCase {

    public ProjectTest(String name) {
        super(name);
    }

    public void testBuild() throws Exception {
        Project project = new Project();
        MockSchedule sched = new MockSchedule();
        project.setLabel("1.2.2");
        project.setName("myproject");
        project.setSchedule(sched);
        project.setLogDir("test-results");
        project.addAuxiliaryLogFile("_auxLog1.xml");
        project.addAuxiliaryLogFile("_auxLogs");
        project.setLabelIncrementer(new DefaultLabelIncrementer());
        project.setModificationSet(new MockModificationSet());
        writeFile("_auxLog1.xml", "<one/>");
        File auxLogsDirectory = new File("_auxLogs");
        auxLogsDirectory.mkdir();
        writeFile("_auxLogs/_auxLog2.xml", "<two/>");
        writeFile("_auxLogs/_auxLog3.xml", "<three/>");

        project.build();

        String expected = "<cruisecontrol><modifications /><info><property name=\"lastbuild\" value=\"" + project.getBuildTime() + "\" /><property name=\"label\" value=\"1.2.2\" /><property name=\"interval\" value=\"0\" /></info><build /><one /><two /><three /></cruisecontrol>";
        assertEquals(expected, readFileToString(project.getLogFileName()));
        assertEquals("Didn't increment the label", "1.2.3", project.getLabel().intern());

        //look for sourcecontrol properties
        java.util.Map props = sched.getBuildProperties();
        assertNotNull("Build properties were null.", props);
        assertEquals("Should be 4 build properties.",4, props.size());
        assertTrue("filemodified not found.", props.containsKey("filemodified"));
        assertTrue("fileremoved not found.", props.containsKey("fileremoved"));
    }

    private String readFileToString(String filename) throws IOException {
        BufferedReader br = null;
        StringBuffer result = null;
        br = new BufferedReader(new FileReader(filename));
        result = new StringBuffer();
        String s = br.readLine();
        while (s != null) {
            result.append(s.trim());
            s = br.readLine();
        }
        br.close();
        return result.toString();
    }

    private void writeFile(String fileName, String contents) throws IOException {
        FileWriter fw = null;

        fw = new FileWriter(fileName);
        fw.write(contents);
        fw.close();

    }
}