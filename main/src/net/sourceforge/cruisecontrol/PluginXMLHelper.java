/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PluginXMLHelper {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(PluginXMLHelper.class);

    /**
     *  given a JDOM Element and a classname, this method will instantiate an object of type className, and
     *  call all setters that correspond to attributes on the Element.
     *
     *  @return fully configured Object
     */
    public Object configure(Element objectElement, String className) throws CruiseControlException {
        Class pluginClass = null;
        Object pluginInstance = null;
        try {
            pluginClass = Class.forName(className);
            pluginInstance = pluginClass.getConstructor(null).newInstance(null);
        } catch (ClassNotFoundException e) {
            log.fatal("Could not find class", e);
            throw new CruiseControlException("Could not find class: " + className);
        } catch (Exception e) {
            log.fatal("Could not instantiate class", e);
            throw new CruiseControlException("Could not instantiate class: " + className);
        }
        configureObject(objectElement, pluginInstance);

        return pluginInstance;
    }

    /**
     * given a JDOM Element and an object, this method will call all setters that correspond to attributes 
     * on the Element.
     */
    protected void configureObject(Element objectElement, Object object) throws CruiseControlException {
        Map setters = new HashMap();
        Map creators = new HashMap();

        Method[] methods = object.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().startsWith("set")) {
                setters.put(methods[i].getName().substring(3).toLowerCase(), methods[i]);
            } else if (methods[i].getName().startsWith("create")) {
                creators.put(methods[i].getName().substring(6).toLowerCase(), methods[i]);
            }
        }

        Iterator attributeIterator = objectElement.getAttributes().iterator();
        while (attributeIterator.hasNext()) {
            Attribute attribute = (Attribute) attributeIterator.next();
            if (setters.containsKey(attribute.getName().toLowerCase())) {
                try {
                    Method method = (Method) setters.get(attribute.getName().toLowerCase());
                    Class[] parameters = method.getParameterTypes();
                    if (String.class.isAssignableFrom(parameters[0])) {
                        method.invoke(object, new Object[]{attribute.getValue()});
                    } else if (int.class.isAssignableFrom(parameters[0])) {
                        method.invoke(object, new Object[]{new Integer(attribute.getIntValue())});
                    }
                } catch (Exception e) {
                    log.fatal("Error configuring plugin.", e);
                }
            } else {
                throw new CruiseControlException("Attribute: '" + attribute.getName() + "' is not supported for class: '" + object.getClass().getName() + "'.");
            }
        }

        Iterator childElementIterator = objectElement.getChildren().iterator();
        while (childElementIterator.hasNext()) {
            Element childElement = (Element) childElementIterator.next();
            if (creators.containsKey(childElement.getName().toLowerCase())) {
                try {
                    Method method = (Method) creators.get(childElement.getName().toLowerCase());
                    Object childObject = method.invoke(object, null);
                    configureObject(childElement, childObject);
                } catch (Exception e) {
                    throw new CruiseControlException(e.getMessage());
                }
            } else {
                throw new CruiseControlException("Nested element: '" + childElement.getName() + "' is not supported for the <" + objectElement.getName() + "> tag.");
            }
        }
    }
}
