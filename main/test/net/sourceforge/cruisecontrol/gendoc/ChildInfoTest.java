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
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.gendoc.testplugins.GoodRoot;

/**
 * Test for the methods of ChildInfo. This also implicitly tests the parser that
 * generates ChildInfo objects.
 * @author pollens@msoe.edu
 *         Date: 9/11/10
 */
public class ChildInfoTest extends TestCase {

    private final PluginInfo goodRoot;
    private final PluginInfo goodChild;
    private final List<ChildInfo> rootChildren;
    private final List<ChildInfo> childChildren;
    
    public ChildInfoTest() {
        goodRoot = GendocTestUtils.loadPluginInfo("goodroot", GoodRoot.class);
        goodChild = goodRoot.getChildPluginByName("goodchild");
        rootChildren = goodRoot.getChildren();
        childChildren = goodChild.getChildren();
    }
    
    public void testRootChildWithManyAnnotations() {
        ChildInfo child = goodRoot.getChildByPluginName("goodchild");
        PluginInfo plugin = goodRoot.getChildPluginByName("goodchild");
        
        assertEquals("goodchild", plugin.getName());
        assertTrue(rootChildren.contains(child));
        
        List<PluginInfo> allowedNodes = child.getAllowedNodes();
        assertEquals(1, allowedNodes.size());
        assertTrue(allowedNodes.contains(plugin));
        
        assertEquals(plugin, child.getAllowedNodeByName("goodchild"));
        assertEquals("C", child.getDescription());
        assertEquals("D", child.getTitle());
        assertEquals(null, child.getCardinalityNote());
        assertEquals(1, child.getMinCardinality());
        assertEquals(15, child.getMaxCardinality());
    }
    
    public void testRootChildWithRename() {
        ChildInfo child = goodRoot.getChildByPluginName("goodchild2");
        PluginInfo plugin = goodRoot.getChildPluginByName("goodchild2");
        
        assertEquals("goodchild2", plugin.getName());
        assertTrue(rootChildren.contains(child));
        
        List<PluginInfo> allowedNodes = child.getAllowedNodes();
        assertEquals(1, allowedNodes.size());
        assertTrue(allowedNodes.contains(plugin));
        
        assertEquals(plugin, child.getAllowedNodeByName("goodchild2"));
        assertEquals(null, child.getDescription());
        assertEquals(null, child.getTitle());
        assertEquals(null, child.getCardinalityNote());
        assertEquals(0, child.getMinCardinality());
        assertEquals(-1, child.getMaxCardinality());
    }
    
    public void testRootChildWithCardinalityNote() {
        ChildInfo child = goodRoot.getChildByPluginName("goodchild3");
        PluginInfo plugin = goodRoot.getChildPluginByName("goodchild3");
        
        assertEquals("goodchild3", plugin.getName());
        assertTrue(rootChildren.contains(child));
        
        List<PluginInfo> allowedNodes = child.getAllowedNodes();
        assertEquals(1, allowedNodes.size());
        assertTrue(allowedNodes.contains(plugin));
        
        assertEquals(plugin, child.getAllowedNodeByName("goodchild3"));
        assertEquals(null, child.getDescription());
        assertEquals(null, child.getTitle());
        assertEquals("E", child.getCardinalityNote());
        assertEquals(2, child.getMinCardinality());
        assertEquals(-1, child.getMaxCardinality());
    }
    
    public void DONTtestRootChildRecursive() {
        ChildInfo child = goodRoot.getChildByPluginName("recursivechild");
        PluginInfo plugin = goodRoot.getChildPluginByName("recursivechild");
        
        assertEquals("recursivechild", plugin.getName());
        assertTrue(rootChildren.contains(child));
        
        List<PluginInfo> allowedNodes = child.getAllowedNodes();
        assertEquals(1, allowedNodes.size());
        assertTrue(allowedNodes.contains(plugin));
        
        assertEquals(plugin, child.getAllowedNodeByName("recursivechild"));
        assertEquals("More Text", child.getDescription());
        assertEquals(null, child.getTitle());
        assertEquals(null, child.getCardinalityNote());
        assertEquals(0, child.getMinCardinality());
        assertEquals(-1, child.getMaxCardinality());
    }
    
    public void DONTtestRootChildWithManySubclasses() {
        // Use the <svn> and <cvs> plugins for testing.
        
        ChildInfo child = goodRoot.getChildByPluginName("svn");
        ChildInfo child2 = goodRoot.getChildByPluginName("cvs");
        PluginInfo plugin1 = goodRoot.getChildPluginByName("svn");
        PluginInfo plugin2 = goodRoot.getChildPluginByName("cvs");
        
        assertEquals(child, child2);
        assertTrue(plugin1 != plugin2);
        assertEquals("svn", plugin1.getName());
        assertEquals("cvs", plugin2.getName());
        assertTrue(rootChildren.contains(child));
        
        List<PluginInfo> allowedNodes = child.getAllowedNodes();
        assertTrue(allowedNodes.size() > 2); // There are more SourceControl implementations that we didn't hard-code into this test.
        assertTrue(allowedNodes.contains(plugin1));
        assertTrue(allowedNodes.contains(plugin2));
        
        assertEquals(plugin1, child.getAllowedNodeByName("svn"));
        assertEquals(plugin2, child.getAllowedNodeByName("cvs"));
        assertEquals("TEXT", child.getDescription());
        assertEquals(null, child.getTitle());
        assertEquals(null, child.getCardinalityNote());
        assertEquals(0, child.getMinCardinality());
        assertEquals(-1, child.getMaxCardinality());
    }
    
    public void testChildChild() {
        ChildInfo child = goodChild.getChildByPluginName("recursivechild");
        PluginInfo plugin = goodChild.getChildPluginByName("recursivechild");
        
        assertEquals("recursivechild", plugin.getName());
        assertTrue(childChildren.contains(child));
        
        List<PluginInfo> allowedNodes = child.getAllowedNodes();
        assertEquals(1, allowedNodes.size());
        assertTrue(allowedNodes.contains(plugin));
        
        assertEquals(plugin, child.getAllowedNodeByName("recursivechild"));
        assertEquals("TEXT", child.getDescription());
        assertEquals(null, child.getTitle());
        assertEquals(null, child.getCardinalityNote());
        assertEquals(0, child.getMinCardinality());
        assertEquals(-1, child.getMaxCardinality());
    }
    
    public void testFetchingWrongName() {
        ChildInfo child = goodRoot.getChildByPluginName("goodchild");
        assertNull(child.getAllowedNodeByName("doesn't exist"));
    }
    
}

