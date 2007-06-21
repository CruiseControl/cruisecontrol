/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResource;

public class TemplateRenderService implements ServletContextAware, InitializingBean {

    private String resourceLoaderPath = "WEB-INF/templates";
    private ServletContext servletContext;
    private Map templates = new HashMap();

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void afterPropertiesSet() {
        try {
            loadTemplates();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template files", e);
        }
    }

    public void loadTemplates() throws IOException {
        loadTemplate("project_xml.template");
        loadTemplate("directory.template");
        loadTemplate("file.template");
    }

    private void loadTemplate(String name) throws IOException {
        Resource resource;
        if (servletContext != null) {
            resource = new ServletContextResource(servletContext, resourceLoaderPath + "/" + name);
        } else {
            resource = new FileSystemResource("webapp/" + resourceLoaderPath + "/" + name);
        }
        if (!resource.exists()) {
            throw new RuntimeException("Failed to load template from [" + resource.getFile().getAbsolutePath() + "].");
        }
        templates.put(name, IOUtils.toString(resource.getInputStream()));
    }

    public String renderTemplate(String templateName, Map values) {
        String template = (String) templates.get(templateName);

        for (Iterator iterator = values.keySet().iterator(); iterator.hasNext();) {
            Object key = iterator.next();
            Object value = values.get(key);
            template = StringUtils.replace(template, key.toString(), value.toString());
        }
        return StringUtils.defaultString(template);
    }

}
