/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import org.jdom.Element;

import java.io.ByteArrayInputStream;

public class XPathAwareChildTest extends TestCase {


    public void testFailsIfCurrentLogNotSet() {
        XPathAwareChild xpathField = new XPathAwareChild();
        xpathField.setXPathExpression("foo");

        try {
            xpathField.validate();
            xpathField.lookupValue(null);
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
            assertTrue(expected.getMessage().indexOf("current cruisecontrol log not set") >= 0);
        }
    }


    public void testXPathExpression() throws CruiseControlException {
        String xmlDocument = "<foo><bar>baz</bar></foo>";
        String bazXPath = "/foo/bar/text()";

        XPathAwareChild xpathField = new XPathAwareChild();
        xpathField.setXPathExpression(bazXPath);
        xpathField.setInputStream(new ByteArrayInputStream(xmlDocument.getBytes()));

        xpathField.validate();
        assertEquals("baz", xpathField.lookupValue(null));
    }

    public void testXPathExpressionAndValueSetOnField() {
        XPathAwareChild xpathField = new XPathAwareChild();
        xpathField.setValue("foo");
        xpathField.setXPathExpression("bar");

        try {
            xpathField.validate();
            fail("Expected a validation exception");
        } catch (CruiseControlException expected) {
        }
    }

    public void testXPathExpressionAndValueSetOnDescription() {
        XPathAwareChild description = new XPathAwareChild();
        description.setValue("foo");
        description.setXPathExpression("bar");

        try {
            description.validate();
            fail("Expected a validation exception");
        } catch (CruiseControlException expected) {
        }
    }

    public void testXMLFileShouldDefaultToLog() throws CruiseControlException {
        String bazXPath = "/cruisecontrol/info/property[@name='builddate']/@value";

        XPathAwareChild xpathField = new XPathAwareChild();
        xpathField.setXPathExpression(bazXPath);
        Element log = TestUtil.createElement(true, true);

        assertNotNull(log.getDocument());

        xpathField.validate();
        assertEquals("11/30/2005 12:07:27", xpathField.lookupValue(log));
    }

    public void testMustValidateFirst() throws CruiseControlException {
        XPathAwareChild child = new XPathAwareChild();
        try {
            child.lookupValue(null);
            fail("Expected an exception");
        } catch (IllegalStateException expected) {
        }
    }

    public void testXmlFileShouldOnlyBeSetIfXPathExpression() {
        XPathAwareChild child = new XPathAwareChild();
        child.setXMLFile("foo");
        try {
            child.validate();
            fail("Should not be able to validate.");
        } catch (CruiseControlException expected) {
            assertTrue("wrong exception caught",
                    expected.getMessage().indexOf("xmlFile should only be set if xpathExpression is also set.") >= 0);
        }
    }
}
