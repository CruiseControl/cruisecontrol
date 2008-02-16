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

        assertEquals(69, messages.size());
        assertLevelAndMessage(messages, 0, MessageLevel.DEBUG,
                "Adding reference: ant.PropertyHelper");
        assertLevelAndMessage(messages, 1, MessageLevel.INFO,
                "Detected Java version: 1.4 in: C:\\pdj\\java\\j2sdk1.4.2_09\\jre");
        assertLevelAndMessage(messages, 2, MessageLevel.WARN,
                "Detected OS: Windows XP");
        assertLevelAndMessage(messages, 3, MessageLevel.ERROR,
                "Cannot find something");
        assertLevelAndMessage(messages, 17, MessageLevel.WARN,
                "Compilation arguments:\n"
                + "'-d'\n"
                + "'C:\\pdj\\src\\cruisecontrol\\target\\webtest\\cruisecontrol-bin-2.4.0-dev\\projects\\"
                        + "connectfour\\target\\classes'\n"
                + "'-classpath'\n"
                + "'C:\\pdj\\src\\cruisecontrol\\target\\webtest\\cruisecontrol-bin-2.4.0-dev\\projects\\"
                        + "connectfour\\target\\classes;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-launcher.jar;"
                        + "C:\\Program Files\\Java\\jre1.5.0_05\\lib\\ext\\QTJava.zip;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-antlr.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-bcel.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-bsf.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-log4j.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-oro.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-regexp.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-resolver.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-commons-logging.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-commons-net.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-icontract.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jai.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-javamail.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jdepend.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jmf.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jsch.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-junit.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-netrexx.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-nodeps.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-starteam.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-stylebook.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-swing.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-trax.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-vaj.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-weblogic.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-xalan1.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-xslp.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\junit-3.8.1.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\xercesImpl.jar;"
                        + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\xml-apis.jar;"
                        + "C:\\pdj\\java\\j2sdk1.4.2_09\\lib\\tools.jar'\n"
                + "'-sourcepath'\n"
                + "'C:\\pdj\\src\\cruisecontrol\\target\\webtest\\cruisecontrol-bin-2.4.0-dev\\projects\\"
                        + "connectfour\\src'\n"
                + "'-g:none'\n"
                + "\n"
                + "The ' characters around the executable and arguments are\n"
                + "not part of the command.");
    }

    private void assertLevelAndMessage(List messages, int index, MessageLevel level, String message) {
        assertEquals(message, ((BuildMessage) messages.get(index)).getMessage());
        assertEquals(level, ((BuildMessage) messages.get(index)).getLevel());
    }

    public void testShouldReturnContentOfErrorAttributeOfBuildElement() throws Exception {
        Map props = new HashMap();
        parseLogFile(DataUtils.getFailedBuildLbuildAsFile(), props);
        assertEquals("This is my error message", props.get(BuildMessageExtractor.KEY_BUILD));
    }
}
