/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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

import java.util.List;
import net.sourceforge.cruisecontrol.gendoc.testplugins.BadRoot;
import net.sourceforge.cruisecontrol.gendoc.testplugins.GoodChild;
import net.sourceforge.cruisecontrol.gendoc.testplugins.GoodRoot;
import net.sourceforge.cruisecontrol.gendoc.testplugins.RecursiveChild;
import junit.framework.TestCase;

/**
 * Test for the methods of PluginInfo. This also implicitly tests the parser that
 * generates PluginInfo objects.
 * @author pollens@msoe.edu
 *         Date: 9/11/10
 */
public class PluginInfoTest extends TestCase {
    
    private final PluginInfo goodRoot;
    private final PluginInfo goodChild;
    private final PluginInfo goodChild2;
    private final PluginInfo goodChild3;
    private final PluginInfo recursiveChild;
    private final PluginInfo badRoot;
    private static final String PATH_SEPARATOR = "::";
    
    public PluginInfoTest() {
        goodRoot = GendocTestUtils.loadPluginInfo("goodroot", GoodRoot.class);
        goodChild = goodRoot.getChildPluginByName("goodchild");
        goodChild2 = goodRoot.getChildPluginByName("goodchild2");
        goodChild3 = goodRoot.getChildPluginByName("goodchild3");
        recursiveChild = goodRoot.getChildPluginByName("recursivechild");
        badRoot = GendocTestUtils.loadPluginInfo("badroot", BadRoot.class);
    }
    
    public void testRootFields() {
        assertEquals("<p>A</p>", goodRoot.getDescription());
        assertEquals("goodroot", goodRoot.getName());
        assertEquals("B", goodRoot.getTitle());
        assertEquals(GoodRoot.class.getName(), goodRoot.getClassName());
    }
    
    public void testRootAncestry() {
        assertEquals("goodroot", goodRoot.getAncestralName());
        assertEquals(null, goodRoot.getDirectParent());
        assertEquals(0, goodRoot.getAllParents().size());
        
        List<PluginInfo> ancestry = goodRoot.getAncestry();
        assertEquals(1, ancestry.size());
        assertEquals(goodRoot, ancestry.get(0));
    }
    
    public void testRootAttributeFetching() {
        List<AttributeInfo> attributes = goodRoot.getAttributes();
        assertEquals(7, attributes.size());
        
        for (String attrName : new String[] {
                "1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "7"
        }) {
            AttributeInfo attrInfo = goodRoot.getAttributeByName(attrName);
            
            assertTrue(attributes.contains(attrInfo));
            assertEquals(attrName, attrInfo.getName());
        }
    }
    
    public void testRootChildFetching() {
        List<ChildInfo> children = goodRoot.getChildren();
        assertEquals(5, children.size());
        
        for (String pluginName : new String[] {
                "goodchild",
                "goodchild2",
                "goodchild3",
                "recursivechild",
                "svn" // An example SourceControl plugin.
        }) {
            ChildInfo child = goodRoot.getChildByPluginName(pluginName);
            PluginInfo plugin = goodRoot.getChildPluginByName(pluginName);
            
            assertTrue(children.contains(child));
            assertEquals(pluginName, plugin.getName());
            assertTrue(child.getAllowedNodes().contains(plugin));
        }
    }
    
    public void testGoodChildFields() {
        assertEquals("<p>123</p>", goodChild.getDescription());
        assertEquals("goodchild", goodChild.getName());
        assertEquals("goodchild", goodChild.getTitle());
        assertEquals(GoodChild.class.getName(), goodChild.getClassName());
    }
    
    public void testGoodChildAncestry() {
        assertEquals("goodroot"+ PATH_SEPARATOR + "goodchild", goodChild.getAncestralName());
        assertEquals(goodRoot, goodChild.getDirectParent());
        assertEquals(1, goodChild.getAllParents().size());
        assertTrue(goodChild.getAllParents().contains(goodRoot));
        
        List<PluginInfo> ancestry = goodChild.getAncestry();
        assertEquals(2, ancestry.size());
        assertEquals(goodRoot, ancestry.get(0));
        assertEquals(goodChild, ancestry.get(1));
    }
    
