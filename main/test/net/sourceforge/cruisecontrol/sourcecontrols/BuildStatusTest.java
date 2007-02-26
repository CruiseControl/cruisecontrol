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

package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Log;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.DateUtil;

/**
 * Unit tests for BuildStatus.java.
 *
 *@author Garrick Olson
 */
public class BuildStatusTest extends TestCase {
    private BuildStatus buildStatus;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    protected void setUp() throws Exception {
        buildStatus = new BuildStatus();
    }

    protected void tearDown() throws Exception {
        buildStatus = null;
        filesToDelete.delete();
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
        filesToDelete.add(tempFile);
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
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

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
        filesToDelete.add(yesterdayLog);
        modifications = buildStatus.getModifications(twoDaysAgo, null);
        assertEquals("Wrong number of modifications", 0, modifications.size());

        // Verify a successful build shows up
        File yesterdayLog2 = new File(tempDir, Log.formatLogFileName(yesterday, "good.1"));
        yesterdayLog2.createNewFile();
        filesToDelete.add(yesterdayLog2);
            
        modifications = buildStatus.getModifications(twoDaysAgo, null);
        assertEquals("Wrong number of modifications", 1, modifications.size());

        Map properties = buildStatus.getProperties();
        // Verify properties were set correctly
        assertEquals("Property was not set correctly", tempDir.getAbsolutePath(),
                     properties.get(BuildStatus.MOST_RECENT_LOGDIR_KEY));
        assertEquals("Property was not set correctly", yesterdayLog2.getName(),
                     properties.get(BuildStatus.MOST_RECENT_LOGFILE_KEY));
        assertEquals("Property was not set correctly", DateUtil.getFormattedTime(yesterday),
                     properties.get(BuildStatus.MOST_RECENT_LOGTIME_KEY));
        assertEquals("Property was not set correctly", "good.1",
                     properties.get(BuildStatus.MOST_RECENT_LOGLABEL_KEY));
        assertEquals(4, properties.size());

            // Verify date range works
        modifications = buildStatus.getModifications(today, null);
        assertEquals("Wrong number of modifications", 0, modifications.size());

        // Verify properties are set correctly when there are multiple modifications
        File todayLog = new File(tempDir, Log.formatLogFileName(today, "good.2"));
        todayLog.createNewFile();
        filesToDelete.add(todayLog);

        buildStatus.setProperty("property");
        modifications = buildStatus.getModifications(twoDaysAgo, null);
        assertEquals("Wrong number of modifications", 2, modifications.size());

        properties = buildStatus.getProperties();
        // Verify properties were set correctly
        assertEquals("Property was not set correctly", tempDir.getAbsolutePath(), properties
                .get(BuildStatus.MOST_RECENT_LOGDIR_KEY));
        assertEquals("Property was not set correctly", todayLog.getName(), properties
                .get(BuildStatus.MOST_RECENT_LOGFILE_KEY));
        assertEquals("Property was not set correctly", DateUtil.getFormattedTime(today), properties
                .get(BuildStatus.MOST_RECENT_LOGTIME_KEY));
        assertEquals("Property was not set correctly", "good.2", properties.get(BuildStatus.MOST_RECENT_LOGLABEL_KEY));
        assertEquals("true", properties.get("property"));
        assertEquals(5, properties.size());
    }
}
