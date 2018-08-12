/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Text;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sourceforge.cruisecontrol.config.PluginPlugin;

/**
 * Helps mapping the XML to object by instantiating and initializing beans.
 *
 * The {@link #configureObject(org.jdom2.Element, Object, boolean)} defines the operations
 * to be performed.
 */
public class PluginXMLHelper {
    private static final Logger LOG = Logger.getLogger(PluginXMLHelper.class);
    private final ProjectHelper projectHelper;
    private final CruiseControlController controller;

    public PluginXMLHelper(final ProjectHelper plugins) {
        this(plugins, null);
    }

    public PluginXMLHelper(final ProjectHelper plugins, final CruiseControlController controller) {
        projectHelper = plugins;
        this.controller = controller;
    }

    /**
     * Given a JDOM Element and a class, this method will instantiate an object
     * of type pluginClass, and configure the element.
     *
     * <p>{@link #configure(org.jdom2.Element, Object, boolean)} to use when one already has an instance.
     *
     * @param objectElement the JDOM Element defining the plugin configuration
     * @param pluginClass the class to instantiate
     * @param skipChildElements <code>false</code> to recurse the configuration, <code>true</code> otherwise
     * @return fully configured Object
     * @see #configure(org.jdom2.Element, Object, boolean)
     * @throws CruiseControlException
     *   if the plugin class cannot be instantiated,
     *   if the configuration fails
     */
    public Object configure(final Element objectElement, final Class pluginClass,
                            final boolean skipChildElements)
            throws CruiseControlException {

        final Object pluginInstance = instantiatePlugin(pluginClass);
        return configure(objectElement, pluginInstance, skipChildElements);

    }

    /**
     * Instantiate a plugin
     * @param pluginClass the class of the plugin to construct
     * @return The instantiated plugin
     * @throws CruiseControlException if the plugin class cannot be instantiated
     */
    private Object instantiatePlugin(final Class< ? > pluginClass) throws CruiseControlException {
        final Object pluginInstance;
        try {
            pluginInstance = pluginClass.getConstructor((Class[]) null).newInstance((Object[]) null);
            if (pluginInstance instanceof ControllerAware) {
                ((ControllerAware) pluginInstance).setController(controller);
            }
        } catch (Exception e) {
            LOG.fatal("Could not instantiate class", e);
            throw new CruiseControlException("Could not instantiate class: "
                    + pluginClass.getName(), e);
        }
        return pluginInstance;
    }

