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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

/**
 * Understands information common to all plugins.
 */
public class GenericPluginDetail implements PluginDetail {
    private final String name;
    private final PluginType type;
    private final Attribute[] requiredAttributes;

    public GenericPluginDetail(String name, Class plugin) {
        this.name = name;
        this.requiredAttributes = lookupRequiredAttributes(plugin);
        this.type = PluginType.find(plugin);
    }

    public String getName() {
        return name;
    }

    public PluginType getType() {
        return type;
    }

    public Attribute[] getRequiredAttributes() {
        return requiredAttributes;
    }

    public int compareTo(Object other) {
        return this.getName().compareTo(((PluginDetail) other).getName());
    }

    public String toString() {
        return type + ":" + name;
    }
    
    private static Attribute[] lookupRequiredAttributes(Class plugin) {
        List attrs = new LinkedList();
        Method[] methods = plugin.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (isRequiredAttribute(method)) {
                String methodName = method.getName();
                String attributeName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                attrs.add(new Attribute(attributeName, method.getParameterTypes()[0]));
            }
        }

        return (Attribute[]) attrs.toArray(new Attribute[attrs.size()]);
    }

    private static boolean isRequiredAttribute(Method method) {
        return method.getName().startsWith("set") && Modifier.isPublic(method.getModifiers());
    }
}
