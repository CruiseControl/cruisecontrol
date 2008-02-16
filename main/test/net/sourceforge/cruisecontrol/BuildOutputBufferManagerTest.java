/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class BuildOutputBufferManagerTest extends TestCase {
    private BuildOutputLoggerManager loggerManager;
    private File tempFile;

    protected void setUp() throws Exception {
        loggerManager = new BuildOutputLoggerManager();
        tempFile = tempFile();
    }

    public void testShouldCreateLogger() throws Exception {
        BuildOutputLogger logger = loggerManager.lookupOrCreate(tempFile);
        assertEquals(0, logger.retrieveLines(0).length);
        logger.consumeLine("1");
        logger.consumeLine("2");
        assertEquals(2, logger.retrieveLines(0).length);
        assertSame(logger,  loggerManager.lookup());
        assertSame(logger,  loggerManager.lookupOrCreate(tempFile));
    }

    public void testShouldCreateTemporaryLoggerWhenLookingUpMissingLogger() throws Exception {
        BuildOutputLogger temporaryLogger = loggerManager.lookup();
        BuildOutputLogger logger = loggerManager.lookupOrCreate(tempFile);
        assertNotSame(temporaryLogger, logger);
        assertSame(logger, loggerManager.lookup());
        assertSame(logger, loggerManager.lookupOrCreate(tempFile));
        assertEquals(0, temporaryLogger.retrieveLines(0).length);
    }

    private File tempFile() throws IOException {
        File file = File.createTempFile("tempOutputlogger", ".tmp");
        file.deleteOnExit();
        return file;
    }
}
