/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.gendoc;

import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.config.PluginPlugin;

/**
 * Provides utility methods for testing GenDoc features.
 * @author pollens@msoe.edu
 *         Date: 9/11/10
 */
public class GendocTestUtils {
    
    /**
     * Parses a plugin.
     * @param name Name of the plugin to use. This can be anything.
     * @param clazz Class of the plugin to be loaded with the given name.
     * @return The loaded PluginInfo, or null if an error occurred.
     */
    public static PluginInfo loadPluginInfo(String name, Class<?> clazz){
        final PluginRegistry registry = PluginRegistry.createRegistry();
        final PluginPlugin config = createPlugin(name, clazz.getName());

        try {
            registry.register(config);
        } catch (CruiseControlException e) {
            return null;
        }
        
        return new PluginInfoParser(registry, name).getRootPlugin();
    }

    /**
     * Safe way of creating {@link PluginPlugin} class
     *
     * @param name value get by {@link PluginPlugin#getName()}
     * @param clazz value get by {@link PluginPlugin#getClass()}
     * @return properly configured plugin class
     */
    public static PluginPlugin createPlugin(String name, String clazz) {
        final Element elem = new Element("plugin");
        final PluginPlugin plugin = new PluginPlugin();

        // Create "xml element"
        elem.setAttribute("name", name);
        elem.setAttribute("classname", clazz);
        // Configure the plugin. In this way it avoids NullPointerException when
        // PluginPlugin#getTransformedElement() is called
        plugin.configure(elem);
        return plugin;
    }
}

