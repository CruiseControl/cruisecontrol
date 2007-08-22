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
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.Util;

/**
 * @deprecated Tests deprecated code
 */
public class CurrentBuildStatusBootstrapperTest extends TestCase {
    private static final String TEST_DIR = System.getProperty("java.io.tmpdir");
    private final FilesToDelete filesToDelete = new FilesToDelete();

    public void tearDown() {
        filesToDelete.delete();
    }

    public void testValidate() throws CruiseControlException {
        CurrentBuildStatusBootstrapper bootstrapper = new CurrentBuildStatusBootstrapper();
        try {
            bootstrapper.validate();
            fail("'file' should be a required attribute on CurrentBuildStatusBootstrapper");
        } catch (CruiseControlException cce) {
        }

        bootstrapper.setFile("somefile");
        bootstrapper.validate();

        bootstrapper.setFile(TEST_DIR + File.separator + "filename");
        bootstrapper.validate();
    }

    public void testBootstrap() throws CruiseControlException, IOException {
        final CurrentBuildStatusBootstrapper bootstrapper = new CurrentBuildStatusBootstrapper();
        final String fileName = TEST_DIR + File.separator + "_testCurrentBuildStatus.txt";
        bootstrapper.setFile(fileName);
        filesToDelete.add(new File(fileName));

        // This should be equivalent to the date used in bootstrap at seconds precision
        final Date date = new Date();

        bootstrapper.bootstrap();

        final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        final String expected = "Current Build Started At:\n" + formatter.format(date);
        assertEquals(expected, Util.readFileToString(fileName));
    }
}
