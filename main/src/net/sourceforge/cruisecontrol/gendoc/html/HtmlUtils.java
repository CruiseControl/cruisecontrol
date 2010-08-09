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
package net.sourceforge.cruisecontrol.gendoc.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import net.sourceforge.cruisecontrol.gendoc.ChildInfo;
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;
import org.apache.log4j.Logger;

/**
 * Contains utilities for writing out the HTML for the plugin page.
 * @author Seth Pollen (pollens@msoe.edu)
 */
public class HtmlUtils {

    private static final Logger LOG = Logger.getLogger(HtmlUtils.class);

    /** Line count for the # lines in the generated hierarchical TOC*/
    private int count;

    /** The release version Information */
    private final String releaseVersion;

    public HtmlUtils() {

        final String tmpVersion = readVerFromManifest();
        if (tmpVersion != null) {
            releaseVersion = tmpVersion;
        } else {
            releaseVersion = readVerFromBuildProps();
        }
    }

    static String readVerFromBuildProps() {
        // should only need this when running from source tree.
        
        File buildPropFile = new File("build.properties");
        if (!buildPropFile.exists()) {
            buildPropFile = new File("../../build.properties");
        }
        final Properties buildProps = new Properties();
        try {
            buildProps.load(new BufferedReader(new FileReader(buildPropFile)));
        } catch (FileNotFoundException e) {
            LOG.debug("Build props not found.", e);
        } catch (IOException e) {
            LOG.debug("IOError reading build props.", e);
        }

        return buildProps.getProperty("cc.version");
    }

    static String readVerFromManifest() {
        // This should work when running from jars.

        final Package pkg = HtmlUtils.class.getPackage();
        if (pkg != null) {
            return pkg.getImplementationVersion();
        }
        return null;
    }

    public String getRelease() {
        return releaseVersion;
    }

    /**
     * Method that generates the Hierarchical TOC
     * 
     * @param info The PluginInfo tree
     * @return The HTML generated for the hierarchical TOC
     */
    public String generateHierarchyToc(PluginInfo info) {
        StringWriter w = new StringWriter();
        try {
            writePluginInfo(info, null, w, 0, "  ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return w.getBuffer().toString();
    }
    
    /**
     * Writes the appropriate HTML to the given Writer
     * 
     * @param node The current node
     * @param parent The node who this node is a child of (null if root)
     * @param w The Writer to write to
     * @param depth The current depth within the tree
     * @param indentationStr The indentation ammount to use
     * @throws IOException In case of Writer error
     */ 
    private void writePluginInfo(PluginInfo node, PluginInfo parent, Writer w,
            int depth, String indentationStr) throws IOException {
        
        if (parent == null || node.getDirectParent() == parent) { // Skip repeated children (null means root of tree)
            count++;
            String nodeName = node.getName();
            indent(w, depth, indentationStr);

            w.append("<a href=\"#");
            w.append(node.getAncestralName());
            w.append("\">&lt;");
            w.append(nodeName);
            
            HashSet<PluginInfo> children = checkChildren(node);
            if (children == null) {
                w.append("/");
            }
            w.append("&gt;</a>\n");
            
            if (children != null) {
                for (PluginInfo pi : children) {
                    writePluginInfo(pi, node, w, depth + 1, indentationStr);
                }
            }

            if (children != null) {
                count++;
                indent(w, depth, indentationStr);
                w.append("&lt;").append("/").append(nodeName).append("&gt;\n");
            }
        }
    }
    
    /**
     * Returns a Set of all Nodes to be displayed, null if there are none.
     * 
     * @param node The Node whose children are to be checked
     * @return The Set containing all nodes to be displayed
     */
    private HashSet<PluginInfo> checkChildren(PluginInfo node) {
        HashSet<PluginInfo> mySet = new HashSet<PluginInfo>();
        if (node.getChildren().size() > 0) {
            for (ChildInfo ci : node.getChildren()) {
                for (PluginInfo pi : ci.getAllowedNodes()) {
                    if (pi.getDirectParent() == node) {
                        mySet.add(pi);
                    }
                }
            }  
        }
        if (mySet.size() == 0) {
            mySet = null;
        }
        return mySet;
    }
    
    
    /**
     * Indents the writer by the appropriate amount
     * 
     * @param w The Writer to indent
     * @param depth The current depth
     * @param indentationChar The indentation amount
     * @throws IOException In case of Writer error
     */
    private void indent(Writer w, int depth, String indentationChar) throws IOException {
        for (int i = 0; i < depth; i++) {
            w.append(indentationChar);
        }
    }

    /**
     * Method that generates the Alphabetical TOC
     * 
     * @param info The PluginInfo tree
     * @return The HTML generated for the Alphabetical TOC
     */
    public String generateAlphabeticalToc(List<PluginInfo> info) {
        StringBuilder toc = new StringBuilder();
        for (PluginInfo pi : info) {
            toc
            .append("<a href=\"#")
            .append(pi.getAncestralName())
            .append("\">&lt;")
            .append(pi.getName())
            .append("&gt;</a>\n");
        }
        return toc.toString();
    }

    /**
     * Returns the number of lines in the hierarchical TOC
     * 
     * @return The number of lines in the hierarchical TOC
     */
    public int getLineCount() {
        return count;
    }
}
