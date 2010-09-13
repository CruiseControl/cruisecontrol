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
package net.sourceforge.cruisecontrol.interceptor;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginDetail;
import net.sourceforge.cruisecontrol.PluginType;

import org.jdom.JDOMException;

/**
 * Understands how to find plugins.
 */
public class PluginLocator {
    private final Configuration configuration;

    public PluginLocator(final Configuration configuration) {
        this.configuration = configuration;
    }

    public PluginDetail[] getAvailablePlugins(final String type) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return getAvailablePlugins(PluginType.find(type));
    }

    public PluginDetail[] getAvailablePlugins(final PluginType type) throws ReflectionException, IOException,
            InstanceNotFoundException, MBeanException, AttributeNotFoundException {

        final PluginDetail[] availablePlugins = configuration.getPluginDetails();
        final Collection<PluginDetail> desiredPlugins = new LinkedList<PluginDetail>();
        for (final PluginDetail nextPlugin : availablePlugins) {
            if (nextPlugin.getType() == type) {
                desiredPlugins.add(nextPlugin);
            }
        }

        return desiredPlugins.toArray(new PluginDetail[desiredPlugins.size()]);
    }

    public PluginDetail[] getConfiguredPlugins(final String project, final String type)
            throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, CruiseControlException,
            JDOMException {

        if ("listener".equals(type)) {
            return configuration.getConfiguredListeners(project);
        } else if ("bootstrapper".equals(type)) {
            return configuration.getConfiguredBootstrappers(project);
        } else if ("sourcecontrol".equals(type)) {
            return configuration.getConfiguredSourceControls(project);
        } else if ("builder".equals(type)) {
            return configuration.getConfiguredBuilders(project);
        } else if ("logger".equals(type)) {
            return configuration.getConfiguredLoggers(project);
        } else if ("publisher".equals(type)) {
            return configuration.getConfiguredPublishers(project);
        } else {
            return null;
        }
    }

    public PluginDetail getPluginDetail(final String name, final String type) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException {

        final PluginDetail[] plugins = getAvailablePlugins(type);
        for (PluginDetail plugin : plugins) {
            if (plugin.getName().equals(name)) {
                return plugin;
            }
        }

        return null;
    }
}
