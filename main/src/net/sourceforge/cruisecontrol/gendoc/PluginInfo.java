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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents an Plugin, storing information such as attributes and children
 * The setter methods of this class are package-private to prevent modification
 * after the class has been constructed.
 * 
 * @author Anthony Love (lovea@msoe.edu)
 * @version 1.0
 */
public class PluginInfo implements Serializable, Comparable<Object> {

    /** ID for serialization. */
    private static final long serialVersionUID = 2L;

    /** Path separator for ancestry paths. */
    private static final char PATH_SEPARATOR = '/';

    /** The name of the Plugin, as it would appear in an XML config file */
    private String name = null; 

    /** The Plugin's description */
    private String description = null;

    /** The human-readable title for the plugin. */
    private String title = null;

    /** List of attributes, preserving order. */
    private final List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();

    /**
     * List of children for this plugin. Default to zero capacity to save space, since most plugins
     * won't have children.
     */
    private final List<ChildInfo> children = new ArrayList<ChildInfo>(0);

    /**
     * List of parents for this plugin. This includes all plugins which claim this plugin as a child.
     * Note that a plugin can have multiple parents, and a plugin can even be its own parent.
     */
    private final List<PluginInfo> parents = new ArrayList<PluginInfo>();

    /**
     * Direct parent for this plugin. This is the parent directly above it on the shortest path
     * up to the root plugin node. This value is lazily calculated and cached here.
     */
    private PluginInfo directParent = null;

    /**
     * Depth of this plugin in the tree. A depth of zero means this is the root plugin of the
     * tree. A value of Integer.MAX_VALUE means the depth still needs to be calculated.
     * */
    private int depth = Integer.MAX_VALUE;

    /**
     * List of errors that occurred while parsing this info. Default to zero capacity to
     * save space, since most plugins should be without errors. 
     */
    private final List<String> parsingErrors = new ArrayList<String>(0);

    /**
     * Creates a new PluginInfo with all fields defaulted.
     */
    public PluginInfo() {
        // Empty.
    }

    /**
     * Returns the name of the plugin
     * 
     * @return The name of the plugin
     */
    public String getName() {
        return name;
    }

    /**
     * Set's the Plugin's name
     * 
     * @param name The desired name
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns this Plugin's description
     * 
     * @return The Plugin's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the plugin's description
     * 
     * @param description The desired description
     */
    protected void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the List of Children for the plugin
     * 
     * @return The List of Children for the plugin
     */
    public List<ChildInfo> getChildren() {
        return children;
    }

    /**
     * Adds the given Child to the List.
     * 
     * @param ch The Child to add. This object must be fully parsed and constructed
     *        before being passed in to this method.
     */
    protected void addChild(ChildInfo ch) {
        ch.addParent(this);
        children.add(ch);
    }

    /**
     * Returns the List of Attributes for this plugin
     * 
     * @return The List of Attributes for the plugin
     */
    public List<AttributeInfo> getAttributes() {
        return attributes;
    }

    /**
     * Adds the given attribute to the List
     * 
     * @param att The attribute to add
     */
    protected void addAttribute(AttributeInfo att) {
        attributes.add(att);
    }

    /**
     * Sets the Title to the given value
     * 
     * @param title The new title.
     */
    protected void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the Plugin's Title
     * 
     * @return The Plugin's Title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets a child plugin for this plugin, given the name of the child plugin.
     * @param pluginName The name of the child plugin to get.
     * @return Null if the named plugin is not an allowed child of this plugin.
     *         Otherwise, the return value is the PluginInfo for the requested
     *         child plugin.
     */
    public PluginInfo getChildPluginByName(String pluginName) {
        for (ChildInfo child : children) {
            PluginInfo node = child.getAllowedNodeByName(pluginName);
            if (node != null) {
                return node;
            }
        }

        // No suitable plugin was found.
        return null;
    }

