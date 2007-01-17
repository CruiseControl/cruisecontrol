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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Understands how to map parameter values to plugin attributes.
 */
public class PluginConfiguration {
    private Map details;
    private String name;
    private PluginType type;

    public PluginConfiguration(PluginDetail pluginDetail, Configuration configuration)
            throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        this.name = pluginDetail.getName();
        this.type = pluginDetail.getType();
        this.details = createDetails(pluginDetail, configuration);
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type.getName();
    }

    public String getParentElementName() {
        return this.type.getParentElementName();
    }

    public Map getDetails() {
        return this.details;
    }

    public void setDetail(String name, String value) {
        Map.Entry detail = getEntryCaseInsensitive(name, details);
        if (detail != null && (StringUtils.isNotBlank((String) detail.getValue()) || StringUtils.isNotBlank(value))) {
            details.remove(detail.getKey());
            details.put(detail.getKey(), value);
        }
    }

    private Map createDetails(PluginDetail pluginDetail, Configuration configuration)
            throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        Map newDetails = new HashMap();
        Element currentConfiguration = getElement(configuration);

        Attribute[] attributes = pluginDetail.getRequiredAttributes();
        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            String key = attribute.getName();
            String realName = key.substring(0, 1).toLowerCase() + key.substring(1);
            newDetails.put(realName, findAttributeValue(currentConfiguration, realName));
        }

        return newDetails;
    }

    private String findAttributeValue(Element configuration, String attributeName) {
        for (Iterator i = configuration.getAttributes().iterator(); i.hasNext();) {
            String nextAttributeName = ((org.jdom.Attribute) i.next()).getName();
            if (attributeName.equalsIgnoreCase(nextAttributeName)) {
                return configuration.getAttributeValue(nextAttributeName);
            }
        }
        return null;
    }

    private static Map.Entry getEntryCaseInsensitive(String key, Map map) {
        for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry nextDetail = (Map.Entry) i.next();
            if (key.equalsIgnoreCase((String) nextDetail.getKey())) {
                return nextDetail;
            }
        }
        return null;
    }

    private Element getElement(Configuration configuration) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, IOException, JDOMException {
        Element currentConfiguration = configuration.getElement(name);
        if (currentConfiguration == null) {
            currentConfiguration = new Element(name);
        }
        return currentConfiguration;
    }
}
