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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Commandline;

public class PVCSTest extends TestCase {

    private PVCS pvcs;

    public void setUp() {
        pvcs = new PVCS();
    }

    public void tearDown() {
        pvcs = null;
    }

    public void testValidate() {
        try {
            pvcs.validate();
            fail("PVCS should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        pvcs.setPvcsproject("project");
        pvcs.setPvcssubproject("subproject");

        try {
            pvcs.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("PVCS should not throw exceptions when required attributes are set.");
        }
    }

    public void testGetExecutable() {
        String testExe = "testexe";
        assertEquals(
            "Wrong pvcs bin setting w/out bin set.",
            testExe,
            pvcs.getExecutable(testExe));

        pvcs.setPvcsbin("mybindir");
        assertEquals(
            "Wrong pvcs bin setting w/ bin set.",
            "mybindir" + File.separator + testExe,
            pvcs.getExecutable(testExe));
    }

    public void testBuildExecCommandWithVersionLabel() {
        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");
        pvcs.setPvcsversionlabel("Test Version Label");

        Commandline ccCommand = pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        String expectedCommand = pvcs.getExecutable("pcli")
                + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" -v\"Test Version Label\" "
                + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", expectedCommand, ccCommand.toStringNoQuoting());
    }

    public void testBuildExecCommandWithNullVersionLabel() {
        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");
        pvcs.setPvcsversionlabel("");

        Commandline ccCommand = pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        String expectedCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" " + "-z /TestProject";
        assertEquals("Wrong PVCS command generated!", expectedCommand, ccCommand.toStringNoQuoting());
    }

    public void testBuildExecCommandWithoutVersionLabel() {
        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");

        Commandline ccCommand = pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        String expectedCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" " + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", expectedCommand, ccCommand.toStringNoQuoting());
  }
    
    public void testBuildExecCommandWithoutLoginId() {
        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");

        Commandline ccCommand = pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        String expectedCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" " + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", expectedCommand, ccCommand.toStringNoQuoting()); 
    }
    
    public void testBuildExecCommandWithEmptyLoginId() {
        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");
        pvcs.setLoginid("");

        Commandline ccCommand = pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        String expectedCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" " + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", expectedCommand, ccCommand.toStringNoQuoting());
    }
    
    public void testBuildExecCommandWithLoginId() {
        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");
        pvcs.setLoginid("TestUser");

        Commandline ccCommand = pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        String expectedCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-id\"TestUser\" -ds\"11/23/2004 8:00AM\" "
                + "-de\"11/23/2004 1:00PM\" -pr\"C:/PVCS-Repos/TestProject/pvcs\" " 
                + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", expectedCommand, ccCommand.toStringNoQuoting());
    }
    
    public void testMakeModifications() throws URISyntaxException {
        Calendar cal = Calendar.getInstance();
        cal.set(2004, 11, 23);
        Date date = cal.getTime();
        pvcs.setLastBuild(date);
        pvcs.setPvcsproject("Services");
        List mods = pvcs.makeModificationsList(new File(new URI(getClass().getResource("vlog.txt").toExternalForm())));
        assertEquals(7, mods.size());
        Modification mod1 = (Modification) mods.get(0);
        assertEquals("Initial revision", mod1.comment);
        Modification mod2 = (Modification) mods.get(1);
        assertEquals("Add code for " + System.getProperty("line.separator") + "Sections", mod2.comment);
        
    }
}
