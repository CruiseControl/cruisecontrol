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

import java.util.Collection;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.gendoc.testplugins.GoodRoot;

/**
 * Test for the methods of AttributeInfo. This also implicitly tests the parser that
 * generates AttributeInfo objects.
 * @author pollens@msoe.edu
 *         Date: 9/11/10
 */
public class AttributeInfoTest extends TestCase {

    private final PluginInfo goodRoot;
    private final PluginInfo goodChild;
    private final Collection<AttributeInfo> rootAttributes;
    private final Collection<AttributeInfo> childAttributes;
    
    public AttributeInfoTest() {
        goodRoot = GendocTestUtils.loadPluginInfo("goodroot", GoodRoot.class);
        goodChild = goodRoot.getChildPluginByName("goodchild");
        rootAttributes = goodRoot.getAttributes();
        childAttributes = goodChild.getAttributes();
    }
    
    public void testRootAttributeWithManyAnnotations() {
        AttributeInfo attr = goodRoot.getAttributeByName("1");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("1", attr.getName());
        assertEquals("H", attr.getDefaultValue());
        assertEquals("F", attr.getDescription());
        assertEquals(null, attr.getCardinalityNote());
        assertEquals("G", attr.getTitle());
        assertEquals(0, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testRootAttributeWithCardinalityNote() {
        AttributeInfo attr = goodRoot.getAttributeByName("2");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("2", attr.getName());
        assertEquals(null, attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals("I", attr.getCardinalityNote());
        assertEquals("2", attr.getTitle());
        assertEquals(1, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testRootAttributeWithRequired() {
        AttributeInfo attr = goodRoot.getAttributeByName("3");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("3", attr.getName());
        assertEquals("100", attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals(null, attr.getCardinalityNote());
        assertEquals("3", attr.getTitle());
        assertEquals(1, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testRootAttributeWithOptional() {
        AttributeInfo attr = goodRoot.getAttributeByName("4");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("4", attr.getName());
        assertEquals(null, attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals(null, attr.getCardinalityNote());
        assertEquals("4", attr.getTitle());
        assertEquals(0, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testRootAttributeWithRequiredNote() {
        AttributeInfo attr = goodRoot.getAttributeByName("5");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("5", attr.getName());
        assertEquals(null, attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals("J", attr.getCardinalityNote());
        assertEquals("5", attr.getTitle());
        assertEquals(1, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testRootAttributeWithOptionalNote() {
        AttributeInfo attr = goodRoot.getAttributeByName("6");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("6", attr.getName());
        assertEquals(null, attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals("K", attr.getCardinalityNote());
        assertEquals("6", attr.getTitle());
        assertEquals(0, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testRootAttributeWithNothing() {
        AttributeInfo attr = goodRoot.getAttributeByName("7");
        
        assertTrue(rootAttributes.contains(attr));
        assertEquals("7", attr.getName());
        assertEquals(null, attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals(null, attr.getCardinalityNote());
        assertEquals("7", attr.getTitle());
        assertEquals(0, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testChildAttributes() {
        // GoodChild just has a bunch of attributes to test the permitted
        // attribute types.
        
        // BOOLEAN types.
        for (String attrName : new String[] {
                "booleanp",
                "booleano"
        }) {
            AttributeInfo attr = goodChild.getAttributeByName(attrName);
            
            assertTrue(childAttributes.contains(attr));
            assertEquals(attrName, attr.getName());
            assertEquals(null, attr.getDefaultValue());
            assertEquals(null, attr.getDescription());
            assertEquals(null, attr.getCardinalityNote());
            assertEquals(attrName, attr.getTitle());
            assertEquals(0, attr.getMinCardinality());
            assertEquals(1, attr.getMaxCardinality());
            
            assertEquals(AttributeType.BOOLEAN, attr.getType());
        }
        
        // NUMBER types.
        for (String attrName : new String[] {
                "integerp",
                "integero",
                "longp",
                "longo",
                "shortp",
                "shorto",
                "bytep",
                "byteo",
                "floatp",
                "floato",
                "doublep",
                "doubleo"
        }) {
            AttributeInfo attr = goodChild.getAttributeByName(attrName);
            
            assertTrue(childAttributes.contains(attr));
            assertEquals(attrName, attr.getName());
            assertEquals(null, attr.getDefaultValue());
            assertEquals(null, attr.getDescription());
            assertEquals(null, attr.getCardinalityNote());
            assertEquals(attrName, attr.getTitle());
            assertEquals(0, attr.getMinCardinality());
            assertEquals(1, attr.getMaxCardinality());
            
            assertEquals(AttributeType.NUMBER, attr.getType());
        }
        
        // STRING types.
        AttributeInfo attr = goodChild.getAttributeByName("string");
        
        assertTrue(childAttributes.contains(attr));
        assertEquals("string", attr.getName());
        assertEquals(null, attr.getDefaultValue());
        assertEquals(null, attr.getDescription());
        assertEquals(null, attr.getCardinalityNote());
        assertEquals("string", attr.getTitle());
        assertEquals(0, attr.getMinCardinality());
        assertEquals(1, attr.getMaxCardinality());
        
        assertEquals(AttributeType.STRING, attr.getType());
    }
    
    public void testAttributeSorting() {
        AttributeInfo
            a = new AttributeInfo(),
            b = new AttributeInfo();
        
        a.setName("a");
        b.setName("b");
        
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
        assertTrue(a != b);
        assertEquals(1, a.compareTo(null));
    }
    
}

