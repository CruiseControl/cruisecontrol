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
package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

public class RakeScriptTest extends TestCase {

    private static final String[] DEFAULT_WINDOWS_COMMAND  = {"cmd", "/c", "rake"};
    private static final String[] DEFAULT_UNIX_COMMAND = { "rake" };
    //private RakeScript script;

    /*
     * default constructor
     */
    public RakeScriptTest(String name) {
        super(name);
    }

    /*
     * setup test environment
     */
    protected void setUp() throws Exception {
       //script = new RakeScript();
       // script.setErrorStr("error in compilation");
    } // setUp

    /*
     * test command line generation
     */
    public void testDefaultWindowsCommand() throws CruiseControlException {
        RakeScript windowsScript = new RakeScript();
        windowsScript.setWindows(true);
        TestUtil.assertArray("detailedCmd", DEFAULT_WINDOWS_COMMAND,
            windowsScript.buildCommandline().getCommandline());
    }

    public void testDefaultUnixCommand() throws CruiseControlException {
        RakeScript unixScript = new RakeScript();
        unixScript.setWindows(false);
        TestUtil.assertArray("detailedCmd", DEFAULT_UNIX_COMMAND,
            unixScript.buildCommandline().getCommandline());
    }

    public void testCommandWithTarget() throws CruiseControlException {
        RakeScript script = new RakeScript();
        script.setWindows(true);
        script.setTarget("default");
        String[] command = { DEFAULT_WINDOWS_COMMAND[0], DEFAULT_WINDOWS_COMMAND[1], DEFAULT_WINDOWS_COMMAND[2],
                             "default" };
        for (int i = 0; i < command.length; i++) {
            System.out.println(command[i]);
        }
        String[] c =  script.buildCommandline().getCommandline();
        for (int i = 0; i < c.length; i++) {
            System.out.println(c[i]);
        }
        TestUtil.assertArray("detailedCmd", command,
            script.buildCommandline().getCommandline());
    }

    public void testCommandWithRakeFile() throws CruiseControlException {
         RakeScript script = new RakeScript();
         script.setWindows(true);
         script.setBuildFile("build.rb");
         String[] command = { DEFAULT_WINDOWS_COMMAND[0], DEFAULT_WINDOWS_COMMAND[1], DEFAULT_WINDOWS_COMMAND[2],
                              "-f", "build.rb" };
         TestUtil.assertArray("detailedCmd", command,
            script.buildCommandline().getCommandline());
    }
}
