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
package net.sourceforge.cruisecontrol.functionaltest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.BuildLoopInformationBuilder;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitor;
import net.sourceforge.cruisecontrol.report.BuildLoopStatusReportTask;
import net.sourceforge.cruisecontrol.util.BuildInformationHelper;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.apache.commons.httpclient.HttpClient;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class BuildLoopMonitorTest extends TestCase {
    private String serverName;

    private BuildLoopStatusReportTask task;

    private ServerSocket serverSocket;

    protected void setUp() throws Exception {
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_JMXPORT, "1234");
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_RMIPORT, "5678");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_USERNAME, "Chris");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_PASSWORD, "123asd");
        serverName = InetAddress.getLocalHost().getCanonicalHostName();
        BuildLoopInformationBuilder builder =
                new BuildLoopInformationBuilder(new CruiseControlControllerStub());
        task = new BuildLoopStatusReportTask(builder, "http://localhost:3333/", new HttpClient(), 2);
    }

    protected void tearDown() throws Exception {
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_JMXPORT, "");
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_RMIPORT, "");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_USERNAME, "");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_PASSWORD, "");
        serverSocket.close();
    }

    public void testShouldPublishValiadXml() throws Exception {
        serverSocket = new ServerSocket(3333);
        new BuildLoopMonitor(new Timer(), task, 5000, 0).start();
        DocumentBuilderFactory buildFactory = DocumentBuilderFactory.newInstance();
        buildFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource",
                BuildLoopMonitorTest.class.getResourceAsStream("buildloop.xsd"));
        buildFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                "http://www.w3.org/2001/XMLSchema");
        buildFactory.setValidating(true);
        DocumentBuilder dbuilder = buildFactory.newDocumentBuilder();
        dbuilder.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }

            public void warning(SAXParseException e) throws SAXException {
            }
        });
        dbuilder.parse(new ByteArrayInputStream(getResponse(serverSocket).getBytes()));
    }

    public void testShouldPublishXmlInCertainFormat() throws Exception {
        serverSocket = new ServerSocket(3333);
        new BuildLoopMonitor(new Timer(), task, 5000, 0).start();
        String response = getResponse(serverSocket);

        BuildLoopInformation buildInfo = new BuildInformationHelper().toObject(response);

        assertNotNull(buildInfo);
        assertEquals(serverName, buildInfo.getServerName());
        assertNotNull(DateUtil.parseIso8601(buildInfo.getTimestamp()));

        ProjectInfo[] projects = buildInfo.getProjects();
        assertEquals(1, projects.length);
        assertEquals("project1", projects[0].getName());
        assertEquals("building", projects[0].getStatus());
        assertNotNull(DateUtil.parseIso8601(projects[0].getBuildStartTime()));

        List modifications = projects[0].getModifications();
        assertEquals(1, modifications.size());
        Modification modification = (Modification) modifications.get(0);
        assertEquals("JK", modification.userName);
        assertEquals("support security check", modification.comment);
        assertEquals("2023", modification.revision);

        BuildLoopInformation.JmxInfo jmxInfo = buildInfo.getJmxInfo();

        assertEquals(expectedUrl("http", "1234"), jmxInfo.getHttpAdpatorUrl());
        assertEquals(expectedUrl("rmi", "5678"), jmxInfo.getRmiUrl());
        assertEquals("Chris", jmxInfo.getUserName());
        assertEquals("123asd", jmxInfo.getPassword());
    }

    private String expectedUrl(String protocol, String port) throws UnknownHostException {
        return protocol + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + port;
    }

    private String getResponse(ServerSocket serverSocket) throws IOException {
        Socket accept = serverSocket.accept();
        BufferedReader br = new BufferedReader(new InputStreamReader(accept.getInputStream()));
        String line;
        StringBuffer result = new StringBuffer();
        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        int start = result.indexOf("<buildloop>");
        return result.substring(start);
    }

    static class CruiseControlControllerStub extends CruiseControlController {
        public List getProjects() {
            ProjectConfig p1 = new ProjectConfig() {
                public String getBuildStartTime() {
                    return "20031212152235";
                }

                public List getModifications() {
                    ArrayList list = new ArrayList();
                    Modification m1 = new Modification();
                    m1.comment = "support security check";
                    m1.userName = "JK";
                    m1.revision = "2023";
                    list.add(m1);
                    return list;
                }

                public String getName() {
                    return "project1";
                }

                public String getStatus() {
                    return "building";
                }

                public boolean isInState(ProjectState state) {
                    return ProjectState.BUILDING.equals(state);
                }
            };
            return Arrays.asList(new ProjectConfig[] {p1});
        }
    }

}
