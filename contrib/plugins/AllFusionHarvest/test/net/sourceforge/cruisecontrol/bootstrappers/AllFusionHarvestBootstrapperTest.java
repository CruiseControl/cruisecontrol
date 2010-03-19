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
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.CruiseControlException;

import junit.framework.TestCase;

public class AllFusionHarvestBootstrapperTest
    extends TestCase {
                
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidate() {
        
        // Nothing set
        AllFusionHarvestBootstrapper harvest = new AllFusionHarvestBootstrapper();
        try {
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Username set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setUsername("username");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Password set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setPassword("password");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Broker set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setBroker("broker");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only State set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setState("state");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Project set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setProject("project");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Process set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setProcess("process");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Clientpath set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setClientpath("clientpath");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only Viewpath set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setViewpath("viewpath");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        // Only File set
        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setFile("file");
            harvest.validate();
            fail("AllFusionHarvest should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException expected) {
        }

        try {
            harvest = new AllFusionHarvestBootstrapper();
            harvest.setUsername("username");
            harvest.setPassword("password");
            harvest.setBroker("broker");
            harvest.setState("state");
            harvest.setProject("project");
            harvest.setProcess("process");
            harvest.setClientpath("clientpath");
            harvest.setViewpath("viewpath");
            harvest.setFile("file");
            harvest.validate();
        } catch (CruiseControlException e) {
            fail("AllFusionHarvest should not throw exceptions when required attributes are set." + e.getMessage());
        }
    }

    public void testOptionalAttributes() {
        try {
            AllFusionHarvestBootstrapper harvest = new AllFusionHarvestBootstrapper();
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
        } catch (CruiseControlException e) {
            fail("AllFusionHarvest should not throw exceptions when optional attributes are set.");
        }
    }
}
