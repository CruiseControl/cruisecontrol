/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import com.ca.harvest.jhsdk.hutils.JCaAttrKey;
import com.ca.harvest.jhsdk.hutils.JCaContainer;
import com.ca.harvest.jhsdk.hutils.JCaTimeStamp;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class AllFusionHarvestTest
    extends TestCase {

    private static Calendar gc = GregorianCalendar.getInstance();
                
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidate() {
        
        // Nothing set
        AllFusionHarvest harvest = new AllFusionHarvest();
        try {
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Username set
        try {
            harvest = new AllFusionHarvest();
            harvest.setUsername("username");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Password set
        try {
            harvest = new AllFusionHarvest();
            harvest.setPassword("password");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Broker set
        try {
            harvest = new AllFusionHarvest();
            harvest.setBroker("broker");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only State set
        try {
            harvest = new AllFusionHarvest();
            harvest.setState("state");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Project set
        try {
            harvest = new AllFusionHarvest();
            harvest.setProject("project");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        try {
            harvest = new AllFusionHarvest();
            harvest.setUsername("username");
            harvest.setPassword("password");
            harvest.setBroker("broker");
            harvest.setState("state");
            harvest.setProject("project");
            harvest.validate();
        } catch (CruiseControlException e) {
            fail("AllFusionHarvest should not throw exceptions when required attributes are set.");
        }
    }
    
    
    public void testOptionalAttributes() {
        try {
            AllFusionHarvest harvest = new AllFusionHarvest();
            harvest.setItem("baseline");
            harvest.setItem("not_modified");
            harvest.setItem("modified");
            harvest.setItem("both");
            harvest.setVersion("latest_in_view");
            harvest.setVersion("all_in_view");
            harvest.setVersion("all");
            harvest.setVersion("latest");
            harvest.setStatus("all");
            harvest.setStatus("all_tags");
            harvest.setStatus("no_tag");
            harvest.setStatus("normal");
            harvest.setStatus("reserved");
            harvest.setStatus("merged");
            harvest.setStatus("removed");
            harvest.setStatus("deleted");
            harvest.setStatus("any");
            harvest.setStatus("any_tag");
            harvest.setBranch("trunk");
            harvest.setBranch("trunk_only");
            harvest.setBranch("branch");
            harvest.setBranch("branch_only");
            harvest.setBranch("trunk_and_branch");
            harvest.setBranch("unmerged");
            harvest.setBranch("unmerged_branch");
            harvest.setMode("version");
            harvest.setMode("package");
            harvest.setPrevState("Development");
        } catch (CruiseControlException e) {
            fail("AllFusionHarvest should not throw exceptions when optional attributes are set.");
        }
    }

    public void testGetVersionsInRange() {
        
        // Setup some data to use in testing
        String[][] data = {
            { "0", "foo.java", "/test", "added", "N", "testuser", "testuser@foobar.com", "Added version" },
            { "1", "bar.java", "/test", "modified", "N", "testuser", "testuser@foobar.com", "Modified version" },
            { "2", "bat.java", "/test", "reserved", "R", "testuser", "testuser@foobar.com", "Reserved version" }
        };
        
        int[][] dates = {
            { 2007, Calendar.JANUARY, 7, 12, 34, 56, 0 },
            { 2007, Calendar.JANUARY, 7, 12, 33, 44, 0 },
            { 2007, Calendar.JANUARY, 7, 12, 44, 55, 0 }
        };
        
        ArrayList reference = new ArrayList();
        JCaContainer versionList = new JCaContainer();
        
        // Test to see whether the JHSDK is present - only if using wrapper - RHT 11/05/2008
        /*
         * if (versionList.getRealObject() == null) {
         *    return;
         * }
         */
        
        // Copy the test data into some reference objects and a data source
        for (int d = 0; d < data.length; d++) {
            
            // Only put non-reserved tagged versions into the expected results
            if (!data[d][4].equals("R")) {
                Modification ref = new Modification("harvest");
                ref.revision = data[d][0];
                Modification.ModifiedFile modfile = ref.createModifiedFile(data[d][1], data[d][2]);
                modfile.action = data[d][3];
                modfile.revision = ref.revision;
                gc.set(dates[d][0], dates[d][1], dates[d][2], dates[d][3], dates[d][4], dates[d][5]);
                gc.set(Calendar.MILLISECOND, dates[d][6]);
                ref.modifiedTime = gc.getTime();
                ref.userName = data[d][5];
                ref.emailAddress = data[d][6];
                ref.comment = data[d][7];
                reference.add(ref);
            }
        
            versionList.setString(JCaAttrKey.CA_ATTRKEY_MAPPED_VERSION_NAME, data[d][0], d);
            versionList.setString(JCaAttrKey.CA_ATTRKEY_NAME, data[d][1], d);
            versionList.setString(JCaAttrKey.CA_ATTRKEY_FULL_PATH_NAME, data[d][2], d);
            versionList.setTimeStamp(JCaAttrKey.CA_ATTRKEY_MODIFIED_TIME,
                new JCaTimeStamp(dates[d][0], dates[d][1] + 1, dates[d][2],
                        dates[d][3], dates[d][4], dates[d][5], dates[d][6]), d);
            versionList.setString(JCaAttrKey.CA_ATTRKEY_VERSION_STATUS, data[d][4], d);
            versionList.setString(JCaAttrKey.CA_ATTRKEY_MODIFIER_NAME, data[d][5], d);
            versionList.setString(JCaAttrKey.CA_ATTRKEY_DESCRIPTION, data[d][7], d);
        }
        
        // Now setup the sourcecontrol and test - this code is based on getVersionsInRange()
        AllFusionHarvest test = new AllFusionHarvest();
        test.setEmailAddress(data[0][5], data[0][6]);

        ArrayList list = new ArrayList();

        // This test is critical, as sometimes the count throws an exception
        int numVers = versionList.isEmpty() ? 0
                : versionList.getKeyElementCount(JCaAttrKey.CA_ATTRKEY_NAME);
        
        for (int n = 0; n < numVers; n++) {
            String status = versionList.getString(JCaAttrKey.CA_ATTRKEY_VERSION_STATUS, n);
            
            // Don't add reserved tagged files - the file hasn't actually changed
            if (!status.equals("R")) {
                list.add(test.transformJCaVersionContainerToModification(versionList, n, true));
            }
        }     
        
        // Check the results
        if (list.size() != reference.size()) {
            fail("AllFusionHarvest should return " + reference.size() + " modification(s)");
        }
        
        Object[] refArray = reference.toArray();
        Object[] retArray = list.toArray();
        
        for (int m = 0; m < retArray.length; m++) {
            Modification mod = (Modification) retArray[m];
            Modification ref = (Modification) refArray[m];

            if (!ref.equals(mod)) {
                fail("AllFusionHarvest does not return expected result for modification #" + m);
            }

            Object[] refFiles = ref.files.toArray();
            Object[] modFiles = mod.files.toArray();
            
            if (refFiles.length != modFiles.length) {
                fail("AllFusionHarvest should return " + refFiles.length + " files for modification #" + m);
            }
            
            for (int f = 0; f < refFiles.length; f++) {
                if (!refFiles[f].equals(modFiles[f])) {
                    fail("AllFusionHarvest does not return expected result for file #"
                         + f + " in modification #" + m);
                }
            }
        }
    }
}
