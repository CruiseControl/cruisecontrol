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

    private Main main;

    protected void setUp() {
        main = new Main();
    }

    public void testParseConfigurationFileName() throws Exception {
        String[] correctArgs = new String[] {"-configfile", "config.xml"};
        String[] missingArgs = new String[] {""};
        String[] incorrectArgs = new String[] {"-configfile"};

        assertEquals(Main.parseConfigFileName(correctArgs, null), "config.xml");

        assertEquals(Main.parseConfigFileName(missingArgs, "config.xml"), "config.xml");

        try {
            Main.parseConfigFileName(incorrectArgs, null);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

        try {
            Main.parseConfigFileName(missingArgs, null);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testParseHttpPort() throws Exception {
        String[] correctArgs = new String[] {"-port", "123"};
        String[] missingArgs = new String[] {""};
        String[] incorrectArgs = new String[] {"-port"};
        String[] invalidArgs = new String[] {"-port", "ABC"};

        assertEquals(Main.parseHttpPort(correctArgs), 123);

        try {
            Main.parseHttpPort(missingArgs);
            fail("Expected exception");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            Main.parseHttpPort(incorrectArgs);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

        try {
            Main.parseHttpPort(invalidArgs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseRmiPort() throws Exception {
        String[] correctArgs = new String[] {"-rmiport", "123"};
        String[] missingArgs = new String[] {""};
        String[] incorrectArgs = new String[] {"-rmiport"};
        String[] invalidArgs = new String[] {"-rmiport", "ABC"};

        assertEquals(Main.parseRmiPort(correctArgs), 123);

        try {
            Main.parseRmiPort(missingArgs);
            fail("Expected exception");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            Main.parseRmiPort(incorrectArgs);
            fail("Expected exception");
        } catch (CruiseControlException e) {
            // expected
        }

        try {
            Main.parseRmiPort(invalidArgs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseXslPath() throws CruiseControlException {
        String[] correctArgs = new String[] {"-xslpath", "xsl"};
        String[] missingArgs = new String[] {""};
        String[] incorrectArgs = new String[] {"-xslpath"};
        String[] invalidArgs = new String[] {"-xslpath", "does_Not_Exist"};

        assertEquals("xsl", Main.parseXslPath(correctArgs));
        assertNull(Main.parseXslPath(missingArgs));

        try {
            Main.parseXslPath(incorrectArgs);
            fail();
        } catch (CruiseControlException expected) {
            assertEquals("'xslpath' argument was not specified.", expected.getMessage());
        }

        try {
            Main.parseXslPath(invalidArgs);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("'xslpath' argument must specify an existing directory.", expected.getMessage());
        }
    }

    public void testParseArgs() throws Exception {
        String argName = "port";
        String defaultValue = "8080";

        //No args specified. Should get the default back.
        String[] args = {
        };
        String foundValue = Main.parseArgument(args, argName, defaultValue);
        assertEquals(defaultValue, foundValue);

        //One arg specified, should get the value specified, not the default.
        String setValue = "100";
        args = new String[] {"-port", setValue};
        foundValue = Main.parseArgument(args, argName, defaultValue);
        assertEquals(setValue, foundValue);

        //More than one arg specified, should still get the value specified.
        args = new String[] {"-port", setValue, "-throwAway", "value"};
        foundValue = Main.parseArgument(args, argName, defaultValue);
        assertEquals(setValue, foundValue);

        //Switch the order around, should still get the value specified.
        args = new String[] {"-throwAway", "value", "-port", setValue};
        foundValue = Main.parseArgument(args, argName, defaultValue);
        assertEquals(setValue, foundValue);

        //If arg name is included, but no arg, then should get an exception.
        args = new String[] {"-port"};
        try {
            foundValue = Main.parseArgument(args, argName, defaultValue);
            fail(
                "Expected to get an exception, because the user specified"
                    + " an argument but didn't provide a value for the argument."
                    + " Got the value '"
                    + foundValue
                    + "' instead.");
        } catch (CruiseControlException e) {
            assertTrue("Good, expected to get an exception.", true);
        }
    }
}