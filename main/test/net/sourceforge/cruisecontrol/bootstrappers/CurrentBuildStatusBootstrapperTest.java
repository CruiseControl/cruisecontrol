/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

public class CurrentBuildStatusBootstrapperTest extends TestCase {

    public CurrentBuildStatusBootstrapperTest(String name) {
        super(name);
    }


    public void testBootstrap() {
        CurrentBuildStatusBootstrapper cbsb = new CurrentBuildStatusBootstrapper();
        try {
            cbsb.bootstrap();
            assertTrue("'file' should be a required attribute on CurrentBuildStatusBootstrapper", false);
        } catch (CruiseControlException cce) {
        }
    }

    public void testWriteFile() {
        CurrentBuildStatusBootstrapper cbsb = new CurrentBuildStatusBootstrapper();
        cbsb.setFile("_testCurrentBuildStatus.txt");
        Date date = new Date();

        try {
            cbsb.writeFile(date);
            SimpleDateFormat formatter = new SimpleDateFormat("MMM/dd/yyyy HH:mm");
            String expected = "<br>&nbsp;<br><b>Current Build Started At:</b><br>" + formatter.format(date);
            assertEquals(expected, readFileToString("_testCurrentBuildStatus.txt"));

            cbsb.setDateFormat("dd/MMM/yyyy");
            cbsb.writeFile(date);
            formatter = new SimpleDateFormat("dd/MMM/yyyy");
            String expected2 = "<br>&nbsp;<br><b>Current Build Started At:</b><br>" + formatter.format(date);
            assertEquals(expected2, readFileToString("_testCurrentBuildStatus.txt"));
        } catch (CruiseControlException cce2) {
            cce2.printStackTrace();
        }
    }

    private String readFileToString(String fileName) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String s = br.readLine();
            StringBuffer sb = new StringBuffer();
            while (s != null) {
                sb.append(s);
                s = br.readLine();
            }
            br.close();
            return sb.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            br = null;
        }
        return "";
    }
}
