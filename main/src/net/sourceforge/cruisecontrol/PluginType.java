/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.cruisecontrol.buildloggers.MergeLogger;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapping;
import net.sourceforge.cruisecontrol.publishers.email.PropertiesMapper;

/**
 * Type-safe, enumeration of CruiseControl plugin types. 
 */
public final class PluginType implements Serializable {
    public static final PluginType BOOTSTRAPPER = new PluginType("bootstrappers");
    public static final PluginType BUILDER = new PluginType("builders");
    public static final PluginType DATE_FORMAT = new PluginType("dateformat");
    public static final PluginType LABEL_INCREMENTER = new PluginType("labelincrementer");
    public static final PluginType LISTENER = new PluginType("listeners");
    public static final PluginType LOG = new PluginType("log");
    public static final PluginType MAP = new PluginType("map");
    public static final PluginType MERGE_LOGGER = new PluginType("mergelogger");
    public static final PluginType MODIFICATION_SET = new PluginType("modificationset");
    public static final PluginType PROJECT = new PluginType("project");
    public static final PluginType PROPERTIES_MAPPER = new PluginType("propertiesmapper");
    public static final PluginType PUBLISHER = new PluginType("publishers");
    public static final PluginType SCHEDULE = new PluginType("schedule");
    public static final PluginType SOURCE_CONTROL = new PluginType("modificationset");

    private static final Map PLUGIN_TYPES = new HashMap() {
        {
            put(Bootstrapper.class, BOOTSTRAPPER);
            put(ProjectConfig.Bootstrappers.class, BOOTSTRAPPER);
            put(Builder.class, BUILDER);
            put(CCDateFormat.class, DATE_FORMAT);
            put(LabelIncrementer.class, LABEL_INCREMENTER);
            put(Listener.class, LISTENER);
            put(ProjectConfig.Listeners.class, LISTENER);
            put(Log.class, LOG);
            put(EmailMapping.class, MAP);
            put(MergeLogger.class, MERGE_LOGGER);
            put(ModificationSet.class, MODIFICATION_SET);
            put(ProjectConfig.class, PROJECT);
            put(PropertiesMapper.class, PROPERTIES_MAPPER);
            put(ProjectConfig.Publishers.class, PUBLISHER);
            put(Publisher.class, PUBLISHER);
            put(Schedule.class, SCHEDULE);
            put(SourceControl.class, SOURCE_CONTROL);
        }
    };

    private String name;
    
    private PluginType(String type) {
        this.name = type;
    }

    public static PluginType find(Class pluginClass) {
        if (pluginClass != null) {
            for (Iterator i = PLUGIN_TYPES.entrySet().iterator(); i.hasNext();) {
                Map.Entry element = (Map.Entry) i.next();
                if (((Class) element.getKey()).isAssignableFrom(pluginClass)) {
                    return (PluginType) element.getValue();
                }
            }
        }

        throw new IllegalArgumentException(pluginClass + " is not a CruiseControl plugin.");
    }

    public String getName() {
        return this.name;
    }
    
    public String toString() {
        return getName();
    }
}
