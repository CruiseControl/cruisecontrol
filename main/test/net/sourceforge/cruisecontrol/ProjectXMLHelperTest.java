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
package net.sourceforge.cruisecontrol;

import java.util.Properties;

import junit.framework.TestCase;

public class ProjectXMLHelperTest extends TestCase {

    public void testParsePropertiesInString1() throws CruiseControlException {
        Properties properties = new Properties();
        properties.put("property", "value");
        String s = ProjectXMLHelper.parsePropertiesInString(properties, "${property}", false);
        assertEquals("value", s);

        properties.put("one", "1");
        properties.put("two", "2");
        String s2 = ProjectXMLHelper.parsePropertiesInString(properties, "a${one}b${two}c", false);
        assertEquals("a1b2c", s2);

        properties.put("one", "1");
        properties.put("two", "2");

        String s3 = ProjectXMLHelper.parsePropertiesInString(properties, "a${oneb${two}}c", false);
        assertEquals("a${oneb2}c", s3);

        properties.put("foo-bar", "b");
        properties.put("two", "bar");
        String s4 = ProjectXMLHelper.parsePropertiesInString(properties, "a${foo-${two}}c", false);
        assertEquals("abc", s4);

        try {
            ProjectXMLHelper.parsePropertiesInString(properties, "${foo", false);
            fail("badly formatted properties. Should have failed");
        } catch (CruiseControlException e) {
           // expected
        }

        String s5 = ProjectXMLHelper.parsePropertiesInString(properties, "${}", false);
        assertEquals("", s5);
    }

    public void testParsePropertiesInString2() throws CruiseControlException {
        Properties properties = new Properties();
        String s = ProjectXMLHelper.parsePropertiesInString(properties, "${missing}", false);
        assertEquals("${missing}", s);
    }
}
