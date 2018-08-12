/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.publishers.origo.OrigoApiClient;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.apache.xmlrpc.XmlRpcException;
import org.jdom2.Element;

public class OrigoPublisherTest extends TestCase {

    private Element successLogHelper;
    private Element fixedLogHelper;
    private Element failureLogHelper;
    private Element firstFailureLogHelper;

    protected Element createLogHelper(boolean success, boolean lastBuildSuccess) {
        return TestUtil.createElement(success, lastBuildSuccess,
             "2 minutes 20 seconds", 5, null);
    }
    
    /**
     * A mock api client that tests, that it is not called at all.
     */
    protected static class MockOrigoApiClientNoCall extends OrigoApiClient {
        public MockOrigoApiClientNoCall() throws MalformedURLException {
            super(new URL("http://api.dummy.tld"));
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public String login(String userKey, String applicationKey)
                throws XmlRpcException, IOException {
            assertFalse(true);
            return "";
        }

        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public Integer retrieveProjectId(String session, String projectName)
                throws XmlRpcException, IOException {
            assertFalse(true);
            return 0;
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public void extendedCommentIssue(String session, Integer projectId,
                Integer bugId, String description, String tags)
                throws XmlRpcException, IOException {
            assertFalse(true);
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public Vector searchIssue(String session, Integer projectId,
                Hashtable<String, String> searchArgs) throws XmlRpcException, IOException {
            assertFalse(true);
            return new Vector();
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public void addIssue(String session, Integer projectId,
                String issueSubject, String issueDescription, String issueTag,
                Boolean issuePrivate) throws XmlRpcException, IOException {
            assertFalse(true);
        }
    }

    /**
     * A mock api client that tests, that it is called to create a new issue.
     */
    protected static class MockOrigoApiClientAddIssue extends OrigoApiClient {
        private int loginCalled;
        private int retrieveProjectIdCalled;
        private int addIssueCalled;
        
        public MockOrigoApiClientAddIssue() throws MalformedURLException {
            super(new URL("http://api.dummy.tld"));
        }
        
        public void checkCalls() {
            assertEquals(1, loginCalled);
            assertEquals(1, retrieveProjectIdCalled);
            assertEquals(1, addIssueCalled);
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public String login(String userKey, String applicationKey)
                throws XmlRpcException, IOException {
            loginCalled++;
            assertEquals("SOMEKEY", userKey);
            assertEquals("KEYFORTHEORIGOCRUISECONTROLPLUGI", applicationKey);
            return "SESSIONID";
        }

        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public Integer retrieveProjectId(String session, String projectName)
                throws XmlRpcException, IOException {
            retrieveProjectIdCalled++;
            assertEquals("SESSIONID", session);
            assertEquals("testproject", projectName);
            return 21;
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public void extendedCommentIssue(String session, Integer projectId,
                Integer bugId, String description, String tags)
                throws XmlRpcException, IOException {
            assertFalse(true);
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public Vector searchIssue(String session, Integer projectId,
                Hashtable<String, String> searchArgs) throws XmlRpcException, IOException {
            assertFalse(true);
            return new Vector();
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public void addIssue(String session, Integer projectId,
                String issueSubject, String issueDescription, String issueTag,
                Boolean issuePrivate) throws XmlRpcException, IOException {
            addIssueCalled++;
            assertEquals("SESSIONID", session);
            assertEquals(new Integer(21), projectId);
            assertEquals("Cruisecontrol failed", issueSubject);
            assertEquals("Build failed see: http://localhost:8180/?log=log20020313120000", issueDescription);
            assertEquals("status::open,cruisecontrol::failed", issueTag);
            assertEquals(Boolean.TRUE, issuePrivate);
        }
    }
    
    /**
     * A mock api client that tests, that the issue is closed.
     */
    protected static class MockOrigoApiClientCloseIssue extends OrigoApiClient {
        private int loginCalled;
        private int retrieveProjectIdCalled;
        private int searchIssueCalled;
        private int extendedCommentIssueCalled;
        
        public MockOrigoApiClientCloseIssue() throws MalformedURLException {
            super(new URL("http://api.dummy.tld"));
        }

        public void checkCalls() {
            assertEquals(1, loginCalled);
            assertEquals(1, retrieveProjectIdCalled);
            assertEquals(1, searchIssueCalled);
            assertEquals(1, extendedCommentIssueCalled);
        }
        
        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public String login(String userKey, String applicationKey)
                throws XmlRpcException, IOException {
            loginCalled++;
            assertEquals("SOMEKEY", userKey);
            assertEquals("KEYFORTHEORIGOCRUISECONTROLPLUGI", applicationKey);
            return "SESSIONID";
        }

        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public Integer retrieveProjectId(String session, String projectName)
                throws XmlRpcException, IOException {
            retrieveProjectIdCalled++;
            assertEquals("SESSIONID", session);
            assertEquals("testproject", projectName);
            return 21;
        }

        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public void extendedCommentIssue(String session, Integer projectId,
                Integer bugId, String description, String tags)
                throws XmlRpcException, IOException {
            extendedCommentIssueCalled++;
            assertEquals("SESSIONID", session);
            assertEquals(new Integer(21), projectId);
            assertEquals(new Integer(33), bugId);
            assertEquals("Build fixed see: http://localhost:8180/?log=log20020313120000", description);
            assertEquals("status::closed,cruisecontrol::failed", tags);
        }

        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public Vector searchIssue(String session, Integer projectId,
                Hashtable<String, String> searchArgs) throws XmlRpcException, IOException {
            searchIssueCalled++;
            assertEquals("SESSIONID", session);
            assertEquals(new Integer(21), projectId);
            final Hashtable<String, String> actualSearchArgs = new Hashtable<String, String>();
            actualSearchArgs.put("status", "open");
            actualSearchArgs.put("tags", "cruisecontrol::failed");
            assertEquals(actualSearchArgs, searchArgs);
            Vector<Object> bugs = new Vector<Object>();
            Hashtable<String, Object> bug = new Hashtable<String, Object>();
            bugs.add(bug);
            bug.put("issue_id", 33);
            return bugs;
        }

        /* Can't use annotations 'til we're at source version 1.5.
        @Override
        */
        public void addIssue(String session, Integer projectId,
                String issueSubject, String issueDescription, String issueTag,
                Boolean issuePrivate) throws XmlRpcException, IOException {
            assertFalse(true);
        }
    }
    
    public void setUp() throws Exception {
        successLogHelper = createLogHelper(true, true);
        failureLogHelper = createLogHelper(false, false);
        fixedLogHelper = createLogHelper(true, false);
        firstFailureLogHelper = createLogHelper(false, true);
    }


    public void testValidate() throws CruiseControlException {
        OrigoPublisher publisher = new OrigoPublisher();
        try {
            publisher.validate();
            fail("OrigoPublisher should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }
        
        publisher.setUserKey("SOMEKEY");
        publisher.setProjectName("testproject");
        publisher.validate();
    }
    
    public void testCreateLinkURL() {
        OrigoPublisher publisher = new OrigoPublisher();
        assertEquals("http://localhost:8180/?log=log20020313120000", publisher.createLinkURL("log20020313120000.xml"));
    }

    public void testPublish() throws Exception {
        OrigoPublisher publisher = new OrigoPublisher();
        publisher.setUserKey("SOMEKEY");
        publisher.setProjectName("testproject");
        
        // success without fixing should not do anything
        publisher.setClient(new MockOrigoApiClientNoCall());
        publisher.publish(successLogHelper);

        // failure from failure should not do anything
        publisher.setClient(new MockOrigoApiClientNoCall());
        publisher.publish(failureLogHelper);
        
        // failure from fixed should add an issue
        MockOrigoApiClientAddIssue mockAdd = new MockOrigoApiClientAddIssue();
        publisher.setClient(mockAdd);
        publisher.publish(firstFailureLogHelper);
        mockAdd.checkCalls();
        
        // fixed from failure should close issue
        MockOrigoApiClientCloseIssue mockClose = new MockOrigoApiClientCloseIssue();
        publisher.setClient(mockClose);
        publisher.publish(fixedLogHelper);
        mockClose.checkCalls();
    }
}
