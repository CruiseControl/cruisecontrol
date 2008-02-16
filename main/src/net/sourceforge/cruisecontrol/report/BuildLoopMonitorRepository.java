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
package net.sourceforge.cruisecontrol.report;

import java.util.Timer;

import net.sourceforge.cruisecontrol.BuildLoopInformationBuilder;
import net.sourceforge.cruisecontrol.CruiseControlController;

public final class BuildLoopMonitorRepository {
    private static BuildLoopMonitor buildLoopMonitor;

    private static BuildLoopPostingConfiguration config;

    private BuildLoopMonitorRepository() {
    }

    public static BuildLoopMonitor getBuildLoopMonitor() {
        return buildLoopMonitor;
    }

    public static BuildLoopPostingConfiguration getBuildLoopMonitorConfig() {
        return config;
    }

    public static void cancelExistingAndStartNewPosting(CruiseControlController controller,
            BuildLoopPostingConfiguration config) {
        cancelPosting();
        createAndStartBuildLoopMonitor(controller, config);
    }

    public static void cancelPosting() {
        if (buildLoopMonitor != null) {
            buildLoopMonitor.cancel();
            buildLoopMonitor = null;
            config = null;
        }
    }

    private static void createAndStartBuildLoopMonitor(CruiseControlController controller,
            BuildLoopPostingConfiguration config) {
        BuildLoopInformationBuilder builder = new BuildLoopInformationBuilder(controller);
        BuildLoopStatusReportTask task =
                new BuildLoopStatusReportTask(builder, config.getUrl() + "/buildloop/listener");
        buildLoopMonitor = new BuildLoopMonitor(new Timer(), task, config.getInterval() * 1000);
        buildLoopMonitor.start();
        BuildLoopMonitorRepository.config = config;
    }
}
