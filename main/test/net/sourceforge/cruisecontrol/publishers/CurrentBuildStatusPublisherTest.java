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
package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

public class CurrentBuildStatusPublisherTest extends TestCase {
    private static final String TEST_DIR = "tmp";

    public void testValidate() {
        CurrentBuildStatusPublisher publisher = new CurrentBuildStatusPublisher();
        try {
            publisher.validate();
            fail("'file' should be a required attribute on CurrentBuildStatusPublisher");
        } catch (CruiseControlException cce) {
        }
    }

    public void testWriteFile() throws CruiseControlException, IOException {
        CurrentBuildStatusPublisher publisher = new CurrentBuildStatusPublisher();
        publisher.setFile(TEST_DIR + File.separator + "_testCurrentBuildStatus.txt");
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        final String buildTime = formatter.format(new Date(date.getTime() + (300 * 1000)));

        publisher.writeFile(date, 300);
        String expected = "<span class=\"link\">Next Build Starts At:<br>" + buildTime + "</span>";
        assertEquals(expected, readFileToString(TEST_DIR + File.separator + "_testCurrentBuildStatus.txt"));
    }

    private String readFileToString(String fileName) throws IOException {
        StringBuffer contents = new StringBuffer();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                contents.append(line);
                line = br.readLine();
            }
            return contents.toString();
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}
