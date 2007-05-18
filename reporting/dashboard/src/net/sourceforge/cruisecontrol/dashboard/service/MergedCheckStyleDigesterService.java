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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MergedCheckStyleDigesterService implements ContentDigesterService {
    public String getDisplayName() {
        return "Merged Check Style";
    }

    public Object getOutput(Map parameters) {
        File logFile = (File) parameters.get(ContentDigesterService.PARAM_BUILD_LOG_FILE);
        try {
            return parseCheckStyle(logFile);
        } catch (Exception e) {
            return null;
        }
    }

    private String parseCheckStyle(File logFile) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        CheckStyleHandler handler = new CheckStyleHandler();
        try {
            saxParser = factory.newSAXParser();
            saxParser.parse(logFile, handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return handler.getCheckStyleResult();
    }

    class CheckStyleHandler extends DefaultHandler {
        private StringBuffer errors = new StringBuffer();

        private String currentFileName = "";

        private boolean isCheckStyleStart;

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("checkstyle".equals(qName)) {
                this.isCheckStyleStart = false;
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if ("checkstyle".equals(qName)) {
                this.isCheckStyleStart = true;
            }
            if (this.isCheckStyleStart) {
                if ("file".equals(qName)) {
                    currentFileName = attributes.getValue("name");
                }
                if ("error".equals(qName)) {
                    errors.append("source = ").append('"').append(currentFileName).append('"')
                            .append(" ").append("line = ").append('"').append(attributes.getValue("line")).append('"')
                            .append(" ").append("severity = ").append('"').append(attributes.getValue("severity"))
                            .append('"').append(" ")
                            .append("message = ").append('"')
                            .append(attributes.getValue("message")).append('"').append("\n");
                }
            }
        }

        public String getCheckStyleResult() {
            return errors.toString().trim();
        }
    }
}
