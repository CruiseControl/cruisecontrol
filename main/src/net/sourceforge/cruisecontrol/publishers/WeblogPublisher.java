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
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcClient;
import org.jdom.Element;

/**
 * <p>
 * Used to publish a blog entry based on the build report using the Blogger API,
 * MetaWeblog API or the LiveJournal API.
 * </p>
 * <p>
 * Here's a sample of the publisher element to put into your <tt>config.xml</tt>:
 * </p>
 *
 * <pre>
 * &lt;weblog blogurl=&quot;http://yourblogserver:port/blog/xmlrpc&quot;
 *         api=&quot;metaweblog&quot;
 *         blogid=&quot;yourblog&quot;
 *         username=&quot;user1&quot;
 *         password=&quot;secret&quot;
 *         category=&quot;cruisecontrol&quot;
 *         reportsuccess=&quot;fixes&quot;
 *         subjectprefix=&quot;[CC]&quot;
 *         buildresultsurl=&quot;http://yourbuildserver:port/cc/buildresults&quot;
 *         logdir=&quot;/var/cruisecontrol/logs/YourProject&quot;
 *         xsldir=&quot;/opt/cruisecontrol/reporting/jsp/xsl&quot;
 *         css=&quot;/opt/cruisecontrol/reporting/jsp/css/cruisecontrol.css&quot;
 * /&gt;
 * </pre>
 *
 * <p>
 * And you also need to register the 'weblog' task with the following entry if
 * you're using this task with an older version of CruiseControl which doesn't
 * have the WeblogPublisher registered by default.
 *
 * <pre>
 * &lt;project name=&quot;foo&quot;&gt;
 *      &lt;plugin name=&quot;weblog&quot;
 *          classname=&quot;net.sourceforge.cruisecontrol.publishers.WeblogPublisher&quot;/&gt;
 *      ...
 * &lt;/project&gt;
 * </pre>
 *
 * @author Lasse Koskela
 */
public class WeblogPublisher implements Publisher {

    private static final long serialVersionUID = -34809809594503919L;

    private static final Logger LOG = Logger.getLogger(WeblogPublisher.class);

    private static final String APP_KEY = "CruiseControl Blog Publisher";

    private static final String DEFAULT_API = "metaweblog";

    private static final String DEFAULT_REPORTSUCCESS = "always";

    private static final boolean DEFAULT_SPAMWHILEBROKEN = true;

    // blogging configurations

    private String blogId;

    private String api = DEFAULT_API;

    private String username;

    private String password;

    private String category = "";

    private String blogUrl;

    private String buildResultsURL;

    private String reportSuccess = DEFAULT_REPORTSUCCESS;

    private boolean spamWhileBroken = DEFAULT_SPAMWHILEBROKEN;

    private String subjectPrefix;

    // transformation resources

    private String xslFile;

    private String xslDir;

    private String css;

    private String logDir;

    private String[] xslFileNames = { "header.xsl", "maven.xsl",
            "checkstyle.xsl", "compile.xsl", "javadoc.xsl", "unittests.xsl",
            "modifications.xsl", "distributables.xsl" };

    private static final Map<String, Class> API_CLIENTS = new HashMap<String, Class>();
    static {
        API_CLIENTS.put("metaweblog", MetaWeblogApiClient.class);
        API_CLIENTS.put("blogger", BloggerApiClient.class);
        API_CLIENTS.put("livejournal", LiveJournalApiClient.class);
    }

    // --- ACCESSORS ---

    /**
     * If xslFile is set then both xslDir and css are ignored. Specified xslFile
     * must take care of entire document -- html open/close, body tags, styles,
     * etc.
     */
    public void setXSLFile(final String fullPathToXslFile) {
        xslFile = fullPathToXslFile;
    }

    /**
     * Directory where xsl files are located.
     */
    public void setXSLDir(final String xslDirectory) {
        xslDir = xslDirectory;
    }

