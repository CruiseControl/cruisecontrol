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

import javax.management.MalformedObjectNameException;

import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.util.ServerNameSingleton;

import com.opensymphony.xwork.Action;
import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.interceptor.AroundInterceptor;

/**
 * Understands how to load the configuration for ConfigurationAware actions.
 */
public class ConfigurationInterceptor extends AroundInterceptor {

    // it appears ActionInvocation.getInvocationContext().getSession() returns Map<String, Object>
    @SuppressWarnings("unchecked")
    protected void before(final ActionInvocation invocation) throws Exception {
        final Action action = invocation.getAction();

        final Map parameters = invocation.getInvocationContext().getParameters();
        if (parameters.get("project") != null) {
            final Map<String, Object> session = invocation.getInvocationContext().getSession();
            session.put("project", ((String[]) parameters.get("project"))[0]);
        }

        final Configuration configuration = getConfiguration(invocation);

        if (action instanceof ConfigurationAware) {
            ((ConfigurationAware) action).setConfiguration(configuration);
        }
    }

    protected void after(final ActionInvocation dispatcher, final String result) throws Exception {
    }

    private Configuration createConfiguration() throws IOException, MalformedObjectNameException {
        final int rmiPort = Integer.parseInt(System.getProperty("cruisecontrol.rmiport"));
        return new Configuration(getJMXServer(), rmiPort);
    }

    // it appears ActionInvocation.getInvocationContext().getSession() returns Map<String, Object>
    @SuppressWarnings("unchecked")
    private Configuration getConfiguration(final ActionInvocation invocation)
            throws IOException, MalformedObjectNameException {

        final Map<String, Object> session = invocation.getInvocationContext().getSession();
        Configuration configuration = (Configuration) session.get("cc-configuration");

        if (configuration == null) {
            configuration = createConfiguration();
            session.put("cc-configuration", configuration);
            invocation.getInvocationContext().setSession(session);
        }

        return configuration;
    }

    private String getJMXServer() {
        return ServerNameSingleton.getServerName();
    }
}
