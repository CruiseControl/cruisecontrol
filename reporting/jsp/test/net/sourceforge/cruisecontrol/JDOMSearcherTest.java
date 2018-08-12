/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import junit.framework.TestCase;

public class JDOMSearcherTest extends TestCase {
    private Document doc;
    private Element root = new Element("root");
    private Element firstChild = new Element("first-child");
    private Element subChild = new Element("sub-child");

    protected void setUp() throws Exception {
        super.setUp();

        root.addContent(firstChild);
        firstChild.addContent(subChild);
        doc = new Document(root);
    }

    public void testFindElement() throws Exception {
        assertEquals(firstChild, JDOMSearcher.findElement(root, "first-child"));
        assertEquals(subChild, JDOMSearcher.findElement(firstChild, "sub-child"));
        assertEquals(subChild, JDOMSearcher.findElement(root, "sub-child"));
    }

    public void testGetElement() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {
        assertEquals(root, JDOMSearcher.getElement(doc, "root"));
        assertEquals(firstChild, JDOMSearcher.getElement(doc, "first-child"));
        assertEquals(subChild, JDOMSearcher.getElement(doc, "sub-child"));
    }
}
