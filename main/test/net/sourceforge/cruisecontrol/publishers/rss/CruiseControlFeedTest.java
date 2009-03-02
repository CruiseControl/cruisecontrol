/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers.rss;

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;

/*
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 * Licensed under the CruiseControl BSD license
 */
public class CruiseControlFeedTest extends TestCase {

    private File tmpFile;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    public void setUp() throws Exception {
        tmpFile = File.createTempFile("CruiseControlFeedTest", ".tmp");
        tmpFile.deleteOnExit();
        filesToDelete.add(tmpFile);
        IO.write(tmpFile, "<rss><channel/></rss>");
    }

    public void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testProjectCount() {

        CruiseControlFeed feed = new CruiseControlFeed(tmpFile);
        assertEquals(0, feed.getProjectCount());
        feed.incrementProjectCount();
        assertEquals(1, feed.getProjectCount());
        feed.incrementProjectCount();
        assertEquals(2, feed.getProjectCount());
    }


    public void testProjectName() {

        CruiseControlFeed feed = new CruiseControlFeed(tmpFile);
        assertNull(feed.getProjectName());

        feed.setProjectName("Project1");
        assertEquals("Project1", feed.getProjectName());

        feed.setProjectName("Project2");
        assertEquals("Project1, Project2", feed.getProjectName());

        feed.setProjectName("Project3");
        assertEquals("Project1, Project2, Project3", feed.getProjectName());
    }

    public void testTitle() {

        CruiseControlFeed feed = new CruiseControlFeed(tmpFile);
        assertEquals("CruiseControl Build Results", feed.getTitle());

        feed.setProjectName("Project1");
        assertEquals("CruiseControl Build Results", feed.getTitle());

        feed.setTitle("new title");
        assertEquals("new title", feed.getTitle());
    }

    public void testDescription() {

        CruiseControlFeed feed = new CruiseControlFeed(tmpFile);
        assertEquals("Automated build results for CruiseControl.", feed.getDescription());

        feed.setProjectName("Project1");
        assertEquals("Automated build results for CruiseControl project(s) Project1", feed.getDescription());

        feed.setDescription("new");
        assertEquals("new", feed.getDescription());
    }
}
