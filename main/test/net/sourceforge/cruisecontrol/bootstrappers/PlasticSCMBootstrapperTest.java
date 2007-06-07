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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;

public class PlasticSCMBootstrapperTest extends TestCase {

    private PlasticSCMBootstrapper bootstrapper;

    private final String workingDir = "plastictestdir";
    private File fileWorkingDir = new File (workingDir);

    protected void setUp()  {
        bootstrapper = new PlasticSCMBootstrapper();
        if (fileWorkingDir.exists()) {
            fileWorkingDir.delete();
        }
    }

    public void tearDown() {
        bootstrapper = null;
        fileWorkingDir.delete();
    }

    public void testValidate() throws CruiseControlException {

        try {
            bootstrapper.validate();
            fail("PlasticSCMBootstrapper should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        try {
            bootstrapper.validate();
            fail("PlasticSCMBootstrapper should throw an exception when the wkspath not exist");
        } catch (CruiseControlException e) {
        }

        bootstrapper = new PlasticSCMBootstrapper();
        fileWorkingDir.mkdir();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setRepository("rep1");
        try {
            bootstrapper.validate();
            fail("PlasticSCMBootstrapper should throw an exception when branch field is not set and repository yes.");
        } catch (CruiseControlException e) {
        }

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.validate();

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setBranch("branch");
        bootstrapper.validate();

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setBranch("branch");
        bootstrapper.setRepository("repository");
        bootstrapper.validate();

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setForced(true);
        bootstrapper.setPathtoupdate(workingDir);
        bootstrapper.validate();
    }

    public void testBuildUpdateCommand() throws CruiseControlException {

        fileWorkingDir.mkdir();

        bootstrapper.setWkspath(workingDir);
        assertEquals("cm update .", bootstrapper.buildUpdateCommand().toString());

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setForced(true);
        assertEquals("cm update . --forced", bootstrapper.buildUpdateCommand().toString());

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setPathtoupdate("test");
        assertEquals("cm update test", bootstrapper.buildUpdateCommand().toString());

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setForced(true);
        bootstrapper.setPathtoupdate("test");
        assertEquals("cm update test --forced", bootstrapper.buildUpdateCommand().toString());

    }

    public void testBuildSwitchToBranchCommand() throws CruiseControlException {

        fileWorkingDir.mkdir();

        bootstrapper.setWkspath(workingDir);
        bootstrapper.setBranch("br:/main");
        assertEquals("cm stb br:/main --noupdate", bootstrapper.buildSwitchToBranchCommand().toString());

        bootstrapper = new PlasticSCMBootstrapper();
        bootstrapper.setWkspath(workingDir);
        bootstrapper.setBranch("br:/main");
        bootstrapper.setRepository("mainrep");
        assertEquals("cm stb br:/main -repository=mainrep --noupdate",
                bootstrapper.buildSwitchToBranchCommand().toString());

    }

}
