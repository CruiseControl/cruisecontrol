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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
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

    private static String readVerFromBuildProps() {
        // Should only need this when running from source tree. Search upwards
        // for build.properties.
        String fileName = "build.properties";
        File dir = new File(System.getProperty("user.dir"));
        File buildPropFile;
        while (true) {
            buildPropFile = new File(dir, fileName);
            if (buildPropFile.exists()) {
                break;
            } else {
                // Move up one level in the directory structure and check again.
                dir = dir.getParentFile();
                if (dir == null) {
                    // We have run out of places to search.
                    LOG.debug("Build props not found.");
                    return "UNKNOWN";
                }
            }
        }
        
        final Properties buildProps = new Properties();
        try {
            // @todo Restore use of Reader after minimum JDK >= 1.6.
            //buildProps.load(new BufferedReader(new FileReader(buildPropFile)));
            buildProps.load(new BufferedInputStream(new FileInputStream(buildPropFile)));
        } catch (FileNotFoundException e) {
            LOG.debug("Build props not found.", e);
            return "UNKNOWN";
        } catch (IOException e) {
            LOG.debug("IOError reading build props.", e);
            return "UNKNOWN";
        }

        return buildProps.getProperty("cc.version");
    }

    private static String readVerFromManifest() {
        // This should work when running from jars.

        final Package pkg = HtmlUtils.class.getPackage();
        if (pkg != null) {
            return pkg.getImplementationVersion();
        }
        return null;
    }

    public String getReleaseVersion() {
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
            
            if (node.getChildren().isEmpty()) {
                w.append("/");
            }
            w.append("&gt;</a>\n");
            
            List<PluginInfo> childrenToPrint = getChildrenToPrint(node);
            for (PluginInfo pi : childrenToPrint) {
                writePluginInfo(pi, node, w, depth + 1, indentationStr);
            }

            if (!childrenToPrint.isEmpty()) {
                count++;
                indent(w, depth, indentationStr);
                w.append("&lt;").append("/").append(nodeName).append("&gt;\n");
            }
        }
    }
    
    /**
     * Returns a List of all Nodes to be displayed under a given node in the
     * hierarchical TOC.
     * 
     * @param node The Node whose children are to be checked
     * @return The List containing all nodes to be displayed. This may be an
     *         empty set.
     */
    private List<PluginInfo> getChildrenToPrint(PluginInfo node) {
        // To make sure each node appears in the TOC only once, only return the
        // nodes that consider this node to be their one and only direct parent.
        List<PluginInfo> list = new ArrayList<PluginInfo>();
        for (ChildInfo ci : node.getChildren()) {
            for (PluginInfo pi : ci.getAllowedNodes()) {
                if (pi.getDirectParent() == node) {
                    list.add(pi);
                }
            }
        }
        return list;
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
     * // @todo Not used. Make package visible, private, or remove?
     * @return The number of lines in the hierarchical TOC
     */
    public int getLineCount() {
        return count;
    }
    
    /**
     * Converts null Strings to empty Strings.
     * @param str Input string, which may be null.
     * @return Output string, which will be "" if the input was null.
     */
    public static String emptyIfNull(String str) {
        return (str == null) ? "" : str;
    }
    
}
