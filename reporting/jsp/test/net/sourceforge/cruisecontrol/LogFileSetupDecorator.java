/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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

import java.io.File;
import java.io.IOException;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * A test decorator that can be used to set up log files for testing.
 *
 * Note: the files are not placed in a project specific folder.
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class LogFileSetupDecorator extends TestSetup {
    public static final File LOG_DIR = new File("tempLogDir");
    private File[] logFiles;

    /**
     * @param decoratedTest the decorated test. This would normally be a TestSuite.
     */
    public LogFileSetupDecorator(Test decoratedTest) {
        super(decoratedTest);
    }

    protected void setUp() throws IOException {
        if (!LOG_DIR.exists()) {
            assertTrue("Failed to create test result dir", LOG_DIR.mkdir());
        }
        logFiles = new File[] { new File(LOG_DIR, "log20020222120000.xml"),
                                new File(LOG_DIR, "log20020223120000LBuild.1.xml"),
                                new File(LOG_DIR, "log20020224120000.xml"),
                                new File(LOG_DIR, "log20020225120000LBuild.2.xml"),
                                new File(LOG_DIR, "log20041018160000.xml.gz"),
                                new File(LOG_DIR, "log20041018170000LBuild.3.xml.gz")};
        for (int i = 0; i < logFiles.length; i++) {
            File logFile = logFiles[i];
            logFile.createNewFile();
        }
    }

    protected void tearDown() throws Exception {
        for (int i = 0; i < logFiles.length; i++) {
            logFiles[i].delete();
        }
        LOG_DIR.delete();
    }
}