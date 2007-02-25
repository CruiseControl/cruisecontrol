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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.publishers.rss.Feed;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.jdom.Element;

/*
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 */
public class RSSPublisherTest extends TestCase {

    private File tmpFile;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = TestUtil.createElement(success, lastBuildSuccess);

        return new XMLLogHelper(cruisecontrolElement);
    }

    public void setUp() throws Exception {
        tmpFile = File.createTempFile("rsspublisher-test", "tmp");
        tmpFile.deleteOnExit();
        filesToDelete.add(tmpFile);
    }

    public void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testValidate() {
        RSSPublisher publisher = new RSSPublisher();
        try {
            publisher.validate();
            fail("RSSPublisher should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        publisher.setFile(tmpFile.getAbsolutePath());
        publisher.setBuildResultsURL("http://cruisecontrol.sourceforge.net");

        try {
            publisher.validate();
        } catch (CruiseControlException e) {
            fail("RSSPublisher should not throw exceptions when required fields are set.");
        }
    }

    public void testMultipleProjects() throws Exception {
        RSSPublisher publisher = new RSSPublisher();

        publisher.setFile(tmpFile.getAbsolutePath());
        publisher.setBuildResultsURL("http://cruisecontrol.sourceforge.net");

        Feed feed1 = RSSPublisher.getRSSFeed(tmpFile);
        Feed feed2 = RSSPublisher.getRSSFeed(tmpFile);
        assertNotNull(feed1);
        assertNotNull(feed2);
        assertEquals(feed1, feed2);


        File tmpFile2 = File.createTempFile("test", "tmp");
        filesToDelete.add(tmpFile2);
        Feed feed3 = RSSPublisher.getRSSFeed(tmpFile2);
        assertNotNull(feed3);
        assertFalse(feed2 == feed3);
        assertFalse(feed1 == feed3);
    }
}
