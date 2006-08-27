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
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.util.IO;

import java.io.File;
import java.io.Serializable;

/**
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class LogFileTest extends TestCase {
    private File logDir;
    private File log1;
    private File log2;
    private File log3;

    public void setUp() {
        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
        }
        log1 = new File(logDir, "log20040903010203.xml");
        log2 = new File(logDir, "log20040905010203Lsuccessful-build-file.1.xml");
        log3 = new File(logDir, "log20051021103500.xml.gz");
    }

    public void tearDown() {
        log1.delete();
        log2.delete();
        log3.delete();
        logDir.delete();

        log1 = null;
        log2 = null;
        log3 = null;
        logDir = null;
    }

    public void testGetLatestLog() throws Exception {
        IO.write(log1, "");
        IO.write(log2, "");
        IO.write(log3, "");

        File result = LogFile.getLatestLogFile(logDir).getFile();
        assertEquals(log3.getName(), result.getName());
    }

    public void testIsCompressed() throws Exception {
        IO.write(log1, "");
        IO.write(log2, "");
        IO.write(log3, "");

        assertFalse(new LogFile(log1).isCompressed());
        assertFalse(new LogFile(log2).isCompressed());
        assertTrue(new LogFile(log3).isCompressed());
    }

    public void testGetLatestSuccessfulLog() throws Exception {
        IO.write(log1, "");
        IO.write(log2, "");
        IO.write(log3, "");

        File result = LogFile.getLatestSuccessfulLogFile(logDir).getFile();
        assertEquals(log2, result);
    }

    public void testSerializable() throws Exception {
        IO.write(log1, "");

        assertTrue("LogFile class must be serializable for Metrics Tab (charting) to work",
                Serializable.class.isAssignableFrom(LogFile.class));
    }
}
