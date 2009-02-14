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
package net.sourceforge.cruisecontrol.publishers.origo;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import net.sourceforge.cruisecontrol.publishers.OrigoPublisher;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Client for the origo api, abstracts the xmlrpc interface.
 * 
 * @author Patrick Ruckstuhl 2008
 */
public class OrigoApiClient {

    private static final Logger LOG = Logger.getLogger(OrigoPublisher.class);
    
    private final XmlRpcClient client;
    
    /**
     * Create with an apiUrl
     * @param apiUrl url
     */
    public OrigoApiClient(URL apiUrl) {
        client = new XmlRpcClient(apiUrl);
    }
    
    /**
     * Execute an xmlrpc call.
     * @param method the method to call
     * @param params the parameters for the call
     * @return the result of the call
     * @throws XmlRpcException if error occurs
     * @throws IOException if error occurs
     */
    protected synchronized Object call(String method, Vector params) throws XmlRpcException, IOException {
        LOG.debug("Executing call " + method + " " + params);
        return client.execute(method, params);
    }
    
    /**
     * Login for a user
     * @param userKey the user key
     * @param applicationKey the application key
     * @return login
     * @throws XmlRpcException if error occurs
     * @throws IOException if error occurs
     */
    public String login(String userKey, String applicationKey) throws XmlRpcException, IOException {
        Vector params = new Vector();
        params.addElement(userKey);
        params.addElement(applicationKey);
        return (String) call("user.login_key", params);
    }

    /**
     * Retrieve the project id.
     * @param session a session
     * @param projectName name of the project
     * @return projectID
     * @throws XmlRpcException if error occurs
     * @throws IOException if error occurs
     */
    public Integer retrieveProjectId(String session, String projectName) throws XmlRpcException, IOException {
        Vector params = new Vector();
        params.addElement(session);
        params.addElement(projectName);
        return (Integer) call("project.retrieve_id", params);
    }

    /**
     * Search an issue.
     * @param session a session
     * @param projectId a project id
     * @param searchArgs search arguments
     * @return issues
     * @throws XmlRpcException if error occurs
     * @throws IOException if error occurs
     */
    public Vector searchIssue(String session, Integer projectId, Hashtable searchArgs) 
            throws XmlRpcException, IOException {
        Vector params = new Vector();
        params.addElement(session);
        params.addElement(projectId);
        params.addElement(searchArgs);
        return (Vector) call("issue.search", params);
    }

    /**
     * Extended comment for an issue.
     * @param session a session
     * @param projectId a project id
     * @param bugId a bug id
     * @param description description
     * @param tags tags to add/set
     * @throws XmlRpcException if error occurs
     * @throws IOException if error occurs
     */
    public void extendedCommentIssue(String session, Integer projectId, Integer bugId, String description, String tags)
            throws XmlRpcException, IOException {
        Vector params = new Vector();
        params.addElement(session);
        params.addElement(projectId);
        params.addElement(bugId);
        params.addElement(description);
        params.addElement(tags);
        params.addElement(0);
        params.addElement(0);
        call("issue.comment_extended_2", params);
    }

    /**
     * Add a new issue.
     * @param session a session
     * @param projectId a project id
     * @param issueSubject subject of the issue
     * @param issueDescription description of the issue
     * @param issueTag tag of the issue
     * @param issuePrivate is the issue private?
     * @throws XmlRpcException if error occurs
     * @throws IOException if error occurs
     */
    public void addIssue(String session, Integer projectId, String issueSubject, String issueDescription,
            String issueTag, Boolean issuePrivate) throws XmlRpcException, IOException {
        Vector params = new Vector();
        params.addElement(session);
        params.addElement(projectId);
        params.addElement(issueSubject);
        params.addElement(issueDescription);
        params.addElement(issueTag);
        params.addElement(issuePrivate);
        params.addElement(0);
        params.addElement(0);
        call("issue.add_2", params);
    }
}
