/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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

import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * User: jfredrick
 * Date: Jan 31, 2004
 * Time: 5:18:43 PM
 */
public class StatusHelperTest extends TestCase {
    private StatusHelper helper;
    private File logDir;

    private FilesToDelete files = new FilesToDelete();

    protected void setUp() throws Exception {
        helper = new StatusHelper();

        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
            files.addFile(logDir);
        }

        File file = new File(logDir, "log20040102030405.xml");
        prepareFile(file);
    }

    protected void tearDown() throws Exception {
        helper = null;
        files.delete();
    }

    public void testGetLastBuildResult() throws IOException {
        assertNull(helper.getLastBuildResult());

        helper.setProjectDirectory(logDir);
        assertEquals("failed", helper.getLastBuildResult());

        File file = new File(logDir, "log20040203040506Lbuild.2.xml");
        prepareFile(file);
        helper.setProjectDirectory(logDir);
        assertEquals("passed", helper.getLastBuildResult());
    }

    public void testGetLastBuildTimeString() throws IOException {
        assertNull(helper.getLastBuildTimeString());

        helper.setProjectDirectory(logDir);
        assertEquals("failed", helper.getLastBuildResult());

        File file = new File(logDir, "log20040203040506Lbuild.2.xml");
        prepareFile(file);
        helper.setProjectDirectory(logDir);
        assertEquals("passed", helper.getLastBuildResult());
    }

    private void prepareFile(File file) throws IOException {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cruisecontrol></cruisecontrol>";
        FileWriter writer = new FileWriter(file);
        writer.write(body);
        writer.close();
        files.addFile(file);
    }

    class FilesToDelete {
        private List files = new Vector();

        void addFile(File file) {
            files.add(file);
        }

        void delete() {
            Iterator fileIterator = files.iterator();
            while (fileIterator.hasNext()) {
                File file = (File) fileIterator.next();
                file.delete();
            }
            files.clear();
        }
    }
}
