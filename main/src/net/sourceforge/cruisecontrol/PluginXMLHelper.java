/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Helps mapping the XML to object by instantiating and initializing beans.
 *
 * Some plugins can be {@link SelfConfiguringPlugin self-configuring}. For the others, the
 * the {@link #configureObject(org.jdom.Element, Object, boolean)} defines the operations
 * to be performed.   
 */
public class PluginXMLHelper {
    private static final Logger LOG = Logger.getLogger(PluginXMLHelper.class);
    private ProjectHelper projectHelper;

    public PluginXMLHelper(ProjectHelper plugins) {
        projectHelper = plugins;
    }

    /**
     * Given a JDOM Element and a class, this method will instantiate an object
     * of type pluginClass, and {@link #configure(org.jdom.Element, Class, boolean) configure} the element.
     * <p>
     * When the plugin is {@link SelfConfiguringPlugin self-configuring} the plugin takes
     * the responsibility of {@link SelfConfiguringPlugin#configure(org.jdom.Element) configuring itself}
     *
     * <p>{@link #configure(org.jdom.Element, Object, boolean)} to use when one already has an instance.
     * 
     * @param objectElement the JDOM Element defining the plugin configuration
     * @param pluginClass the class to instantiate
     * @param skipChildElements <code>false</code> to recurse the configuration, <code>true</code> otherwise
     * @return fully configured Object
     * @see #configure(org.jdom.Element, Object, boolean)
     * @throws CruiseControlException
     *   if the plugin class cannot be instantiated,
     *   if the configuration fails
     */
    public Object configure(Element objectElement, Class pluginClass,
                            boolean skipChildElements)
            throws CruiseControlException {

        Object pluginInstance = instantiatePlugin(pluginClass);
        return configure(objectElement, pluginInstance, skipChildElements);

    }

    /**
     * Instantiate a plugin
     * @param pluginClass
     * @return The instantiated plugin
     * @throws CruiseControlException if the plugin class cannot be instantiated
     */
    private Object instantiatePlugin(Class pluginClass) throws CruiseControlException {
        Object pluginInstance;
        try {
            pluginInstance = pluginClass.getConstructor(null).newInstance(null);
        } catch (Exception e) {
            LOG.fatal("Could not instantiate class", e);
            throw new CruiseControlException("Could not instantiate class: "
                    + pluginClass.getName());
        }
        return pluginInstance;
    }

    /**
     * Same as {@link #configure(org.jdom.Element, Class, boolean)}, except that
     * the client already has a pluginInstance.
     * @throws CruiseControlException if the configuration fails
     */
    public Object configure(Element objectElement, Object pluginInstance,
                            boolean skipChildElements) throws CruiseControlException {
        
        LOG.debug("configure " + objectElement.getName() + " instance " + pluginInstance.getClass()
                  + " self configuring: " + (pluginInstance instanceof SelfConfiguringPlugin)
                  + " skip:" + skipChildElements);
        if (pluginInstance instanceof SelfConfiguringPlugin) {
            ((SelfConfiguringPlugin) pluginInstance).configure(objectElement);
        } else {
            configureObject(objectElement, pluginInstance, skipChildElements);
        }
        return pluginInstance;
    }

    /**
     * Configure the specified plugin object given the JDOM Element defining the plugin configuration.
     *
     * <ul>
     * <li>calls setters that corresponds to element attributes</li>
     * <li>calls <code>public Yyy createXxx()</code> methods that corresponds to non-plugins child elements
     * (i.e. known by the instance class). The returned instance must be assignable to the Yyy type</li>
     * <li>calls <code>public void add(Xxx)</code> methods that corresponds to child elements which are
     * plugins themselves, e.g. which will require asking the ProjectXMLHelper to
     * {@link ProjectXMLHelper#configurePlugin(org.jdom.Element, boolean) configure the plugin}</li>
     * </ul>
     *
     * @param objectElement the JDOM Element defining the plugin configuration
     * @param object the instance to configure to instantiate
     * @param skipChildElements <code>false</code> to recurse the configuration, <code>true</code> otherwise
     */
    protected void configureObject(Element objectElement, Object object, boolean skipChildElements)
            throws CruiseControlException {

        LOG.debug("configuring object " + objectElement.getName()
            + " object " + object.getClass() + " skip " + skipChildElements);

        Map setters = new HashMap();
        Map creators = new HashMap();
        Set adders = new HashSet();

        Method[] methods = object.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            final String name = method.getName();
            if (name.startsWith("set")) {
                setters.put(name.substring("set".length()).toLowerCase(), method);
            } else if (name.startsWith("create")) {
                creators.put(name.substring("create".length()).toLowerCase(), method);
            } else if (name.equals("add") && method.getParameterTypes().length == 1) {
                adders.add(method);
            }
        }

        setFromAttributes(objectElement, setters, object);

        if (!skipChildElements) {
            Iterator childElementIterator = objectElement.getChildren().iterator();
            while (childElementIterator.hasNext()) {
                Element childElement = (Element) childElementIterator.next();
                if (creators.containsKey(childElement.getName().toLowerCase())) {
                    LOG.debug("treating child with creator " + childElement.getName());
                    try {
                        Method method = (Method) creators.get(childElement.getName().toLowerCase());
                        Object childObject = method.invoke(object, null);
                        configureObject(childElement, childObject, false);
                    } catch (Exception e) {
                        throw new CruiseControlException(e.getMessage());
                    }
                } else {
                    // instanciate object from element via registry
                    Object childObject = projectHelper.configurePlugin(childElement, false);

                    Method adder = null;
                    // iterate over adders to find one that will take the object
                    for (Iterator iterator = adders.iterator(); iterator.hasNext();) {
                        Method method = (Method) iterator.next();
                        Class type = method.getParameterTypes()[0];
                        if (type.isAssignableFrom(childObject.getClass())) {
                            adder = method;
                            break;
                        }
                    }

                    if (adder != null) {
                        try {
                            LOG.debug("treating child with adder " + childElement.getName() + " adding " + childObject);
                            adder.invoke(object, new Object[]{childObject});
                        } catch (Exception e) {
                            LOG.fatal("Error configuring plugin.", e);
                        }
                    } else {
                        throw new CruiseControlException("Nested element: '" + childElement.getName()
                                + "' is not supported for the <" + objectElement.getName() + "> tag.");
                    }
                }
            }
        }
    }

    private void setFromAttributes(Element objectElement, Map setters, Object object) throws CruiseControlException {
        for (Iterator iter = objectElement.getAttributes().iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            callSetter(attribute.getName(), attribute.getValue(), setters, object);
        }
    }
    
    private void callSetter(String propName, String propValue, Map setters, Object object)
        throws CruiseControlException {
        
        if (setters.containsKey(propName.toLowerCase())) {
            LOG.debug("Setting " + propName.toLowerCase() + " to " + propValue);
            try {
                Method method = (Method) setters.get(propName.toLowerCase());
                Class[] parameters = method.getParameterTypes();
                if (String.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, new Object[]{propValue});
                } else if (int.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, new Object[]{Integer.valueOf(propValue)});
                } else if (long.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, new Object[]{Long.valueOf(propValue)});
                } else if (boolean.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object,
                            new Object[]{Boolean.valueOf(propValue)});
                } else {
                    LOG.error("rCouldn't invoke setter " + propName.toLowerCase());
                }
            } catch (Exception e) {
                LOG.fatal("Error configuring plugin.", e);
            }
        } else {
            throw new CruiseControlException("Attribute: '" + propName
                    + "' is not supported for class: '" + object.getClass().getName() + "'.");
        }
    }

}
