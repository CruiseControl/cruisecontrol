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
 // CHANGED by RHT 08/05/2008 so that the errors contain newlines and are grouped
 // by the file they appear in.  The filename is truncated using the name of the
 // project to remove the cruise directory path and make the lines shorter.
package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.File;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MergedCheckStyleWidget implements Widget {
    public String getDisplayName() {
        return "Merged Check Style";
    }

    public Object getOutput(Map parameters) {
        final String pjtName = (String) parameters.get(Widget.PARAM_PJT_NAME);
        final File logFile = (File) parameters.get(Widget.PARAM_BUILD_LOG_FILE);
        try {
            return parseCheckStyle(logFile, pjtName);
        } catch (Exception e) {
            return null;
        }
    }

    private String parseCheckStyle(final File logFile, final String pjtName) {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser;
        final CheckStyleHandler handler = new CheckStyleHandler(pjtName);
        try {
            saxParser = factory.newSAXParser();
            saxParser.parse(logFile, handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return handler.getCheckStyleResult();
    }

    class CheckStyleHandler extends DefaultHandler {
        private final StringBuffer errors = new StringBuffer("<h2>Check Style Results:</h2>");

        private String currentFileName = "";
        private String previousFileName;
        private final String currentPjtName;

        private boolean isCheckStyleStart;

        public CheckStyleHandler(final String pjtName) {
            currentPjtName = pjtName;
        }

        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if ("checkstyle".equals(qName)) {
                this.isCheckStyleStart = false;
            }
        }

        public void startElement(final String uri, final String localName, final String qName,
                                 final Attributes attributes)
                throws SAXException {
            if ("checkstyle".equals(qName)) {
                this.isCheckStyleStart = true;
            }
            if (this.isCheckStyleStart) {
                if ("file".equals(qName)) {
                    currentFileName = attributes.getValue("name");
                    final int index = currentFileName.indexOf(currentPjtName);
                    if (index != -1 && (currentFileName.length() > index + currentPjtName.length() + 1)) {
                        currentFileName = currentFileName.substring(index + currentPjtName.length() + 1);
                    }
                }
                if ("error".equals(qName)) {
                    if ((previousFileName == null) || !currentFileName.equals(previousFileName)) {
                        errors.append("<br><b>").append(currentFileName).append(":").append("</b><br>");
                        previousFileName = currentFileName;
                    }
                    errors.append(attributes.getValue("severity")).append(": ")
                          .append(attributes.getValue("message")).append(" at line ")
                          .append(attributes.getValue("line")).append("<br>");
                }
            }
        }

        public String getCheckStyleResult() {
            return errors.toString().trim();
        }
    }
}
