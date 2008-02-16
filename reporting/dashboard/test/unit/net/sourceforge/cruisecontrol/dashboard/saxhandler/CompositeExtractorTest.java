/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.cruisecontrol.dashboard.exception.ShouldStopParsingException;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CompositeExtractorTest extends MockObjectTestCase {

    public void testShouldCallAllHandlers() throws Exception {
        Mock handler1 = mock(SAXBasedExtractor.class);
        Mock handler2 = mock(SAXBasedExtractor.class);

        handler1.expects(once()).method("characters").with(eq(null), eq(0), eq(2));
        handler2.expects(once()).method("characters").with(eq(null), eq(0), eq(2));

        handler1.expects(once()).method("canStop").will(returnValue(false));

        CompositeExtractor handler =
                new CompositeExtractor(extractors(handler1, handler2));

        handler.characters(null, 0, 2);
    }

    public void testShouldCallAllExtractors() throws Exception {
        Mock extractor1 = mock(SAXBasedExtractor.class);
        Mock extractor2 = mock(SAXBasedExtractor.class);

        extractor1.expects(once()).method("report").with(ANYTHING);
        extractor2.expects(once()).method("report").with(ANYTHING);

        CompositeExtractor handler =
                new CompositeExtractor(extractors(extractor1, extractor2));

        handler.report(null);
    }

    public void testShouldThrowExceptionToStopParsingWhenAllHandlersCanStop() throws Exception {
        Mock handler1 = mock(SAXBasedExtractor.class);
        Mock handler2 = mock(SAXBasedExtractor.class);

        handler1.expects(once()).method("characters").with(eq(null), eq(0), eq(2));
        handler2.expects(once()).method("characters").with(eq(null), eq(0), eq(2));

        handler1.expects(once()).method("canStop").will(returnValue(true));
        handler2.expects(once()).method("canStop").will(returnValue(true));

        CompositeExtractor handler =
                new CompositeExtractor(extractors(handler1, handler2));

        try {
            handler.characters(null, 0, 2);
            fail();
        } catch (ShouldStopParsingException e) {
            // ok.
        }
    }

    public void testShouldCallRespondingCallbacksWhenParseRealXml() throws Exception {
        File logXml = DataUtils.getConfigXmlAsFile();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        CompositeExtractor extractor =
                new CompositeExtractor(Arrays.asList(new SAXBasedExtractor[] {new ExtractorStub()}));

        parser.parse(logXml, extractor);
        Map result = new HashMap();
        extractor.report(result);

        assertTrue(((Boolean) result.get("allCallbackWereCalled")).booleanValue());
    }

    private List extractors(final Mock handler1, final Mock handler2) {
        SAXBasedExtractor[] extractors = new SAXBasedExtractor[]{
                (SAXBasedExtractor) handler1.proxy(), (SAXBasedExtractor) handler2.proxy()};
        return Arrays.asList(extractors);
    }

    private static class ExtractorStub extends SAXBasedExtractor {

        private boolean charCalled;

        private boolean startElementCalled;

        private boolean endElementCalled;

        public void report(Map resultSet) {
            resultSet.put("allCallbackWereCalled", Boolean.valueOf(charCalled && startElementCalled
                    && endElementCalled));
        }

        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
            charCalled = true;
        }

        public void endElement(String arg0, String arg1, String arg2) throws SAXException {
            startElementCalled = true;
        }

        public void startElement(String arg0, String arg1, String arg2, Attributes arg3)
                throws SAXException {
            endElementCalled = true;
        }
    }
}
