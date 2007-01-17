/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;

public class MainArgsTest extends TestCase {
    public void testParseArgs() throws Exception {
        final String argName = "port";
        final String defaultIfNoParam = "8080";
        final String defaultIfNoValue = "8000";

        //No args specified. Should get the default back.
        String[] args = {
        };
        String foundValue = MainArgs.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(defaultIfNoParam, foundValue);

        //One arg specified, should get the value specified, not the default.
        String setValue = "100";
        args = new String[] {"-port", setValue};
        foundValue = MainArgs.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(setValue, foundValue);

        //More than one arg specified, should still get the value specified.
        args = new String[] {"-port", setValue, "-throwAway", "value"};
        foundValue = MainArgs.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(setValue, foundValue);

        //Switch the order around, should still get the value specified.
        args = new String[] {"-throwAway", "value", "-port", setValue};
        foundValue = MainArgs.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(setValue, foundValue);

        //If arg name is included, but no arg, then should get defaultIfNoValue
        args = new String[] {"-port"};
        foundValue = MainArgs.parseArgument(args, argName, defaultIfNoParam, defaultIfNoValue);
        assertEquals(defaultIfNoValue, foundValue);
    }

    public void testArgumentPresent() {
        String[] args = {"-port", "8000"};
        assertTrue(MainArgs.argumentPresent(args, "port"));
        assertFalse(MainArgs.argumentPresent(args, "foo"));

        assertFalse(MainArgs.argumentPresent(new String[0], "foo"));

    }
}