    /**
     * Gets an attribute of this plugin, given the attribute name.
     * @param attributeName The name of the attribute whose information is to be fetched.
     * @return The AttributeInfo for the named attribute, or null if no attribute
     *         exists with that name.
     */
    public AttributeInfo getAttributeByName(String attributeName) {
        // Assume the list has been sorted.
        int index = Collections.binarySearch(attributes, attributeName);
        if (index < 0) {
            return null;
        } else {
            return attributes.get(index);
        }
    }

    /**
     * Gets a ChildInfo object which contains a specified plugin as an allowed node.
     * @param pluginName The name of the plugin to look for.
     * @return A ChildInfo object that is a child of this plugin and contains the plugin
     *         'pluginName' as an allowed node, or null if no such ChildInfo exists.
     */
    public ChildInfo getChildByPluginName(String pluginName) {
        for (ChildInfo child : children) {
            if (child.getAllowedNodeByName(pluginName) != null) {
                return child;
            }
        }

        // No suitable child was found.
        return null;
    }

    /**
     * Logs an error during parsing.
     * @param errorMessage a parsing error message to log.
     */
    protected void addParsingError(String errorMessage) {
        parsingErrors.add(errorMessage);
    }

    /**
     * Gets all the parsing errors that were encountered when this plugin was parsed.
     * @return The list of error messages.
     */
    public List<String> getParsingErrors() {
        return parsingErrors;
    }

    /**
     * Adds a parent to this plugin.
     * @param p The parent to add.
     */
    protected void addParent(PluginInfo p) {
        parents.add(p);
    }

    /**
     * Gets the direct parent of this plugin. The direct parent is the plugin immediately above
     * it on the shortest path that leads back to the root node. This method may take a lot of
     * time to perform the search, so its result is cached. Therefore, it should only be called
     * if necessary. Also, this method may only be called after the plugin tree has been fully
     * parsed and computeDepth() has been called on the root node of the tree.
     * @return The direct parent, or null if this Plugin has no parents.
     */
    public PluginInfo getDirectParent() {
        if (parents.isEmpty()) {
            return null; // No parents.
        } else {
            if (directParent == null) { // Direct parent not cached; we need to compute it.
                // Simply find the parent with the shallowest depth in the tree.
                PluginInfo shallowestParent = null;
                for (PluginInfo parent : parents) {
                    if ((shallowestParent == null) || (parent.depth < shallowestParent.depth)) {
                        shallowestParent = parent;
                    }
                }
                directParent = shallowestParent;
            }
            return directParent;
        }
    }

    /**
     * Gets a list of plugins in the hierarchy which starts at the root plugin and ends at this
     * plugin.
     * @return The list of plugins.
     */
    public List<PluginInfo> getAncestry() {
        List<PluginInfo> ancestry = new ArrayList<PluginInfo>();

        // Build the list in reverse (from children to parents).
        PluginInfo current = this;
        do {
            ancestry.add(current);
            current = current.getDirectParent();
        } while (current != null);

        // Flip the list before returning it.
        Collections.reverse(ancestry);

        return ancestry;
    }

    /**
     * Gets the fully-qualified name for this plugin, which includes its ancestors' names,
     * separated by periods.
     * @return The name.
     */
    public String getAncestralName() {
        StringBuilder text = new StringBuilder();
        boolean first = true;

        for (PluginInfo ancestor : getAncestry()) {
            if (first) {
                first = false;
            } else {
                text.append(PATH_SEPARATOR);
            }
            text.append(ancestor.getName());
        }

        return text.toString();
    }

