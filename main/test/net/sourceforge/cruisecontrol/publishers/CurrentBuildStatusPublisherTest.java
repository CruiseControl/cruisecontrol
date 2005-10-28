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
import net.sourceforge.cruisecontrol.util.Util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @deprecated Test deprecated with code
 */
public class CurrentBuildStatusPublisherTest extends TestCase {
    private static final String TEST_DIR = "tmp";
    private final List filesToClear = new ArrayList();
    
    public CurrentBuildStatusPublisherTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
        Logger.getLogger(this.getClass()).getLoggerRepository().setThreshold(Level.OFF);
    }

    public void tearDown() {
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.exists()) {
                file.delete();
            }
        }
        filesToClear.clear();
    }

    public void testValidate() throws CruiseControlException {
        CurrentBuildStatusPublisher publisher = new CurrentBuildStatusPublisher();
        try {
            publisher.validate();
            fail("'file' should be a required attribute on CurrentBuildStatusPublisher");
        } catch (CruiseControlException cce) {
        }

        publisher.setFile("somefile");
        publisher.validate();

        publisher.setFile("holycowbatman" + File.separator + "filename");
        publisher.validate();
    }

    public void testWriteFile() throws CruiseControlException, IOException {
        CurrentBuildStatusPublisher publisher = new CurrentBuildStatusPublisher();
        final String fileName = TEST_DIR + File.separator + "_testCurrentBuildStatus.txt";
        publisher.setFile(fileName);
        filesToClear.add(new File(fileName));
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        final String buildTime = formatter.format(new Date(date.getTime() + (300 * 1000)));

        publisher.writeFile(date, 300);
        String expected = "Next Build Starts At:\n" + buildTime;
        assertEquals(expected, Util.readFileToString(TEST_DIR + File.separator + "_testCurrentBuildStatus.txt"));
    }
}
