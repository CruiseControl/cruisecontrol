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
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.jdom2.JDOMException;

import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginDetail;

import com.opensymphony.xwork.Action;
import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.interceptor.AroundInterceptor;

/**
 * Understands how to load available plugins for PluginAware actions.
 */
public class PluginsInterceptor extends AroundInterceptor {
    protected void before(ActionInvocation invocation) throws Exception {
        Action action = invocation.getAction();

        if (action instanceof PluginsAware) {
            ActionContext invocationContext = invocation.getInvocationContext();
            Map parameters = invocationContext.getParameters();

            String pluginType = ((String[]) parameters.get("pluginType"))[0];
            if (pluginType != null) {
                PluginsAware pluginsAction = (PluginsAware) action;
                Configuration configuration = getConfiguration(invocationContext);
                PluginLocator locator = new PluginLocator(configuration);

                pluginsAction.setAvailablePlugins(getAvailablePlugins(locator, pluginType));
                Map session = invocation.getInvocationContext().getSession();
                pluginsAction.setConfiguredPlugins(getConfiguredPlugins(locator, pluginType,
                        (String) session.get("project")));
            }
        }
    }

    protected void after(ActionInvocation dispatcher, String result) throws Exception {
    }

    private PluginDetail[] getAvailablePlugins(PluginLocator locator, String pluginType)
            throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException,
            IOException {
        return locator.getAvailablePlugins(pluginType);
    }

    private PluginDetail[] getConfiguredPlugins(PluginLocator locator, String pluginType, String project)
            throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, CruiseControlException, JDOMException {
        if (project != null) {
            return locator.getConfiguredPlugins(project, pluginType);
        }

        return null;
    }

    private Configuration getConfiguration(ActionContext invocationContext) {
        return (Configuration) invocationContext.getSession().get("cc-configuration");
    }
}
