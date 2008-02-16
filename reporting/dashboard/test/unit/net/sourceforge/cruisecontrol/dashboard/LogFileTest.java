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
package net.sourceforge.cruisecontrol.dashboard;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

public class LogFileTest extends TestCase {
    public void testShouldReturnFileInputStreamWhenUnzippedLogFile() throws Exception {
        InputStream in = DataUtils.getPassingBuildLbuildAsFile().getInputStream();
        assertTrue("Should be a FileInputStream.", in instanceof FileInputStream);
    }

    public void testShouldReturnGZipInputStreamWhenZippedLogFile() throws Exception {
        InputStream in = DataUtils.getZippedBuildAsFile().getInputStream();
        assertTrue("Should be a GZIPInputStream.", in instanceof GZIPInputStream);
    }

    public void testShouldComputeBuildDateTimeFromPassedLogFileName() throws Exception {
        LogFile logFile = DataUtils.getPassingBuildLbuildAsFile();
        assertEquals("2005-12-09 12:21.03", logFile.getDateTime());
    }

    public void testShouldComputeBuildDateTimeFromFailedLogFileName() throws Exception {
        LogFile logFile = DataUtils.getFailedBuildLbuildAsFile();
        assertEquals("2005-12-09 12:21.04", logFile.getDateTime());
    }

    public void testShouldThrowExceptionWhenInvalidLogFileName() throws Exception {
        assertInvalidFileName("12320050312121003L1.xml");
        assertInvalidFileName("12320050312121003.xml");
        assertInvalidFileName("log200503121210.xml");
        assertInvalidFileName("log.xml");
        assertInvalidFileName("logL1.xml");
    }

    public void testShouldComputeLabelFromPassedLogFileName() throws Exception {
        LogFile logFile = DataUtils.getPassingBuildLbuildAsFile();
        assertEquals("build.489", logFile.getLabel());
        LogFile zippedLogFile = new LogFile(DataUtils.PASSING_BUILD_LBUILD_0_XML + ".gz");
        assertEquals("build.489", zippedLogFile.getLabel());
    }

    public void testShouldHaveEmptyLabelFromFailedLogFileName() throws Exception {
        LogFile logFile = DataUtils.getFailedBuildLbuildAsFile();
        assertEquals("", logFile.getLabel());
        LogFile zippedLogFile = new LogFile(DataUtils.FAILING_BUILD_XML + ".gz");
        assertEquals("", zippedLogFile.getLabel());
    }

    private void assertInvalidFileName(String invalidLogFileName) {
        try {
            new LogFile(invalidLogFileName);
            fail("Should not be able to create invalid log file " + invalidLogFileName);
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid logfile name: " + invalidLogFileName, e.getMessage());
        }
    }
}