    /**
     * Same as {@link #configure(org.jdom2.Element, Class, boolean)}, except that
     * the client already has a pluginInstance.
     * @param objectElement the JDOM Element defining the plugin configuration
     * @param pluginInstance a live plugin instance
     * @param skipChildElements <code>false</code> to recurse the configuration, <code>true</code> otherwise
     * @return fully configured Object
     * @throws CruiseControlException if the configuration fails
     */
    public Object configure(final Element objectElement, final Object pluginInstance,
                            final boolean skipChildElements) throws CruiseControlException {

        LOG.debug("configure " + objectElement.getName() + " instance " + pluginInstance.getClass()
                  + " skip:" + skipChildElements);
        if (pluginInstance instanceof ResolverUser) {
            ((ResolverUser) pluginInstance).setFileResolver(projectHelper.getFileResolver());
        }
        if (pluginInstance instanceof PluginPlugin) {
            ((PluginPlugin) pluginInstance).configure(objectElement);
        } else {
            // Having all properties defined for the object, try to resolve all the unresolved
            // properties
            projectHelper.resolveProperties(objectElement);
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
     * {@link ProjectXMLHelper#configurePlugin(org.jdom2.Element, boolean) configure the plugin}</li>
     * </ul>
     *
     * @param objectElement the JDOM Element defining the plugin configuration
     * @param object the instance to configure to instantiate
     * @param skipChildElements <code>false</code> to recurse the configuration, <code>true</code> otherwise
     * @throws CruiseControlException if an error occurs while configuring the object, like if a child element is not
     * supported.
     */
    protected void configureObject(final Element objectElement, final Object object, final boolean skipChildElements)
            throws CruiseControlException {

        LOG.debug("configuring object " + objectElement.getName()
            + " object " + object.getClass() + " skip " + skipChildElements);

        final Map<String, Method> setters = new HashMap<String, Method>();
        final Map<String, Method> creators = new HashMap<String, Method>();
        final Set<Method> adders = new HashSet<Method>();

        final Method[] methods = object.getClass().getMethods();
        for (final Method method : methods) {
            final String name = method.getName();
            final Class[] params = method.getParameterTypes();
            if (name.startsWith("set") && params.length == 1) {
                setters.put(name.substring("set".length()).toLowerCase(Locale.US), method);
            } else if (name.startsWith("create") && params.length == 0) {
                creators.put(name.substring("create".length()).toLowerCase(Locale.US), method);
            } else if (name.equals("add") && params.length == 1) {
                adders.add(method);
            } else if (name.equals("xmltext") && params.length == 1 && Text.class.isAssignableFrom(params[0])) {
                setters.put("XMLTEXT", method);
            }
        }

        setFromAttributes(objectElement, setters, object);

        if (!skipChildElements) {
//            for (final Object o : objectElement.getChildren()) {
            for (final Object o : objectElement.getContent()) {
                if (o instanceof Element) {
                    final Element childElement = (Element) o;    
                    if (creators.containsKey(childElement.getName().toLowerCase(Locale.US))) {
                        LOG.debug("treating child with creator " + childElement.getName());
                        try {
                            final Method method = creators.get(childElement.getName().toLowerCase(Locale.US));
                            final Object childObject = method.invoke(object, (Object[]) null);
                            configureObject(childElement, childObject, false);
                        } catch (Exception e) {
                            throw new CruiseControlException(e.getMessage());
                        }
                    } else {
                        // instanciate object from element via registry
                        final Object childObject = projectHelper.configurePlugin(childElement, false);

                        Method adder = null;
                        // iterate over adders to find one that will take the object
                        for (final Method method : adders) {
                            final Class< ? > type = method.getParameterTypes()[0];
                            if (type.isAssignableFrom(childObject.getClass())) {
                                adder = method;
                                break;
                            }
                        }

                        if (adder != null) {
                            try {
                                LOG.debug("treating child with adder " + childElement.getName() + " adding "
                                    + childObject);
                                adder.invoke(object, childObject);
                            } catch (Exception e) {
                                LOG.fatal("Error configuring plugin.", e);
                            }
                        } else {
                                throw new CruiseControlException("Nested element: '" + childElement.getName()
                                    + "' is not supported for the <" + objectElement.getName() + "> tag.");
                        }
                    }
                    continue;
                }
                if (o instanceof Text) {
                    final Method setter = setters.get("XMLTEXT");
                    if (setter != null) {
                        LOG.debug("Setting text content of XML node" + objectElement.getName());
                        try {
                            setter.invoke(object, o);
                        } catch (Exception e) {
                            LOG.fatal("Error configuring plugin.", e);
                        }
//                    } else {
//                        LOG.warn("Couldn't set content of XML node - method not defined");
                    }
                    continue;
                }
            }
        }
    }

    private void setFromAttributes(final Element objectElement, final Map<String, Method> setters, final Object object)
            throws CruiseControlException {

        for (final Object o : objectElement.getAttributes()) {
            final Attribute attribute = (Attribute) o;
            callSetter(attribute.getName(), attribute.getValue(), setters, object);
        }
    }

    private void callSetter(final String propName, final String propValue,
                            final Map<String, Method> setters, final Object object)
        throws CruiseControlException {

        if (setters.containsKey(propName.toLowerCase(Locale.US))) {
            LOG.debug("Setting " + propName.toLowerCase(Locale.US) + " to " + propValue);
            try {
                final Method method = setters.get(propName.toLowerCase(Locale.US));
                final Class[] parameters = method.getParameterTypes();
                if (String.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, propValue);
                } else if (int.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, Integer.valueOf(propValue));
                } else if (long.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, Long.valueOf(propValue));
                } else if (boolean.class.isAssignableFrom(parameters[0])) {
                    method.invoke(object, Boolean.valueOf(propValue));
                } else {
                    LOG.error("rCouldn't invoke setter " + propName.toLowerCase(Locale.US));
                }
            } catch (Exception e) {
                LOG.fatal("Error configuring plugin.", e);
            }
        } else {
            LOG.warn("Ignoring attribute: '" + propName
                  + "' for class: '" + object.getClass().getName() + "'.");
//            throw new CruiseControlException("Attribute: '" + propName
//                    + "' is not supported for class: '" + object.getClass().getName() + "'.");
        }
    }

}
