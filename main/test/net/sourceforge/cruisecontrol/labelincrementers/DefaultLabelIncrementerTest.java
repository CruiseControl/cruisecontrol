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
package net.sourceforge.cruisecontrol.labelincrementers;

import junit.framework.TestCase;

public class DefaultLabelIncrementerTest extends TestCase {

    private DefaultLabelIncrementer incrementer;

    public DefaultLabelIncrementerTest(String name) {
        super(name);
    }

    public void setUp() {
        incrementer = new DefaultLabelIncrementer();
    }

    public void testIsValidLabel() {
        assertTrue(incrementer.isValidLabel("x.88"));
        assertFalse(incrementer.isValidLabel("x.y"));
        assertFalse(incrementer.isValidLabel("x88"));

        incrementer.setSeparator("#");
        assertFalse(incrementer.isValidLabel("x.88"));
        assertTrue(incrementer.isValidLabel("x#88"));

        incrementer = new DefaultLabelIncrementer();
        assertTrue(incrementer.isValidLabel("x.88"));
        assertFalse(incrementer.isValidLabel("x#88"));
    }

    public void testIncrementLabel() {
        assertEquals("x.89", incrementer.incrementLabel("x.88", null));
    }

    public void testSetPreBuildIncrementer() {
        assertFalse(incrementer.isPreBuildIncrementer());
        incrementer.setPreBuildIncrementer(true);
        assertTrue(incrementer.isPreBuildIncrementer());

        incrementer = new DefaultLabelIncrementer();
        assertFalse(incrementer.isPreBuildIncrementer());
    }

    public void testGetDefaultLabel() {
        assertEquals("build.1", incrementer.getDefaultLabel());
        incrementer.setSeparator("#");
        assertEquals("build#1", incrementer.getDefaultLabel());
        assertTrue(incrementer.isValidLabel(incrementer.getDefaultLabel()));
    }

    public void testDefaultLabel() {
        incrementer.setDefaultLabel("foo.69");
        assertEquals("foo.69", incrementer.getDefaultLabel());
    }

    public void testDefaultLabelNonDefaultSeparator() {
        incrementer.setDefaultLabel("R_604");
        incrementer.setSeparator("_");
        assertEquals("R_604", incrementer.getDefaultLabel());
    }

}
