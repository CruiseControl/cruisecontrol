/****************************************************************************
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.util;

import java.util.Map;

import junit.framework.TestCase;

public class PropertiesHelperTest extends TestCase {
    private static final String TEST_PROPERTIES_FILENAME = "testdist.properties";
    private static final String TEST_PROP_NAME = "testProperty";

    public void testLoadOptionalProperties() {
        Map propertiesMap = PropertiesHelper.loadOptionalProperties(TEST_PROPERTIES_FILENAME);
        assertNotNull(propertiesMap);
        String value = (String) propertiesMap.get(TEST_PROP_NAME);
        assertEquals("true", value);

        propertiesMap = PropertiesHelper.loadOptionalProperties("bogus.properties");
        assertNotNull(propertiesMap);
        value = (String) propertiesMap.get(TEST_PROP_NAME);
        assertNull(value);
    }

    public void testLoadRequiredProperties() {
        Map propertiesMap = PropertiesHelper.loadRequiredProperties(TEST_PROPERTIES_FILENAME);
        assertNotNull(propertiesMap);
        String value = (String) propertiesMap.get(TEST_PROP_NAME);
        assertEquals("true", value);

        try {
            PropertiesHelper.loadRequiredProperties("bogus.properties");
            fail("Should throw RuntimeException when a required properties file is not found");
        } catch (RuntimeException e) {
            // Expected
            assertEquals("Failed to load required properties file 'bogus.properties'",
                e.getMessage());
        }
    }

}
