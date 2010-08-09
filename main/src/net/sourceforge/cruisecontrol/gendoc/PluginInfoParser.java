/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.gendoc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.gendoc.annotations.Title;

/**
 * Utility class for parsing PluginInfo objects reflexively.
 * @author Seth Pollen (pollens@msoe.edu)
 */
public class PluginInfoParser {
    
    /** Mapping of Java types to allowed XML attribute types. */
    private static final Map<Class< ? >, AttributeType> ATTRIBUTE_TYPES = new HashMap<Class< ? >, AttributeType>();
    static {
        ATTRIBUTE_TYPES.put(boolean.class, AttributeType.BOOLEAN);
        ATTRIBUTE_TYPES.put(Boolean.class, AttributeType.BOOLEAN);
    
        ATTRIBUTE_TYPES.put(int.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(Integer.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(short.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(Short.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(byte.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(Byte.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(long.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(Long.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(double.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(Double.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(float.class, AttributeType.NUMBER);
        ATTRIBUTE_TYPES.put(Float.class, AttributeType.NUMBER);
        
        ATTRIBUTE_TYPES.put(String.class, AttributeType.STRING);
    }
    
    /** PluginRegistry used to look up mappings of XML node names to class names. */
    private final PluginRegistry registry;
    
    /**
     * Map containing all the inheritance relationships of all the classes from the PluginRegistry.
     * The mapping is from a superclass to a Set of all its subclasses contained in the
     * PluginRegistry.
     */
    private final Map<Class< ? >, Set<Class< ? >>> subclassesFromRegistry =
            new HashMap<Class< ? >, Set<Class< ? >>>(); 
    
    /** Cache of already-parsed plugins. */
    private final Map<PluginKey, PluginInfo> parsedPluginCache = new HashMap<PluginKey, PluginInfo>();
    
    /** Cache to contain the root plugin, once it has been parsed. */
    private final PluginInfo rootPlugin;
    
    /** List of general errors occurring during parsing. Use a Set to prevent duplicate messages. */
    private final Set<String> parsingErrors = new LinkedHashSet<String>();
    
    /**
     * Creates a new PluginInfoParser.
     * @param registry PluginRegistry to use for this parser.
     * @param rootPluginName the name of the root plugin. See CruiseControlControllerJMXAdaptor.ROOT_PLUGIN.
     */
    public PluginInfoParser(final PluginRegistry registry, final String rootPluginName) {
        this.registry = registry;
        buildSubclassesFromRegistryMap();
        
        // Fetch the root plugin class.
        final Class< ? > rootClass;
        try {
            rootClass = registry.getPluginClass(rootPluginName);
        } catch (CruiseControlException e) {
            // This should never happen; we should always be able to load the root node.
            throw new RuntimeException(e);
        }
        
        rootPlugin = parsePlugin(rootClass, null);
        rootPlugin.computeDepths();
    }
    
    /**
     * Gets the PluginInfo representing the root node.
     * @return The plugin tree, fully parsed.
     */
    public PluginInfo getRootPlugin() {
        return rootPlugin;
    }
    
    /**
     * Gets all the plugins parsed by this parser, in alphabetical order.
     * @return The list of all the plugins that have been parsed.
     */
    public List<PluginInfo> getAllPlugins() {
        List<PluginInfo> list = new ArrayList<PluginInfo>(parsedPluginCache.values());
        Collections.sort(list); // Sort the plugins by name.
        return list;
    }
    
    /**
     * Gets a list of parsing errors that occurred. Errors returned by this method will not
     * be associated with a specific PluginInfo. Errors that are associated with a specific
     * PluginInfo instance can be accessed using that instance's getParsingErrors method.
     * @return List of error messages.
     */
    public List<String> getParsingErrors() {
        return new ArrayList<String>(parsingErrors);
    }
    
    /**
     * Initializes the subclassesFromRegistry Map based on the contents of the registry.
     */
    private void buildSubclassesFromRegistryMap() {
        for (String pluginClassName : registry) {
            // Load the class, using its name.
            try {
                final Class< ? > subclass = Class.forName(pluginClassName);
                
                // Iterate through all available superclasses and add mappings for each one.
                Class< ? > superclass = subclass;
                do {
                    // Add a mapping for the direct superclass.
                    addSubclassMapping(subclass, superclass);
                    
                    // Add mappings for all interfaces declared by the superclass.
                    for (Class< ? > intrface : superclass.getInterfaces()) {
                        addSubclassMapping(subclass, intrface);
                    }
                    
                    // Move to the next superclass up in the hierarchy.
                    superclass = superclass.getSuperclass();
                } while (superclass != null); // Stop at the top of the inheritance hierarchy.
                
            } catch (ClassNotFoundException e) {
                // Don't try to parse this class, since it won't load. Instead, record an error.
                parsingErrors.add("Failed to load class from PluginRegistry: " + pluginClassName);
            }
        }
    }
    
    /**
     * Adds a mapping to the subclassesFromRegistry Map that indicates a single inheritance
     * relationship.
     * @param subclass The subclass to associate with the superclass.
     * @param superclass The superclass to whose list of subclasses the subclass will be added. 
     */
    private void addSubclassMapping(Class< ? > subclass, Class< ? > superclass) {
        // Check if a Set has already been started for this superclass.
        Set<Class< ? >> subclasses = subclassesFromRegistry.get(superclass);
        if (subclasses == null) { // Create the list.
            subclasses = new HashSet<Class< ? >>();
            subclassesFromRegistry.put(superclass, subclasses);
        }
        
        // Add the given subclass to the list of subclasses inheriting from the
        // given superclass.
        subclasses.add(subclass);
    }
    
    /**
     * Parses a plugin from a Class.
     * @param pluginClass The Class representing the plugin to be parsed.
     * @param pluginName The name to use for the plugin. If this is null, the default name will be inferred
     *        from the plugin registry or the class name.
     * @return A PluginInfo object representing the class (and including representations of all its methods).
     */
    private PluginInfo parsePlugin(Class< ? > pluginClass, String pluginName) {
        // Check if we need to infer the name or if it was manually specified.
        if (pluginName == null) {
            // Infer the name.
            pluginName = computePluginName(pluginClass);
        }
        
        // Construct a key for querying the cache.
        PluginKey key = new PluginKey(pluginClass, pluginName);
        
        // Check the cache to see if we already parsed this class.
        PluginInfo pluginInfo = parsedPluginCache.get(key);
        if (pluginInfo == null) {
            // We haven't done this class already, so we have to do it now. Associate this parser with
            // the plugin.
            pluginInfo = new PluginInfo();
            
            // Put the plugin info object into the cache now (before parsing it), in case it makes
            // a reference to itself in one of its children.
            parsedPluginCache.put(key, pluginInfo);
            
            // Populate the plugin info fields.
            pluginInfo.setName(pluginName);
            pluginInfo.setDescription(computePluginDescription(pluginClass));
            pluginInfo.setTitle(computePluginTitle(pluginClass));
            
            // Parse attributes and children.
            parsePluginAttributes(pluginInfo, pluginClass);
            parsePluginChildren(pluginInfo, pluginClass);
        }
        
        return pluginInfo;
    }
    
    private String computePluginName(Class< ? > pluginClass) {
        // First, check if this plugin is in the registry.
        String name = registry.getPluginName(pluginClass);
        if (name == null) {
            // We didn't find it in the registry, so just use the class name.
            name = pluginClass.getSimpleName().toLowerCase(Locale.US);
        }
        return name;
    }
    
    private String computePluginDescription(Class< ? > pluginClass) {
        Description annotation = pluginClass.getAnnotation(Description.class);
        if (annotation == null) {
            return null;
        } else {
            String description = annotation.value();
            
            // Make sure the description is properly surrounded with <p></p> tags.
            if (!description.startsWith("<p>")) {
                description = "<p>" + description;
            }
            if (!description.endsWith("</p>")) {
                description = description + "</p>";
            }
            
            return description;
        }
    }
    
    private String computePluginTitle(Class< ? > pluginClass) {
        Title annotation = pluginClass.getAnnotation(Title.class);
        if (annotation == null) {
            return null;
        } else {
            return annotation.value();
        }
    }
    
    /**
     * Parses the attributes of a plugin into a PluginInfo object.
     * @param pluginInfo PluginInfo where attribute metadata will be placed.
     * @param pluginClass The plugin class to be parsed.
     */
    private void parsePluginAttributes(PluginInfo pluginInfo, Class< ? > pluginClass) {
        // Search for methods with the signature setXXX (and which are not annotated to be skipped).
        for (Method attributeMethod : pluginClass.getMethods()) {
            String methName = attributeMethod.getName();
            if (methName.startsWith("set") && (null == attributeMethod.getAnnotation(SkipDoc.class))) {
                // This method represents an attribute.
                AttributeInfo attributeInfo = new AttributeInfo();
                
                try {
                    // Populate the AttributeInfo properties.
                    String attributeName = methName.substring(3).toLowerCase(Locale.US); // Remove the "set"
                    
                    // Check for duplicate attribute names.
                    if (null != pluginInfo.getAttributeByName(attributeName)) {
                        throw new PluginInfoParsingException(
                                "Two different setter methods found defining attribute '" + attributeName + "'");
                    }
                    
                    attributeInfo.setName(attributeName);
                    attributeInfo.setDescription(computeMemberDescription(attributeMethod));
                    attributeInfo.setTitle(computeMemberTitle(attributeMethod));
                    attributeInfo.setType(computeAttributeType(attributeMethod));
                    attributeInfo.setDefaultValue(computeAttributeDefaultValue(attributeMethod));
                    parseAttributeCardinality(attributeInfo, attributeMethod);
                    
                    // Store the AttributeInfo.
                    pluginInfo.addAttribute(attributeInfo);
                } catch (PluginInfoParsingException e) {
                    // Log the error.
                    pluginInfo.addParsingError(
                            "Error while parsing attribute '" + attributeMethod
                            + "' - " + e.getMessage());
                }
            }
        }
        
        // Finally, sort the attributes.
        pluginInfo.sortAttributes();
    }
    
    private String computeMemberDescription(Method method) {
        Description annotation = method.getAnnotation(Description.class);
        if (annotation == null) {
            return null;
        } else {
            // Don't wrap member declarations with <p> tags, since most members won't have
            // multi-paragraph descriptions.
            return annotation.value();
        }
    }
    
    private String computeMemberTitle(Method method) {
        Title annotation = method.getAnnotation(Title.class);
        if (annotation == null) {
            return null;
        } else {
            return annotation.value();
        }
    }
    
    private AttributeType computeAttributeType(Method attributeMethod) throws PluginInfoParsingException {
        Class< ? >[] paramTypes = attributeMethod.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new PluginInfoParsingException(
                    "Attribute setter method of plugin class must have exactly one parameter");
        }
        
        Class< ? > javaType = paramTypes[0];
        AttributeType attributeType = ATTRIBUTE_TYPES.get(javaType);
        if (attributeType == null) {
            throw new PluginInfoParsingException(
                    "Attribute setter method of plugin class must accept a boolean, String, or numeric type");
        } else {
            return attributeType;
        }
    }
    
    private String computeAttributeDefaultValue(Method attributeMethod) {
        Default annotation = attributeMethod.getAnnotation(Default.class);
        if (annotation == null) {
            return null;
        } else {
            return annotation.value();
        }
    }
    
    private void parseAttributeCardinality(AttributeInfo memberInfo, Method memberMethod)
            throws PluginInfoParsingException {
        // Check for an Optional or Required tag.
        boolean hasOptional =  null != memberMethod.getAnnotation(Optional.class);
        boolean hasRequired =  null != memberMethod.getAnnotation(Required.class);
        if (hasOptional && hasRequired) {
            throw new PluginInfoParsingException(
                    "Cannot have @Required and @Optional annotations on the same attribute method");
        }
        
        // Multiplicity of attributes is not permitted. Thus, the maximum cardinality is always 1.
        memberInfo.setMaxCardinality(1);
        
        Cardinality cardinality = memberMethod.getAnnotation(Cardinality.class);
        if (cardinality == null) {
            if (hasRequired) {
                // Make it required (1..1)
                memberInfo.setMinCardinality(1);
            } else {
                // For attributes, multiplicity is not permitted, so default the cardinality
                // to optional (0..1)
                memberInfo.setMinCardinality(0);
            }
        } else {
            // There is a Cardinality annotation. Error check for the other annotations.
            if (hasOptional) {
                throw new PluginInfoParsingException(
                        "Cannot have @Cardinality and @Optional annotations on the same attribute method");
            }
            if (hasRequired) {
                throw new PluginInfoParsingException(
                        "Cannot have @Cardinality and @Required annotations on the same attribute method");
            }
            
            final int min = cardinality.min();
            final int max = cardinality.max();
            
            if (max != 1) {
                throw new PluginInfoParsingException(
                        "Maximum cardinality must be 1 for attributes.");
            }
            if ((min != 0) && (min != 1)) {
                throw new PluginInfoParsingException(
                        "Minimum cardinality must be 0 or 1 for attributes.");
            }
            
            memberInfo.setMinCardinality(min);
        }
    }
    
    /**
     * Parses the children of a plugin into a PluginInfo object.
     * @param pluginInfo PluginInfo where child metadata will be placed.
     * @param pluginClass The plugin class to be parsed.
     */
    private void parsePluginChildren(PluginInfo pluginInfo, Class< ? > pluginClass) {
        // Search for methods with the signature addXXX or createXXX (and which are not annotated to be skipped).
        for (Method childMethod : pluginClass.getMethods()) {
            String methName = childMethod.getName();
            if (
                    (methName.startsWith("add") || methName.startsWith("create"))
                    && (null == childMethod.getAnnotation(SkipDoc.class))
            ) {
                try {
                    // This method represents a child.
                    ChildInfo childInfo = new ChildInfo();

                    // Populate the ChildInfo properties.
                    childInfo.setDescription(computeMemberDescription(childMethod));
                    childInfo.setTitle(computeMemberTitle(childMethod));
                    parseChildCardinality(childInfo, childMethod);
                    parseChildAllowedTypes(childInfo, childMethod);
                    
                    // Store the ChildInfo.
                    pluginInfo.addChild(childInfo);
                } catch (PluginInfoParsingException e) {
                    // Log the error.
                    pluginInfo.addParsingError(
                            "Error while parsing child method '" + childMethod
                            + "' - " + e.getMessage());
                }
            }
        }
    }
    
    private void parseChildCardinality(ChildInfo memberInfo, Method memberMethod) throws PluginInfoParsingException {
        // Make sure there is no Optional or Required tag; these are not allowed for children.
        if (null != memberMethod.getAnnotation(Optional.class)) {
            throw new PluginInfoParsingException(
                    "@Optional annotation not allowed on child method; use @Cardinality instead");
        }
        if (null != memberMethod.getAnnotation(Required.class)) {
            throw new PluginInfoParsingException(
                    "@Required annotation not allowed on child method; use @Cardinality instead");
        }
        
        Cardinality cardinality = memberMethod.getAnnotation(Cardinality.class);
        if (cardinality == null) {
            // For children, the default cardinality is 0..*
            memberInfo.setMinCardinality(0);
            memberInfo.setMaxCardinality(-1);
        } else {
            final int min = cardinality.min();
            final int max = cardinality.max();
            
            if (min < 0) {
                throw new PluginInfoParsingException(
                        "Minimum cardinality cannot be negative.");
            }
            if ((max != -1) && (max < min)) {
                throw new PluginInfoParsingException(
                        "Maximum cardinality may not be less than minimum cardinality.");
            }
            
            memberInfo.setMinCardinality(cardinality.min());
            memberInfo.setMaxCardinality(cardinality.max());
        }
    }
    
    /**
     * Parses the allowed node types for a child and adds them to a ChildInfo object.
     * @param childInfo ChildInfo being constructed. This will receive the parsed allowed type
     *        information.
     * @param childMethod Java createXXX or addXXX Method that is generating this child.
     * @throws PluginInfoParsingException If the plugin class to be parsed is malformed and cannot be parsed.
     */
    private void parseChildAllowedTypes(ChildInfo childInfo, Method childMethod) throws PluginInfoParsingException {
        boolean isAddMethod = childMethod.getName().startsWith("add");
        Class< ? > childType = computeChildType(childMethod, isAddMethod);
        
        if (isAddMethod) {
            // Check the plugin registry for classes that inherit from the declared child type. For each
            // possible child implementation class, parse a PluginInfo and add it to the ChildInfo's list.
            Set<Class< ? >> subclasses = subclassesFromRegistry.get(childType);
            if (subclasses != null) {
                for (Class< ? > subclass : subclasses) {
                    // Pass null for the name, since manual renaming of addXXX children is not allowed.
                    PluginInfo childPluginInfo = parsePlugin(subclass, null);
                    childInfo.addAllowedNode(childPluginInfo);
                }
            }
            
        } else { // This is a createXXX method.
            // For createXXX methods, there can only be one child class; it is the concrete return
            // type of the method. createXXX methods also allow manual renaming of children. Check
            // for the manual renaming annotation to see if a manual name should be supplied.
            String manualName = null;
            ManualChildName annotation = childMethod.getAnnotation(ManualChildName.class);
            if (annotation != null) {
                manualName = annotation.value();
            }
            
            // Parse the plugin, using the manual name if it was supplied.
            PluginInfo childPluginInfo = parsePlugin(childType, manualName);
            childInfo.addAllowedNode(childPluginInfo);
        }
        
        // Make sure the child can accept at least one type of node.
        if (childInfo.getAllowedNodes().isEmpty()) {
            throw new PluginInfoParsingException(
                    "Child method does not accept any valid node classes.");
        }
        
        // Sort the plugins in the child's list of allowed nodes.
        childInfo.sortAllowedNodes();
    }

    private Class< ? > computeChildType(Method childMethod, boolean isAddMethod) throws PluginInfoParsingException {
        if (isAddMethod) {
            // It's an addXXX method, so use the first argument type as the child class.
            Class< ? >[] paramTypes = childMethod.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new PluginInfoParsingException(
                        "Child add method of plugin class must have exactly one parameter");
            }
            return paramTypes[0];
            
        } else {
            // Check the number of parameters. This will prevent us from unknowingly parsing a createXXX method
            // that we really should skip.
            if (childMethod.getParameterTypes().length != 0) {
                throw new PluginInfoParsingException(
                        "Child create method of plugin class may not take parameters");
            }
            
            // It's a createXXX method, so use the return type as the child class.
            return childMethod.getReturnType();
        }
    }
    
    /**
     * Class to represent the unique key to identify a plugin. This is a tuple of the plugin
     * class and the XML name for the plugin. This tuple is required because there may be
     * multiple plugins with different XML names but the same underlying Java class.
     * @author pollens@msoe.edu
     */
    private static class PluginKey {
        
        /** The class that generated the plugin. */
        private final Class< ? > clazz;
        
        /** The name used to identify the plugin in an XML config file. */
        private final String name;
        
        public PluginKey(Class< ? > clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof PluginKey) {
                PluginKey other = (PluginKey) o;
                return
                        this.clazz.equals(other.clazz)
                        && this.name.equals(other.name);
            } else {
                return false;
            }
        }
        
        @Override
        public int hashCode() {
            return clazz.hashCode() + name.hashCode();
        }
        
        @Override
        public String toString() {
            return "(" + clazz + ", \"" + name + "\")";
        }
        
    }
    
}
