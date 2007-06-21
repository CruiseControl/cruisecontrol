/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.joda.time.DateTime;

public class ProjectBuildStatusTest extends TestCase {
    public void testShouldReturnInactiveAsTheDefaultStatus() {
        ProjectBuildStatus building = ProjectBuildStatus.getProjectBuildStatus(null);
        assertTrue(building instanceof StatusInactive);
    }

    public void testShouldReturnBuildingObjectBaseOnValue() {
        String exptected = new StatusBuilding().toString();
        ProjectBuildStatus building = ProjectBuildStatus.getProjectBuildStatus(exptected);
        assertTrue(building instanceof StatusBuilding);
        assertEquals("Building", building.getStatus());
    }

    public void testShouldReturnBuildingObjectBaseOnValuePlusTimestamp() {
        String exptected = new StatusBuilding().toString() + " since 20070420170000";
        ProjectBuildStatus building = ProjectBuildStatus.getProjectBuildStatus(exptected);
        assertTrue(building instanceof StatusBuilding);
        assertEquals("Building", building.getStatus());
    }

    public void testShouldReturnStatusBootStrappingObjectBaseOnValue() {
        String exptected = new StatusBootStrapping().toString();
        ProjectBuildStatus bootstrapping = ProjectBuildStatus.getProjectBuildStatus(exptected);
        assertTrue(bootstrapping instanceof StatusBootStrapping);
        assertEquals("Bootstrapping", bootstrapping.getStatus());
    }

    public void testShouldReturnStatusModificationSetObjectBaseOnValue() {
        String exptected = new StatusModificationSet().toString();
        ProjectBuildStatus modificationset = ProjectBuildStatus.getProjectBuildStatus(exptected);
        assertTrue(modificationset instanceof StatusModificationSet);
        assertEquals("ModificationSet", modificationset.getStatus());
    }

    public void testShouldReturnWaitingObjectBaseOnValue() {
        String exptected = new StatusWaiting().toString();
        ProjectBuildStatus waiting = ProjectBuildStatus.getProjectBuildStatus(exptected);
        assertTrue(waiting instanceof StatusWaiting);
        assertEquals("Waiting", waiting.getStatus());
    }

    public void testShouldExtractTimestampFromStatusIfPresent() throws Exception {
        String statusString = new StatusBuilding().toString() + " since 20070420170000";
        DateTime date = ProjectBuildStatus.getTimestamp(statusString);
        assertEquals(CCDateFormatter.format("2007-04-20 17:00:00", "yyyy-MM-dd HH:mm:ss"), date);
    }
}
