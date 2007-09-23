/*****************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
 *****************************************************************************/
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;
import java.io.IOException;

/**
 * @see <a href="http://git.or.cz/">git.or.cz</a>
 * @author <a href="rschiele@gmail.com">Robert Schiele</a>
 */
public class GitBootstrapperTest extends TestCase {
    private GitBootstrapper bootStrapper;

    protected void setUp()  {
        bootStrapper = new GitBootstrapper();
    }

    public void testValidate() throws IOException {
        try {
            bootStrapper.validate();
            fail("should throw an exception when no attributes are set");
        } catch (CruiseControlException e) {
        }

        bootStrapper = new GitBootstrapper();
        bootStrapper.setLocalWorkingCopy("invalid directory");
        try {
            bootStrapper.validate();
            fail("should throw an exception when an invalid "
                 + "'localWorkingCopy' attribute is set");
        } catch (CruiseControlException e) {
            // expected
        }

        File tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();

        bootStrapper = new GitBootstrapper();
        bootStrapper.setLocalWorkingCopy(tempFile.getParent());
        try {
            bootStrapper.validate();
        } catch (CruiseControlException e) {
            fail("should not throw an exception when at least a valid "
                 + "'localWorkingCopy' attribute is set");
        }

        bootStrapper = new GitBootstrapper();
        bootStrapper.setLocalWorkingCopy(tempFile.getAbsolutePath());
        try {
            bootStrapper.validate();
            fail("should throw an exception when 'localWorkingCopy' is a "
                 + "file instead of a directory.");
        } catch (CruiseControlException e) {
            // expected
        }
    }
}