    /**
     * Computes the depth of this plugin (and all its children) in the tree, according to the shortest
     * path that leads back to the tree root. The result is stored in each object's depth field. This
     * method may only be called on the root plugin, or loops in the tree may not be handled properly
     * and may result in stack overflow.
     */
    protected void computeDepths() {
        // Make sure we are the root plugin.
        if (!parents.isEmpty()) {
            throw new UnsupportedOperationException("computeDepth() can only be invoked on root plugin");
        }

        // Traverse the plugin tree in a breadth-first manner, computing the depth at each
        // level as we encounter it.
        int currentDepth = 0;
        List<PluginInfo> pluginsAtThisDepth = new ArrayList<PluginInfo>();
        List<PluginInfo> pluginsAtNextDepth = new ArrayList<PluginInfo>();
        pluginsAtThisDepth.add(this); // Start with this, the root plugin.
        do {
            // Search through all the plugins at this depth.
            for (PluginInfo plugin : pluginsAtThisDepth) {
                // Only deal with the node if it hasn't been touched yet.
                if (currentDepth < plugin.depth) {
                    plugin.depth = currentDepth;

                    // We now know that all the children of this plugin are at the next depth down,
                    // so add all the children to the next list to be searched.
                    for (ChildInfo child : plugin.getChildren()) {
                        for (PluginInfo childPlugin : child.getAllowedNodes()) {
                            pluginsAtNextDepth.add(childPlugin);
                        }
                    }
                }
            }

            // We are now done with this level. Move to the next one. Swap lists to reuse allocated memory.
            currentDepth++;
            List<PluginInfo> temp = pluginsAtThisDepth;
            pluginsAtThisDepth = pluginsAtNextDepth;
            pluginsAtNextDepth = temp;
            pluginsAtNextDepth.clear(); // Get ready for next iteration.
        } while (!pluginsAtThisDepth.isEmpty()); // Keep going until we have done all the plugins.
    }

    /**
     * Sorts the attributes in this plugin by name.
     */
    protected void sortAttributes() {
        Collections.sort(attributes);
    }

    /**
     * Generates HTML to document this plugin, its attributes, and its children. This will be a
     * fragment of the full HTML documentation that can be generated for all plugins, and it may
     * contain hyperlinks to other plugins' documentation.
     * @return The HTML text.
     */
    public String getHtmlDocumentation() {
        StringBuilder text = new StringBuilder();
        
        text
        .append("<div class=\"elementdocumentation\">\n")
        .append("<a class=\"toplink\" href=\"#top\">top</a>\n")
        .append("<h2><a name=\"")
        .append(getName()) // Allow linking by simple name ...
        .append("\"><a name=\"")
        .append(getAncestralName()) // ... or by full ancestral name.
        .append("\">&lt;")
        .append(getName())
        .append("&gt;</a></a></h2>\n")
        .append("<div class=\"hierarchy\">\n")
        .append("<pre>")
        .append(formatHtmlAncestry())
        .append("</pre>\n")
        .append("</div>\n")
        .append(emptyIfNull(getDescription()))
        .append("\n");

        // Print attributes in a table.
        if (!attributes.isEmpty()) {
            text
            .append("<h3>Attributes</h3>\n")
            .append("<table class=\"documentation\">\n")
            .append("<thead>\n")
            .append("<tr>\n")
            .append("<th>Attribute</th>\n")
            .append("<th>Required?</th>\n")
            .append("<th>Cardinality</th>\n")
            .append("<th>Default value</th>\n")
            .append("<th>Description</th>\n")
            .append("</tr>\n")
            .append("</thead>\n")
            .append("<tbody>\n");

            for (AttributeInfo attribute : attributes) {
                text
                .append("<tr>\n")
                .append("<td>")
                .append(attribute.getName())
                .append("</td>\n");

                if (attribute.getMinCardinality() > 0) {
                    text.append("<td>Required</td>\n");
                } else {
                    text.append("<td>Optional</td>\n");
                }

                text
                .append("<td>")
                .append(formatMemberCardinality(attribute))
                .append("</td>\n")
                .append("<td>")
                .append(emptyIfNull(attribute.getDefaultValue()))
                .append("</td>\n")
                .append("<td>")
                .append(emptyIfNull(attribute.getDescription()))
                .append("</td>\n")
                .append("</tr>\n");
            }

            text
            .append("</tbody>\n")
            .append("</table>\n");
        }

        // Print children in a table.
        if (!children.isEmpty()) {
            text
            .append("<h3>Child Elements</h3>\n")
            .append("<table class=\"documentation\">\n")
            .append("<thead>\n")
            .append("<tr>\n")
            .append("<th>Element</th>\n")
            .append("<th>Required?</th>\n")
            .append("<th>Cardinality</th>\n")
            .append("<th>Description</th>\n")
            .append("</tr>\n")
            .append("</thead>\n")
            .append("<tbody>\n");

            for (ChildInfo child : children) {
                text
                .append("<tr>\n")
                .append("<td>\n");

                for (PluginInfo childNode : child.getAllowedNodes()) {
                    text
                    .append("<a href=\"#")
                    .append(childNode.getAncestralName())
                    .append("\">&lt;")
                    .append(childNode.getName())
                    .append("&gt;</a><br/>\n");
                }

                text.append("</td>\n");

                if (child.getMinCardinality() > 0) {
                    text.append("<td>Required</td>\n");
                } else {
                    text.append("<td>Optional</td>\n");
                }

                text
                .append("<td>")
                .append(formatMemberCardinality(child))
                .append("</td>\n")
                .append("<td>")
                .append(emptyIfNull(child.getDescription()))
                .append("</td>\n")
                .append("</tr>\n");
            }

            text
            .append("</tbody>\n")
            .append("</table>\n");
        }

        // Print parsing errors in a table.
        if (!parsingErrors.isEmpty()) {
            text
            .append("<h3 class=\"errors\">Parsing Errors</h3>\n")
            .append("<p class=\"errors\">This HTML generator encountered errors when parsing\n")
            .append("the source code for this plugin.</p>\n")
            .append("<table class=\"documentation\">\n")
            .append("<tbody>\n");

            for (String error : parsingErrors) {
                text
                .append("<tr><td class=\"errors\">\n")
                .append(error)
                .append("</td></tr>\n");
            }

            text
            .append("</tbody>\n")
            .append("</table>\n");
        }

        text.append("</div> <!-- elementdocumentation -->\n");
        return text.toString();
    }     
    
