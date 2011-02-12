package net.sourceforge.cruisecontrol.gendoc.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.gendoc.AttributeInfo;
import net.sourceforge.cruisecontrol.gendoc.ChildInfo;
import net.sourceforge.cruisecontrol.gendoc.MemberInfo;
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;
import net.sourceforge.cruisecontrol.gendoc.PluginInfoParser;

/**
 * @author Dan Rollo
 *         Date: Aug 8, 2010
 *         Time: 9:46:38 PM
 */
public class ConfigHtmlGeneratorTest extends TestCase {

    private ConfigHtmlGenerator configHtmlGenerator;
    private String html;
    
    private final List<String> head = new ArrayList<String>();
    private final List<String> body = new ArrayList<String>();
    private final List<String> errors = new ArrayList<String>();
    private final List<String> footer = new ArrayList<String>();
    
    private StringBuilder plugAttr;
    private StringBuilder plugChild;
    
    private HtmlUtils utils = new HtmlUtils();
    private PluginInfoParser parser;

    /** The static content present from the Velocity Template for testing purposes.*/
    private String[] BODY_CONTENT = {
            "<body>",
            "<div class=\"header\">",
            "<a name=\"top\"/>",
            "<div class=\"hostedby\">",
            "Hosted By:<br/>",
            "<a href=\"http://sourceforge.net\"><img src=\"http://sourceforge.net/sflogo.php?group_id=23523&amp;type=1\" width=\"88\" height=\"31\" alt=\"SourceForge\"/></a>",
            "</div>",
            "<img alt=\"CruiseControl\" src=\"http://cruisecontrol.sourceforge.net/banner.png\"/>",
            "</div> <!-- header -->",
            "<div class=\"container\">&nbsp;",
            "<div id=\"menu\">",
            "<ul id=\"menulist\">",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/index.html\">home</a></li>",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/download.html\">download</a></li>",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/license.html\">license</a></li>",
            "<li><h2>documentation</h2></li>",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/overview.html\">overview</a></li>",
            "<li><p id=\"menuselected\">config ref</p></li>",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/faq.html\">faq</a></li>",
            "<li><a class=\"external\" href=\"http://confluence.public.thoughtworks.org/display/CC/Home\">wiki</a></li>",
            "<li><h2>contributing</h2></li>",
            "<li><a class=\"expandmenu\" href=\"http://cruisecontrol.sourceforge.net/developers.html\">developers</a></li>",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/contact.html\">mailing lists</a></li>",
            "<li><a href=\"http://cruisecontrol.sourceforge.net/svn.html\">source repository</a></li>",
            "<li><p id=\"menubottom\">Release: " + utils.getReleaseVersion() + "</p></li>",
            "</ul>",
            "</div>",
            "<div class=\"content\">",
            "<h1><span class=\"printonly\">CruiseControl</span> Configuration Reference</h1>",

            "<p>CruiseControl configuration files are written in XML. This document",
            "describes the XML elements and attributes for a valid configuration",
            "file.</p>",

            "<p>The use of <a href=\"plugins.html\">plugins</a> means that other",
            "elements not documented here can also be used in the configuration.",
            "At a minimum, though, the config file contains a single top level",
            "<code>&lt;cruisecontrol&gt;</code> element, with one or more child",
            "<code>&lt;project&gt;</code> elements.</p>",

            "<!-- BEGIN INITIAL ERROR LIST -->"
    };

    private String[] HEAD_CONTENT = {
            "<head>",
            "<title>CruiseControl Configuration Reference</title>",
            "<link href=\"../cruisecontrol.css\" type=\"text/css\" rel=\"stylesheet\" />",
            "<link href=\"../configxml-gendoc.css\" type=\"text/css\" rel=\"stylesheet\" />",
            "<link href=\"../print.css\" type=\"text/css\" rel=\"stylesheet\" media=\"print\"/>",
            "<script type=\"text/javascript\" src=\"http://cruisecontrol.sourceforge.net/tables.js\"></script>",
            "</head>"
    };

    private final String[] FOOTER_CONTENT = {
            "<div class=\"elementdocumentation\">", 
            "<a class=\"toplink\" href=\"#top\">top</a>", 

            "<h2><a name=\"buildingpluginsthatneedexternallibs\">Building Plugins That Need External Libraries</a></h2>", 

            "<p>Several plugins require libraries to build that can't be distributed with CruiseControl. To use these",
            "plugins you'll need to acquire the required libraries and build them. The source for these plugins is",
            "available in the source distribution or in the repository at <code>cruisecontrol/contrib/plugin/</code>.</p>", 

            "<p>To build the plugin you'll need to put the required jars for the plugin into the <code>[name]/lib</code> directory",
            "then run the build file <code>[name]/build.xml</code>. After you've built the plugin you'll need to move all the files",
            "in <code>[name]/target/dist</code> into the library path of CruiseControl &mdash; which means just copying them to the",
            "<code>cruisecontrol/lib</code> &mdash; and restart the server.</p>", 
            "</div>", 

            "</div> <!-- content -->",
            "</div> <!-- container -->",
            "</body>",
            "</html>"
    };

