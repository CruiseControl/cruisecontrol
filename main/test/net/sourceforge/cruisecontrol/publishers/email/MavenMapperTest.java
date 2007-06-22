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
package net.sourceforge.cruisecontrol.publishers.email;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class MavenMapperTest extends TestCase {

    private MavenMapper mapper;

    public void setUp() throws IOException, CruiseControlException {
        /*
         * Write a dummy Maven project descriptor to a temporary file
         */
        String mavenPom = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
                + "<project>"
                + "<!-- the version of maven's project object model -->"
                + "<pomVersion>3</pomVersion>"
                + "<developers><developer>"
                + "<name>Master Foo</name>"
                + "<id>foo</id>"
                + "<email>foo@barbaz</email>"
                + "<timezone>1</timezone>"
                + "</developer><developer>"
                + "<name>Scholar Foofoo</name>"
                + "<id>bar</id>"
                + "<email>foofoo@barbaz</email>"
                + "<timezone>3</timezone>"
                + "<organization>Foo Buildmasters Inc.</organization>"
                + "</developer></developers></project>";
        File tmpFile = File.createTempFile("MavenMapperTest", null);
        tmpFile.deleteOnExit();
        String tmpFilePath = tmpFile.getAbsolutePath();
        BufferedWriter out = new BufferedWriter(new FileWriter(tmpFilePath));
        out.write(mavenPom);
        out.close();
        /*
         * Configure the MavenMapper for the tests
         */
        mapper = new MavenMapper();
        mapper.setProjectFile(tmpFilePath);
        mapper.open();
    }

    public void testMapping() {

        /*
         * Use the dummy Maven project descriptor to proof the right mapping
         */
        assertEquals("foo@barbaz", mapper.mapUser("foo"));
        assertEquals("foofoo@barbaz", mapper.mapUser("bar"));
    }
}
