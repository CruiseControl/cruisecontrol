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

import net.sourceforge.cruisecontrol.gendoc.html.HtmlUtils;

/**
 * This class represents an Child of a Plugin
 * The setter methods of this class are package-private to prevent modification
 * after the class has been constructed.
 * 
 * @author Anthony Love (lovea@msoe.edu)
 * @version 1.0
 */
public class ChildInfo extends MemberInfo implements Serializable {

    /** ID for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The (eventually sorted) List of allowable child XML Nodes */
    private final List<PluginInfo> allowedNodes = new ArrayList<PluginInfo>();
    
    /**
     * Creates a Child with all fields defaulted.
     */
    public ChildInfo() {
        // Empty.
    }
    
    /**
     * Creates a pre-populated ChildInfo. This can be used to create mock objects
     * for testing.
     * @param description Description.
     * @param title Title.
     * @param minCardinality Minimum cardinality.
     * @param maxCardinality Maximum cardinality.
     * @param cardinalityNote Cardinality note.
     * @param allowedNodes Array of allowed PluginInfos.
     */
    public ChildInfo(
            String description,
            String title,
            int minCardinality,
            int maxCardinality,
            String cardinalityNote,
            PluginInfo[] allowedNodes
    ) {
        super(description, title, minCardinality, maxCardinality, cardinalityNote);
        
        for (PluginInfo allowedNode : allowedNodes) {
            addAllowedNode(allowedNode);
        }
        
        sortAllowedNodes();
    }
    
    /**
     * Adds a plugin as the parent to all the child plugins contained in this ChildInfo object.
     * @param p The parent plugin to add.
     */
    void addParent(PluginInfo p) {
        for (PluginInfo node : allowedNodes) {
            node.addParent(p);
        }
    }
    
    /**
     * Returns the List of allowed XML Nodes
     * 
     * @return The List of XML Nodes
     */
    public List<PluginInfo> getAllowedNodes() {
        return allowedNodes;
    }
    
    /**
     * Gets the information for an allowed node for this child, given the node's name.
     * @param name Name of the plugin node to get.
     * @return The requested node's PluginInfo object, if it is an allowed node for this child.
     *         Otherwise, null is returned.
     */
    public PluginInfo getAllowedNodeByName(String name) {
        // Assume the list has been sorted.
        int index = Collections.binarySearch(allowedNodes, name);
        if (index < 0) {
            return null;
        } else {
            return allowedNodes.get(index);
        }
    }
    
    /**
     * Adds the given Node to the List of allowed Nodes
     * 
     * @param node The node to add
     */
    void addAllowedNode(PluginInfo node) {
        allowedNodes.add(node);
    }
    
    /**
     * Sorts the allowed nodes in this child by name.
     */
    void sortAllowedNodes() {
        Collections.sort(allowedNodes);
    }
    
    /**
     * Generates the HTML documentation for this child.
     * @param text Text buffer to write to.
     */
    void writeHtml(StringBuilder text) {
        text
        .append("<tr>\n")
        .append("<td>\n");

        for (PluginInfo childNode : getAllowedNodes()) {
            text
            .append("<a href=\"#")
            .append(childNode.getAncestralName())
            .append("\">&lt;")
            .append(childNode.getName())
            .append("&gt;</a><br/>\n");
        }

        text.append("</td>\n");
        
        writeMemberRequired(text);
        writeMemberCardinality(text);
        
        text
        .append("<td>")
        .append(HtmlUtils.emptyIfNull(getDescription()))
        .append("</td>\n")
        .append("</tr>\n");
    }
    
}
