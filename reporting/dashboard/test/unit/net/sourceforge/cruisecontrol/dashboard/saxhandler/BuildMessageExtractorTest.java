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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildMessage;
import net.sourceforge.cruisecontrol.dashboard.MessageLevel;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

public class BuildMessageExtractorTest extends TestCase {

    private void parseLogFile(File buildLogFile, Map props) throws Exception {
        SAXBasedExtractor extractor = new BuildMessageExtractor();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(buildLogFile, extractor);
        extractor.report(props);
    }

    public void testCanReadBuildLevelMessages() throws Exception {
        Map props = new HashMap();
        parseLogFile(DataUtils.getFailedBuildLbuildAsFile(), props);
        List messages = (List) props.get(BuildMessageExtractor.KEY_MESSAGES);

        assertEquals(4, messages.size());
        assertEquals("Adding reference: ant.PropertyHelper", ((BuildMessage) messages.get(0)).getMessage());
        assertEquals(MessageLevel.DEBUG, ((BuildMessage) messages.get(0)).getLevel());
        assertEquals("Detected Java version: 1.4 in: C:\\pdj\\java\\j2sdk1.4.2_09\\jre",
                ((BuildMessage) messages.get(1)).getMessage());
        assertEquals(MessageLevel.INFO, ((BuildMessage) messages.get(1)).getLevel());
        assertEquals("Detected OS: Windows XP", ((BuildMessage) messages.get(2)).getMessage());
        assertEquals(MessageLevel.WARN, ((BuildMessage) messages.get(2)).getLevel());
        assertEquals("Cannot find something", ((BuildMessage) messages.get(3)).getMessage());
        assertEquals(MessageLevel.ERROR, ((BuildMessage) messages.get(3)).getLevel());
    }

    public void testShouldReturnContentOfErrorAttributeOfBuildElement() throws Exception {
        Map props = new HashMap();
        parseLogFile(DataUtils.getFailedBuildLbuildAsFile(), props);
        assertEquals("This is my error message", props.get(BuildMessageExtractor.KEY_BUILD));
    }
}
