/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
import java.util.Locale;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;

/**
 * User: jfredrick Date: Jan 31, 2004 Time: 5:18:43 PM
 *
 * @author <a href="mailto:jeffjensen@upstairstechnology.com">Jeff Jensen </a>
 */
public class StatusHelperTest extends TestCase {
    private static final String LOG_DIR = "target/testresults/";
    private static final String PROJECT_NAME = "testProject";
    private static final String STATUS_FILENAME = "buildStatus.txt";

    private static final String TEXT = "the test status";
    private static final String TIME = "12/17/2005 20:11:25";
    private static final String PLAIN_TEXT = TEXT + "\n" + TIME + "\n";
    private static final String HTML_TEXT = TEXT + "\n<br/>" + TIME + "\n<br/>";
    private static final String XML_LOGGER_WITH_STATUS_OUTPUT = TEXT + "\n" + TIME
                + "<br><span class=\"link\">11:47:33&nbsp;[-force-atriuum-stop]&nbsp;</span>"
                + "<br><span class=\"link\">11:47:34&nbsp;[-clean]&nbsp;</span>"
                + "<br><span class=\"link\">11:47:34&nbsp;[-checkout]&nbsp;</span>"
                + "<br><span class=\"link\">11:47:34&nbsp;[-update]&nbsp;</span>"
                + "<br><span class=\"link\">11:48:29&nbsp;[-clean-old-test-data]&nbsp;</span>"
                + "<br><span class=\"link\">11:48:29&nbsp;[clean]&nbsp;</span>"
                + "<br><span class=\"link\">11:48:30&nbsp;[checkstyle]&nbsp;</span>"
                + "<br><span class=\"link\">11:48:34&nbsp;[-init]&nbsp;</span>"
                + "<br><span class=\"link\">11:48:34&nbsp;[-build]&nbsp;</span>";

    private static final String LOG_CONTENTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<cruisecontrol></cruisecontrol>";

    private StatusHelper helper;
    private File logDir;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    protected void setUp() throws Exception {
        helper = new StatusHelper();

        // make base log dir
        logDir = new File(LOG_DIR);
        if (logDir.isFile()) {
            logDir.delete();
        }
        if (!logDir.isDirectory()) {
            if (!logDir.getParentFile().exists()) {
                // Pre-create parent dir to minimize chance of error creating project-log dir on Winz
                logDir.getParentFile().mkdirs();
            }
            assertTrue("Failed to create test result dir " + logDir.getAbsolutePath(), logDir.mkdirs());
            filesToDelete.add(logDir);
        }

        // make multi project log dir
        File projectLogDir = new File(logDir, PROJECT_NAME + "/");
        if (!projectLogDir.exists()) {
            assertTrue("Failed to create project log dir", projectLogDir.mkdir());
            filesToDelete.add(logDir);
        }

        File file = new File(logDir, "log20040102030405.xml");
        prepareFile(file, LOG_CONTENTS);

        // for single project
        file = new File(logDir, STATUS_FILENAME);
        prepareFile(file, PLAIN_TEXT);

        // for multi project
        file = new File(projectLogDir, STATUS_FILENAME);
        prepareFile(file, PLAIN_TEXT);
    }

    protected void tearDown() throws Exception {
        helper = null;
        filesToDelete.delete();
    }

    public void testGetLastBuildResult() throws IOException, CruiseControlException {
        assertNull(helper.getLastBuildResult());

        helper.setProjectDirectory(logDir);
        assertEquals("failed", helper.getLastBuildResult());

        File file = new File(logDir, "log20040203040506Lbuild.2.xml");
        prepareFile(file, LOG_CONTENTS);
        helper.setProjectDirectory(logDir);
        assertEquals("passed", helper.getLastBuildResult());
    }

    public void testGetLastBuildTimeString() throws IOException, CruiseControlException {
        assertNull(helper.getLastBuildTimeString(Locale.US));

        helper.setProjectDirectory(logDir);
        assertEquals("01/02/2004 03:04:05", helper.getLastBuildTimeString(Locale.US));

        File file = new File(logDir, "log20040203040506Lbuild.2.xml");
        prepareFile(file, LOG_CONTENTS);
        helper.setProjectDirectory(logDir);
        assertEquals("02/03/2004 04:05:06", helper.getLastBuildTimeString(Locale.US));
    }

    public void testGetLastSuccessfulBuildLabel() throws IOException, CruiseControlException {
        assertNull(helper.getLastSuccessfulBuildLabel());

        helper.setProjectDirectory(logDir);
        assertNull(helper.getLastSuccessfulBuildLabel());

        File file = new File(logDir, "log20040203040506Lbuild.2.xml");
        prepareFile(file, LOG_CONTENTS);
        helper.setProjectDirectory(logDir);
        assertEquals("build.2", helper.getLastSuccessfulBuildLabel());
    }

    public void testGetLastSuccessfulBuildTimeString() throws IOException, CruiseControlException {
        assertNull(helper.getLastSuccessfulBuildTimeString(Locale.US));

        helper.setProjectDirectory(logDir);
        assertNull(helper.getLastSuccessfulBuildTimeString(Locale.US));

        File file = new File(logDir, "log20040203040506Lbuild.2.xml");
        prepareFile(file, LOG_CONTENTS);
        helper.setProjectDirectory(logDir);
        assertEquals("02/03/2004 04:05:06", helper.getLastSuccessfulBuildTimeString(Locale.US));
    }

    public void testGetCurrentStatus() {
        String logDirPath = logDir.getAbsolutePath();
        String actual = helper.getCurrentStatus("true", logDirPath, PROJECT_NAME, STATUS_FILENAME);
        assertEquals("testing single project: ", HTML_TEXT, actual);

        actual = helper.getCurrentStatus("false", logDirPath, PROJECT_NAME, STATUS_FILENAME);
        assertEquals("testing multi project: ", HTML_TEXT, actual);
    }

    public void testWithXmlLoggerWithStatusOutput() throws IOException, CruiseControlException {
        File projectLogDir = new File(logDir, "xmlusingproject");
        projectLogDir.mkdir();
        File file = new File(projectLogDir, "status.txt");
        prepareFile(file, XML_LOGGER_WITH_STATUS_OUTPUT);

        String actual = helper.getCurrentStatus("false", logDir.getAbsolutePath(), "xmlusingproject", "status.txt");
        assertEquals(HTML_TEXT, actual);
    }

    private void prepareFile(File file, String body) throws CruiseControlException {
        IO.write(file, body);
        filesToDelete.add(file);
    }
}