    /**
     * Generates the HTML text (without the <pre></pre> tags) to display the hierarchy of this
     * plugin's ancestry.
     * @return The formatted HTML text of this plugin's ancestry.
     */
    private String formatHtmlAncestry() {
        StringBuilder text = new StringBuilder();
        final String indentation = "  ";
        int level = 0;

        for (PluginInfo ancestor : getAncestry()) {
            // Indent each row.
            for (int i = 0; i < level; i++) {
                text.append(indentation);
            }

            // Generate a hyperlink for each ancestor that isn't the plugin itself.
            boolean notFinalPlugin = (ancestor != this);
            if (notFinalPlugin) {
                text.append("<a href=\"#").append(ancestor.getAncestralName()).append("\">");
            }
            text.append("&lt;");
            text.append(ancestor.getName());
            text.append("&gt;");
            if (notFinalPlugin) {
                text.append("</a>\n"); // If this is the final plugin, we don't need a newline.
            }

            level++;
        }

        return text.toString();
    }
    
    /**
     * Generates the HTML text to display the cardinality of an attribute or child.
     * @param member the item who's cardinality will be read.
     * @return The formatted HTML text representation of the member's cardinality.
     */
    private static String formatMemberCardinality(MemberInfo member) {
        final int min = member.getMinCardinality();
        final int max = member.getMaxCardinality();

        StringBuilder text = new StringBuilder();
        if (min == max) {
            text.append(min);
        } else {
            text.append(min);
            text.append("..");
            if (max == -1) {
                text.append("*");
            } else {
                text.append(max);
            }
        }

        return text.toString();
    }
    
    /**
     * Converts null Strings to empty Strings.
     * @param str Input string, which may be null.
     * @return Output string, which will be "" if the input was null.
     */
    private static String emptyIfNull(String str) {
        return (str == null) ? "" : str;
    }

    /**
     * Imposes an alphabetical by-name ordering on PluginInfo objects. See toString().
     * @param o Object to compare to.
     * @return Result of comparison.
     */
    public int compareTo(Object o) {
        // Sort plugins by name.
        return this.toString().compareTo(o.toString());
    }
    
    /**
     * Gets a string representation (i.e. the name) for this plugin. This is used to properly
     * implement compareTo(Object).
     * @return The plugin's name.
     */
    @Override
    public String toString() {
        return name;
    }

}
