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
 * Handles "registering" plugins that will be used by the CruiseControl
 * configuration file .
 *
 * Also contains the default list of plugins, i.e. those
 * that are already registered like AntBuilder that don't have to be registered
 * seperately in the configuration file.
 */
public final class PluginRegistry {

    /**
     * Map of plugins where the key is the plugin name (e.g. ant) and the value is
     * the fully qualified classname
     * (e.g. net.sourceforge.cruisecontrol.builders.AntBuilder).
     */
    private final Map plugins;

    /**
     * Creates a new PluginRegistry with no plugins registered. Use
     * <code>PluginRegistry.getDefaultPluginRegistry<code> for a PluginRegistry
     * instance containing all the default plugins.
     */
    public PluginRegistry() {
        plugins = new HashMap();
    }

    /**
     * @param pluginName The name for the plugin, e.g. ant. Note that plugin
     * names are always treated as case insensitive, so Ant, ant, and AnT are
     * all treated as the same plugin.
     *
     * @param pluginClassname The fully qualified classname for the
     * plugin class, e.g. net.sourceforge.cruisecontrol.builders.AntBuilder.
     */
    public void register(String pluginName, String pluginClassname) {

        plugins.put(pluginName.toLowerCase(), pluginClassname);
    }

    /**
     * @return Returns null if no plugin has been registered with the specified
     * name, otherwise a String representing the fully qualified classname
     * for the plugin class. Note that plugin
     * names are always treated as case insensitive, so Ant, ant, and AnT are
     * all treated as the same plugin.
     */
    public String getPluginClassname(String pluginName) {
        if (!isPluginRegistered(pluginName)) {
            return null;
        }

        return (String) plugins.get(pluginName.toLowerCase());
    }

    /**
     * @return Returns null if no plugin has been registered with the specified
     * name, otherwise the Class representing the the plugin class. Note that
     * plugin names are always treated as case insensitive, so Ant, ant,
     * and AnT are all treated as the same plugin.
     *
     * @throws CruiseControlException If the class provided cannot be loaded.
     */
    public Class getPluginClass(String pluginName)
            throws CruiseControlException {
        if (!isPluginRegistered(pluginName)) {
            return null;
        }

        String pluginClassname = getPluginClassname(pluginName);

        Class pluginClass = null;
        try {
            pluginClass = Class.forName(pluginClassname);
        } catch (ClassNotFoundException e) {
            throw new CruiseControlException(
                    "Attemping to load plugin named [" + pluginName
                    + "], but couldn't load corresponding class ["
                    + pluginClassname + "].");
        }
        return pluginClass;
    }

    /**
     * @return True if this registry contains an entry for the plugin
     * specified by the name. The name is the short name for the plugin, not
     * the classname, e.g. ant. Note that plugin
     * names are always treated as case insensitive, so Ant, ant, and AnT are
     * all treated as the same plugin.
     *
     * @throws NullPointerException If a null pluginName is passed, then
     * a NullPointerException will occur. It's recommended to not pass a
     * null pluginName.
     */
    public boolean isPluginRegistered(String pluginName) {
        return plugins.containsKey(pluginName.toLowerCase());
    }

    /**
     * Returns a new Map instance containing all the default plugins, where
     * the where the key is the plugin name (e.g. ant) and the value is
     * the fully qualified classname
     * (e.g. net.sourceforge.cruisecontrol.builders.AntBuilder).
     */
    public static PluginRegistry getDefaultPluginRegistry() {
        PluginRegistry registry = new PluginRegistry();
        // bootstrappers
        registry.register(
            "currentbuildstatusbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper");
        registry.register(
            "cvsbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper");
        registry.register("p4bootstrapper", "net.sourceforge.cruisecontrol.bootstrappers.P4Bootstrapper");
        registry.register(
            "vssbootstrapper",
            "net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper");
        // sourcecontrols
        registry.register("clearcase", "net.sourceforge.cruisecontrol.sourcecontrols.ClearCase");
        registry.register("cvs", "net.sourceforge.cruisecontrol.sourcecontrols.CVS");
        registry.register("filesystem", "net.sourceforge.cruisecontrol.sourcecontrols.FileSystem");
        registry.register("mks", "net.sourceforge.cruisecontrol.sourcecontrols.MKS");
        registry.register("p4", "net.sourceforge.cruisecontrol.sourcecontrols.P4");
        registry.register("pvcs", "net.sourceforge.cruisecontrol.sourcecontrols.PVCS");
        registry.register("starteam", "net.sourceforge.cruisecontrol.sourcecontrols.StarTeam");
        registry.register("vss", "net.sourceforge.cruisecontrol.sourcecontrols.Vss");
        registry.register("vssjournal", "net.sourceforge.cruisecontrol.sourcecontrols.VssJournal");
        // builders
        registry.register("ant", "net.sourceforge.cruisecontrol.builders.AntBuilder");
        registry.register("maven", "net.sourceforge.cruisecontrol.builders.MavenBuilder");
        registry.register("pause", "net.sourceforge.cruisecontrol.PauseBuilder");
        // label incrementer -- only one!
        registry.register(
            "labelincrementer",
            "net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer");
        // publishers
        registry.register(
            "artifactspublisher",
            "net.sourceforge.cruisecontrol.publishers.ArtifactsPublisher");
        registry.register(
            "currentbuildstatuspublisher",
            "net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher");
        registry.register("email", "net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher");
        registry.register("htmlemail", "net.sourceforge.cruisecontrol.publishers.HTMLEmailPublisher");
        registry.register("execute", "net.sourceforge.cruisecontrol.publishers.ExecutePublisher");
        registry.register("scp", "net.sourceforge.cruisecontrol.publishers.SCPPublisher");
        // other
        registry.register("modificationset", "net.sourceforge.cruisecontrol.ModificationSet");
        registry.register("schedule", "net.sourceforge.cruisecontrol.Schedule");
        registry.register("log", "net.sourceforge.cruisecontrol.Log");
        registry.register("merge", "net.sourceforge.cruisecontrol.buildloggers.MergeLogger");

        return registry;
    }
}
