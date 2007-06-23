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
package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class CVSBootstrapperTest extends TestCase {

    private CVSBootstrapper bootStrapper;

    protected void setUp()  {
        bootStrapper = new CVSBootstrapper();
    }

    public void testValidate() throws IOException {
        try {
            bootStrapper.validate();
            fail("CVSBootstrapper should throw an exception when no attributes are set.");
        } catch (CruiseControlException e) {
        }

        bootStrapper.setCvsroot("someroot");
        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("CVSBootstrapper should not throw an exception when a valid attribute is set.");
        }

        bootStrapper = new CVSBootstrapper();
        bootStrapper.setFile("somefile");
        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("CVSBootstrapper should not throw an exception when a valid attribute is set.");
        }

        File tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();

        bootStrapper.setLocalWorkingCopy(tempFile.getParent());
        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("CVSBootstrapper should not throw an exception when a valid attribute is set.");
        }

        bootStrapper.setLocalWorkingCopy(tempFile.getAbsolutePath());
        try {
            bootStrapper.validate();
            fail("validate() should fail when 'localWorkingCopy' is file instead of directory.");
        } catch (CruiseControlException e) {
        }

        String badDirName = "z:/foo/foo/foo/bar";
        bootStrapper.setLocalWorkingCopy(badDirName);
        try {
            bootStrapper.validate();
            fail("validate() should throw exception on non-existant directory.");
        } catch (CruiseControlException e) {
        }
    }

    public void testCompressionValidation() {
        bootStrapper.setCvsroot("someroot");

        assertCompressionLevelInvalid("A");
        assertCompressionLevelInvalid("-1");
        assertCompressionLevelInvalid("1.1");
        assertCompressionLevelInvalid("10");
        assertCompressionLevelInvalid("");
        assertCompressionLevelInvalid("   ");
        assertCompressionLevelInvalid("\n\n\t\r");
        assertCompressionLevelValid("1");
        assertCompressionLevelValid("2");
        assertCompressionLevelValid("3");
        assertCompressionLevelValid("4");
        assertCompressionLevelValid("5");
        assertCompressionLevelValid("6");
        assertCompressionLevelValid("7");
        assertCompressionLevelValid("8");
        assertCompressionLevelValid("9");
        assertCompressionLevelValid(null);
    }

    private void assertCompressionLevelValid(String candidate) {
        bootStrapper.setCompression(candidate);
        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("validate() should NOT throw exception on '" + candidate + "' compression value.");
        }
    }

    private void assertCompressionLevelInvalid(String candidate) {
        bootStrapper.setCompression(candidate);
        try {
            bootStrapper.validate();
            fail("validate() should throw exception on '" + candidate + "' compression value.");
        } catch (CruiseControlException e) {
        }
    }

    public void testBuildUpdateCommand() throws CruiseControlException {
        String tempDir = System.getProperty("java.io.tmpdir");

        bootStrapper.setLocalWorkingCopy(tempDir);
        assertEquals(
            "cvs update -dP",
            bootStrapper.buildUpdateCommand().toString());

        bootStrapper.setFile("somefile");
        assertEquals(
            "cvs update -dP somefile",
            bootStrapper.buildUpdateCommand().toString());

        bootStrapper.setCvsroot("somecvsroot");
        assertEquals(
            "cvs -d somecvsroot update -dP somefile",
            bootStrapper.buildUpdateCommand().toString());

        bootStrapper.setResetStickyTags(true);
        assertEquals("cvs -d somecvsroot update -dPA somefile",
                bootStrapper.buildUpdateCommand().toString());

        bootStrapper.setOverwriteChanges(true);
        assertEquals("cvs -d somecvsroot update -dPAC somefile",
                bootStrapper.buildUpdateCommand().toString());

        bootStrapper.setCompression("9");
        assertEquals("cvs -z9 -d somecvsroot update -dPAC somefile",
                bootStrapper.buildUpdateCommand().toString());
    }

}
