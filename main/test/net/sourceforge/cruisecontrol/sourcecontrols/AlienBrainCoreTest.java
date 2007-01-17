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
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;

/**
 * The unit test for the base class from which most all CruiseControl /
 * AlienBrain interactions derive.
 *
 * @author <a href="mailto:scottj+cc@escherichia.net">Scott Jacobs</a>
 */
public class AlienBrainCoreTest extends TestCase {

    public void testBuildCommonCommand() {
        AlienBrainCore ab = new AlienBrainCore();

        String username = "user";
        String password = "pass";
        String server = "server";
        String database = "StudioVault";

        ab.setUser(username);
        ab.setPassword(password);
        ab.setServer(server);
        ab.setDatabase(database);

        assertEquals("ab -u user -p pass -s server -d StudioVault",
            ab.buildCommonCommand().toString());
    }


    //The following tests all actually use the AlienBrain executable and
    //may need to access a server.  Therefore they can only be run if you
    //have a licensed command-line client and access to a server.
/*
    //In order for some of the following tests to pass, these members must
    //be assigned values valid for your AlienBrain server.
    private static final String TESTING_BRANCH = "Root Branch/Overmatch";
    // Set any of the following to null if you do not want to
    // override any NXN_AB_* environment variables you may be using.
    private static final String TESTING_USERNAME = null; //"sjacobs";
    private static final String TESTING_PASSWORD = null; //"pass123";
    private static final String TESTING_SERVER = null; //"abserver";
    private static final String TESTING_DATABASE = null; //"StudioVault";

    private String getActiveBranch() throws java.io.IOException {
        AlienBrainCore ab = new AlienBrainCore();

        ab.setServer(TESTING_SERVER);
        ab.setDatabase(TESTING_DATABASE);
        ab.setUser(TESTING_USERNAME);
        ab.setPassword(TESTING_PASSWORD);

        net.sourceforge.cruisecontrol.util.ManagedCommandline cmdLine = ab.buildCommonCommand();
        cmdLine.createArgument("getactivebranch");

        cmdLine.execute();
        return cmdLine.getStdoutAsString();
    }

    public void testSetActiveBranch() throws java.io.IOException, net.sourceforge.cruisecontrol.CruiseControlException {
        AlienBrainCore ab = new AlienBrainCore();

        ab.setServer(TESTING_SERVER);
        ab.setDatabase(TESTING_DATABASE);
        ab.setUser(TESTING_USERNAME);
        ab.setPassword(TESTING_PASSWORD);

        ab.setActiveBranch(TESTING_BRANCH);
        assertEquals("setActiveBranch failed!",
            "The current active branch is: \"" + TESTING_BRANCH + "\"\n",
            getActiveBranch());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AlienBrainCoreTest.class);
    }
*/  // End of tests that require an actual AlienBrain installation.
}
