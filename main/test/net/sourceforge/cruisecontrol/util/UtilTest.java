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
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class UtilTest extends TestCase {

    private File propsFile;
    private Properties testProps;

    public void setUp() throws Exception {

        //Create a properties file to test properties loading
        propsFile = File.createTempFile("testload", "properties");
        propsFile.deleteOnExit();
        StringBuffer props = new StringBuffer();
        props.append("#Test properties file\n");
        props.append("property1=value1\n");
        props.append("property2 = value2\n");
        props.append("property3=value3\n");
        IO.write(propsFile, props.toString());

        //Create a Properties object to test storing properties
        testProps = new Properties();
        testProps.setProperty("stored1", "value1");
        testProps.setProperty("stored2", "value2");
    }

    public void testLoadPropertiesFromFileMustExist() {
        File file = new File("NoSuchFile");
        try {
            Util.loadPropertiesFromFile(file);
            fail("A non-existant properties file should cause an exception!");
        } catch (Exception e) {
        }
    }

    public void testLoadPropertiesFromFile() throws CruiseControlException,
            IOException {
        Properties properties = Util.loadPropertiesFromFile(propsFile);
        assertEquals(3, properties.size());
        assertEquals("value1", properties.getProperty("property1"));
        assertEquals("value2", properties.getProperty("property2"));
        assertEquals("value3", properties.getProperty("property3"));
    }

    public void testStorePropertiesToFile() throws CruiseControlException,
            IOException {
        File file = File.createTempFile("teststore", "properties");
        file.deleteOnExit();
        Util.storePropertiesToFile(testProps, "Sample Header", file);
        Properties properties = Util.loadPropertiesFromFile(file);
        assertEquals(2, properties.size());
        assertEquals("value1", properties.getProperty("stored1"));
        assertEquals("value2", properties.getProperty("stored2"));
    }
}
