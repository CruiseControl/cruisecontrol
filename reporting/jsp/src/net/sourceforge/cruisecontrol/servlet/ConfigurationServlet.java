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

import java.io.IOException;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.interceptor.ConfigurationAware;

import org.jdom2.JDOMException;

import com.opensymphony.xwork.ActionSupport;
import com.opensymphony.webwork.interceptor.SessionAware;

/**
 * Understands how to edit the configuration via a web interface.
 */
public class ConfigurationServlet extends ActionSupport implements ConfigurationAware, SessionAware {
    private Configuration configuration;
    private String project;

    public String execute() {
        return SUCCESS;
    }

    public String reload() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException, IOException, JDOMException {
        configuration.load();
        addActionMessage("Reloaded configuration.");
        return SUCCESS;
    }

    public String save() throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException, IOException {
        configuration.save();
        addActionMessage("Saved configuration.");
        return SUCCESS;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getContents() throws AttributeNotFoundException, InstanceNotFoundException,
            MalformedObjectNameException, NumberFormatException, MBeanException, ReflectionException, IOException,
            JDOMException {
        return configuration.getConfiguration();
    }

    public void setContents(String contents) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MalformedObjectNameException, NumberFormatException, MBeanException,
            ReflectionException, IOException {
        this.configuration.setConfiguration(contents);
    }

    public void setSession(Map map) {
        project = (String) map.get("project");
    }

    public String getProject() {
        return project;
    }
}
