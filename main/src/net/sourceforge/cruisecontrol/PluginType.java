/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.buildloggers.MergeLogger;
import net.sourceforge.cruisecontrol.builders.CMakeBuilderOptions;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapper;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapping;
import net.sourceforge.cruisecontrol.config.ConfigurationPlugin;
import net.sourceforge.cruisecontrol.config.DashboardConfigurationPlugin;
import net.sourceforge.cruisecontrol.config.IncludeProjectsPlugin;
import net.sourceforge.cruisecontrol.config.PluginPlugin;
import net.sourceforge.cruisecontrol.config.SystemPlugin;
import net.sourceforge.cruisecontrol.config.ThreadsPlugin;
import net.sourceforge.cruisecontrol.config.DefaultPropertiesPlugin;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe, enumeration of CruiseControl plugin types.
 */
public final class PluginType implements Serializable {
    public static final PluginType BOOTSTRAPPER = new PluginType("bootstrapper", "bootstrappers");
    public static final PluginType BOOTSTRAPPERS = new PluginType("bootstrappers", "project");
    public static final PluginType BUILDER = new PluginType("builder", "schedule");
    public static final PluginType CONFIGURATION = new PluginType("configuration", "system");
    public static final PluginType CRUISECONTROL = new PluginType("cruisecontrol", "");
    public static final PluginType DATE_FORMAT = new PluginType("dateformat", "project");
    public static final PluginType INCLUDE_PROJECTS = new PluginType("include.projects", "cruisecontrol");
    public static final PluginType DASHBOARD_CONFIGURATION = new PluginType("dashboard", "cruisecontrol");
    public static final PluginType LABEL_INCREMENTER = new PluginType("labelincrementer", "project");
    public static final PluginType LISTENER = new PluginType("listener", "listeners");
    public static final PluginType LISTENERS = new PluginType("listeners", "project");
    public static final PluginType LOG = new PluginType("log", "project");
    public static final PluginType MAP = new PluginType("map", "email");
    public static final PluginType MERGE_LOGGER = new PluginType("logger", "log");
    public static final PluginType MANIPULATORS = new PluginType("manipulators", "log");
    public static final PluginType MODIFICATION_SET = new PluginType("modificationset", "project");
    public static final PluginType PROJECT = new PluginType("project", "cruisecontrol");
    public static final PluginType PLUGIN = new PluginType("plugin", "cruisecontrol");
    public static final PluginType EMAIL_MAPPER = new PluginType("propertiesmapper", "email");
    public static final PluginType PAUSE = new PluginType("pause", "schedule");
    public static final PluginType PROPERTIES = new PluginType("property", "cruisecontrol");
    public static final PluginType PUBLISHER = new PluginType("publisher", "publishers");
    public static final PluginType PUBLISHERS = new PluginType("publishers", "project");
    public static final PluginType SCHEDULE = new PluginType("schedule", "project");
    public static final PluginType SOURCE_CONTROL = new PluginType("sourcecontrol", "modificationset");
    public static final PluginType SYSTEM = new PluginType("system", "cruisecontrol");
    public static final PluginType THREADS = new PluginType("threads", "configuration");
    public static final PluginType CMAKEOPTIONS = new PluginType("cmakeoptions", "cmake");

    private static final Map<Class< ? >, PluginType> PLUGIN_TYPES = new HashMap<Class< ? >, PluginType>() {
        {
            put(Bootstrapper.class, BOOTSTRAPPER);
            put(ProjectConfig.Bootstrappers.class, BOOTSTRAPPERS);
            put(Builder.class, BUILDER);
            put(ConfigurationPlugin.class, CONFIGURATION);
            put(CruiseControlConfig.class, CRUISECONTROL);
            put(LabelIncrementer.class, LABEL_INCREMENTER);
            put(IncludeProjectsPlugin.class, INCLUDE_PROJECTS);
            put(Listener.class, LISTENER);
            put(ProjectConfig.Listeners.class, LISTENERS);
            put(Log.class, LOG);
            put(EmailMapping.class, MAP);
            put(MergeLogger.class, MERGE_LOGGER);
            put(Manipulator.class, MANIPULATORS);
            put(ModificationSet.class, MODIFICATION_SET);
            put(ProjectConfig.class, PROJECT);
            put(PluginPlugin.class, PLUGIN);
            put(EmailMapper.class, EMAIL_MAPPER);
            put(ProjectConfig.Publishers.class, PUBLISHERS);
            put(PauseBuilder.class, PAUSE);
            put(DefaultPropertiesPlugin.class, PROPERTIES);
            put(Publisher.class, PUBLISHER);
            put(Schedule.class, SCHEDULE);
            put(SourceControl.class, SOURCE_CONTROL);
            put(SystemPlugin.class, SYSTEM);
            put(ThreadsPlugin.class, THREADS);
            put(DashboardConfigurationPlugin.class, DASHBOARD_CONFIGURATION);
            put(CMakeBuilderOptions.class, CMAKEOPTIONS);
        }
    };

    private final String name;
    private final String parentElementName;


    private PluginType(final String type, final String parentElementName) {
        this.name = type;
        this.parentElementName = parentElementName;
    }

    public static PluginType find(final Class< ? > pluginClass) {
        if (pluginClass != null) {
            for (final Map.Entry<Class< ? >, PluginType> element : PLUGIN_TYPES.entrySet()) {
                if (element.getKey().isAssignableFrom(pluginClass)) {
                    return element.getValue();
                }
            }
        }

        throw new IllegalArgumentException(pluginClass + " is not a CruiseControl plugin.");
    }

    public static PluginType[] getTypes() {
        final Set<PluginType> uniqueValues = new HashSet<PluginType>(PLUGIN_TYPES.values());
        return uniqueValues.toArray(new PluginType[uniqueValues.size()]);
    }

    public static PluginType find(String name) {
        if (name != null) {
            for (final Map.Entry<Class< ? >, PluginType> element : PLUGIN_TYPES.entrySet()) {
                PluginType nextType = element.getValue();
                if (nextType.getName().equals(name)) {
                    return nextType;
                }
            }
        }

        throw new IllegalArgumentException(name + " is not a CruiseControl plugin.");
    }

    public String getName() {
        return this.name;
    }

    public String getParentElementName() {
        return parentElementName;
    }

    public String toString() {
        return getName();
    }

    private Object readResolve() {
        return PluginType.find(this.name);
    }
}
