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

import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.MBeanServerConnectionBuildOutputStub;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.MBeanServerConnectionCommitMessageStub;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.MBeanServerConnectionProjectsStatusStub;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.MBeanServerConnectionStatusStub;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.MBeanServerErrorConnectionStub;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class CruiseControlJMXServiceTest extends MockObjectTestCase {
    private static final String PROJECT_NAME = "connectfour";

    private CruiseControlJMXService jmxService;

    private Mock mockJMXFactory;

    protected void setUp() throws Exception {
        mockJMXFactory =
                mock(JMXFactory.class, new Class[] {EnvironmentService.class,
                        JMXConnectorFactory.class}, new Object[] {new EnvironmentService(),
                        new JMXConnectorFactory()});
        jmxService =
                new CruiseControlJMXService((JMXFactory) mockJMXFactory.proxy(),
                        new EnvironmentService());

    }

    public void testShouldGetStatusByProjectName() throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerConnectionStatusStub()));
        String status = jmxService.getBuildStatus(PROJECT_NAME);
        assertEquals("waiting for next time to build", status);
    }

    public void testStatusOfProjectShouldBeChangedAfterForceBuildByRMIConnection() throws Exception {
        MBeanServerConnectionStatusStub beanServerConnectionStatusStub =
                new MBeanServerConnectionStatusStub();
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(beanServerConnectionStatusStub));
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(beanServerConnectionStatusStub));
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(beanServerConnectionStatusStub));
        String statusBefore = jmxService.getBuildStatus(PROJECT_NAME);
        jmxService.forceBuild(PROJECT_NAME);
        String statusAfter = jmxService.getBuildStatus(PROJECT_NAME);
        assertFalse(statusBefore.equals(statusAfter));
    }

    public void testStatusUnknownShouldBeReturnedJMXErrorHappens() throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerErrorConnectionStub()));
        mockJMXFactory.expects(once()).method("closeConnector");
        String buildStatus = jmxService.getBuildStatus(PROJECT_NAME);
        assertNull(buildStatus);
    }

    public void testShouldReturnArrayContainsCommiterAndCommitMessageWhenInvokeJMXWithAttributeCommitMessage()
            throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerConnectionCommitMessageStub()));
        List commitMessages = jmxService.getCommitMessages(PROJECT_NAME);
        ModificationKey message = (ModificationKey) commitMessages.get(0);
        assertEquals("commiter", message.getUser());
        assertEquals("message 1", message.getComment());
    }

    public void testShouldNOTReturnDuplicateCommitMessage() throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerConnectionCommitMessageStub()));
        List commitMessages = jmxService.getCommitMessages(PROJECT_NAME);

        assertEquals(2, commitMessages.size());
        ModificationKey message = (ModificationKey) commitMessages.get(0);
        assertEquals("commiter", message.getUser());
        assertEquals("message 1", message.getComment());
        message = (ModificationKey) commitMessages.get(1);
        assertEquals("commiter", message.getUser());
        assertEquals("message 2", message.getComment());
    }

    public void testShouldReturnBuildOutput() throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerConnectionBuildOutputStub()));
        String[] output = jmxService.getBuildOutput(PROJECT_NAME, 0);
        assertEquals("Build Failed", output[0]);
        assertEquals("Build Duration: 10s", output[1]);
    }

    public void testShouldReturnEmptyArrayWhenExceptionOccurs() throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerErrorConnectionStub()));
        mockJMXFactory.expects(once()).method("closeConnector");
        List commitMessages = jmxService.getCommitMessages(PROJECT_NAME);
        assertNotNull(commitMessages);
        assertEquals(0, commitMessages.size());
    }

    public void testShouldReturnArrayContainsProjectNameAndStatusWhenInvokeJMXWithAttributeAllProjectsStatus()
            throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerConnectionProjectsStatusStub()));
        Map projectsStatus = jmxService.getAllProjectsStatus();
        assertEquals("now building since 20070420174744", projectsStatus.get("project1"));
    }

    public void testShouldThrowExceptionWhenJMXCallThrowException() throws Exception {
        mockJMXFactory.expects(once()).method("getJMXConnection").will(
                returnValue(new MBeanServerErrorConnectionStub()));
        mockJMXFactory.expects(once()).method("closeConnector");
        assertTrue(jmxService.getAllProjectsStatus().isEmpty());
    }

    public void testShouldThrowExceptionWhenForceBuildIsDisabled() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "false");
        mockJMXFactory.expects(never()).method("getJMXConnection");
        try {
            jmxService.forceBuild(PROJECT_NAME);
            fail();
        } catch (Exception e) {
            // expected exception
        }
    }

}