    /**
     * Method to override the default list of file names that will be looked for
     * in the directory specified by xslDir. By default these are the standard
     * CruseControl xsl files: <br>
     * <ul>
     * header.xsl maven.xsl etc ...
     * </ul>
     * I expect this to be used by a derived class to allow someone to change
     * the order of xsl files or to add/remove one to/from the list or a
     * combination.
     *
     * @param fileNames list of file names that will be looked for
     * in the directory specified by xslDir
     */
    protected void setXSLFileNames(final String[] fileNames) {
        if (fileNames == null) {
            throw new IllegalArgumentException(
                    "xslFileNames can't be null (but can be empty)");
        }
        xslFileNames = fileNames;
    }

    /**
     * Provided as an alternative to setXSLFileNames for changing the list of
     * files to use.
     *
     * @return xsl files to use in generating the email
     */
    protected String[] getXslFileNames() {
        return xslFileNames;
    }

    /**
     * Path to cruisecontrol.css. Only used with xslDir, not xslFile.
     */
    public void setCSS(final String cssFilename) {
        css = cssFilename;
    }

    /**
     * Path to the log file as set in the log element of the configuration xml
     * file.
     */
    public void setLogDir(final String directory) {
        if (directory == null) {
            throw new IllegalArgumentException("logDir cannot be null!");
        }
        this.logDir = directory;
    }

    /**
     * The API used for posting to your blog. Currently, acceptable values are
     * <tt>blogger</tt>,<tt>metaweblog</tt> and <tt>livejournal</tt>.
     */
    public void setApi(final String api) {
        this.api = api;
    }

    /**
     * The "blog ID" for the blog you're posting to. The value depends on your
     * particular weblog product.
     */
    public void setBlogId(final String blogId) {
        this.blogId = blogId;
    }

    /**
     * The URL where your blog's remote API is running at. For example, the
     * value could look like <tt>http://www.yoursite.com/blog/xmlrpc</tt> or
     * <tt>http://www.livejournal.com/interface/xmlrpc</tt>.
     */
    public void setBlogUrl(final String blogUrl) {
        this.blogUrl = blogUrl;
    }

    /**
     * The username to use for authentication.
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * The password to use for authentication.
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * The category to set for the blog entry. When using the MetaWeblogAPI, you
     * can also use a comma-separated list of several categories.
     */
    public void setCategory(final String category) {
        this.category = category;
    }

    /**
     * The prefix to be used before the title of the blog entry. If
     * <tt>null</tt>, no prefix will be used.
     */
    public void setSubjectPrefix(final String prefix) {
        this.subjectPrefix = prefix;
    }

    /**
     * The base build results URL where your CruiseControl reporting application
     * is running. For example, <tt>http://buildserver:8080/cc/myproject</tt>.
     */
    public void setBuildResultsURL(final String url) {
        this.buildResultsURL = url;
    }

    /**
     * The rule for posting a blog entry for successful builds. Accepted values
     * are <tt>never</tt>,<tt>always</tt> and <tt>fixes</tt>.
     */
    public void setReportSuccess(final String reportSuccess) {
        this.reportSuccess = reportSuccess;
    }

    /**
     * The rule for posting a blog entry for each subsequent failed build.
     * Accepted values are <tt>true</tt> and <tt>false</tt>.
     */
    public void setSpamWhileBroken(final boolean spamWhileBroken) {
        this.spamWhileBroken = spamWhileBroken;
    }

    // --- METHODS ---

    /**
     * Implementing the <code>Publisher</code> interface.
     *
     * @param cruisecontrolLog
     *            The build results XML
     */
    public void publish(Element cruisecontrolLog) {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        try {
            if (shouldSend(helper)) {
                postBlogEntry(createSubject(helper), createMessage(helper
                        .getProjectName(), helper.getLogFileName()));
            } else {
                LOG.debug("shouldSend() indicated we should not"
                        + " post a blog entry at this time");
            }
        } catch (CruiseControlException e) {
            LOG.error("", e);
        }
    }