    /**
     * Generates the HTML and parses it for testing purposes.
     */
    protected void setUp() throws Exception {        
        configHtmlGenerator = new ConfigHtmlGenerator();
        parser = new PluginInfoParser(
                PluginRegistry.createRegistry(), PluginRegistry.ROOT_PLUGIN);
        html = configHtmlGenerator.generate(parser).trim();

        parseHTML();
    }

    /**
     * Tests that generating on a null reference generates an Exception
     * @throws Exception if test fails
     */
    public void testGenerateWithNull() throws Exception {
        try  {
            configHtmlGenerator.generate(null);
            fail("should fail with null parser");
        } catch (NullPointerException e) {
            assertNull(e.getMessage());
        }
    }

    /**
     * Tests the static content at the end of the HTML file
     * @throws Exception if test fails
     */
    public void testFooterContent() throws Exception {             
        assertEquals(FOOTER_CONTENT.length, footer.size());
        for (String s : FOOTER_CONTENT) {
            assertTrue(s + " not found", footer.contains(s));
        }
    }

    /**
     * Tests the static content at the beginning of the HTML file
     */
    public void testStartContent() {
        assertTrue(html.startsWith(
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html>"));
    }

    /**
     * Tests the static head content at beginning of the HTML file
     */
    public void testHeadContent() {
        assertEquals(HEAD_CONTENT.length, head.size());
        for (String s : HEAD_CONTENT) {
            assertTrue(s + " not found", head.contains(s));
        }
    }

    /**
     * Tests the static content present in the body of the HTML file
     */
    public void testBodyContent() {
        assertEquals(BODY_CONTENT.length, body.size());
        for (String s : BODY_CONTENT) {
            assertTrue(s + " not found", body.contains(s));   
        }
    }

    /**
     * Verifies no parsing errors are present, or else fails the tests
     */
    public void testErrorList() {
        assertEquals(errors.size(), 1);
    }

    /**
     * Tests the content generated by Gendoc for each plugin.
     */
    public void testGeneratedContent() {
        for (PluginInfo s : parser.getAllPlugins()) {
            parsePluginHtml(s.getHtmlDocumentation());
            checkAttributes(s);
            checkChildren(s);
        }
    }

    /**
     * Checks the given PluginInfo's attribute data against the generated html
     * 
     * @param info PluginInfo object to test
     */
    private void checkAttributes(PluginInfo info) {
        Scanner scan = new Scanner(plugAttr.toString());
        while(scan.hasNextLine()) {
            String s = scan.nextLine().trim();
            if(s.equalsIgnoreCase("<tbody>")) {
                
                while(scan.hasNextLine()) { 
                    s = scan.nextLine().trim();
                    
                    if(s.equalsIgnoreCase("<tr>")) {
                        String attribute = scan.nextLine().trim();
                        String required = scan.nextLine().trim();
                        String defaultValue = scan.nextLine().trim();
                        String description = scan.nextLine().trim();
                        
                        String name = attribute.substring(4, attribute.length() - 5);
                        AttributeInfo ai = info.getAttributeByName(name);
                        
                        assertNotNull(ai);
                        assertEquals(formatReq(ai), required);
                        assertEquals("<td>" + HtmlUtils.emptyIfNull(ai.getDefaultValue()) + "</td>", defaultValue);
                        assertEquals("<td>" + HtmlUtils.emptyIfNull(ai.getDescription()) + "</td>", description);
                    }
                }
            }
        }
    }
    
    /**
     * @return Formats the required for testing purposes.
     * @param member MemberInfo to test
     */
    private String formatReq(MemberInfo member) {
        StringBuilder text = new StringBuilder();
        text.append(
                (member.getMinCardinality() > 0) ? "<td><b>Required</b>" : "<td>Optional");
        
        String note = member.getCardinalityNote();
        if (note != null) {
            text.append(". ").append(note);
        }
        
        text.append("</td>");
        
        return text.toString();
    }
    
    /**
     * @return Formats the Attribute Cardinality for Testing Purposes.
     * @param member the item who's cardinality will be read.
     */
    private String formatCard(MemberInfo member) {
        final int min = member.getMinCardinality();
        final int max = member.getMaxCardinality();
        StringBuilder text = new StringBuilder();
        
        text.append("<td>");
        
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
        
        text.append("</td>");
        return text.toString();
    }
    
    private void checkChildren(PluginInfo info) {
        List<String> childElements = new ArrayList<String>();
        List<String> childRequired = new ArrayList<String>();
        List<String> childCardinality = new ArrayList<String>();
        List<String> childDescription = new ArrayList<String>();
        
        for(ChildInfo childNode : info.getChildren()) {
            for (PluginInfo childInfoNode : childNode.getAllowedNodes()) {
                StringBuilder text = new StringBuilder();
                text
                .append("<a href=\"#")
                .append(childInfoNode.getAncestralName())
                .append("\">&lt;")
                .append(childInfoNode.getName())
                .append("&gt;</a><br/>");
                
                childElements.add(text.toString());
            }
            childRequired.add(formatReq(childNode));
            childCardinality.add(formatCard(childNode));
            childDescription.add("<td>" + HtmlUtils.emptyIfNull(childNode.getDescription()) + "</td>");
        }
        
        Scanner scan = new Scanner(plugChild.toString());
        while(scan.hasNextLine()) {
            String s = scan.nextLine().trim();
            if(s.equalsIgnoreCase("<tbody>")) {
                
                while(scan.hasNextLine()) { 
                    s = scan.nextLine().trim();
                    if(s.equalsIgnoreCase("<tr>")) {
                        scan.nextLine(); //<td>
                        List<String> elements = new ArrayList<String>();
                        
                        while(!(s = scan.nextLine().trim()).equalsIgnoreCase("</td>")) {
                            elements.add(s);
                        }
                        
                        String required = scan.nextLine().trim();
                        String cardinality = scan.nextLine().trim();
                        String description = scan.nextLine().trim();
                        
                        assertTrue(childElements.containsAll(elements));
                        assertTrue(required + " not found", childRequired.contains(required));
                        assertTrue(cardinality + " not found", childCardinality.contains(cardinality));
                        assertTrue(description + " not found", childDescription.contains(description));
                    }
                }
            }
        }
    }

    /**
     * Parses attribute and child documentation for a plugin from HTML.
     * @param html HTML of the plugin to be parsed.
     */
    private void parsePluginHtml(String html) {
        plugAttr = new StringBuilder();
        plugChild = new StringBuilder();
        Scanner scan = new Scanner(html);
        
        while(scan.hasNextLine()) {
            String s = scan.nextLine().trim();
            if(s.equalsIgnoreCase("<h3>Attributes</h3>")) {
                
                while(scan.hasNextLine()) {
                    s = scan.nextLine().trim();
                    if(s.equalsIgnoreCase("</div> <!-- elementdocumentation -->")){
                        break;
                        
                    } else if(s.equalsIgnoreCase("<h3>Child Elements</h3>")) {
                        
                        while(scan.hasNextLine()) {
                            s = scan.nextLine().trim();
                            if(s.equalsIgnoreCase("</div> <!-- elementdocumentation -->")) {
                                break;
                            } else {
                                if(!s.isEmpty()) {
                                    plugChild.append(s);
                                    plugChild.append("\n");
                                }
                            }
                        }
                        break;
                        
                    } else {
                        if(!s.equalsIgnoreCase("")) {
                            plugAttr.append(s);
                            plugAttr.append("\n");
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses the HTML content into different ArrayLists for testing purposes.
     */
    private void parseHTML() {
        head.clear();
        body.clear();
        errors.clear();
        footer.clear();
        
        Scanner scan = new Scanner(html);
        while(scan.hasNextLine()) {
            String s = scan.nextLine().trim();

            if(s.equals("<head>")){
                head.add(s);
                
                while(scan.hasNextLine()) {
                    s = scan.nextLine().trim();
                    if(!s.equals("")) {
                        head.add(s);
                    }
                    if(s.equals("</head>")) {
                        break;
                    }
                }
                
            } else if(s.equals("<body>")){
                body.add(s);
                
                while(scan.hasNextLine()) {
                    s = scan.nextLine().trim();
                    if(!s.equals("")) {
                        body.add(s);                        
                    }
                    if (s.equals("<!-- BEGIN INITIAL ERROR LIST -->")) {
                        
                        while(scan.hasNextLine()) {
                            s = scan.nextLine().trim();
                            if(!s.equals("")) {
                                errors.add(s);
                            }
                            if (s.equals("<!-- BEGIN TABLE OF CONTENTS -->")) {
                                
                                while(scan.hasNextLine()) {
                                    s = scan.nextLine().trim();
                                    if(s.equals("<!-- BEGIN CONTENTS -->")) {
                                        
                                        while(scan.hasNextLine()) {
                                            s = scan.nextLine().trim();
                                            if(s.equals("<!-- BEGIN FOOTER -->")) {
                                                
                                                while(scan.hasNextLine()) {
                                                    s = scan.nextLine().trim();
                                                    if(!s.equals("")) {
                                                        footer.add(s);
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
