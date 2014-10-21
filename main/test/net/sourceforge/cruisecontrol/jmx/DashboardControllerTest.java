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
package net.sourceforge.cruisecontrol.jmx;

import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.Main;
import net.sourceforge.cruisecontrol.launch.Configuration;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitor;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import junit.framework.TestCase;

public class DashboardControllerTest extends TestCase {
    private DashboardController dashboardController;

    protected void setUp() throws Exception {
        super.setUp();
        BuildLoopMonitorRepository.cancelPosting();
        dashboardController = new DashboardController(new CruiseControlController());
    }

    public void testShouldBeAbleToDisablePosting() throws Exception {
        new Main().startPostingToDashboard(Configuration.getInstance(new String[0]));
        assertNotNull(BuildLoopMonitorRepository.getBuildLoopMonitor());
        dashboardController.stopPostingToDashboard();
        assertNull(BuildLoopMonitorRepository.getBuildLoopMonitor());
    }

    public void testShouldBeAbleToStartPostingIfThereIsNOExistingPosting() throws Exception {
        assertNull(BuildLoopMonitorRepository.getBuildLoopMonitor());
        dashboardController.startPostingToDashboard("http://localhost:1919", 10);
        assertNotNull(BuildLoopMonitorRepository.getBuildLoopMonitor());
    }

    public void testShouldBeAbleToResetPostingIfThereIsExistingPosting() throws Exception {
        new Main().startPostingToDashboard(Configuration.getInstance(new String[0]));
        BuildLoopMonitor existingPosting = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNotNull(existingPosting);

        dashboardController.startPostingToDashboard("http://localhost:1919", 10);
        BuildLoopMonitor newPosting = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNotNull(newPosting);

        assertNotSame(existingPosting, newPosting);
    }
}