    /**
     * The interface for abstracting away the specific blogging API being used.
     *
     * @author Lasse Koskela
     */
    interface BloggingApi extends Serializable {
        /**
         * Post a new blog entry.
         *
         * @param subject
         *            The blog entry's subject.
         * @param content
         *            The blog entry's content.
         * @return The newly created blog entry's identifier.
         */
        public Object newPost(String blogUrl, String blogId, String username,
                String password, String category, String subject, String content);
    }

    /**
     * A <tt>BloggingApi</tt> implementation for the Blogger API.
     *
     * @author Lasse Koskela
     */
    public static class BloggerApiClient implements BloggingApi {

        private static final long serialVersionUID = 6614787780439141028L;

        public Object newPost(final String blogUrl, final String blogId, final String username,
                final String password, final String category, final String subject, String content) {
            // the Blogger API doesn't support titles for blog entries so
            // we're using the common (de facto standard) workaround to embed
            // the title into the content and let the weblog software parse
            // the title from there, if supported.
            content = "<title>" + subject + "</title>" + content;
            Object postId = null;
            try {
                final XmlRpcClient xmlrpc = new XmlRpcClient(blogUrl);
                final Vector<Object> params = new Vector<Object>();
                params.add(APP_KEY);
                params.add(blogId);
                params.add(username);
                params.add(password);
                params.add(content);
                params.add(Boolean.TRUE);
                postId = xmlrpc.execute("blogger.newPost", params);
            } catch (Exception e) {
                LOG.error("", e);
            }
            return postId;
        }
    }

    /**
     * A <tt>BloggingApi</tt> implementation for the MetaWeblogAPI.
     *
     * @author Lasse Koskela
     */
    public static class MetaWeblogApiClient implements BloggingApi {

        private static final long serialVersionUID = 5980798548858885672L;

        public Object newPost(final String blogUrl, final String blogId, final String username,
                final String password, final String category, final String subject, final String content) {
            Object postId = null;
            try {
                final XmlRpcClient xmlrpc = new XmlRpcClient(blogUrl);
                final Vector<Object> params = new Vector<Object>();
                params.add(blogId);
                params.add(username);
                params.add(password);

                // MetaWeblogAPI expects the blog entry data elements in an
                // internal map-structure unlike Blogger API does.
                final Hashtable<String, Object> struct = new Hashtable<String, Object>();
                struct.put("title", subject);
                struct.put("description", content);
                final Vector<Object> categories = new Vector<Object>();
                if (category != null) {
                    StringTokenizer tok = new StringTokenizer(category, ",");
                    while (tok.hasMoreTokens()) {
                        categories.add(tok.nextToken().trim());
                    }
                }
                struct.put("categories", categories);

                params.add(struct);
                params.add(Boolean.TRUE);
                postId = xmlrpc.execute("metaWeblog.newPost", params);
            } catch (Exception e) {
                LOG.error("", e);
            }
            return postId;
        }
    }

    /**
     * A <tt>BloggingApi</tt> implementation for the LiveJournal API.
     *
     * @author Lasse Koskela
     */
    public static class LiveJournalApiClient implements BloggingApi {

        private static final long serialVersionUID = -372653261263803994L;

        // TODO: make this smarter so that it won't strip away linefeeds from
        // within &lt;pre&gt;formatted blocks...
        private String stripLineFeeds(final String input) {
            final StringBuilder s = new StringBuilder();
            final char[] chars = input.toCharArray();
            for (char aChar : chars) {
                if (aChar != '\n' && aChar != '\r') {
                    s.append(aChar);
                }
            }
            return s.toString();
        }

