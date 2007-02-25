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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;

/*
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 */
public class FeedTest extends TestCase {

    private File tempFile;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    public void setUp() throws Exception {
        tempFile = File.createTempFile("FeedTest", "tmp");
        tempFile.deleteOnExit();
        filesToDelete.add(tempFile);

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
        InputStream is =  getClass().getResourceAsStream("RSSFeed.xml");

        int bytesRead;
        byte[] buffer = new byte[1024];
        while ((bytesRead = is.read(buffer)) > 0) {
            bos.write(buffer, 0, bytesRead);
        }
        bos.close();
        is.close();
    }

    public void tearDown() {
        filesToDelete.delete();
    }

    public void testConstructors() {
        Feed feed = new Feed(tempFile);

        assertEquals("CruiseControl Build Results", feed.getTitle());
        assertEquals("http://MyMachine.MyDomain.com/cruisecontrol/", feed.getLink());
        assertEquals(
            "Automated build results for CruiseControl project(s) VERSION_10",
            feed.getDescription());

        //validate the number of items and the contents of the first item.
        assertEquals(11, feed.getItems().size());
        Item item = (Item) feed.getItems().get(0);
        assertEquals("VERSION_10 build.7 Build Successful", item.getTitle());
        assertEquals("http://MyMachine.MyDomain.com/cruisecontrol/buildresults/"
            + "VERSION_10?log=log20050817084109Lbuild.7", item.getLink());
        assertEquals(
            "<em>Build Time:</em> Wed Aug 17 08:41:09 MDT 2005<br/>"
            + "<em>Label:</em> build.7<br/><em>Modifications: </em>1<br/>"
            + "\n<ul><li>//depot/MyProduct/VERSION_10/dev/main/src/datacenter/"
            + "ApplicationServer/PlayTime/default.build"
            + "  by jefferson (deploy the mock object dll)</li></ul>",
            item.getDescription());
    }


    public void testWrite() throws Exception {
        FileWriter fw = null;
        File outFile = null;

        try {
            outFile = File.createTempFile("FeedTest", "tmp");
            filesToDelete.add(outFile);
            fw = new FileWriter(outFile);
            Feed feed = new Feed(tempFile);
            feed.write(fw);
            fw.close();

            // Feed feed2 = new Feed(outFile);
            assertEquals("CruiseControl Build Results", feed.getTitle());
            assertEquals("http://MyMachine.MyDomain.com/cruisecontrol/", feed.getLink());
            assertEquals(
                "Automated build results for CruiseControl project(s) VERSION_10",
                feed.getDescription());

            //validate the number of items and the contents of the first item.
            assertEquals(11, feed.getItems().size());
            Item item = (Item) feed.getItems().get(0);
            assertEquals("VERSION_10 build.7 Build Successful", item.getTitle());
            assertEquals(
                "http://MyMachine.MyDomain.com/cruisecontrol/buildresults/"
                + "VERSION_10?log=log20050817084109Lbuild.7",
                item.getLink());
            assertEquals(
            "<em>Build Time:</em> Wed Aug 17 08:41:09 MDT 2005<br/>"
            + "<em>Label:</em> build.7<br/><em>Modifications: </em>1<br/>"
            + "\n<ul><li>//depot/MyProduct/VERSION_10/dev/main/src/datacenter/"
            + "ApplicationServer/PlayTime/default.build"
            + "  by jefferson (deploy the mock object dll)</li></ul>",
                item.getDescription());
        } finally {
            IO.close(fw);
        }
    }
}