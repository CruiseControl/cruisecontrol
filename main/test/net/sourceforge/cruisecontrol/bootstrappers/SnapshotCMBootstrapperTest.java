/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Retrieves change history from SnapshotCM source control using whist command.
 *
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class SnapshotCMBootstrapperTest extends TestCase {

    public void testValidate() throws CruiseControlException, IOException {

        SnapshotCMBootstrapper bootstrapper = new SnapshotCMBootstrapper();

        try {
            bootstrapper.validate();
            fail("SnapshotCMBootstrapper should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        bootstrapper.setFile("thepath");

        try {
            bootstrapper.validate();
        } catch (CruiseControlException e) {
            fail("SnapshotCMBootstrapper should not throw exceptions when required fields are set.");
        }

        // test validity of the path format?
    }

    public void testBuildUpdateCommand() throws IOException, CruiseControlException {

        SnapshotCMBootstrapper bootstrapper = new SnapshotCMBootstrapper();
        bootstrapper.setFile("somefile");

        assertEquals("wco -fR somefile", bootstrapper.buildUpdateCommand().toString());
    }
}
