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
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class PVCSTest extends TestCase {

    public PVCSTest(String name) {
        super(name);
    }

    public void testValidate() {
        PVCS pvcs = new PVCS();

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
        PVCS pvcs = new PVCS();

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
        PVCS pvcs = new PVCS();
        String testCommand = "";
        String ccCommand = "";

        setName("PVCS Exec Command With Version Label Test");

        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");
        pvcs.setPvcsversionlabel("Test Version Label");

        ccCommand = pvcs.getExecutable("pcli") + " "
                + pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        testCommand = pvcs.getExecutable("pcli")
                + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" -v\"Test Version Label\" "
                + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", ccCommand, testCommand);
    }

    public void testBuildExecCommandWithNullVersionLabel() {
        PVCS pvcs = new PVCS();
        String testCommand = "";
        String ccCommand = "";

        setName("PVCS Exec Command With Null Version Label Test");

        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");
        pvcs.setPvcsversionlabel("");

        ccCommand = pvcs.getExecutable("pcli") + " "
                + pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        testCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" " + "-z /TestProject";
        assertEquals("Wrong PVCS command generated!", ccCommand, testCommand);
    }

    public void testBuildExecCommandWithoutVersionLabel() {
        PVCS pvcs = new PVCS();
        String testCommand = "";
        String ccCommand = "";

        setName("PVCS Exec Command Without Version Label Test");

        pvcs.setPvcsproject("C:/PVCS-Repos/TestProject/pvcs");
        pvcs.setPvcssubproject("/TestProject");

        ccCommand = pvcs.getExecutable("pcli") + " "
                + pvcs.buildExecCommand("11/23/2004 8:00AM", "11/23/2004 1:00PM");
        testCommand = pvcs.getExecutable("pcli") + " "
                + "run -ns -q -xo\"vlog.txt\" -xe\"vlog.txt\" vlog "
                + "-ds\"11/23/2004 8:00AM\" -de\"11/23/2004 1:00PM\" "
                + "-pr\"C:/PVCS-Repos/TestProject/pvcs\" " + "-z /TestProject";

        assertEquals("Wrong PVCS command generated!", ccCommand, 
testCommand);
  }
}
