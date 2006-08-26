/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006 Liberto Enterprises LLC.  All rights reserved.
 * This software is the property of Liberto Enterprises LLC.
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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * Test class for AllFusionHarvestCM511.
 *
 * @author <a href="mailto:lj@libertoenterprises.com">Larry Liberto</a>
 * @see <a href="http://libertoenterprises.com/harvest.html/">libertoenterprises.com/harvest.html</a>
 */
public class AllFusionHarvestCM511Test extends TestCase {
    private AllFusionHarvestCM511 harvest;
    private TimeZone originalTimeZone;

    protected void setUp() throws Exception {
        harvest = new AllFusionHarvestCM511();
        originalTimeZone = TimeZone.getDefault();
    }

    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
        harvest = null;
        originalTimeZone = null;
    }

    /**
     * Method used to test the validation of an AllFusionHarvestCM511 object.
     */
    public void testValidate() throws CruiseControlException, IOException {
        try {
            harvest.validate();
            fail("should throw an exception when no attributes are set");
        } catch (CruiseControlException e) {
            // expected
        }

        harvest.setBrokerName("myBroker");
        harvest.setUsername("myUserId");
        harvest.setPassword("myPassword");
        harvest.setRepositoryName("myPackage");

        try {
            harvest.validate();
        } catch (CruiseControlException e) {
            fail(
                    "should not throw an exception when all the attributes are set");
        }
    }

    /**
     * Method used to test the creation of the command used to retrieve a list of modifications from
     * AllFusion Harvest CM 5.1.1.
     */
    public void testBuildHistoryCommand() throws CruiseControlException {
        String brokerName = "myBroker";
        String userName = "myUserId";
        String passWord = "myPassword";
        String packageName = "myPackage";

        harvest.setBrokerName(brokerName);
        harvest.setUsername(userName);
        harvest.setPassword(passWord);
        harvest.setRepositoryName(packageName);

        try {
            harvest.validate();
        } catch (CruiseControlException e) {
            fail(
                    "should not throw an exception when all the attributes are set");
        }


        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        String[] expectedCmd =
                new String[]{
                        "hsql",
                        "-t",
                        "-b",
                        brokerName,
                        "-usr",
                        userName,
                        "-pw",
                        passWord};
        String[] actualCmd = harvest.buildHistoryCommand(lastBuild, checkTime).getCommandline();
        assertEquals("Number of parameters should be the same.", expectedCmd.length, actualCmd.length);
        for (int r = 0; r < actualCmd.length; r++) {
            assertTrue(expectedCmd[r].trim().equals(actualCmd[r].trim()));
        }
    }

    public void testParseModifications() throws JDOMException, ParseException, IOException {
        File tmpHarvestRsltsFile = File.createTempFile("Harvest", ".rslts");

        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        harvest.parseFile(tmpHarvestRsltsFile, lastBuild, checkTime);

        //TODO: Add some asserts . . .
    }
}
