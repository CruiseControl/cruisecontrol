/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Util;

import java.io.File;

public class VssBootstrapperTest extends TestCase {

  public VssBootstrapperTest(String name) { super(name); }

  public void testValidate() {
    VssBootstrapper bootstrapper = new VssBootstrapper();
    try {
        bootstrapper.validate();
        fail("VssBootstrapper should throw exception if required attributes not set");
    } catch (CruiseControlException ex) {
        String message = ex.getMessage();
        assertEquals(
            "exception message when required attributes not set",
            "VssBootstrapper has required attributes vssPath and localDirectory",
            message);
    }

    bootstrapper.setVssPath("$test/vss/path/file.ext");
    try {
        bootstrapper.validate();
        fail("VssBootstrapper should throw exception if required attributes not set");
    } catch (CruiseControlException ex) {
        String message = ex.getMessage();
        assertEquals(
            "exception message when required attributes not set",
            "VssBootstrapper has required attributes vssPath and localDirectory",
            message);
    }

    bootstrapper.setLocalDirectory(".");
    try {
        bootstrapper.validate();
    } catch (CruiseControlException ex) {
        fail("validate() shouldn't fail when required attributes have been set");
    }

    bootstrapper.setLocalDirectory("c:/not/an/existing/directory");
    try {
        bootstrapper.validate();
        fail("validate() should fail when given a file path that doesn't exist");
    } catch (CruiseControlException ex) {
    }
  }

  public void testCommandLine() {
    VssBootstrapper bootstrapper = new VssBootstrapper();
    String commandLine = bootstrapper.generateCommandLine().toStringNoQuoting();
    assertNotNull("command line should never be null", commandLine);

    final String vssPath = "$Project/subproject/file.ext";
    bootstrapper.setVssPath(vssPath);
    final String localDirectory = "c:/foo";
    bootstrapper.setLocalDirectory(localDirectory);

    commandLine = bootstrapper.generateCommandLine().toStringNoQuoting();
    String expectedCommandLine = "ss.exe get \"" + vssPath + "\" -GL\"" + localDirectory + "\" -I-N";
    assertEquals(expectedCommandLine, commandLine);

    bootstrapper.setLogin("bob,password");
    commandLine = bootstrapper.generateCommandLine().toStringNoQuoting();
    expectedCommandLine = expectedCommandLine + " -Ybob,password";
    assertEquals(expectedCommandLine, commandLine);

    final String ssDir = "c:\\buildtools\\vss";
    bootstrapper.setSsDir(ssDir);
    final String serverPath = "t:\\vss\\foo";
    bootstrapper.setServerPath(serverPath);
    // VSS commands with paths inside the VSS repo actually use backslashes, with $ as root
    // Like: $/myProject/mySubProject/...
    // Also we need to adjust the expected path to the VSS dir according to the OS on which we're
    // running the unit test. Even more yummy, we must NOT do the replaceAll when on Windows,
    // or we get a StringIndexOutOfBoundsException.
    // Never mind that you'll NEVER actually execute and .exe on a non-windows OS...gotta love
    // cross platform madness!
    expectedCommandLine = (Util.isWindows() ? ssDir : ssDir.replaceAll("\\\\", File.separator))
            + File.separatorChar + expectedCommandLine;
    commandLine = bootstrapper.generateCommandLine().toStringNoQuoting();
    assertEquals(expectedCommandLine, commandLine);
  }
}