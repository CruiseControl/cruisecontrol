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
package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.File;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EmmaArtifactWidget implements Widget {
    public String getDisplayName() {
        return "Emma Artifact";
    }

    public Object getOutput(Map parameters) {
        File artifactosRoot = (File) parameters.get(Widget.PARAM_BUILD_ARTIFACTS_ROOT);
        try {
            return parseEmma(new File(artifactosRoot, "coverage.xml"));
        } catch (Exception e) {
            return null;
        }
    }

    private String parseEmma(File logFile) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        EmmaHandler handler = new EmmaHandler();
        try {
            saxParser = factory.newSAXParser();
            saxParser.parse(logFile, handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return handler.getEmmaResult();
    }

    private static class EmmaHandler extends DefaultHandler {
        private StringBuffer coverages = new StringBuffer();

        private boolean isLevelAll = true;

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if ("all".equals(qName)) {
                this.isLevelAll = true;
            }
            if ("package".equals(qName)) {
                this.isLevelAll = false;
            }
            if (this.isLevelAll) {
                if ("coverage".equals(qName)) {
                    coverages.append(attributes.getValue("type")).append(" : ").append(
                            attributes.getValue("value")).append("\n");
                }
            }
        }

        public String getEmmaResult() {
            return coverages.toString().trim();
        }
    }
}
