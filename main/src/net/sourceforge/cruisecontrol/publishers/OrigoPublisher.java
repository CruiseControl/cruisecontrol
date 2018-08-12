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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.publishers.origo.OrigoApiClient;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * Implementation of the <code>Publisher</code> interface, add/update an issue
 * for an Origo project.
 * 
 * @author Patrick Ruckstuhl 2008
 */
public class OrigoPublisher implements Publisher {
    private static final long serialVersionUID = 4476791333216452959L;

    private static final Logger LOG = Logger.getLogger(OrigoPublisher.class);

    private OrigoApiClient client;
    
    private static final String APPLICATION_KEY = "KEYFORTHEORIGOCRUISECONTROLPLUGI";

    private String apiURL = "http://api.origo.ethz.ch/api/xmlrpc";
    private String projectName;
    private String userKey;
    private String issueSubject = "Cruisecontrol failed";
    private String issueTag = "cruisecontrol::failed";
    private Boolean issuePrivate = Boolean.TRUE;
    private String buildResultsURL = "http://localhost:8180/";

    public OrigoPublisher() {
    }
    
    /**
     * Called after the configuration is read to make sure that all the
     * mandatory parameters were specified..
     * 
     * @throws CruiseControlException
     *             if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getApiURL(), "apiurl", this.getClass());
        ValidationHelper.assertIsSet(getUserKey(), "userkey", this.getClass());
        ValidationHelper.assertIsSet(getProjectName(), "projectname", this.getClass());
        try {
            URL url = new URL(getApiURL());
            client = new OrigoApiClient(url);
        } catch (MalformedURLException e) {
            ValidationHelper.fail(getApiURL() + " is not a valid URL", e);
        }
    }

    /**
     * Generate a link to the build results.
     * @param logFileName file name to link to.
     * @return a link to the given file name
     */
    @SkipDoc
    public String createLinkURL(final String logFileName) {
        if (buildResultsURL == null) {
            return "";
        }

        final int startName = logFileName.lastIndexOf(File.separator) + 1;
        final int endName = logFileName.lastIndexOf(".");
        final String baseLogFileName = logFileName.substring(startName, endName);
        final StringBuilder url = new StringBuilder(buildResultsURL);
        if (buildResultsURL.indexOf("?") == -1) {
            url.append("?");
        } else {
            url.append("&");
        }
        url.append("log=");
        url.append(baseLogFileName);

        return url.toString();
    }

    /**
     * Implementing the <code>Publisher</code> interface. If the build newly failed, 
     * create a new issue, if we fixed a broken build close the issue.
     *  
     * @param cruisecontrolLog build log
     * @throws CruiseControlException if something breaks
     */
    public void publish(final Element cruisecontrolLog) throws CruiseControlException {
        final XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);

        try {
            // build fixed
            if (helper.isBuildFix()) {
                // login
                final String session = client.login(userKey, APPLICATION_KEY);
                LOG.debug("Got session " + session);

                // get project id
                final Integer projectId = client.retrieveProjectId(session, projectName);

                // search bug
                final Hashtable<String, String> searchArgs = new Hashtable<String, String>();
                searchArgs.put("status", "open");
                searchArgs.put("tags", issueTag);

                final Vector bugs = client.searchIssue(session, projectId, searchArgs);
                if (bugs.size() == 1) {
                    // close bug
                    final Hashtable bug = (Hashtable) bugs.get(0);
                    final Integer bugId = (Integer) bug.get("issue_id");
                    LOG.info("Found bug with id" + bugId);

                    client.extendedCommentIssue(session, projectId, bugId, "Build fixed see: "
                            + createLinkURL(helper.getLogFileName()), "status::closed," + issueTag);
                } else {
                    LOG.warn("Did not find exactly one match, found "
                            + bugs.size() + " bugs.");
                }
            // build failed
            } else if (helper.wasPreviousBuildSuccessful()
                    && !helper.isBuildSuccessful()) {
                // login
                final String session = client.login(userKey, APPLICATION_KEY);
                LOG.debug("Got session " + session);

                // get project id
                final Integer projectId = client.retrieveProjectId(session, projectName);

                final String issueDescription = "Build failed see: "
                        + createLinkURL(helper.getLogFileName());
                client.addIssue(session, projectId, issueSubject, issueDescription,
                    "status::open," + issueTag, issuePrivate);
            }
        } catch (Exception e) {
            LOG.error("Problem during xmlrpc call", e);
            throw new CruiseControlException(e);
        }
    }

    /**
     * @param url set api url
     */
    public void setApiURL(String url) {
        apiURL = url;
    }

    /**
     * @return get api url
     */
    public String getApiURL() {
        return apiURL;
    }

    /**
     * @param key set user key
     */
    public void setUserKey(String key) {
        userKey = key;
    }

    /**
     * @return get user key
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * @return the buildResultsURL
     */
    public String getBuildResultsURL() {
        return buildResultsURL;
    }

    /**
     * @param buildResultsURL
     *            the buildResultsURL to set
     */
    public void setBuildResultsURL(String buildResultsURL) {
        this.buildResultsURL = buildResultsURL;
    }

    /**
     * @return the issuePrivate
     */
    public Boolean getIssuePrivate() {
        return issuePrivate;
    }

    /**
     * @param issuePrivate
     *            the issuePrivate to set
     */
    public void setIssuePrivate(Boolean issuePrivate) {
        this.issuePrivate = issuePrivate;
    }

    /**
     * @return the issueSubject
     */
    public String getIssueSubject() {
        return issueSubject;
    }

    /**
     * @param issueSubject
     *            the issueSubject to set
     */
    public void setIssueSubject(String issueSubject) {
        this.issueSubject = issueSubject;
    }

    /**
     * @return the issueTag
     */
    public String getIssueTag() {
        return issueTag;
    }

    /**
     * @param issueTag
     *            the issueTag to set
     */
    public void setIssueTag(String issueTag) {
        this.issueTag = issueTag;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @param client the origo api client to set
     */
    @SkipDoc
    public void setClient(OrigoApiClient client) {
        this.client = client;
    }
}
