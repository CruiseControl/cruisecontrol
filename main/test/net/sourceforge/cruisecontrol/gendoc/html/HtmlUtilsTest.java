package net.sourceforge.cruisecontrol.gendoc.html;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.gendoc.ChildInfo;
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;
import net.sourceforge.cruisecontrol.gendoc.PluginInfoParser;
import junit.framework.TestCase;

/**
 * @author Dan Rollo
 *         Date: Aug 8, 2010
 *         Time: 11:45:48 PM
 */
public class HtmlUtilsTest extends TestCase {

    private HtmlUtils htmlUtils;
    private PluginInfoParser parser;
    private String alphTOC;
    private String hierTOC;
    private List<String> alphTOCList = new ArrayList<String>();
    private List<String> hierTOCList = new ArrayList<String>();

    protected void setUp() throws Exception {
        htmlUtils = new HtmlUtils();

        parser = new PluginInfoParser(PluginRegistry.createRegistry(), PluginRegistry.ROOT_PLUGIN);

        alphTOC = htmlUtils.generateAlphabeticalToc(parser.getAllPlugins());
        hierTOC = htmlUtils.generateHierarchyToc(parser.getRootPlugin());

        parseTOC();
    }

    public void testAlphabeticalTOC() {
        for (PluginInfo s : parser.getAllPlugins()) {
            assertTrue(alphTOC.contains(
                    "<a href=\"#" + s.getAncestralName() + "\">&lt;" + s.getName() + "&gt;</a>"
            ));
        }
    }

    public void testHierarchicalTOC() {
        recursiveTestPluginInfo(parser.getRootPlugin(), null);
    }

    /**
     * Recursively tests the generated HTML for the hierarchical TOC.
     * 
     * @param node The current node being tested
     * @param parent The parent node to test
     */ 
    private void recursiveTestPluginInfo(PluginInfo node, PluginInfo parent) {
        if (parent == null || node.getDirectParent() == parent) { // Skip repeated children (null means root of tree)
            String nodeName = node.getName();

            StringBuilder w = new StringBuilder();
            
            w.append("<a href=\"#")
            .append(node.getAncestralName())
            .append("\">&lt;")
            .append(nodeName);

            HashSet<PluginInfo> children = checkChildren(node);
            if (node.getChildren().isEmpty()) {
                w.append("/");
            }
            w.append("&gt;</a>");
            
            if (!hierTOCList.contains(w.toString()))
            assertTrue(hierTOCList.contains(w.toString()));

            if (children != null) {
                for (PluginInfo pi : children) {
                    recursiveTestPluginInfo(pi, node);
                }
            }
            
            w = new StringBuilder();
            if (children != null) {
                w.append("&lt;").append("/").append(nodeName).append("&gt;");
                assertTrue(hierTOCList.contains(w.toString()));
            }
        }
    }

    /**
     * Returns a Set of all children nodes to be tested
     * 
     * @param node The Node whose children are to be tested
     * @return The Set containing all nodes to be tested
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
        return (mySet.size() == 0) ? null : mySet;
    }

    public void testGetReleaseVersion() {
        assertNotNull(htmlUtils.getReleaseVersion());
    }

    public void testEmptyIfNull() {
        assertEquals("", HtmlUtils.emptyIfNull(null));
        assertEquals("", HtmlUtils.emptyIfNull(""));
        assertEquals("a", HtmlUtils.emptyIfNull("a"));
    }

    private void parseTOC() {
        alphTOCList.clear();
        hierTOCList.clear();

        Scanner scan = new Scanner(alphTOC);
        while(scan.hasNextLine()) {
            String s = scan.nextLine().trim();
            if(s.startsWith("<a href=\"#")) {
                alphTOCList.add(s);
            }
        }

        scan = new Scanner(hierTOC);
        while(scan.hasNextLine()) {
            String s = scan.nextLine().trim();
            if(!s.equalsIgnoreCase("")) {
                hierTOCList.add(s);
            }
        }
    }
}
