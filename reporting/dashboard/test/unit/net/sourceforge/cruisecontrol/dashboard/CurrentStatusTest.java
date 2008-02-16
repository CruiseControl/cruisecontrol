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

public class CurrentStatusTest extends TestCase {
    public void testShouldReturnInactiveAsTheDefaultStatus() {
        CurrentStatus building = CurrentStatus.getProjectBuildStatus(null);
        assertSame(CurrentStatus.DISCONTINUED, building);
    }

    public void testShouldReturnBuildingObjectBaseOnValue() {
        CurrentStatus exptected = CurrentStatus.BUILDING;
        CurrentStatus building = CurrentStatus.getProjectBuildStatus(exptected.getCruiseStatus());
        assertSame(CurrentStatus.BUILDING, building);
        assertEquals("Building", building.getStatus());
    }

    public void testShouldReturnBuildingObjectBaseOnValuePlusTimestamp() {
        String exptected = CurrentStatus.BUILDING.getCruiseStatus() + " since 20070420170000";
        CurrentStatus building = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.BUILDING, building);
        assertEquals("Building", building.getStatus());
    }

    public void testShouldReturnStatusBootStrappingObjectBaseOnValue() {
        String exptected = CurrentStatus.BOOTSTRAPPING.getCruiseStatus();
        CurrentStatus bootstrapping = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.BOOTSTRAPPING, bootstrapping);
        assertEquals("Bootstrapping", bootstrapping.getStatus());
    }

    public void testShouldReturnStatusModificationSetObjectBaseOnValue() {
        String exptected = CurrentStatus.MODIFICATIONSET.getCruiseStatus();
        CurrentStatus modificationset = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.MODIFICATIONSET, modificationset);
        assertEquals("ModificationSet", modificationset.getStatus());
    }

    public void testShouldReturnWaitingObjectBaseOnValue() {
        String exptected = CurrentStatus.WAITING.getCruiseStatus();
        CurrentStatus waiting = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.WAITING, waiting);
        assertEquals("Waiting", waiting.getStatus());
    }
}
