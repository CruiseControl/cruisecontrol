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
package net.sourceforge.cruisecontrol;

import java.util.Map;
import java.util.HashMap;


/**
 * [TODO]Handles "registering" plugins that will be used by the CruiseControl
 * configuration file .
 *
 * Also contains the default list of plugins, i.e. those
 * that are already registered like AntBuilder that don't have to be registered
 * seperately in the configuration file.
 */
public final class PluginRegistry {

    /**
     * This is a utility class for now...so private constructor.
     */
    private PluginRegistry() {
    }

    public static Map getDefaultPluginRegistry() {
        Map registry = new HashMap();
        // bootstrappers
        registry.put(
            "currentbuildstatusbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper");
        registry.put(
            "cvsbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper");
        registry.put("p4bootstrapper", "net.sourceforge.cruisecontrol.bootstrappers.P4Bootstrapper");
        registry.put(
            "vssbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper");
        // sourcecontrols
        registry.put("clearcase", "net.sourceforge.cruisecontrol.sourcecontrols.ClearCase");
        registry.put("cvs", "net.sourceforge.cruisecontrol.sourcecontrols.CVS");
        registry.put("filesystem", "net.sourceforge.cruisecontrol.sourcecontrols.FileSystem");
        registry.put("mks", "net.sourceforge.cruisecontrol.sourcecontrols.MKS");
        registry.put("p4", "net.sourceforge.cruisecontrol.sourcecontrols.P4");
        registry.put("pvcs", "net.sourceforge.cruisecontrol.sourcecontrols.PVCS");
        registry.put("starteam", "net.sourceforge.cruisecontrol.sourcecontrols.StarTeam");
        registry.put("vss", "net.sourceforge.cruisecontrol.sourcecontrols.Vss");
        registry.put("vssjournal", "net.sourceforge.cruisecontrol.sourcecontrols.VssJournal");
        // builders
        registry.put("ant", "net.sourceforge.cruisecontrol.builders.AntBuilder");
        registry.put("maven", "net.sourceforge.cruisecontrol.builders.MavenBuilder");
        registry.put("pause", "net.sourceforge.cruisecontrol.PauseBuilder");
        // label incrementer -- only one!
        registry.put(
            "labelincrementer",
            "net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer");
        // publishers
        registry.put(
            "artifactspublisher",
            "net.sourceforge.cruisecontrol.publishers.ArtifactsPublisher");
        registry.put(
            "currentbuildstatuspublisher",
            "net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher");
        registry.put("email", "net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher");
        registry.put("htmlemail", "net.sourceforge.cruisecontrol.publishers.HTMLEmailPublisher");
        registry.put("execute", "net.sourceforge.cruisecontrol.publishers.ExecutePublisher");
        registry.put("scp", "net.sourceforge.cruisecontrol.publishers.SCPPublisher");
        // other
        registry.put("modificationset", "net.sourceforge.cruisecontrol.ModificationSet");
        registry.put("schedule", "net.sourceforge.cruisecontrol.Schedule");

        return registry;
    }
}
