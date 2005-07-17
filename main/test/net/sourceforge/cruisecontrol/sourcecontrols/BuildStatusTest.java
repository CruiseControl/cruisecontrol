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

package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Log;
import net.sourceforge.cruisecontrol.util.DateUtil;

/**
 * Unit tests for BuildStatus.java.
 *
 *@author Garrick Olson
 */
public class BuildStatusTest extends TestCase {
    private BuildStatus buildStatus;

    protected void setUp() throws Exception {
        buildStatus = new BuildStatus();
    }

    protected void tearDown() throws Exception {
        buildStatus = null;
    }

    /**
     * Make sure the validate() method works properly.
     */
    public void testValidate() throws Exception {
        // Verify log directory is mandatory
        try {
            buildStatus.validate();
            fail("Should have thrown exception indicating log directory is mandatory");
        } catch (CruiseControlException e) {
            assertEquals("Wrong exception", "'logdir' is required for BuildStatus", e.getMessage());
        }

        // Verify log directory must exist
        buildStatus.setLogDir("does not exist");

        try {
            buildStatus.validate();
            fail("Should have thrown exception indicating log directory must exist");
        } catch (CruiseControlException e) {
            assertTrue("Wrong exception", e.getMessage().startsWith("Log directory does not exist"));
        }
        
        // Verify log directory must be a directory
        File tempFile = File.createTempFile("temp", "txt");
        tempFile.deleteOnExit();
        buildStatus.setLogDir(tempFile.getAbsolutePath());

        try {
            buildStatus.validate();
            fail("Should have thrown exception indicating log directory must be a directory");
        } catch (CruiseControlException e) {
            assertTrue("Wrong exception", e.getMessage().startsWith("Log directory is not a directory"));
        }

        // Set it to a directory and everything should be okay
        buildStatus.setLogDir(tempFile.getParentFile().getAbsolutePath());
        buildStatus.validate();
    }

    /**
     * Verify the getModifications() method works properly.
     */
    public void testGetModifications() throws Exception {
        File tempFile = File.createTempFile("temp", "txt");
        File tempDir = tempFile.getParentFile();
        tempFile.delete();

        buildStatus.setLogDir(tempDir.getAbsolutePath());
        buildStatus.validate();

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.add(Calendar.DATE, -1);
        Date yesterday = calendar.getTime();
        calendar.add(Calendar.DATE, -1);
        Date twoDaysAgo = calendar.getTime();

        // Should be no modifications at this point
        List modifications = buildStatus.getModifications(twoDaysAgo, null);
        assertEquals("Wrong number of modifications", 0, modifications.size());

        // Verify an unsuccessful build does not show up
        File yesterdayLog = new File(tempDir, Log.formatLogFileName(yesterday));
        yesterdayLog.createNewFile();
        try {
            modifications = buildStatus.getModifications(twoDaysAgo, null);
            assertEquals("Wrong number of modifications", 0, modifications.size());
        } finally {
            yesterdayLog.delete();
        }

        // Verify a successful build shows up
        File yesterdayLog2 = new File(tempDir, Log.formatLogFileName(yesterday, "good.1"));
        yesterdayLog2.createNewFile();
        try {
            modifications = buildStatus.getModifications(twoDaysAgo, null);
            assertEquals("Wrong number of modifications", 1, modifications.size());

            // Verify properties were set correctly
            assertEquals("Property was not set correctly", tempDir.getAbsolutePath(),
                         buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGDIR_KEY));
            assertEquals("Property was not set correctly", yesterdayLog2.getName(),
                         buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGFILE_KEY));
            assertEquals("Property was not set correctly", DateUtil.getFormattedTime(yesterday),
                         buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGTIME_KEY));
            assertEquals("Property was not set correctly", "good.1",
                         buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGLABEL_KEY));

            // Verify date range works
            modifications = buildStatus.getModifications(today, null);
            assertEquals("Wrong number of modifications", 0, modifications.size());

            // Verify properties are set correctly when there are multiple modifications
            File todayLog = new File(tempDir, Log.formatLogFileName(today, "good.2"));
            todayLog.createNewFile();
            try {
                modifications = buildStatus.getModifications(twoDaysAgo, null);
                assertEquals("Wrong number of modifications", 2, modifications.size());

                // Verify properties were set correctly
                assertEquals("Property was not set correctly", tempDir.getAbsolutePath(),
                             buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGDIR_KEY));
                assertEquals("Property was not set correctly", todayLog.getName(),
                             buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGFILE_KEY));
                assertEquals("Property was not set correctly", DateUtil.getFormattedTime(today),
                             buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGTIME_KEY));
                assertEquals("Property was not set correctly", "good.2",
                             buildStatus.getProperties().get(BuildStatus.MOST_RECENT_LOGLABEL_KEY));
            } finally {
                todayLog.delete();
            }
        } finally {
            yesterdayLog2.delete();
        }
    }

    public void testSetProperty() {
        try {
            buildStatus.setProperty("blowup");
            fail();
        } catch (UnsupportedOperationException expected) {
            assertEquals("attribute 'property' is not supported", expected.getMessage());
        }
    }

    public void testSetPropertyOnDelete() {
        try {
            buildStatus.setPropertyOnDelete("blowup");
            fail();
        } catch (UnsupportedOperationException expected) {
            assertEquals("attribute 'propertyOnDelete' is not supported", expected.getMessage());
        }
    }
}
