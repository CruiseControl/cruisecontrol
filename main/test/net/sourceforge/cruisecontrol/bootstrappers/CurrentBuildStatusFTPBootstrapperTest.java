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
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;

/**
 * @deprecated Tests deprecated code
 */
public class CurrentBuildStatusFTPBootstrapperTest extends TestCase {
    private final FilesToDelete filesToDelete = new FilesToDelete();

    public CurrentBuildStatusFTPBootstrapperTest(String name) {
        super(name);
    }

    public void tearDown() {
        filesToDelete.delete();
    }

    public void testValidate1() {
        CurrentBuildStatusFTPBootstrapper cbsfb =
            new CurrentBuildStatusFTPBootstrapper();
        try {
            cbsfb.validate();
            fail("did not fail for unset properties");
        } catch (CruiseControlException cce) {
            // test exception?
        }
    }

    public void testValidate2() {
        CurrentBuildStatusFTPBootstrapper cbsfb =
            new CurrentBuildStatusFTPBootstrapper();
        cbsfb.setTargetHost("x");
        try {
            cbsfb.validate();
            fail("'file' should be a required attribute on CurrentBuildStatusFTPBootstrapper");
        } catch (CruiseControlException cce) {
            // test exception?
        }
    }

    public void testValidate3() {
        CurrentBuildStatusFTPBootstrapper cbsfb =
            new CurrentBuildStatusFTPBootstrapper();
        cbsfb.setFile("x");
        try {
            cbsfb.validate();
            fail("'targetHost' should be a required attribute on CurrentBuildStatusFTPBootstrapper");
        } catch (CruiseControlException cce) {
            // test exception?
        }
    }

    public void testValidate4() throws Exception {
        CurrentBuildStatusFTPBootstrapper cbsfb =
            new CurrentBuildStatusFTPBootstrapper();
        cbsfb.setTargetHost("x");
        cbsfb.setFile("x");
        cbsfb.setDestDir("x");
        cbsfb.validate();
    }

    public void testMakeFile1() throws Exception {
        CurrentBuildStatusFTPBootstrapper cbsfb =
            new CurrentBuildStatusFTPBootstrapper();
        cbsfb.setFile("_testCurrentBuildStatus1.txt");
        filesToDelete.add(new File("_testCurrentBuildStatus1.txt"));

        // This should be equivalent to the date used in bootstrap at seconds
        // precision
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String expected =
            "Current Build Started At:\n"
            + formatter.format(new Date());
        String out = cbsfb.makeFile();
        assertEquals(expected, out);
    }
}