    public void testGoodChildAttributeFetching() {
        List<AttributeInfo> attributes = goodChild.getAttributes();
        assertEquals(15, attributes.size());
        
        // Leave other checks for the AttributeInfoTest.
    }
    
    public void testRecursiveChildFields() {
        assertNull(recursiveChild.getDescription());
        assertEquals("recursivechild", recursiveChild.getName());
        assertEquals("recursivechild", recursiveChild.getTitle());
        assertEquals(RecursiveChild.class.getName(), recursiveChild.getClassName());
    }
    
    public void testRecursiveChildAncestry() {
        assertEquals("goodroot" + PATH_SEPARATOR + "recursivechild", recursiveChild.getAncestralName());
        assertEquals(goodRoot, recursiveChild.getDirectParent());
        
        List<PluginInfo> parents = recursiveChild.getAllParents();
        assertEquals(5, parents.size());
        assertTrue(parents.contains(goodRoot));
        assertTrue(parents.contains(goodChild));
        assertTrue(parents.contains(goodChild2));
        assertTrue(parents.contains(goodChild3));
        assertTrue(parents.contains(recursiveChild));
        
        List<PluginInfo> ancestry = recursiveChild.getAncestry();
        assertEquals(2, ancestry.size());
        assertEquals(goodRoot, ancestry.get(0));
        assertEquals(recursiveChild, ancestry.get(1));
    }
    
    public void testRecursiveChildAttributeFetching() {
        List<AttributeInfo> attributes = recursiveChild.getAttributes();
        assertEquals(0, attributes.size());
    }
    
    public void testRecursiveChildReuse() {
        assertEquals(recursiveChild, goodChild.getChildPluginByName("recursivechild"));
        assertEquals(recursiveChild, recursiveChild.getChildPluginByName("recursivechild"));
    }
    
    public void testParsingErrors() {
        assertEquals(0, goodRoot.getParsingErrors().size());
        assertEquals(0, goodChild.getParsingErrors().size());
        
        // For the BadRoot, there should be 2 successfully parsed attributes and
        // 1 successfully parsed child. Everything else should have errored out.
        assertEquals(1, badRoot.getAttributes().size());
        assertNotNull(badRoot.getAttributeByName("ok"));        
        assertEquals(1, badRoot.getChildren().size());
        assertNotNull(badRoot.getChildByPluginName("svn")); // Verify there is a sourcecontrol plugin.
        
        // Verify that the number of parsing errors is at least as great as the number
        // of bad methods we put in the BadRoot class.
        assertTrue(badRoot.getParsingErrors().size() >= 12);
        
        // Make sure the HTML parsing errors were noted.
        boolean descriptionError = false;
        boolean cardinalityError = false;
        for(String s : badRoot.getParsingErrors()) {
            System.out.println(s);
            if (s.contains("Invalid XML content in description")) {
                descriptionError = true;
            } else if (s.contains("Invalid XML content in cardinality")) {
                cardinalityError = true;
            }
        }
        assertTrue("HTML description error message not found", descriptionError);
        assertTrue("HTML cardinality error message not found", cardinalityError);
    }
    
    public void testPluginSorting() {
        PluginInfo
            a = new PluginInfo("z"),
            b = new PluginInfo("y");
        
        a.setName("a");
        b.setName("b");
        
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
        assertTrue(a != b);
        assertEquals(1, a.compareTo(null));
    }
    
    public void testFetchingWrongName() {
        assertNull(goodRoot.getAttributeByName("doesn't exist"));
        assertNull(goodRoot.getChildByPluginName("doesn't exist"));
        assertNull(goodRoot.getChildPluginByName("doesn't exist"));
    }
    
}
