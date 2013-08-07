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
package net.sourceforge.cruisecontrol;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ServerNameSingleton;
import net.sourceforge.cruisecontrol.util.UniqueBuildloopIdentifier;

import org.apache.tools.ant.util.DateUtils;

public class BuildLoopInformationBuilderTest extends TestCase {

    private String[] oldProperties;

    private String[] properties =
            {BuildLoopInformation.JmxInfo.CRUISECONTROL_JMXPORT,
                    BuildLoopInformation.JmxInfo.CRUISECONTROL_RMIPORT,
                    BuildLoopInformation.JmxInfo.JMX_HTTP_USERNAME,
                    BuildLoopInformation.JmxInfo.JMX_HTTP_PASSWORD};

    private BuildLoopInformationBuilder builder;

    protected void setUp() throws Exception {
        oldProperties = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            oldProperties[i] = System.getProperty(properties[i]);
        }

        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_JMXPORT, "1234");
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_RMIPORT, "5678");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_USERNAME, "Chris");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_PASSWORD, "123asd");

        builder = new BuildLoopInformationBuilder(new CruiseControlController());
        buildLoopInformation = builder.buildBuildLoopInformation();
    }

    protected void tearDown() throws Exception {
        for (int i = 0; i < properties.length; i++) {
            if (oldProperties[i] != null) {
                System.setProperty(properties[i], oldProperties[i]);
            }
        }
    }

    public void testShouldGetServerName() throws Exception {
        String expected = ServerNameSingleton.getServerName();
        assertEquals(expected, buildLoopInformation.getServerName());
    }

    public void testShouldContainBuildloopUUID() throws Exception {
        String xml = buildLoopInformation.toXml();
        String uuid = UniqueBuildloopIdentifier.id().toString();
        assertContains(xml, "<uuid>" + uuid + "</uuid>");
    }

    public void testShouldGetJmxInformation() throws Exception {
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_JMXPORT, "1234");
        System.setProperty(BuildLoopInformation.JmxInfo.CRUISECONTROL_RMIPORT, "5678");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_USERNAME, "Chris");
        System.setProperty(BuildLoopInformation.JmxInfo.JMX_HTTP_PASSWORD, "123asd");
        builder = new BuildLoopInformationBuilder(new CruiseControlController());
        BuildLoopInformation.JmxInfo jmxInfo = builder.buildBuildLoopInformation().getJmxInfo();

        assertEquals(expectedUrl("http", "1234"), jmxInfo.getHttpAdpatorUrl());
        assertEquals(expectedUrl("rmi", "5678"), jmxInfo.getRmiUrl());
        assertEquals("Chris", jmxInfo.getUserName());
        assertEquals("123asd", jmxInfo.getPassword());
    }

    private String expectedUrl(String protocol, String port) {
        return protocol + "://" + ServerNameSingleton.getServerName() + ":" + port;
    }

    public void testShouldReturnTimestamp() throws Exception {
        String ts = buildLoopInformation.getTimestamp();
        long timestamp = DateUtil.parseIso8601(ts).getTime();

        long now = new Date().getTime();

        long testingElapse = now - timestamp;
        assertTrue(0 <= testingElapse && testingElapse < 60 * 1000);
    }

    public void testShouldContainJmxInfoInXml() throws Exception {
        String xml = buildLoopInformation.toXml();

        assertContains(xml, "<jmx>");
        assertContains(xml, "<httpurl>" + expectedUrl("http", "1234") + "</httpurl>");
        assertContains(xml, "<rmiurl>" + expectedUrl("rmi", "5678") + "</rmiurl>");
        assertContains(xml, "<username>Chris</username>");
        assertContains(xml, "<password>123asd</password>");
    }

    public void testShouldContainProjectsInXml() throws Exception {
        ProjectState state = ProjectState.BUILDING;
        BuildLoopInformation buildLoopInformationProvider = createBuildLoopInformationProvider(state);
        String xml = buildLoopInformationProvider.toXml();

        assertEquals(1, buildLoopInformationProvider.getProjects().length);
        assertBasicXml(xml, state);

        assertEquals(1, buildLoopInformationProvider.getProjects()[0].getModifications().size());
        assertContains(xml, "<modifications>");
        assertContains(xml, "<username>bob</username>");
        assertContains(xml, "<comment>support security check</comment>");
        assertContains(xml, "<revision>123</revision>");
    }

    public void testShouldNotContainModificationsWhenNotBuilding() throws Exception {
        ProjectState state = ProjectState.WAITING;
        BuildLoopInformation buildLoopInformationProvider = createBuildLoopInformationProvider(state);
        String xml = buildLoopInformationProvider.toXml();

        assertEquals(1, buildLoopInformationProvider.getProjects().length);
        assertBasicXml(xml, state);

        assertEquals(0, buildLoopInformationProvider.getProjects()[0].getModifications().size());
        assertContains(xml, "<modifications/>");
        assertNotContains(xml, "<username>bob</username>");
        assertNotContains(xml, "<comment>support security check</comment>");
        assertNotContains(xml, "<revision>123</revision>");
    }

    private void assertBasicXml(String xml, ProjectState state) {
        assertContains(xml, "<projects>");
        assertContains(xml, "<project>");
        assertContains(xml, "<name>project1</name>");
        assertContains(xml, "<status>" + state.getDescription() + "</status>");
        assertContains(xml, "<buildstarttime>" + dateStringIso8601 + "</buildstarttime>");
    }

    private BuildLoopInformation createBuildLoopInformationProvider(ProjectState state) {
        CruiseControlControllerStub controllerStub = new CruiseControlControllerStub(state);
        BuildLoopInformationBuilder buildInfoBuilder = new BuildLoopInformationBuilder(controllerStub);
        return buildInfoBuilder.buildBuildLoopInformation();
    }

    private void assertNotContains(String xml, String s) {
        assertTrue("Should not contain " + s + " in " + xml, xml.indexOf(s) == -1);
    }

    private void assertContains(String xml, String s) {
        assertTrue("Should contain " + s + " in " + xml, xml.indexOf(s) > -1);
    }

    private static String dateString;

    private static String dateStringIso8601;

    private BuildLoopInformation buildLoopInformation;

    static {
        dateString = "20031212152235";
        Date date;
        try {
            date = DateUtil.parseFormattedTime(dateString, "");
        } catch (CruiseControlException e) {
            throw new RuntimeException(e);
        }
        dateStringIso8601 = DateUtils.format(date, DateUtils.ISO8601_DATETIME_PATTERN);
    }

    static class CruiseControlControllerStub extends CruiseControlController {
        private final ProjectState state;

        CruiseControlControllerStub(ProjectState state) {
            this.state = state;
        }

        public List<ProjectInterface> getProjects() {
            final ProjectConfig p1 = new ProjectConfigStub();
            final List<ProjectInterface> lstProjs = new ArrayList<ProjectInterface>();
            lstProjs.add(p1);
            return lstProjs;
        }
        
        private class ProjectConfigStub extends ProjectConfig {

            public String getBuildStartTime() {
                return dateString;
            }

            public List<Modification> getModifications() {
                if (!isInState(ProjectState.BUILDING)) {
                    throw new RuntimeException("Should not call this method when not building");
                }
                final ArrayList<Modification> list = new ArrayList<Modification>();
                final Modification m1 = new Modification();
                m1.comment = "support security check";
                m1.userName = "bob";
                m1.revision = "123";
                list.add(m1);
                return list;
            }

            public String getName() {
                return "project1";
            }

            public String getStatus() {
                return state.getDescription();
            }

            public boolean isInState(ProjectState other) {
                return state.equals(other);
            }
        }
    }
}
