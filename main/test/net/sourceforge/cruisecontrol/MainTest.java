/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import junit.framework.TestCase;

public class MainTest extends TestCase {
    public void testParsePassword() throws CruiseControlException {
        String[] correctArgs = new String[] { "-password", "password" };
        String[] missingValue = new String[] { "-password" };
        String[] missingParam = new String[] { "" };
        assertEquals("password", Main.parsePassword(correctArgs));
        assertEquals(null, Main.parseUser(missingValue));
        assertEquals(null, Main.parseUser(missingParam));
    }
    public void testParseUser() throws CruiseControlException {
        String[] correctArgs = new String[] { "-user", "user" };
        String[] missingValue = new String[] { "-user" };
        String[] missingParam = new String[] { "" };
        assertEquals("user", Main.parseUser(correctArgs));
        assertEquals(null, Main.parseUser(missingValue));
        assertEquals(null, Main.parseUser(missingParam));
    }

    public void testParseConfigurationFileName() throws Exception {
        String[] correctArgs = new String[] {"-configfile", "myconfig.xml"};
        String[] missingParam = new String[] {""};
        String[] missingValue = new String[] {"-configfile"};

        assertEquals("myconfig.xml", Main.parseConfigFileName(correctArgs, null));
        assertEquals("config.xml", Main.parseConfigFileName(missingParam, "config.xml"));

        try {
            Main.parseConfigFileName(missingValue, null);
            fail("Expected CruiseControlException on missing configfile value");
        } catch (CruiseControlException e) {
            // expected
        }

    }

    public void testParseHttpPort() throws Exception {
        String[] correctArgs = new String[] {"-port", "123"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-port"};
        String[] invalidArgs = new String[] {"-port", "ABC"};

        assertEquals(123, Main.parseHttpPort(correctArgs));
        assertEquals(Main.NOT_FOUND, Main.parseHttpPort(missingParam));
        assertEquals(8000, Main.parseHttpPort(defaultValue));

        try {
            Main.parseHttpPort(invalidArgs);
            fail("Expected IllegalArgumentException on non-int ABC");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseRmiPort() throws Exception {
        String[] correctArgs = new String[] {"-rmiport", "123"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-rmiport"};
        String[] invalidArgs = new String[] {"-rmiport", "ABC"};

        assertEquals(123, Main.parseRmiPort(correctArgs));
        assertEquals(Main.NOT_FOUND, Main.parseRmiPort(missingParam));
        assertEquals(1099, Main.parseRmiPort(defaultValue));

        try {
            Main.parseRmiPort(invalidArgs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseXslPath() throws CruiseControlException {
        String[] correctArgs = new String[] {"-xslpath", "tmp"};
        String[] missingParam = new String[] {""};
        String[] missingValue = new String[] {"-xslpath"};
        final String invalidXsl = "does_Not_Exist";
        String[] invalidArgs = new String[] {"-xslpath", invalidXsl};

        assertEquals("tmp", Main.parseXslPath(correctArgs));
        assertNull(Main.parseXslPath(missingParam));
        assertNull(Main.parseXslPath(missingValue));

        try {
            Main.parseXslPath(invalidArgs);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("'xslpath' argument must specify an existing directory but was " + invalidXsl,
                         expected.getMessage());
        }
    }

    public void testParseArgs() throws Exception {
        final String argName = "port";
        final String defaultIfNoParam = "8080";
        final String defaultIfNoValue = "8000";

        //No args specified. Should get the default back.
        String[] args = {
        };
        String foundValue = Main.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(defaultIfNoParam, foundValue);

        //One arg specified, should get the value specified, not the default.
        String setValue = "100";
        args = new String[] {"-port", setValue};
        foundValue = Main.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(setValue, foundValue);

        //More than one arg specified, should still get the value specified.
        args = new String[] {"-port", setValue, "-throwAway", "value"};
        foundValue = Main.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(setValue, foundValue);

        //Switch the order around, should still get the value specified.
        args = new String[] {"-throwAway", "value", "-port", setValue};
        foundValue = Main.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(setValue, foundValue);

        //If arg name is included, but no arg, then should get defaultIfNoValue
        args = new String[] {"-port"};
        foundValue = Main.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(defaultIfNoValue, foundValue);
    }

    public void testUsage() {
        String[] usage = {"-?"};
        String[] notusage = {"-port", "8000"};
        assertTrue(Main.printUsage(usage));
        assertFalse(Main.printUsage(notusage));
    }

    public void testshouldStartController() throws Exception {
        String[] bothArgs = new String[]{"-port", "8085",
                                            "-rmiport", "8086"};
        String[] rmiPort = new String[]{"-rmiport", "8086"};
        String[] httpPort = new String[]{"-port", "8085"};
        String[] httpPortWithDefault = new String[]{"-port"};
        String[] neitherArg = new String[]{"-foo", "blah"};

        assertTrue(Main.shouldStartController(bothArgs));
        assertTrue(Main.shouldStartController(rmiPort));
        assertTrue(Main.shouldStartController(httpPort));
        assertTrue(Main.shouldStartController(httpPortWithDefault));
        assertFalse(Main.shouldStartController(neitherArg));
    }
}