        public Object newPost(final String blogUrl, final String blogId, final String username,
                final String password, final String category, final String subject, final String content) {
            Object postId = null;
            try {
                final XmlRpcClient xmlrpc = new XmlRpcClient(blogUrl);
                final Vector<Object> params = new Vector<Object>();
                final Hashtable<String, Object> struct = new Hashtable<String, Object>();
                struct.put("username", username);

                // TODO: use challenge-based security
                struct.put("auth_method", "clear");
                struct.put("password", password);

                struct.put("subject", subject);
                struct.put("event", stripLineFeeds(content));
                struct.put("lineendings", "\n");
                struct.put("security", "public");
                final Calendar now = Calendar.getInstance();
                struct.put("year", "" + now.get(Calendar.YEAR));
                struct.put("mon", "" + (now.get(Calendar.MONTH) + 1));
                struct.put("day", "" + now.get(Calendar.DAY_OF_MONTH));
                struct.put("hour", "" + now.get(Calendar.HOUR_OF_DAY));
                struct.put("min", "" + now.get(Calendar.MINUTE));
                params.add(struct);
                postId = xmlrpc.execute("LJ.XMLRPC.postevent", params);
            } catch (Exception e) {
                LOG.error("", e);
            }
            return postId;
        }
    }

    /**
     * Selects a <tt>BloggingApi</tt> implementation based on a user-friendly
     * name.
     *
     * @param apiName
     *            The name of the blogging API to use. One of <tt>blogger</tt>,
     *            <tt>metaweblog</tt> or <tt>livejournal</tt>.
     * @return The <tt>BloggingApi</tt> implementation or <tt>null</tt> if
     *         no matching implementation was found.
     * @throws CruiseControlException if something breaks
     */
    public BloggingApi getBloggingApiImplementation(final String apiName)
            throws CruiseControlException {
        final Class implClass = API_CLIENTS.get(apiName);
        if (implClass != null) {
            LOG.debug("Mapped " + apiName + " to " + implClass.getName());
            try {
                return (BloggingApi) implClass.newInstance();
            } catch (Exception e) {
                throw new CruiseControlException(
                        "Failed to instantiate Blogging API implementation, "
                                + implClass.getName() + ", due to a "
                                + e.getClass().getName() + ": "
                                + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Posts the build results to the blog.
     *
     * @param subject
     *            The subject for the blog entry.
     * @param content
     *            The content for the blog entry.
     */
    public void postBlogEntry(final String subject, final String content) {
        LOG.debug("Posting a blog entry to " + blogUrl);
        LOG.debug("    blogId=" + blogId);
        LOG.debug("    username=" + username);
        LOG.debug("    subject=" + subject);
        LOG.debug("    content=" + content);
        try {
            final BloggingApi apiClient = getBloggingApiImplementation(api);
            if (apiClient != null) {
                final Object postId = apiClient.newPost(blogUrl, blogId, username,
                        password, category, subject, content);
                if (postId != null) {
                    LOG.info("Blog entry " + postId + " created at " + blogUrl);
                } else {
                    LOG.debug("Blog entry ID not available from " + blogUrl);
                }
            } else {
                LOG.error("No API associated with '" + api + "'");
            }
        } catch (Exception e) {
            LOG.error("", e);
        }
    }

    /**
     * Called after the configuration is read to make sure that all the
     * mandatory parameters were specified..
     *
     * @throws CruiseControlException
     *             if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        validateRequiredField("username", username);
        validateRequiredField("password", password);
        validateRequiredField("blogid", blogId);
        validateRequiredField("blogurl", blogUrl);
        validateURL("blogurl", blogUrl);
        validateOneOf("api", API_CLIENTS.keySet(), api);

        if (buildResultsURL != null) {
            validateURL("buildresultsurl", buildResultsURL);
        }

        if (logDir != null) {
            verifyDirectory("WeblogPublisher.logDir", logDir);
        } else {
            LOG.info("Using default log directory \"logs/<projectname>\"");
        }

        if (xslFile == null) {
            verifyDirectory("WeblogPublisher.xslDir", xslDir);
            verifyFile("WeblogPublisher.css", css);

            final String[] fileNames = getXslFileNames();
            if (fileNames == null) {
                throw new CruiseControlException(
                        "WeblogPublisher.getXslFileNames() can't return null");
            }
            for (final String fileName : fileNames) {
                verifyFile("WeblogPublisher.xslDir/" + fileName, new File(
                        xslDir, fileName));
            }
        } else {
            verifyFile("WeblogPublisher.xslFile", xslFile);
        }
    }

    private void validateOneOf(final String fieldName, final Collection validValues,
            final String value) throws CruiseControlException {
        if (!validValues.contains(value)) {
            throw new CruiseControlException("Value for '" + fieldName
                    + "' must be one of " + commaSeparated(validValues));
        }
    }

    private String commaSeparated(final Collection values) {
        final StringBuilder s = new StringBuilder();
        final Iterator i = values.iterator();
        while (i.hasNext()) {
            s.append("'").append(i.next()).append("'");
            if (i.hasNext()) {
                s.append(", ");
            }
        }
        return s.toString();
    }

    private void validateURL(final String fieldName, final String url)
            throws CruiseControlException {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new CruiseControlException(fieldName
                    + " must be a valid URL: " + url);
        }
    }

    private void validateRequiredField(final String fieldName, final String value)
            throws CruiseControlException {
        if (value == null) {
            throw new CruiseControlException("Attribute " + fieldName
                    + " is required.");
        }
    }

    private void verifyDirectory(final String dirName, final String dir)
            throws CruiseControlException {
        if (dir == null) {
            throw new CruiseControlException(dirName
                    + " not specified in configuration file");
        }
        final File dirFile = new File(dir);
        if (!dirFile.exists()) {
            throw new CruiseControlException(dirName + " does not exist : "
                    + dirFile.getAbsolutePath());
        }
        if (!dirFile.isDirectory()) {
            throw new CruiseControlException(dirName + " is not a directory : "
                    + dirFile.getAbsolutePath());
        }
    }

    private void verifyFile(final String fileName, final String file)
            throws CruiseControlException {
        if (file == null) {
            throw new CruiseControlException(fileName
                    + " not specified in configuration file");
        }
        verifyFile(fileName, new File(file));
    }

    private void verifyFile(final String fileName, final File file)
            throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException(fileName + " does not exist: "
                    + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new CruiseControlException(fileName + " is not a file: "
                    + file.getAbsolutePath());
        }
    }

    /**
     * Determines if the conditions are right for the blog entry to be posted.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return whether or not the mail message should be sent.
     * @throws CruiseControlException if something breaks
     */
    boolean shouldSend(final XMLLogHelper logHelper) throws CruiseControlException {
        if (logHelper.isBuildSuccessful()) {
            return shouldSendForSuccessfulBuild(logHelper);
        } else {
            return shouldSendForFailedBuild(logHelper);
        }
    }

    /**
     * Determines if the conditions are right for the blog entry to be posted.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return whether or not the mail message should be sent.
     * @throws CruiseControlException if something breaks
     */
    boolean shouldSendForFailedBuild(final XMLLogHelper logHelper)
            throws CruiseControlException {
        if (!logHelper.wasPreviousBuildSuccessful()
                && logHelper.isBuildNecessary() && !spamWhileBroken) {
            LOG.debug("spamWhileBroken is false, not sending email");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determines if the conditions are right for the blog entry to be posted.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return whether or not the mail message should be sent.
     * @throws CruiseControlException if something breaks
     */
    boolean shouldSendForSuccessfulBuild(final XMLLogHelper logHelper)
            throws CruiseControlException {
        if (reportSuccess.equalsIgnoreCase(DEFAULT_REPORTSUCCESS)) {
            return true;
        } else if (reportSuccess.equalsIgnoreCase("never")) {
            return false;
        } else if (reportSuccess.equalsIgnoreCase("fixes")) {
            if (logHelper.wasPreviousBuildSuccessful()) {
                LOG.debug("reportSuccess is set to 'fixes', "
                        + "not sending emails for repeated "
                        + "successful builds.");
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the subject for the blog entry.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return <code>String</code> containing the subject line.
     * @throws CruiseControlException if something breaks
     */
    String createSubject(final XMLLogHelper logHelper) throws CruiseControlException {
        final String projectName = logHelper.getProjectName();
        final String label = logHelper.getLabel();
        final boolean buildSuccessful = logHelper.isBuildSuccessful();
        final boolean isFix = logHelper.isBuildFix();
        return createSubject(projectName, label, buildSuccessful, isFix);
    }

    /**
     * Creates the subject for the blog entry.
     *
     * @param projectName project name
     * @param label label
     * @param buildSuccessful true if build successful
     * @param isFix true if build fixed prior failure
     * @return <code>String</code> containing the subject line.
     * @throws CruiseControlException if something breaks
     */
    String createSubject(final String projectName, final String label,
            final boolean buildSuccessful, final boolean isFix)
            throws CruiseControlException {
        final StringBuilder subject = new StringBuilder();
        if (subjectPrefix != null && subjectPrefix.trim().length() > 0) {
            subject.append(subjectPrefix).append(" ");
        }
        subject.append(projectName);
        if (buildSuccessful) {
            if (label.length() > 0) {
                subject.append(" ").append(label);
            }
            subject.append(isFix ? " - Build Fixed" : " - Build Successful");
        } else {
            subject.append(" - Build Failed");
        }
        return subject.toString();
    }

    /**
     * Create the text to be blogged.
     *
     * @param projectName project name
     * @param logFileName log file name
     * @return created message; empty string if logDir not set
     */
    String createMessage(final String projectName, final String logFileName) {
        String message;
        File inFile = null;
        try {
            if (logDir == null) {
                logDir = getDefaultLogDir(projectName);
            }
            inFile = new File(logDir, logFileName);
            message = transform(inFile);
        } catch (Exception ex) {
            LOG.error("error transforming " + (inFile != null ? inFile.getAbsolutePath() : ""), ex);
            message = createLinkLine(logFileName);
        }
        return message;
    }

    String getDefaultLogDir(final String projectName) throws CruiseControlException {
        // TODO: extract this duplication with ProjectXMLHelper.getLog()
        // into a single method somewhere
        return "logs" + File.separator + projectName;
    }

    String transform(final File xml) throws TransformerException, IOException {
        final StringBuilder messageBuffer = new StringBuilder();
        if (xslFile != null) {
            transformWithSingleStylesheet(xml, messageBuffer);
        } else {
            messageBuffer.append(createLinkLine(xml.getName()));
            transformWithMultipleStylesheets(xml, messageBuffer);
        }
        return messageBuffer.toString();
    }

    void transformWithMultipleStylesheets(final File inFile,
            final StringBuilder messageBuffer) throws IOException,
            TransformerException {
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        final File xslDirectory = new File(xslDir);
        final String[] fileNames = getXslFileNames();
        for (final String fileName : fileNames) {
            final File xsl = new File(xslDirectory, fileName);
            messageBuffer.append("<p>\n");
            appendTransform(inFile, xsl, messageBuffer, tFactory);
        }
    }

    void transformWithSingleStylesheet(final File inFile, final StringBuilder messageBuffer)
            throws IOException, TransformerException {
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        appendTransform(inFile, new File(xslFile), messageBuffer, tFactory);
    }

    void appendTransform(final File xml, final File xsl, final StringBuilder messageBuffer,
            TransformerFactory tFactory) throws TransformerException {
        LOG.debug("Transforming file " + xml.getName() + " with "
                + xsl.getName() + " ...");
        final Transformer tformer = tFactory.newTransformer(new StreamSource(xsl));
        final StringWriter sw = new StringWriter();
        try {
            tformer.transform(new StreamSource(xml), new StreamResult(sw));
            LOG.debug("Transformed file " + xml.getName() + " with "
                    + xsl.getName() + " ...");
        } catch (Exception e) {
            LOG.error("error transforming with xslFile " + xsl.getName(), e);
            return;
        }
        messageBuffer.append(sw.toString());
    }

    String createLinkLine(final String logFileName) {
        if (buildResultsURL == null) {
            return "";
        }
        final String url = createBuildResultsUrl(logFileName);
        final StringBuilder linkLine = new StringBuilder();
        linkLine.append("<p>View results here -&gt; <a href=\"");
        linkLine.append(url);
        linkLine.append("\">");
        linkLine.append(url);
        linkLine.append("</a></p>");
        return linkLine.toString();
    }

    String createBuildResultsUrl(final String logFileName) {
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
}
