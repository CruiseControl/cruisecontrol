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
package net.sourceforge.cruisecontrol.servlet;

import java.util.Iterator;
import java.util.Map;

import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.PluginConfiguration;
import net.sourceforge.cruisecontrol.interceptor.ConfigurationAware;
import net.sourceforge.cruisecontrol.interceptor.DetailsAware;

import com.opensymphony.webwork.interceptor.ParameterAware;
import com.opensymphony.xwork.ActionSupport;

/**
 * Understands how to edit plugin details via a web interface.
 */
public class PluginDetailsServlet extends ActionSupport implements
        ConfigurationAware, DetailsAware, ParameterAware {
    private Configuration configuration;
    private Map parameters;
    private PluginConfiguration pluginConfiguration;

    public String execute() throws Exception {
        setDetails();
        configuration.updatePluginConfiguration(pluginConfiguration);
        addActionMessage("Updated configuration.");
        return SUCCESS;
    }

    public String load() {
        return INPUT;
    }

    public String getName() {
        return this.pluginConfiguration.getName();
    }

    public String getType() {
        return this.pluginConfiguration.getType();
    }

    public Map getDetails() {
        return this.pluginConfiguration.getDetails();
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setDetails(PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    private void setDetails() {
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            this.pluginConfiguration.setDetail((String) entry.getKey(),
                    ((String[]) entry.getValue())[0]);
        }
    }

    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }
}
